package ru.mirea.rehab.security;

import org.junit.jupiter.api.Test;
import ru.mirea.rehab.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет минимальную серверную политику паролей. */
class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("Correct-Horse-2026!", "doctor@example.org"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordContainingLogin() {
        assertThatThrownBy(() -> policy.validate("Doctor-Strong-2026!", "doctor@example.org"))
                .isInstanceOf(BadRequestException.class);
    }
}

