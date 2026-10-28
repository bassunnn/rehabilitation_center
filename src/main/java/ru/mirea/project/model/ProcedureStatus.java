package ru.mirea.project.model;

public enum ProcedureStatus {
    PLANNED("Запланирована"),
    IN_PROGRESS("Выполняется"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена");

    private final String title;

    ProcedureStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static ProcedureStatus fromString(String text) {
        if (text == null) {
            return null;
        }
        for (ProcedureStatus status : ProcedureStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || status.title.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Неизвестный статус процедуры: " + text);
    }

    @Override
    public String toString() {
        return title;
    }
}
