package ru.mirea.rehab.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Выполняет CPU-ёмкое хэширование и проверку паролей (BCrypt) в отдельном пуле потоков,
 * чтобы не блокировать потоки HTTP-сервера и не удерживать транзакции БД.
 */
@Service
public class PasswordHashingService {
    private final PasswordEncoder passwordEncoder;
    private final Executor executor;

    public PasswordHashingService(PasswordEncoder passwordEncoder,
                                  @Qualifier("passwordHashExecutor") Executor executor) {
        this.passwordEncoder = passwordEncoder;
        this.executor = executor;
    }

    public CompletableFuture<String> encodeAsync(CharSequence rawPassword) {
        return CompletableFuture.supplyAsync(() -> passwordEncoder.encode(rawPassword), executor);
    }

    public CompletableFuture<Boolean> matchesAsync(CharSequence rawPassword, String encodedPassword) {
        return CompletableFuture.supplyAsync(() -> passwordEncoder.matches(rawPassword, encodedPassword), executor);
    }

    public String encode(CharSequence rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
