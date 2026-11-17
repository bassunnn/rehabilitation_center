package ru.mirea.rehab.domain;

/**
 * Роли доступа в информационной системе реабилитационного центра.
 */
public enum Role {
    ADMIN,
    DOCTOR,
    VIEWER;

    public String authority() {
        return "ROLE_" + name();
    }
}

