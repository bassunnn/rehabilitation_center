package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.repository.PatientRepository;
import ru.mirea.project.repository.ProcedureRepository;
import ru.mirea.project.repository.TherapistRepository;
import ru.mirea.project.util.CsvExporter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProcedureService {
    private final ProcedureRepository repository;
    private final PatientRepository patientRepository;
    private final TherapistRepository therapistRepository;

    public ProcedureService(ProcedureRepository repository, PatientRepository patientRepository,
                            TherapistRepository therapistRepository) {
        this.repository = repository;
        this.patientRepository = patientRepository;
        this.therapistRepository = therapistRepository;
    }

    public Procedure create(Procedure procedure) {
        validate(procedure);
        if (procedure.getStatus() == ProcedureStatus.COMPLETED
                || procedure.getStatus() == ProcedureStatus.CANCELLED) {
            throw new BusinessException("Новая процедура должна иметь статус "
                    + ProcedureStatus.PLANNED.getTitle() + " или " + ProcedureStatus.IN_PROGRESS.getTitle());
        }
        return repository.save(procedure);
    }

    public List<Procedure> findAll() {
        return repository.findAll();
    }

    public Procedure findById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID процедуры должен быть положительным числом");
        }
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Процедура с ID " + id + " не найдена"));
    }

    public void update(Procedure procedure) {
        if (procedure == null) {
            throw new BusinessException("Процедура не задана");
        }
        Procedure old = findById(procedure.getId());
        validate(procedure);
        validateStatusTransition(old.getStatus(), procedure.getStatus());
        if (procedure.getStatus() == ProcedureStatus.COMPLETED
                && procedure.getProcedureDate().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Нельзя завершить процедуру, которая еще не началась");
        }
        repository.update(procedure);
    }

    public void delete(Long id) {
        Procedure procedure = findById(id);
        if (procedure.getStatus() == ProcedureStatus.COMPLETED) {
            throw new BusinessException("Завершенную процедуру нельзя удалить");
        }
        if (!repository.deleteById(id)) {
            throw new EntityNotFoundException("Процедура с ID " + id + " не найдена");
        }
    }

    public List<Procedure> sortByDate(boolean ascending) {
        Comparator<Procedure> comparator = Comparator.comparing(Procedure::getProcedureDate);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<Procedure> sortByName(boolean ascending) {
        Comparator<Procedure> comparator = Comparator.comparing(Procedure::getName,
                String.CASE_INSENSITIVE_ORDER);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<Procedure> filterByStatus(ProcedureStatus status) {
        if (status == null) {
            throw new BusinessException("Статус процедуры не выбран");
        }
        return findAll().stream().filter(item -> item.getStatus() == status).collect(Collectors.toList());
    }

    public List<Procedure> filterByPatient(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new BusinessException("ID пациента должен быть положительным числом");
        }
        return findAll().stream().filter(item -> patientId.equals(item.getPatientId()))
                .collect(Collectors.toList());
    }

    public List<Procedure> filterByDate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Неверный диапазон дат процедуры");
        }
        return findAll().stream()
                .filter(item -> !item.getProcedureDate().toLocalDate().isBefore(from)
                        && !item.getProcedureDate().toLocalDate().isAfter(to))
                .collect(Collectors.toList());
    }

    public void exportToCsv(List<Procedure> procedures, String fileName) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Пациент ID", "Специалист ID", "Название", "Описание",
                "Дата и время", "Статус"});
        for (Procedure procedure : procedures) {
            rows.add(new String[]{String.valueOf(procedure.getId()), String.valueOf(procedure.getPatientId()),
                    String.valueOf(procedure.getTherapistId()), procedure.getName(), procedure.getDescription(),
                    String.valueOf(procedure.getProcedureDate()), procedure.getStatus().name()});
        }
        CsvExporter.write(fileName, rows);
    }

    private void validate(Procedure procedure) {
        if (procedure == null) {
            throw new BusinessException("Процедура не задана");
        }
        if (procedure.getPatientId() == null || procedure.getPatientId() <= 0
                || !patientRepository.existsById(procedure.getPatientId())) {
            throw new EntityNotFoundException("Указанный пациент не найден");
        }
        if (procedure.getTherapistId() == null || procedure.getTherapistId() <= 0
                || !therapistRepository.existsById(procedure.getTherapistId())) {
            throw new EntityNotFoundException("Указанный специалист не найден");
        }
        if (isBlank(procedure.getName())) {
            throw new BusinessException("Название процедуры обязательно");
        }
        if (procedure.getProcedureDate() == null) {
            throw new BusinessException("Дата процедуры обязательна");
        }
        if (procedure.getStatus() == null) {
            throw new BusinessException("Статус процедуры не выбран");
        }
    }

    private void validateStatusTransition(ProcedureStatus oldStatus, ProcedureStatus newStatus) {
        if (oldStatus == ProcedureStatus.COMPLETED || oldStatus == ProcedureStatus.CANCELLED) {
            if (oldStatus != newStatus) {
                throw new BusinessException("Завершенный или отмененный статус нельзя изменить");
            }
            return;
        }
        boolean allowed = oldStatus == newStatus
                || oldStatus == ProcedureStatus.PLANNED
                && (newStatus == ProcedureStatus.IN_PROGRESS || newStatus == ProcedureStatus.CANCELLED)
                || oldStatus == ProcedureStatus.IN_PROGRESS
                && (newStatus == ProcedureStatus.COMPLETED || newStatus == ProcedureStatus.CANCELLED);
        if (!allowed) {
            throw new BusinessException("Недопустимый переход статуса процедуры");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
