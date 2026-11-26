package ru.mirea.rehab.security;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashingServiceTest {

    @Test
    void encodeAndMatchesRunOffCallerThread() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setThreadNamePrefix("password-hash-");
        executor.initialize();

        try {
            String callerThread = Thread.currentThread().getName();
            AtomicReference<String> hashThread = new AtomicReference<>();
            AtomicReference<String> matchThread = new AtomicReference<>();

            PasswordHashingService service = new PasswordHashingService(new BCryptPasswordEncoder(4), task ->
                    executor.execute(() -> {
                        if (hashThread.get() == null) {
                            hashThread.set(Thread.currentThread().getName());
                        } else {
                            matchThread.set(Thread.currentThread().getName());
                        }
                        task.run();
                    }));

            String encoded = service.encodeAsync("StrongPass!123").join();
            CompletableFuture<Boolean> valid = service.matchesAsync("StrongPass!123", encoded);
            CompletableFuture<Boolean> invalid = service.matchesAsync("WrongPass!123", encoded);

            assertThat(valid.join()).isTrue();
            assertThat(invalid.join()).isFalse();
            assertThat(hashThread.get()).startsWith("password-hash-").isNotEqualTo(callerThread);
            assertThat(matchThread.get()).startsWith("password-hash-").isNotEqualTo(callerThread);
        } finally {
            executor.shutdown();
        }
    }
}
