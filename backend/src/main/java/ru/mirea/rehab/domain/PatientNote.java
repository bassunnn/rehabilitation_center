package ru.mirea.rehab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Заметка / комментарий врача в карте пациента.
 */
@Entity
@Table(name = "patient_note")
public class PatientNote extends AuditedVersionedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean pinned;

    protected PatientNote() {
    }

    public PatientNote(UUID id, Patient patient, UserAccount author, String body, boolean pinned) {
        this.id = id;
        this.patient = patient;
        this.author = author;
        this.body = body;
        this.pinned = pinned;
    }

    public UUID getId() { return id; }
    public Patient getPatient() { return patient; }
    public UserAccount getAuthor() { return author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}

