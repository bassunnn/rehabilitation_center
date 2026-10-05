package ru.mirea.project.model;

public enum PlanStatus {
    DRAFT("Черновик"),
    ACTIVE("Активен"),
    COMPLETED("Завершен"),
    CANCELLED("Отменен");

    private final String title;

    PlanStatus(String title) {
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
