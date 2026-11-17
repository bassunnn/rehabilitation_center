package ru.mirea.rehab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.rehab.domain.PatientNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for patient notes. */
public interface PatientNoteRepository extends JpaRepository<PatientNote, UUID> {
    List<PatientNote> findByPatientIdOrderByPinnedDescCreatedAtDesc(UUID patientId);
    Optional<PatientNote> findByIdAndPatientId(UUID id, UUID patientId);
    void deleteAllByPatientId(UUID patientId);
}

