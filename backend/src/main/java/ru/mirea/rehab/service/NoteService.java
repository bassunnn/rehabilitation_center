package ru.mirea.rehab.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.rehab.domain.Patient;
import ru.mirea.rehab.domain.PatientNote;
import ru.mirea.rehab.domain.Role;
import ru.mirea.rehab.domain.UserAccount;
import ru.mirea.rehab.dto.NoteDtos.NoteRequest;
import ru.mirea.rehab.dto.NoteDtos.NoteResponse;
import ru.mirea.rehab.error.ConflictException;
import ru.mirea.rehab.error.NotFoundException;
import ru.mirea.rehab.repository.PatientNoteRepository;
import ru.mirea.rehab.repository.PatientRepository;
import ru.mirea.rehab.security.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Управляет заметками в карте пациента и авторскими правами на их изменение. */
@Service
public class NoteService {
    private final PatientNoteRepository notes;
    private final PatientRepository patients;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public NoteService(PatientNoteRepository notes, PatientRepository patients,
                       CurrentUserService currentUser, AuditService audit) {
        this.notes = notes;
        this.patients = patients;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID patientId) {
        requirePatient(patientId);
        return notes.findByPatientIdOrderByPinnedDescCreatedAtDesc(patientId).stream().map(NoteService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID patientId, NoteRequest request) {
        Patient patient = requirePatient(patientId);
        UserAccount actor = currentUser.requireEntity();
        PatientNote note = new PatientNote(UUID.randomUUID(), patient, actor, request.body().trim(), request.pinned());
        notes.save(note);
        audit.record("NOTE_CREATED", "PATIENT_NOTE", note.getId(), "Добавлена заметка в карту пациента", Map.of("patientId", patientId));
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID patientId, UUID noteId, NoteRequest request) {
        PatientNote note = requireNote(patientId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        if (request.version() == null || request.version() != note.getVersion())
            throw new ConflictException("Заметка уже изменена. Обновите данные");
        note.setBody(request.body().trim());
        note.setPinned(request.pinned());
        audit.record("NOTE_UPDATED", "PATIENT_NOTE", noteId, "Изменена заметка", Map.of("patientId", patientId));
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID patientId, UUID noteId) {
        PatientNote note = requireNote(patientId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        notes.delete(note);
        audit.record("NOTE_DELETED", "PATIENT_NOTE", noteId, "Удалена заметка", Map.of("patientId", patientId));
    }

    private Patient requirePatient(UUID id) {
        return patients.findById(id).orElseThrow(() -> new NotFoundException("Пациент не найден"));
    }

    private PatientNote requireNote(UUID patientId, UUID noteId) {
        PatientNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        if (!note.getPatient().getId().equals(patientId)) throw new NotFoundException("Заметка не найдена");
        return note;
    }

    private static void assertOwnerOrAdmin(PatientNote note, UserAccount actor) {
        if (!note.getAuthor().getId().equals(actor.getId()) && !actor.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Только автор заметки или администратор может ее изменить");
        }
    }

    private static NoteResponse toResponse(PatientNote note) {
        return new NoteResponse(note.getId(), note.getPatient().getId(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(), note.isPinned(),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}

