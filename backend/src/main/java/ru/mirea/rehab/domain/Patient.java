package ru.mirea.rehab.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Основная доменная сущность: Пациент реабилитационного центра.
 *
 * <p>Обычное удаление реализовано через архивирование, благодаря чему история
 * болезни и связанные назначения остаются доступными для восстановления.</p>
 */
@Entity
@Table(name = "patient")
public class Patient extends AuditedVersionedEntity {

    @Id
    private UUID id;

    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PatientStatus status = PatientStatus.UNDER_OBSERVATION;

    @Column(name = "rehabilitation_score", precision = 5, scale = 2)
    private BigDecimal rehabilitationScore;

    @Column(name = "requires_assistance", nullable = false)
    private boolean requiresAssistance;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private UserAccount archivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserAccount updatedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "patient_tag", joinColumns = @JoinColumn(name = "patient_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected Patient() {
    }

    public Patient(UUID id, String firstName, String lastName, UserAccount creator) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdBy = creator;
        this.updatedBy = creator;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public PatientStatus getStatus() { return status; }
    public void setStatus(PatientStatus status) { this.status = status; }
    public BigDecimal getRehabilitationScore() { return rehabilitationScore; }
    public void setRehabilitationScore(BigDecimal rehabilitationScore) { this.rehabilitationScore = rehabilitationScore; }
    public boolean isRequiresAssistance() { return requiresAssistance; }
    public void setRequiresAssistance(boolean requiresAssistance) { this.requiresAssistance = requiresAssistance; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public UserAccount getArchivedBy() { return archivedBy; }
    public void setArchivedBy(UserAccount archivedBy) { this.archivedBy = archivedBy; }
    public UserAccount getCreatedBy() { return createdBy; }
    public UserAccount getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UserAccount updatedBy) { this.updatedBy = updatedBy; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = new LinkedHashSet<>(tags); }
}

