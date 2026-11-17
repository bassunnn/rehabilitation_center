package ru.mirea.rehab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO-контракты заметок в карте пациента. */
public final class NoteDtos {
    private NoteDtos() {
    }

    public record NoteRequest(
            @NotBlank @Size(max = 4000) String body,
            boolean pinned,
            Long version) {
    }

    public record NoteResponse(
            UUID id,
            UUID patientId,
            UUID authorId,
            String authorName,
            String body,
            boolean pinned,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}

