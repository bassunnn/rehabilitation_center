package ru.mirea.project.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Procedure {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Long id;
    private Long patientId;
    private String patientFullName;
    private String name;
    private String description;
    private LocalDate procedureDate;
    private ProcedureStatus status;

    public Procedure() {}

    public Procedure(Long patientId, String name, String description, LocalDate procedureDate, ProcedureStatus status) {
        this.patientId = patientId;
        this.name = name;
        this.description = description;
        this.procedureDate = procedureDate;
        this.status = status;
    }

    public Procedure(Long id, Long patientId, String name, String description, LocalDate procedureDate, ProcedureStatus status) {
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

    public String getPatientFullName() {
        return patientFullName;
    }

    public void setPatientFullName(String patientFullName) {
        this.patientFullName = patientFullName;
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

    public LocalDate getProcedureDate() {
        return procedureDate;
    }

    public void setProcedureDate(LocalDate procedureDate) {
        this.procedureDate = procedureDate;
    }

    public ProcedureStatus getStatus() {
        return status;
    }

    public void setStatus(ProcedureStatus status) {
        this.status = status;
    }

    public String getStatusTitle() {
        return status != null ? status.getTitle() : "";
    }

    public String getFormattedProcedureDate() {
        return procedureDate != null ? procedureDate.format(DATE_FORMATTER) : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Procedure procedure = (Procedure) o;
        return Objects.equals(id, procedure.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", name, getFormattedProcedureDate(), getStatusTitle());
    }
}
