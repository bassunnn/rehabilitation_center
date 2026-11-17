package ru.mirea.project.service;

import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.repository.ProcedureRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ProcedureService {
    private final ProcedureRepository procedureRepository;
    private final ValidationService validationService;

    public ProcedureService(ProcedureRepository procedureRepository, ValidationService validationService) {
        this.procedureRepository = procedureRepository;
        this.validationService = validationService;
    }

    public List<Procedure> getAllProcedures() {
        return procedureRepository.findAll();
    }

    public Optional<Procedure> getProcedureById(Long id) {
        return procedureRepository.findById(id);
    }

    public List<Procedure> getProceduresByPatientId(Long patientId) {
        if (patientId == null) {
            return List.of();
        }
        return procedureRepository.findByPatientId(patientId);
    }

    public Procedure createProcedure(Procedure procedure) {
        validationService.validateProcedure(procedure);
        return procedureRepository.save(procedure);
    }

    public void updateProcedure(Procedure procedure) {
        if (procedure.getId() == null) {
            throw new IllegalArgumentException("Нельзя обновить процедуру без идентификатора.");
        }
        validationService.validateProcedure(procedure);
        procedureRepository.update(procedure);
    }

    public void deleteProcedure(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Идентификатор процедуры не может быть пустым.");
        }
        boolean deleted = procedureRepository.deleteById(id);
        if (!deleted) {
            throw new EntityNotFoundException("Процедура с ID " + id + " не найдена для удаления.");
        }
    }

    public List<Procedure> searchAndFilter(String query, ProcedureStatus status, LocalDate fromDate, LocalDate toDate) {
        return procedureRepository.searchAndFilter(query, status, fromDate, toDate);
    }
}

