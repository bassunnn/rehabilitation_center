package ru.mirea.rehab.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mirea.rehab.domain.Role;
import ru.mirea.rehab.domain.UserAccount;
import ru.mirea.rehab.dto.UserDtos.CreateUserRequest;
import ru.mirea.rehab.dto.UserDtos.ResetPasswordRequest;
import ru.mirea.rehab.dto.UserDtos.UpdateUserRequest;
import ru.mirea.rehab.dto.UserDtos.UserResponse;
import ru.mirea.rehab.error.ConflictException;
import ru.mirea.rehab.error.NotFoundException;
import ru.mirea.rehab.repository.RefreshSessionRepository;
import ru.mirea.rehab.repository.UserAccountRepository;
import ru.mirea.rehab.security.CurrentUserService;
import ru.mirea.rehab.security.PasswordHashingService;
import ru.mirea.rehab.security.PasswordPolicy;
import ru.mirea.rehab.service.AuditService.AuditContext;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Административное управление пользователями без раскрытия хешей паролей. */
@Service
public class UserAdminService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordHashingService passwordHashing;
    private final PasswordPolicy policy;
    private final CurrentUserService currentUser;
    private final AuditService audit;
    private final TransactionTemplate transactionTemplate;

    public UserAdminService(UserAccountRepository users, RefreshSessionRepository sessions,
                            PasswordHashingService passwordHashing, PasswordPolicy policy,
                            CurrentUserService currentUser, AuditService audit,
                            TransactionTemplate transactionTemplate) {
        this.users = users;
        this.sessions = sessions;
        this.passwordHashing = passwordHashing;
        this.policy = policy;
        this.currentUser = currentUser;
        this.audit = audit;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(UserAdminService::toResponse);
    }

    public CompletableFuture<UserResponse> create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("Пользователь с таким email уже существует");
        policy.validate(request.temporaryPassword(), email);
        Set<Role> roles = normalizeRoles(request.roles());
        String displayName = request.displayName().trim();
        AuditContext auditCtx = audit.captureContext();

        return passwordHashing.encodeAsync(request.temporaryPassword())
                .thenApply(encodedPassword -> transactionTemplate.execute(status -> {
                    if (users.existsByEmailIgnoreCase(email)) {
                        throw new ConflictException("Пользователь с таким email уже существует");
                    }
                    UserAccount user = new UserAccount(UUID.randomUUID(), email, displayName, encodedPassword, roles);
                    user.setPasswordChangeRequired(true);
                    users.save(user);
                    audit.record("USER_CREATED", "USER", user.getId(), "Создан пользователь " + email,
                            Map.of("roles", user.getRoles()), auditCtx);
                    return toResponse(user);
                }));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        UserAccount user = require(id);
        if (request.version() != user.getVersion()) throw new ConflictException("Пользователь уже изменен. Обновите список");
        String email = normalizeEmail(request.email());
        users.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Пользователь с таким email уже существует");
        });
        Set<Role> roles = normalizeRoles(request.roles());
        UserAccount actor = currentUser.requireEntity();
        if (actor.getId().equals(id) && (!request.enabled() || !roles.contains(Role.ADMIN))) {
            throw new AccessDeniedException("Администратор не может отключить себя или снять свою роль ADMIN");
        }
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setEnabled(request.enabled());
        user.setRoles(roles);
        if (!request.enabled()) sessions.revokeAllForUser(id, Instant.now());
        audit.record("USER_UPDATED", "USER", id, "Изменен пользователь " + email, Map.of("roles", roles, "enabled", request.enabled()));
        return toResponse(user);
    }

    public CompletableFuture<Void> resetPassword(UUID id, ResetPasswordRequest request) {
        UserAccount user = require(id);
        policy.validate(request.temporaryPassword(), user.getEmail());
        AuditContext auditCtx = audit.captureContext();

        return passwordHashing.encodeAsync(request.temporaryPassword())
                .thenAccept(encodedPassword -> transactionTemplate.executeWithoutResult(status -> {
                    UserAccount managed = require(id);
                    managed.setPasswordHash(encodedPassword);
                    managed.setPasswordChangeRequired(true);
                    managed.setFailedLoginAttempts(0);
                    managed.setLockedUntil(null);
                    sessions.revokeAllForUser(id, Instant.now());
                    audit.record("USER_PASSWORD_RESET", "USER", id, "Администратор сбросил пароль пользователя", Map.of(), auditCtx);
                }));
    }

    private UserAccount require(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private static Set<Role> normalizeRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) throw new ConflictException("У пользователя должна быть хотя бы одна роль");
        return Set.copyOf(roles);
    }

    private static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEnabled(),
                user.isPasswordChangeRequired(), Set.copyOf(user.getRoles()), user.getLockedUntil(),
                user.getCreatedAt(), user.getUpdatedAt(), user.getVersion());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

