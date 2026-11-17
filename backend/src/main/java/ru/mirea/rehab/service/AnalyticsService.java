package ru.mirea.rehab.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.rehab.dto.AnalyticsDtos.AnalyticsResponse;
import ru.mirea.rehab.dto.AnalyticsDtos.NamedCount;
import ru.mirea.rehab.repository.PatientRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Формирует компактную аналитическую сводку на стороне базы данных. */
@Service
public class AnalyticsService {
    private final PatientRepository patients;

    public AnalyticsService(PatientRepository patients) {
        this.patients = patients;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        long total = patients.count();
        long active = patients.countByArchivedFalse();
        long archived = patients.countByArchivedTrue();
        long requiringAssistance = patients.countByArchivedFalseAndRequiresAssistanceTrue();
        BigDecimal average = patients.averageScoreForActive();
        if (average != null) average = average.setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(total, active, archived, requiringAssistance, average,
                map(patients.countActiveByDepartment()), map(patients.countActiveByStatus()), map(patients.countActiveByRisk()));
    }

    private static List<NamedCount> map(List<Object[]> rows) {
        return rows.stream().map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue())).toList();
    }
}

