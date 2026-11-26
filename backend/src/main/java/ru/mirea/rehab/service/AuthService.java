package ru.mirea.rehab.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mirea.rehab.config.AppProperties;
import ru.mirea.rehab.domain.RefreshSession;
import ru.mirea.rehab.domain.UserAccount;
import ru.mirea.rehab.dto.AuthDtos.AuthResponse;
import ru.mirea.rehab.dto.AuthDtos.ChangePasswordRequest;
import ru.mirea.rehab.dto.AuthDtos.CurrentUser;
import ru.mirea.rehab.dto.AuthDtos.InitialPasswordChangeRequest;
import ru.mirea.rehab.dto.AuthDtos.LoginRequest;
import ru.mirea.rehab.error.AuthenticationFailedException;
import ru.mirea.rehab.error.BadRequestException;
import ru.mirea.rehab.repository.RefreshSessionRepository;
import ru.mirea.rehab.repository.UserAccountRepository;
import ru.mirea.rehab.security.CurrentUserService;
import ru.mirea.rehab.security.JwtService;
import ru.mirea.rehab.security.PasswordHashingService;
import ru.mirea.rehab.security.PasswordPolicy;
import ru.mirea.rehab.service.AuditService.AuditContext;
import ru.mirea.rehab.web.RequestMetadata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Выполняет вход, ротацию refresh-токена, выход и смену пароля. */
@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordHashingService passwordHashing;
    private final PasswordPolicy passwordPolicy;
    private final LoginAttemptService attempts;
    private final JwtService jwtService;
    private final CurrentUserService currentUser;
    private final AppProperties properties;
    private final RequestMetadata metadata;
    private final AuditService audit;
    private final TransactionTemplate transactionTemplate;
    private final CompletableFuture<String> dummyHashFuture;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository users, RefreshSessionRepository sessions,
                       PasswordHashingService passwordHashing, PasswordPolicy passwordPolicy,
                       LoginAttemptService attempts, JwtService jwtService,
                       CurrentUserService currentUser, AppProperties properties,
                       RequestMetadata metadata, AuditService audit,
                       TransactionTemplate transactionTemplate) {
        this.users = users;
        this.sessions = sessions;
        this.passwordHashing = passwordHashing;
        this.passwordPolicy = passwordPolicy;
        this.attempts = attempts;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.properties = properties;
        this.metadata = metadata;
        this.audit = audit;
        this.transactionTemplate = transactionTemplate;
        this.dummyHashFuture = passwordHashing.encodeAsync("constant-dummy-password-never-used");
    }

    public CompletableFuture<AuthResponse> login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AuditContext auditCtx = audit.captureContext();
        String userAgent = metadata.userAgent();
        String ipAddress = metadata.ipAddress();
        UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
        String hash = user == null ? dummyHashFuture.join() : user.getPasswordHash();

        return passwordHashing.matchesAsync(request.password(), hash).thenApply(passwordMatches ->
                transactionTemplate.execute(status -> {
                    if (user == null || !user.isEnabled() || attempts.isLocked(user) || !passwordMatches) {
                        if (user != null && user.isEnabled()) attempts.recordFailure(user.getId());
                        throw new AuthenticationFailedException();
                    }
                    attempts.recordSuccess(user.getId());
                    UserAccount managed = users.findById(user.getId())
                            .orElseThrow(AuthenticationFailedException::new);
                    AuthResponse response = issueSession(managed, userAgent, ipAddress);
                    audit.record("LOGIN_SUCCESS", "USER", managed.getId(), "Успешный вход " + email, Map.of(), auditCtx);
                    return response;
                }));
    }

    @Transactional
    public AuthResponse refresh(String rawRefresh) {
        if (rawRefresh == null || rawRefresh.isBlank()) throw new AuthenticationFailedException();
        RefreshSession session = sessions.findByTokenHash(hash(rawRefresh.trim()))
                .filter(RefreshSession::isActive).orElseThrow(AuthenticationFailedException::new);
        UserAccount user = session.getUser();
        if (!user.isEnabled()) {
            sessions.revokeAllForUser(user.getId(), Instant.now());
            throw new AuthenticationFailedException();
        }
        session.setRevokedAt(Instant.now());
        session.setLastUsedAt(Instant.now());
        AuthResponse response = issueSession(user);
        session.setReplacedBy(response.user().id());
        return response;
    }

    @Transactional
    public void logout(String rawRefresh) {
        if (rawRefresh != null && !rawRefresh.isBlank()) {
            sessions.findByTokenHash(hash(rawRefresh.trim())).ifPresent(s -> {
                s.setRevokedAt(Instant.now());
                s.setLastUsedAt(Instant.now());
            });
        }
    }

    @Transactional(readOnly = true)
    public CurrentUser me() {
        return toCurrentUser(currentUser.requireEntity());
    }

    public CompletableFuture<Void> changePassword(ChangePasswordRequest request) {
        UserAccount user = currentUser.requireEntity();
        UUID userId = user.getId();
        String currentHash = user.getPasswordHash();
        AuditContext auditCtx = audit.captureContext();
        passwordPolicy.validate(request.newPassword(), user.getEmail());

        CompletableFuture<Boolean> currentMatches = passwordHashing.matchesAsync(request.currentPassword(), currentHash);
        CompletableFuture<Boolean> sameAsCurrent = passwordHashing.matchesAsync(request.newPassword(), currentHash);

        return CompletableFuture.allOf(currentMatches, sameAsCurrent)
                .thenCompose(ignored -> {
                    if (!currentMatches.join()) {
                        throw new BadRequestException("Текущий пароль указан неверно");
                    }
                    if (sameAsCurrent.join()) {
                        throw new BadRequestException("Новый пароль должен отличаться от текущего");
                    }
                    return passwordHashing.encodeAsync(request.newPassword());
                })
                .thenAccept(encodedPassword -> transactionTemplate.executeWithoutResult(status -> {
                    UserAccount managed = users.findById(userId).filter(UserAccount::isEnabled)
                            .orElseThrow(AuthenticationFailedException::new);
                    managed.setPasswordHash(encodedPassword);
                    managed.setPasswordChangeRequired(false);
                    sessions.revokeAllForUser(managed.getId(), Instant.now());
                    audit.record("PASSWORD_CHANGED", "USER", managed.getId(), "Пользователь сменил рабочий пароль", Map.of(), auditCtx);
                }));
    }

    public CompletableFuture<Void> changeInitialPassword(InitialPasswordChangeRequest request) {
        UserAccount user = currentUser.requireEntity();
        if (!user.isPasswordChangeRequired()) {
            throw new BadRequestException("Первичная смена пароля для этой учётной записи уже выполнена");
        }
        UUID userId = user.getId();
        String currentHash = user.getPasswordHash();
        AuditContext auditCtx = audit.captureContext();
        passwordPolicy.validate(request.newPassword(), user.getEmail());

        return passwordHashing.matchesAsync(request.newPassword(), currentHash)
                .thenCompose(sameAsCurrent -> {
                    if (sameAsCurrent) {
                        throw new BadRequestException("Новый пароль должен отличаться от текущего");
                    }
                    return passwordHashing.encodeAsync(request.newPassword());
                })
                .thenAccept(encodedPassword -> transactionTemplate.executeWithoutResult(status -> {
                    UserAccount managed = users.findById(userId).filter(UserAccount::isEnabled)
                            .orElseThrow(AuthenticationFailedException::new);
                    if (!managed.isPasswordChangeRequired()) {
                        throw new BadRequestException("Первичная смена пароля для этой учётной записи уже выполнена");
                    }
                    managed.setPasswordHash(encodedPassword);
                    managed.setPasswordChangeRequired(false);
                    managed.setFailedLoginAttempts(0);
                    managed.setLockedUntil(null);
                    users.saveAndFlush(managed);
                    audit.record("INITIAL_PASSWORD_CHANGED", "USER", managed.getId(), "Пользователь заменил временный пароль", Map.of(), auditCtx);
                }));
    }

    private AuthResponse issueSession(UserAccount user) {
        return issueSession(user, metadata.userAgent(), metadata.ipAddress());
    }

    private AuthResponse issueSession(UserAccount user, String userAgent, String ipAddress) {
        JwtService.IssuedAccessToken access = jwtService.issue(user);
        String rawRefresh = randomToken();
        Instant refreshExpiry = Instant.now().plus(properties.jwt().refreshDays(), ChronoUnit.DAYS);
        sessions.save(new RefreshSession(UUID.randomUUID(), user, hash(rawRefresh), refreshExpiry,
                userAgent, ipAddress));
        return new AuthResponse(access.token(), access.expiresAt(), rawRefresh, refreshExpiry, toCurrentUser(user));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the JDK", ex);
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static CurrentUser toCurrentUser(UserAccount user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName(), Set.copyOf(user.getRoles()),
                user.isPasswordChangeRequired());
    }
}

