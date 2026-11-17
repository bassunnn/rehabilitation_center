package ru.mirea.rehab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO-контракты отделений реабилитационного центра. */
public final class DepartmentDtos {
    private DepartmentDtos() {
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 50) String code,
            @Size(max = 200) String name,
            @Min(1) @Max(1000) Integer capacity,
            Boolean active,
            Long version) {
    }

    public record DepartmentResponse(
            UUID id,
            String code,
            String name,
            Integer capacity,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}

