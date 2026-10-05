package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Therapist;
import ru.mirea.project.model.TherapistSpecialization;
import ru.mirea.project.repository.TherapistRepository;
import ru.mirea.project.util.CsvExporter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TherapistService {
    private final TherapistRepository repository;

    public TherapistService(TherapistRepository repository) {
        this.repository = repository;
    }

    public Therapist create(Therapist therapist) {
        validate(therapist, null);
        if (repository.existsByPhoneOrEmail(therapist.getPhone(), therapist.getEmail(), null)) {
            throw new BusinessException("Телефон или email специалиста уже используется");
        }
        return repository.save(therapist);
    }

    public List<Therapist> findAll() {
        return repository.findAll();
    }

    public Therapist findById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID специалиста должен быть положительным числом");
        }
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Специалист с ID " + id + " не найден"));
    }

    public void update(Therapist therapist) {
        if (therapist == null) {
            throw new BusinessException("Специалист не задан");
        }
        findById(therapist.getId());
        validate(therapist, therapist.getId());
        if (repository.existsByPhoneOrEmail(therapist.getPhone(), therapist.getEmail(), therapist.getId())) {
            throw new BusinessException("Телефон или email специалиста уже используется");
        }
        repository.update(therapist);
    }

    public void delete(Long id) {
        findById(id);
        if (repository.hasLinkedRecords(id)) {
            throw new BusinessException("Специалиста с процедурами или планами нельзя удалить");
        }
        if (!repository.deleteById(id)) {
            throw new EntityNotFoundException("Специалист с ID " + id + " не найден");
        }
    }

    public List<Therapist> sortByName(boolean ascending) {
        Comparator<Therapist> comparator = Comparator.comparing(Therapist::getFullName,
                String.CASE_INSENSITIVE_ORDER);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<Therapist> sortByHireDate(boolean ascending) {
        Comparator<Therapist> comparator = Comparator.comparing(Therapist::getHireDate);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<Therapist> filterBySpecialization(TherapistSpecialization specialization) {
        if (specialization == null) {
            throw new BusinessException("Специализация не выбрана");
        }
        return findAll().stream().filter(item -> item.getSpecialization() == specialization)
                .collect(Collectors.toList());
    }

    public List<Therapist> filterByHireDate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Неверный диапазон даты приема на работу");
        }
        return findAll().stream()
                .filter(item -> !item.getHireDate().isBefore(from) && !item.getHireDate().isAfter(to))
                .collect(Collectors.toList());
    }

    public void exportToCsv(List<Therapist> therapists, String fileName) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "ФИО", "Специализация", "Телефон", "Email", "Дата приема"});
        for (Therapist therapist : therapists) {
            rows.add(new String[]{String.valueOf(therapist.getId()), therapist.getFullName(),
                    therapist.getSpecialization().name(), therapist.getPhone(), therapist.getEmail(),
                    String.valueOf(therapist.getHireDate())});
        }
        CsvExporter.write(fileName, rows);
    }

    private void validate(Therapist therapist, Long excludedId) {
        if (therapist == null) {
            throw new BusinessException("Специалист не задан");
        }
        if (isBlank(therapist.getFullName())) {
            throw new BusinessException("ФИО специалиста обязательно");
        }
        if (therapist.getSpecialization() == null) {
            throw new BusinessException("Специализация обязательна");
        }
        if (isBlank(therapist.getPhone()) || isBlank(therapist.getEmail())) {
            throw new BusinessException("Телефон и email специалиста обязательны");
        }
        if (therapist.getHireDate() == null || therapist.getHireDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Дата приема обязательна и не может быть будущей");
        }
        if (excludedId != null && (therapist.getId() == null || !therapist.getId().equals(excludedId))) {
            throw new BusinessException("ID специалиста нельзя изменить");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
