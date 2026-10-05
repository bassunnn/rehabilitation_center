package ru.mirea.project.model;

public enum PatientStatus {
    ACTIVE("Активен"),
    INACTIVE("Неактивен"),
    ARCHIVED("В архиве");

    private final String title;

    PatientStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
