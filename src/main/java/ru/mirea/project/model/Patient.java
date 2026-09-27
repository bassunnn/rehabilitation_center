package ru.mirea.project.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Patient {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private LocalDate birthDate;

    public Patient(String firstName, String lastName, String phone, String email, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
    }

    public Patient(Long id, String firstName, String lastName, String phone, String email, LocalDate birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
    }

    public String getFullName() {
        return  firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format("Пациент %d: %s %s | Тел: %s | Email: %s | Дата рождения: %s",
                id , firstName, lastName, phone, email, birthDate);
    }
}
