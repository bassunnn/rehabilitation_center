package ru.mirea.rehab.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.rehab.config.AppProperties;
import ru.mirea.rehab.domain.Department;
import ru.mirea.rehab.domain.Patient;
import ru.mirea.rehab.domain.PatientStatus;
import ru.mirea.rehab.domain.UserAccount;
import ru.mirea.rehab.dto.PatientDtos.BulkAction;
import ru.mirea.rehab.dto.PatientDtos.BulkFailure;
import ru.mirea.rehab.dto.PatientDtos.BulkRequest;
import ru.mirea.rehab.dto.PatientDtos.BulkResult;
import ru.mirea.rehab.dto.PatientDtos.PatientRequest;
import ru.mirea.rehab.dto.PatientDtos.PatientResponse;
import ru.mirea.rehab.error.BadRequestException;
import ru.mirea.rehab.error.ConflictException;
import ru.mirea.rehab.error.NotFoundException;
import ru.mirea.rehab.filter.PatientFilter;
import ru.mirea.rehab.filter.PatientSpecifications;
import ru.mirea.rehab.filter.SmartFilterParser;
import ru.mirea.rehab.repository.DepartmentRepository;
import ru.mirea.rehab.repository.PatientNoteRepository;
import ru.mirea.rehab.repository.PatientRepository;
import ru.mirea.rehab.security.CurrentUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Основной прикладной сервис карточек пациентов. */
@Service
public class PatientService {
    private final PatientRepository patients;
    private final PatientNoteRepository notes;
    private final DepartmentRepository departments;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final PatientMapper mapper;
    private final RiskCalculator riskCalculator;
    private final AuditService audit;
    private final AppProperties properties;

    public PatientService(PatientRepository patients, PatientNoteRepository notes,
                          DepartmentRepository departments, CurrentUserService currentUser,
                          SmartFilterParser parser, PatientMapper mapper,
                          RiskCalculator riskCalculator, AuditService audit,
                          AppProperties properties) {
        this.patients = patients;
        this.notes = notes;
        this.departments = departments;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.riskCalculator = riskCalculator;
        this.audit = audit;
        this.properties = properties;
    }

    /** Выполняет безопасный умный поиск с пагинацией. */
    @Transactional(readOnly = true)
    public Page<PatientResponse> search(String query, Pageable pageable) {
        PatientFilter filter = parser.parse(query);
        return patients.findAll(PatientSpecifications.from(filter), pageable).map(mapper::toResponse);
    }

    /** Возвращает одну карточку независимо от архивного статуса. */
    @Transactional(readOnly = true)
    public PatientResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    /** Создает карточку и рассчитывает риск на сервере. */
    @Transactional
    public PatientResponse create(PatientRequest request) {
        UserAccount actor = currentUser.requireEntity();
        validateExternalId(request.externalId(), null);
        Patient patient = new Patient(UUID.randomUUID(), normalizeRequired(request.firstName()),
                normalizeRequired(request.lastName()), actor);
        apply(patient, request, actor);
        patients.save(patient);
        audit.record("PATIENT_CREATED", "PATIENT", patient.getId(),
                "Создана карточка пациента " + displayName(patient), Map.of());
        return mapper.toResponse(patient);
    }

    /** Изменяет карточку только при совпадении optimistic lock version. */
    @Transactional
    public PatientResponse update(UUID id, PatientRequest request) {
        Patient patient = require(id);
        verifyVersion(request.version(), patient.getVersion());
        validateExternalId(request.externalId(), id);
        UserAccount actor = currentUser.requireEntity();
        patient.setFirstName(normalizeRequired(request.firstName()));
        patient.setLastName(normalizeRequired(request.lastName()));
        apply(patient, request, actor);
        audit.record("PATIENT_UPDATED", "PATIENT", id,
                "Обновлена карточка пациента " + displayName(patient), Map.of());
        return mapper.toResponse(patient);
    }

    /** Архивирование вместо физического удаления сохраняет историю. */
    @Transactional
    public PatientResponse archive(UUID id) {
        Patient patient = require(id);
        if (patient.isArchived()) return mapper.toResponse(patient);
        UserAccount actor = currentUser.requireEntity();
        archiveInternal(patient, actor);
        audit.record("PATIENT_ARCHIVED", "PATIENT", id,
                "Пациент " + displayName(patient) + " перенесен в архив", Map.of());
        return mapper.toResponse(patient);
    }

    /** Восстанавливает пациента из архива. */
    @Transactional
    public PatientResponse restore(UUID id) {
        Patient patient = require(id);
        if (!patient.isArchived()) return mapper.toResponse(patient);
        UserAccount actor = currentUser.requireEntity();
        patient.setArchived(false);
        patient.setArchivedAt(null);
        patient.setArchivedBy(null);
        patient.setUpdatedBy(actor);
        audit.record("PATIENT_RESTORED", "PATIENT", id,
                "Пациент " + displayName(patient) + " восстановлен из архива", Map.of());
        return mapper.toResponse(patient);
    }

    /** Окончательное удаление доступно только администратору. */
    @Transactional
    public void permanentDelete(UUID id, String confirmation) {
        if (!properties.permanentDeleteEnabled()) {
            throw new BadRequestException("Окончательное удаление отключено на сервере");
        }
        if (!"DELETE".equals(confirmation)) {
            throw new BadRequestException("Для окончательного удаления требуется заголовок X-Confirm-Delete: DELETE");
        }
        Patient patient = require(id);
        if (!patient.isArchived()) {
            throw new BadRequestException("Окончательно удалить можно только архивную карточку");
        }
        notes.deleteAllByPatientId(id);
        patients.delete(patient);
        audit.record("PATIENT_PERMANENTLY_DELETED", "PATIENT", id,
                "Окончательно удален пациент " + displayName(patient), Map.of());
    }

    /** Массовые операции возвращают подробный отчет об успехах и отказах. */
    @Transactional
    public BulkResult bulk(BulkRequest request) {
        validateBulkRequest(request);
        UserAccount actor = currentUser.requireEntity();
        int success = 0;
        List<BulkFailure> failures = new ArrayList<>();

        for (UUID id : request.ids()) {
            try {
                Patient patient = patients.findById(id).orElse(null);
                if (patient == null) {
                    failures.add(new BulkFailure(id, "Пациент не найден"));
                    continue;
                }
                applyBulkAction(patient, request, actor);
                success++;
            } catch (RuntimeException ex) {
                failures.add(new BulkFailure(id, ex.getMessage()));
            }
        }
        audit.record("PATIENT_BULK_ACTION", "PATIENT", null,
                "Массовая операция " + request.action() + ": успешно " + success + ", ошибок " + failures.size(),
                Map.of("action", request.action().name(), "succeeded", success, "failed", failures.size()));
        return new BulkResult(request.ids().size(), success, failures);
    }

    void apply(Patient patient, PatientRequest request, UserAccount actor) {
        patient.setExternalId(normalizeNullable(request.externalId()));
        patient.setMiddleName(normalizeNullable(request.middleName()));
        patient.setEmail(normalizeEmail(request.email()));
        patient.setPhone(normalizeNullable(request.phone()));
        patient.setBirthDate(request.birthDate());
        patient.setDepartment(resolveDepartment(request.departmentId()));
        patient.setStatus(request.status());
        patient.setRehabilitationScore(request.rehabilitationScore());
        patient.setRequiresAssistance(request.requiresAssistance());
        patient.setRiskLevel(riskCalculator.calculate(request.status(), request.rehabilitationScore(), request.requiresAssistance()));
        patient.setTags(normalizeTags(request.tags()));
        patient.setUpdatedBy(actor);
    }

    private void applyBulkAction(Patient patient, BulkRequest request, UserAccount actor) {
        switch (request.action()) {
            case ARCHIVE -> archiveInternal(patient, actor);
            case RESTORE -> {
                patient.setArchived(false);
                patient.setArchivedAt(null);
                patient.setArchivedBy(null);
                patient.setUpdatedBy(actor);
            }
            case SET_STATUS -> {
                patient.setStatus(request.status());
                patient.setRiskLevel(riskCalculator.calculate(request.status(), patient.getRehabilitationScore(), patient.isRequiresAssistance()));
                patient.setUpdatedBy(actor);
            }
            case ADD_TAG -> {
                Set<String> tags = new LinkedHashSet<>(patient.getTags());
                tags.add(normalizeTag(request.tag()));
                patient.setTags(tags);
                patient.setUpdatedBy(actor);
            }
            case REMOVE_TAG -> {
                Set<String> tags = new LinkedHashSet<>(patient.getTags());
                tags.remove(normalizeTag(request.tag()));
                patient.setTags(tags);
                patient.setUpdatedBy(actor);
            }
        }
    }

    private static void validateBulkRequest(BulkRequest request) {
        if (request.action() == BulkAction.SET_STATUS && request.status() == null) {
            throw new BadRequestException("Для SET_STATUS требуется status");
        }
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Для операции с тегом требуется tag");
        }
    }

    private static void archiveInternal(Patient patient, UserAccount actor) {
        patient.setArchived(true);
        patient.setArchivedAt(Instant.now());
        patient.setArchivedBy(actor);
        patient.setUpdatedBy(actor);
    }

    private Patient require(UUID id) {
        return patients.findById(id).orElseThrow(() -> new NotFoundException("Пациент не найден"));
    }

    private Department resolveDepartment(UUID id) {
        if (id == null) return null;
        Department department = departments.findById(id).orElseThrow(() -> new BadRequestException("Отделение не найдено"));
        if (!department.isActive()) throw new BadRequestException("Нельзя назначить неактивное отделение");
        return department;
    }

    private void validateExternalId(String externalId, UUID currentId) {
        String normalized = normalizeNullable(externalId);
        if (normalized == null) return;
        boolean exists = currentId == null
                ? patients.existsByExternalIdIgnoreCase(normalized)
                : patients.existsByExternalIdIgnoreCaseAndIdNot(normalized, currentId);
        if (exists) throw new ConflictException("Внешний идентификатор (номер медкарты) уже используется");
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null) throw new BadRequestException("Для изменения требуется поле version");
        if (requested != actual) throw new ConflictException("Карточка уже изменена. Обновите данные и повторите операцию");
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) result.add(normalizeTag(tag));
        if (result.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
        return result;
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 50) throw new BadRequestException("Тег длиннее 50 символов");
        return normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Обязательное текстовое поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static String displayName(Patient patient) {
        return patient.getLastName() + " " + patient.getFirstName();
    }
}

