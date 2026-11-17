package ru.mirea.rehab.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.rehab.dto.DepartmentDtos.DepartmentRequest;
import ru.mirea.rehab.dto.DepartmentDtos.DepartmentResponse;
import ru.mirea.rehab.service.DepartmentService;

import java.util.List;
import java.util.UUID;

/** REST-контур отделений реабилитационного центра (справочная сущность). */
@RestController
@RequestMapping({"/api/departments", "/api/references", "/api/groups"})
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {
    private final DepartmentService departments;

    public DepartmentController(DepartmentService departments) {
        this.departments = departments;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','DOCTOR','ADMIN')")
    public List<DepartmentResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return departments.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(201).body(departments.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse update(@PathVariable UUID id, @Valid @RequestBody DepartmentRequest request) {
        return departments.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse setActive(@PathVariable UUID id, @RequestParam boolean active, @RequestParam long version) {
        return departments.setActive(id, active, version);
    }
}

