package ru.mirea.project.model;

public enum ProcedureStatus {
    PLANNED("Запланирована"),
    IN_PROGRESS("Выполняется"),
    COMPLETED("Заверщена"),
    CANCELLED("Отменена");

    private final String title;

    ProcedureStatus(String title){
        this.title = title;
    }

    public String getTitle(){
        return title;
    }

    @Override
    public String toString(){
        return title;
    }
}
