package ru.mirea.project.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Procedure {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Long id;
    private Long patientId;
    private String name;
    private String description;
    private LocalDateTime procedureDate;
    private ProcedureStatus status;

    public Procedure(Long patientId, String name, String description, LocalDateTime procedureDate, ProcedureStatus status) {
        this.patientId = patientId;
        this.name = name;
        this.description = description;
        this.procedureDate = procedureDate;
        this.status = status;
    }

    public Procedure(Long id, Long patientId, String name, String description, LocalDateTime procedureDate, ProcedureStatus status) {
        this.id = id;
        this.patientId = patientId;
        this.name = name;
        this.description = description;
        this.procedureDate = procedureDate;
        this.status = status;
    }

    @Override
    public String toString() {
        String formattedDate = procedureDate != null ? procedureDate.format(FORMATTER) : "не указана";
        return String.format("Процедура #%d [Пациент #%d] | %s | %s | Статус: %s | Описание: %s ",
                id, patientId, name, formattedDate, status.getTitle(), description );
    }
}
