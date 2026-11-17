package ru.mirea.rehab.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.rehab.domain.Department;
import ru.mirea.rehab.dto.DepartmentDtos.DepartmentRequest;
import ru.mirea.rehab.dto.DepartmentDtos.DepartmentResponse;
import ru.mirea.rehab.error.BadRequestException;
import ru.mirea.rehab.error.ConflictException;
import ru.mirea.rehab.error.NotFoundException;
import ru.mirea.rehab.repository.DepartmentRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Прикладные операции над отделениями реабилитационного центра. */
@Service
public class DepartmentService {
    private final DepartmentRepository departments;
    private final AuditService audit;

    public DepartmentService(DepartmentRepository departments, AuditService audit) {
        this.departments = departments;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list(boolean activeOnly) {
        return (activeOnly ? departments.findByActiveTrueOrderByCodeAsc() : departments.findAllByOrderByCodeAsc())
                .stream().map(DepartmentService::toResponse).toList();
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String code = normalizeCode(request.code());
        if (departments.existsByCodeIgnoreCase(code)) throw new ConflictException("Отделение с таким кодом уже существует");
        Department department = new Department(UUID.randomUUID(), code, normalizeNullable(request.name()), request.capacity());
        if (request.active() != null) department.setActive(request.active());
        departments.save(department);
        audit.record("DEPARTMENT_CREATED", "DEPARTMENT", department.getId(), "Создано отделение " + code, Map.of());
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        Department department = require(id);
        if (request.version() == null || request.version() != department.getVersion())
            throw new ConflictException("Отделение уже изменено. Обновите список");
        String code = normalizeCode(request.code());
        departments.findByCodeIgnoreCase(code).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Отделение с таким кодом уже существует");
        });
        department.setCode(code);
        department.setName(normalizeNullable(request.name()));
        department.setCapacity(request.capacity());
        if (request.active() != null) department.setActive(request.active());
        audit.record("DEPARTMENT_UPDATED", "DEPARTMENT", id, "Изменено отделение " + code, Map.of());
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse setActive(UUID id, boolean active, long version) {
        Department department = require(id);
        if (version != department.getVersion()) throw new ConflictException("Отделение уже изменено. Обновите список");
        department.setActive(active);
        audit.record(active ? "DEPARTMENT_ACTIVATED" : "DEPARTMENT_DEACTIVATED", "DEPARTMENT", id,
                (active ? "Активировано" : "Деактивировано") + " отделение " + department.getCode(), Map.of());
        return toResponse(department);
    }

    private Department require(UUID id) {
        return departments.findById(id).orElseThrow(() -> new NotFoundException("Отделение не найдено"));
    }

    private static DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(department.getId(), department.getCode(), department.getName(),
                department.getCapacity(), department.isActive(), department.getCreatedAt(),
                department.getUpdatedAt(), department.getVersion());
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Код отделения обязателен");
        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }
}

