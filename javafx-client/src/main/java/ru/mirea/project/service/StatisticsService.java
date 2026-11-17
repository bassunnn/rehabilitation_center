package ru.mirea.project.service;

import ru.mirea.project.model.Patient;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.model.StatisticsSummary;
import ru.mirea.project.repository.PatientRepository;
import ru.mirea.project.repository.ProcedureRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private final PatientRepository patientRepository;
    private final ProcedureRepository procedureRepository;

    public StatisticsService(PatientRepository patientRepository, ProcedureRepository procedureRepository) {
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
    }

    public StatisticsSummary calculateStatistics() {
        List<Patient> patients = patientRepository.findAll();
        List<Procedure> procedures = procedureRepository.findAll();

        StatisticsSummary summary = new StatisticsSummary();
        summary.setTotalPatients(patients.size());
        summary.setTotalProcedures(procedures.size());

        // Распределение по статусам
        Map<ProcedureStatus, Integer> byStatus = new EnumMap<>(ProcedureStatus.class);
        for (ProcedureStatus status : ProcedureStatus.values()) {
            byStatus.put(status, 0);
        }

        YearMonth currentMonth = YearMonth.now();
        int monthCount = 0;

        for (Procedure proc : procedures) {
            if (proc.getStatus() != null) {
                byStatus.put(proc.getStatus(), byStatus.getOrDefault(proc.getStatus(), 0) + 1);
            }
            if (proc.getProcedureDate() != null) {
                YearMonth procMonth = YearMonth.from(proc.getProcedureDate());
                if (procMonth.equals(currentMonth)) {
                    monthCount++;
                }
            }
        }
        summary.setProceduresByStatus(byStatus);
        summary.setProceduresThisMonth(monthCount);

        // Доля завершенных процедур
        int completed = byStatus.getOrDefault(ProcedureStatus.COMPLETED, 0);
        if (procedures.size() > 0) {
            summary.setCompletionRate((double) completed * 100.0 / procedures.size());
            summary.setAverageProceduresPerPatient(patients.size() > 0 ? (double) procedures.size() / patients.size() : 0.0);
        } else {
            summary.setCompletionRate(0.0);
            summary.setAverageProceduresPerPatient(0.0);
        }

        // Возрастная статистика пациентов
        if (!patients.isEmpty()) {
            int totalAge = 0;
            int minAge = Integer.MAX_VALUE;
            int maxAge = Integer.MIN_VALUE;

            for (Patient patient : patients) {
                int age = patient.getAge();
                totalAge += age;
                if (age < minAge) minAge = age;
                if (age > maxAge) maxAge = age;
            }

            summary.setAveragePatientAge((double) totalAge / patients.size());
            summary.setMinPatientAge(minAge);
            summary.setMaxPatientAge(maxAge);
        } else {
            summary.setAveragePatientAge(0.0);
            summary.setMinPatientAge(0);
            summary.setMaxPatientAge(0);
        }

        return summary;
    }
}

