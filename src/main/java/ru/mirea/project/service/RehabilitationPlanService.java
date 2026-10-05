package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.PlanStatus;
import ru.mirea.project.model.RehabilitationPlan;
import ru.mirea.project.repository.PatientRepository;
import ru.mirea.project.repository.RehabilitationPlanRepository;
import ru.mirea.project.repository.TherapistRepository;
import ru.mirea.project.util.CsvExporter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RehabilitationPlanService {
    private final RehabilitationPlanRepository repository;
    private final PatientRepository patientRepository;
    private final TherapistRepository therapistRepository;

    public RehabilitationPlanService(RehabilitationPlanRepository repository, PatientRepository patientRepository,
                                     TherapistRepository therapistRepository) {
        this.repository = repository;
        this.patientRepository = patientRepository;
        this.therapistRepository = therapistRepository;
    }

    public RehabilitationPlan create(RehabilitationPlan plan) {
        validate(plan);
        if (plan.getStatus() == PlanStatus.COMPLETED && plan.getEndDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Нельзя создать уже завершенный план с будущей датой окончания");
        }
        return repository.save(plan);
    }

    public List<RehabilitationPlan> findAll() {
        return repository.findAll();
    }

    public RehabilitationPlan findById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID плана должен быть положительным числом");
        }
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("План с ID " + id + " не найден"));
    }

    public void update(RehabilitationPlan plan) {
        if (plan == null) {
            throw new BusinessException("План не задан");
        }
        RehabilitationPlan old = findById(plan.getId());
        validate(plan);
        validateStatusTransition(old.getStatus(), plan.getStatus());
        if (plan.getStatus() == PlanStatus.COMPLETED && plan.getEndDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Нельзя завершить план до даты окончания");
        }
        repository.update(plan);
    }

    public void delete(Long id) {
        RehabilitationPlan plan = findById(id);
        if (plan.getStatus() == PlanStatus.ACTIVE) {
            throw new BusinessException("Активный план нельзя удалить");
        }
        if (!repository.deleteById(id)) {
            throw new EntityNotFoundException("План с ID " + id + " не найден");
        }
    }

    public List<RehabilitationPlan> sortByStartDate(boolean ascending) {
        Comparator<RehabilitationPlan> comparator = Comparator.comparing(RehabilitationPlan::getStartDate);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<RehabilitationPlan> sortByTitle(boolean ascending) {
        Comparator<RehabilitationPlan> comparator = Comparator.comparing(RehabilitationPlan::getTitle,
                String.CASE_INSENSITIVE_ORDER);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<RehabilitationPlan> filterByStatus(PlanStatus status) {
        if (status == null) {
            throw new BusinessException("Статус плана не выбран");
        }
        return findAll().stream().filter(item -> item.getStatus() == status).collect(Collectors.toList());
    }

    public List<RehabilitationPlan> filterByPatient(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new BusinessException("ID пациента должен быть положительным числом");
        }
        return findAll().stream().filter(item -> patientId.equals(item.getPatientId()))
                .collect(Collectors.toList());
    }

    public List<RehabilitationPlan> filterByDate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Неверный диапазон дат плана");
        }
        return findAll().stream()
                .filter(item -> !item.getStartDate().isAfter(to) && !item.getEndDate().isBefore(from))
                .collect(Collectors.toList());
    }

    public void exportToCsv(List<RehabilitationPlan> plans, String fileName) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Пациент ID", "Специалист ID", "Название", "Цель",
                "Дата начала", "Дата окончания", "Статус"});
        for (RehabilitationPlan plan : plans) {
            rows.add(new String[]{String.valueOf(plan.getId()), String.valueOf(plan.getPatientId()),
                    String.valueOf(plan.getTherapistId()), plan.getTitle(), plan.getGoal(),
                    String.valueOf(plan.getStartDate()), String.valueOf(plan.getEndDate()),
                    plan.getStatus().name()});
        }
        CsvExporter.write(fileName, rows);
    }

    private void validate(RehabilitationPlan plan) {
        if (plan == null) {
            throw new BusinessException("План не задан");
        }
        if (plan.getPatientId() == null || plan.getPatientId() <= 0
                || !patientRepository.existsById(plan.getPatientId())) {
            throw new EntityNotFoundException("Указанный пациент не найден");
        }
        if (plan.getTherapistId() == null || plan.getTherapistId() <= 0
                || !therapistRepository.existsById(plan.getTherapistId())) {
            throw new EntityNotFoundException("Указанный специалист не найден");
        }
        if (isBlank(plan.getTitle()) || isBlank(plan.getGoal())) {
            throw new BusinessException("Название и цель плана обязательны");
        }
        if (plan.getStartDate() == null || plan.getEndDate() == null
                || plan.getEndDate().isBefore(plan.getStartDate())) {
            throw new BusinessException("Дата окончания должна быть не раньше даты начала");
        }
        if (plan.getStatus() == null) {
            throw new BusinessException("Статус плана не выбран");
        }
    }

    private void validateStatusTransition(PlanStatus oldStatus, PlanStatus newStatus) {
        if (oldStatus == PlanStatus.COMPLETED || oldStatus == PlanStatus.CANCELLED) {
            if (oldStatus != newStatus) {
                throw new BusinessException("Завершенный или отмененный план нельзя изменить");
            }
            return;
        }
        boolean allowed = oldStatus == newStatus
                || oldStatus == PlanStatus.DRAFT
                && (newStatus == PlanStatus.ACTIVE || newStatus == PlanStatus.CANCELLED)
                || oldStatus == PlanStatus.ACTIVE
                && (newStatus == PlanStatus.COMPLETED || newStatus == PlanStatus.CANCELLED);
        if (!allowed) {
            throw new BusinessException("Недопустимый переход статуса плана");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
