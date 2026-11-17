package ru.mirea.rehab.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.rehab.dto.NoteDtos.NoteRequest;
import ru.mirea.rehab.dto.NoteDtos.NoteResponse;
import ru.mirea.rehab.service.NoteService;

import java.util.List;
import java.util.UUID;

/** REST-контур медицинских заметок в картах пациентов. */
@RestController
@RequestMapping({"/api/patients/{patientId}/notes", "/api/students/{patientId}/notes", "/api/items/{patientId}/notes"})
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) {
        this.notes = notes;
    }

    @GetMapping
    public List<NoteResponse> list(@PathVariable UUID patientId) {
        return notes.list(patientId);
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@PathVariable UUID patientId,
                                               @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(patientId, request));
    }

    @PutMapping("/{noteId}")
    public NoteResponse update(@PathVariable UUID patientId, @PathVariable UUID noteId,
                               @Valid @RequestBody NoteRequest request) {
        return notes.update(patientId, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID noteId) {
        notes.delete(patientId, noteId);
        return ResponseEntity.noContent().build();
    }
}

