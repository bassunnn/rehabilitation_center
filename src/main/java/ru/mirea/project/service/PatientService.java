package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Patient;
import ru.mirea.project.model.PatientStatus;
import ru.mirea.project.repository.PatientRepository;
import ru.mirea.project.util.CsvExporter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PatientService {
    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public Patient create(Patient patient) {
        validate(patient, null);
        if (repository.existsByPhoneOrEmail(patient.getPhone(), patient.getEmail(), null)) {
            throw new BusinessException("Телефон или email пациента уже используется");
        }
        return repository.save(patient);
    }

    public List<Patient> findAll() {
        return repository.findAll();
    }

    public Patient findById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID пациента должен быть положительным числом");
        }
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент с ID " + id + " не найден"));
    }

    public void update(Patient patient) {
        if (patient == null) {
            throw new BusinessException("Пациент не задан");
        }
        Patient old = findById(patient.getId());
        validate(patient, patient.getId());
        if (old.getStatus() == PatientStatus.ARCHIVED && patient.getStatus() != PatientStatus.ARCHIVED) {
            throw new BusinessException("Архивного пациента нельзя вернуть в активный список");
        }
        if (repository.existsByPhoneOrEmail(patient.getPhone(), patient.getEmail(), patient.getId())) {
            throw new BusinessException("Телефон или email пациента уже используется");
        }
        repository.update(patient);
    }

    public void delete(Long id) {
        Patient patient = findById(id);
        if (patient.getStatus() == PatientStatus.ACTIVE) {
            throw new BusinessException("Активного пациента нельзя удалить: сначала измените статус на неактивный или архивный");
        }
        if (!repository.deleteById(id)) {
            throw new EntityNotFoundException("Пациент с ID " + id + " не найден");
        }
    }

    public List<Patient> sortByName(boolean ascending) {
        Comparator<Patient> comparator = Comparator.comparing(Patient::getLastName,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Patient::getFirstName, String.CASE_INSENSITIVE_ORDER);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<Patient> sortByBirthDate(boolean ascending) {
        Comparator<Patient> comparator = Comparator.comparing(Patient::getBirthDate);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<Patient> filterByStatus(PatientStatus status) {
        if (status == null) {
            throw new BusinessException("Статус пациента не выбран");
        }
        return findAll().stream().filter(patient -> patient.getStatus() == status).collect(Collectors.toList());
    }

    public List<Patient> filterByBirthDate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Неверный диапазон дат рождения");
        }
        return findAll().stream()
                .filter(patient -> !patient.getBirthDate().isBefore(from)
                        && !patient.getBirthDate().isAfter(to))
                .collect(Collectors.toList());
    }

    public void exportToCsv(List<Patient> patients, String fileName) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Имя", "Фамилия", "Телефон", "Email", "Дата рождения", "Статус"});
        for (Patient patient : patients) {
            rows.add(new String[]{String.valueOf(patient.getId()), patient.getFirstName(), patient.getLastName(),
                    patient.getPhone(), patient.getEmail(), String.valueOf(patient.getBirthDate()),
                    patient.getStatus().name()});
        }
        CsvExporter.write(fileName, rows);
    }

    private void validate(Patient patient, Long excludedId) {
        if (patient == null) {
            throw new BusinessException("Пациент не задан");
        }
        if (isBlank(patient.getFirstName()) || isBlank(patient.getLastName())) {
            throw new BusinessException("Имя и фамилия пациента обязательны");
        }
        if (isBlank(patient.getPhone()) || isBlank(patient.getEmail())) {
            throw new BusinessException("Телефон и email пациента обязательны");
        }
        if (patient.getBirthDate() == null || patient.getBirthDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Дата рождения обязательна и не может быть будущей");
        }
        if (patient.getStatus() == null) {
            throw new BusinessException("Статус пациента не выбран");
        }
        if (excludedId != null && (patient.getId() == null || !patient.getId().equals(excludedId))) {
            throw new BusinessException("ID пациента нельзя изменить");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
