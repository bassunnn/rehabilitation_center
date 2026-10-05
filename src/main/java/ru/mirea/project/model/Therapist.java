package ru.mirea.project.model;

import java.time.LocalDate;

public class Therapist {
    private Long id;
    private String fullName;
    private TherapistSpecialization specialization;
    private String phone;
    private String email;
    private LocalDate hireDate;

    public Therapist(String fullName, TherapistSpecialization specialization, String phone,
                     String email, LocalDate hireDate) {
        this.fullName = fullName;
        this.specialization = specialization;
        this.phone = phone;
        this.email = email;
        this.hireDate = hireDate;
    }

    public Therapist(Long id, String fullName, TherapistSpecialization specialization,
                     String phone, String email, LocalDate hireDate) {
        this(fullName, specialization, phone, email, hireDate);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public TherapistSpecialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(TherapistSpecialization specialization) {
        this.specialization = specialization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    @Override
    public String toString() {
        return String.format("Специалист #%d: %s | Специализация: %s | Тел: %s | Email: %s | Принят: %s",
                id, fullName, specialization == null ? "не указана" : specialization.getTitle(),
                phone, email, hireDate);
    }
}
