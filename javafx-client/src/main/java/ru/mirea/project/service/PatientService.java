package ru.mirea.project.service;

import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Patient;
import ru.mirea.project.repository.PatientRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PatientService {
    private final PatientRepository patientRepository;
    private final ValidationService validationService;

    public PatientService(PatientRepository patientRepository, ValidationService validationService) {
        this.patientRepository = patientRepository;
        this.validationService = validationService;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Patient createPatient(Patient patient) {
        validationService.validatePatient(patient);
        return patientRepository.save(patient);
    }

    public void updatePatient(Patient patient) {
        if (patient.getId() == null) {
            throw new IllegalArgumentException("Нельзя обновить пациента без идентификатора.");
        }
        validationService.validatePatient(patient);
        patientRepository.update(patient);
    }

    public void deletePatient(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Идентификатор пациента не может быть пустым.");
        }
        boolean deleted = patientRepository.deleteById(id);
        if (!deleted) {
            throw new EntityNotFoundException("Пациент с ID " + id + " не найден для удаления.");
        }
    }

    public List<Patient> searchPatients(String query) {
        return patientRepository.search(query);
    }

    public List<Patient> filterPatientsByAge(Integer minAge, Integer maxAge) {
        List<Patient> all = patientRepository.findAll();
        return all.stream()
                .filter(p -> (minAge == null || p.getAge() >= minAge))
                .filter(p -> (maxAge == null || p.getAge() <= maxAge))
                .collect(Collectors.toList());
    }
}

