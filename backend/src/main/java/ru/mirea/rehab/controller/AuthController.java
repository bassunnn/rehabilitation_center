package ru.mirea.rehab.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.rehab.config.AppProperties;
import ru.mirea.rehab.domain.Role;
import ru.mirea.rehab.dto.AuthDtos.AuthResponse;
import ru.mirea.rehab.dto.AuthDtos.ChangePasswordRequest;
import ru.mirea.rehab.dto.AuthDtos.ClientType;
import ru.mirea.rehab.dto.AuthDtos.CurrentUser;
import ru.mirea.rehab.dto.AuthDtos.InitialPasswordChangeRequest;
import ru.mirea.rehab.dto.AuthDtos.LoginRequest;
import ru.mirea.rehab.dto.AuthDtos.RefreshRequest;
import ru.mirea.rehab.service.AuthService;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/** REST endpoints аутентификации для web и native-клиентов. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "rehab_refresh";
    private static final String SWAGGER_COOKIE = "rehab_swagger";
    private final AuthService auth;
    private final AppProperties properties;

    public AuthController(AuthService auth, AppProperties properties) {
        this.auth = auth;
        this.properties = properties;
    }

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        boolean web = request.effectiveClientType() == ClientType.WEB;
        return auth.login(request).thenApply(issued -> {
            if (!web) {
                return ResponseEntity.ok(issued);
            }
            HttpHeaders headers = new HttpHeaders();
            addRefreshCookie(headers, issued.refreshToken(), issued.refreshExpiresAt());
            syncSwaggerCookie(headers, issued);
            return ResponseEntity.ok().headers(headers).body(withoutRefreshToken(issued));
        });
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody(required = false) RefreshRequest request,
                                @CookieValue(name = REFRESH_COOKIE, required = false) String cookie,
                                HttpServletResponse response) {
        boolean web = cookie != null && !cookie.isBlank();
        String raw = web ? cookie : request == null ? null : request.refreshToken();
        AuthResponse issued = auth.refresh(raw);
        if (web) {
            setRefreshCookie(response, issued.refreshToken(), issued.refreshExpiresAt());
            syncSwaggerCookie(response, issued);
            return withoutRefreshToken(issued);
        }
        return issued;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                       @CookieValue(name = REFRESH_COOKIE, required = false) String cookie,
                                       HttpServletResponse response) {
        auth.logout(cookie != null ? cookie : request == null ? null : request.refreshToken());
        clearRefreshCookie(response);
        clearSwaggerCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public CurrentUser me() {
        return auth.me();
    }

    @PostMapping("/change-password")
    public CompletableFuture<ResponseEntity<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return auth.changePassword(request).thenApply(ignored -> ResponseEntity.noContent().build());
    }

    @PostMapping("/change-initial-password")
    public CompletableFuture<ResponseEntity<Void>> changeInitialPassword(
            @Valid @RequestBody InitialPasswordChangeRequest request) {
        return auth.changeInitialPassword(request).thenApply(ignored -> ResponseEntity.noContent().build());
    }

    private void addRefreshCookie(HttpHeaders headers, String value, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshCookie(HttpServletResponse response, String value, Instant expiresAt) {
        HttpHeaders headers = new HttpHeaders();
        addRefreshCookie(headers, value, expiresAt);
        headers.getOrEmpty(HttpHeaders.SET_COOKIE).forEach(v -> response.addHeader(HttpHeaders.SET_COOKIE, v));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(properties.cookieSecure()).sameSite("Strict")
                .path("/api/auth").maxAge(Duration.ZERO).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void syncSwaggerCookie(HttpHeaders headers, AuthResponse issued) {
        boolean admin = issued.user().roles().contains(Role.ADMIN);
        boolean permanentPassword = !issued.user().passwordChangeRequired();

        if (!admin || !permanentPassword) {
            headers.add(HttpHeaders.SET_COOKIE, buildClearedSwaggerCookie().toString());
            return;
        }

        Duration maxAge = Duration.between(Instant.now(), issued.accessExpiresAt());

        ResponseCookie cookie = ResponseCookie.from(SWAGGER_COOKIE, issued.accessToken())
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();

        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void syncSwaggerCookie(HttpServletResponse response, AuthResponse issued) {
        HttpHeaders headers = new HttpHeaders();
        syncSwaggerCookie(headers, issued);
        headers.getOrEmpty(HttpHeaders.SET_COOKIE).forEach(v -> response.addHeader(HttpHeaders.SET_COOKIE, v));
    }

    private void clearSwaggerCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildClearedSwaggerCookie().toString());
    }

    private ResponseCookie buildClearedSwaggerCookie() {
        return ResponseCookie.from(SWAGGER_COOKIE, "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private static AuthResponse withoutRefreshToken(AuthResponse source) {
        return new AuthResponse(source.accessToken(), source.accessExpiresAt(), null,
                source.refreshExpiresAt(), source.user());
    }
}

