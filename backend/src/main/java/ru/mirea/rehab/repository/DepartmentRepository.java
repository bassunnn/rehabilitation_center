package ru.mirea.rehab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.rehab.domain.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for departments. */
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
    Optional<Department> findByCodeIgnoreCase(String code);
    List<Department> findByActiveTrueOrderByCodeAsc();
    List<Department> findAllByOrderByCodeAsc();
}

