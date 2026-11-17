package ru.mirea.rehab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.rehab.domain.Patient;
import ru.mirea.rehab.domain.PatientStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for patients, smart specifications and compact analytics. */
public interface PatientRepository extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {
    boolean existsByExternalIdIgnoreCase(String externalId);
    boolean existsByExternalIdIgnoreCaseAndIdNot(String externalId, UUID id);
    Optional<Patient> findByExternalIdIgnoreCase(String externalId);
    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndRequiresAssistanceTrue();
    long countByArchivedFalseAndStatus(PatientStatus status);

    @Query("select coalesce(avg(p.rehabilitationScore), 0) from Patient p where p.archived = false and p.rehabilitationScore is not null")
    BigDecimal averageScoreForActive();

    @Query("select coalesce(d.code, 'Без отделения'), count(p) from Patient p left join p.department d " +
           "where p.archived = false group by d.code order by count(p) desc")
    List<Object[]> countActiveByDepartment();

    @Query("select p.status, count(p) from Patient p where p.archived = false group by p.status order by count(p) desc")
    List<Object[]> countActiveByStatus();

    @Query("select p.riskLevel, count(p) from Patient p where p.archived = false group by p.riskLevel order by count(p) desc")
    List<Object[]> countActiveByRisk();
}

