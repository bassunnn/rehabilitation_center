package ru.mirea.rehab.service;

import org.springframework.stereotype.Component;
import ru.mirea.rehab.domain.Patient;
import ru.mirea.rehab.dto.PatientDtos.PatientResponse;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Преобразует entity в публичный DTO внутри активной транзакции. */
@Component
public class PatientMapper {
    public PatientResponse toResponse(Patient patient) {
        String fullName = Stream.of(patient.getLastName(), patient.getFirstName(), patient.getMiddleName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        return new PatientResponse(
                patient.getId(),
                patient.getExternalId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getMiddleName(),
                fullName,
                patient.getEmail(),
                patient.getPhone(),
                patient.getBirthDate(),
                patient.getDepartment() == null ? null : patient.getDepartment().getId(),
                patient.getDepartment() == null ? null : patient.getDepartment().getCode(),
                patient.getStatus(),
                patient.getRehabilitationScore(),
                patient.isRequiresAssistance(),
                patient.getRiskLevel(),
                patient.isArchived(),
                patient.getArchivedAt(),
                Set.copyOf(patient.getTags()),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.getVersion()
        );
    }
}

