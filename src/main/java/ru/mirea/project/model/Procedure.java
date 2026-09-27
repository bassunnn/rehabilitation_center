package ru.mirea.project.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getProcedureDate() {
        return procedureDate;
    }

    public void setProcedureDate(LocalDateTime procedureDate) {
        this.procedureDate = procedureDate;
    }

    public ProcedureStatus getStatus() {
        return status;
    }

    public void setStatus(ProcedureStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        String formattedDate = procedureDate != null ? procedureDate.format(FORMATTER) : "не указана";
        return String.format("Процедура #%d [Пациент #%d] | %s | %s | Статус: %s | Описание: %s ",
                id, patientId, name, formattedDate, status.getTitle(), description );
    }
}
