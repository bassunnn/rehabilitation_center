package ru.mirea.rehab.error;

import org.springframework.security.core.AuthenticationException;

public class AuthenticationFailedException extends AuthenticationException {
    public AuthenticationFailedException() {
        super("Неверный логин или пароль, либо учетная запись заблокирована");
    }
}

