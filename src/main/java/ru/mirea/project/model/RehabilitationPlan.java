package ru.mirea.project.model;

import java.time.LocalDate;

public class RehabilitationPlan {
    private Long id;
    private Long patientId;
    private Long therapistId;
    private String title;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private PlanStatus status;

    public RehabilitationPlan(Long patientId, Long therapistId, String title, String goal,
                              LocalDate startDate, LocalDate endDate, PlanStatus status) {
        this.patientId = patientId;
        this.therapistId = therapistId;
        this.title = title;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public RehabilitationPlan(Long id, Long patientId, Long therapistId, String title, String goal,
                              LocalDate startDate, LocalDate endDate, PlanStatus status) {
        this(patientId, therapistId, title, goal, startDate, endDate, status);
        this.id = id;
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

    public Long getTherapistId() {
        return therapistId;
    }

    public void setTherapistId(Long therapistId) {
        this.therapistId = therapistId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("План #%d | Пациент #%d | Специалист #%d | %s | Цель: %s | %s - %s | Статус: %s",
                id, patientId, therapistId, title, goal, startDate, endDate,
                status == null ? "не указан" : status.getTitle());
    }
}
