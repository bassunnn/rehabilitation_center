package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.Patient;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.repository.PatientRepository;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class ValidationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\-\\s()]{7,20}$");

    private final PatientRepository patientRepository;

    public ValidationService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public void validatePatient(Patient patient) {
        if (patient == null) {
            throw new BusinessException("Данные пациента не могут быть пустыми.");
        }

        if (patient.getLastName() == null || patient.getLastName().trim().isEmpty()) {
            throw new BusinessException("Поле 'Фамилия' обязательно для заполнения.");
        }
        if (patient.getLastName().trim().length() < 2 || patient.getLastName().trim().length() > 50) {
            throw new BusinessException("Длина фамилии должна быть от 2 до 50 символов.");
        }

        if (patient.getFirstName() == null || patient.getFirstName().trim().isEmpty()) {
            throw new BusinessException("Поле 'Имя' обязательно для заполнения.");
        }
        if (patient.getFirstName().trim().length() < 2 || patient.getFirstName().trim().length() > 50) {
            throw new BusinessException("Длина имени должна быть от 2 до 50 символов.");
        }

        if (patient.getPhone() == null || patient.getPhone().trim().isEmpty()) {
            throw new BusinessException("Поле 'Номер телефона' обязательно для заполнения.");
        }
        String cleanPhone = patient.getPhone().trim();
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new BusinessException("Номер телефона имеет некорректный формат (например: +79991234567).");
        }
        if (patientRepository.existsByPhone(cleanPhone, patient.getId())) {
            throw new BusinessException("Пациент с таким номером телефона уже зарегистрирован в системе.");
        }

        if (patient.getEmail() == null || patient.getEmail().trim().isEmpty()) {
            throw new BusinessException("Поле 'Email' обязательно для заполнения.");
        }
        String cleanEmail = patient.getEmail().trim();
        if (cleanEmail.length() > 100 || !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new BusinessException("Электронная почта имеет некорректный формат (например: patient@example.com).");
        }
        if (patientRepository.existsByEmail(cleanEmail, patient.getId())) {
            throw new BusinessException("Пациент с таким адресом электронной почты уже существует.");
        }

        if (patient.getBirthDate() == null) {
            throw new BusinessException("Поле 'Дата рождения' обязательно для заполнения.");
        }
        if (patient.getBirthDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Дата рождения не может быть в будущем.");
        }
        int age = patient.getAge();
        if (age < 0 || age > 125) {
            throw new BusinessException("Указан некорректный возраст пациента: " + age + " лет.");
        }
    }

    public void validateProcedure(Procedure procedure) {
        if (procedure == null) {
            throw new BusinessException("Данные процедуры не могут быть пустыми.");
        }

        if (procedure.getPatientId() == null || procedure.getPatientId() <= 0) {
            throw new BusinessException("Необходимо выбрать пациента для назначения процедуры.");
        }

        if (procedure.getName() == null || procedure.getName().trim().isEmpty()) {
            throw new BusinessException("Поле 'Название процедуры' обязательно для заполнения.");
        }
        if (procedure.getName().trim().length() < 2 || procedure.getName().trim().length() > 100) {
            throw new BusinessException("Название процедуры должно содержать от 2 до 100 символов.");
        }

        if (procedure.getDescription() != null && procedure.getDescription().trim().length() > 500) {
            throw new BusinessException("Описание процедуры не должно превышать 500 символов.");
        }

        if (procedure.getProcedureDate() == null) {
            throw new BusinessException("Поле 'Дата процедуры' обязательно для заполнения.");
        }

        if (procedure.getStatus() == null) {
            throw new BusinessException("Необходимо указать статус процедуры.");
        }
    }
}

