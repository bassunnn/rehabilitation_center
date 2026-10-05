package ru.mirea.project.model;

import java.time.LocalDate;

public class Patient {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private PatientStatus status;

    public Patient(String firstName, String lastName, String phone, String email,
                   LocalDate birthDate, PatientStatus status) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
        this.status = status;
    }

    public Patient(String firstName, String lastName, String phone, String email, LocalDate birthDate) {
        this(firstName, lastName, phone, email, birthDate, PatientStatus.ACTIVE);
    }

    public Patient(Long id, String firstName, String lastName, String phone, String email,
                   LocalDate birthDate, PatientStatus status) {
        this(firstName, lastName, phone, email, birthDate, status);
        this.id = id;
    }

    public Patient(Long id, String firstName, String lastName, String phone, String email, LocalDate birthDate) {
        this(id, firstName, lastName, phone, email, birthDate, PatientStatus.ACTIVE);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format("Пациент #%d: %s %s | Тел: %s | Email: %s | Дата рождения: %s | Статус: %s",
                id, firstName, lastName, phone, email, birthDate,
                status == null ? "не указан" : status.getTitle());
    }
}
