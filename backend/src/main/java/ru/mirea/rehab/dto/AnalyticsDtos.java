package ru.mirea.rehab.dto;

import java.math.BigDecimal;
import java.util.List;

/** DTO аналитической панели реабилитационного центра. */
public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record NamedCount(String name, long count) {
    }

    public record AnalyticsResponse(
            long total,
            long active,
            long archived,
            long requiringAssistance,
            BigDecimal averageScore,
            List<NamedCount> byDepartment,
            List<NamedCount> byStatus,
            List<NamedCount> byRisk) {

        // Алиасы совместимости
        public long withDebt() { return requiringAssistance; }
        public BigDecimal averageGrade() { return averageScore; }
        public List<NamedCount> byGroup() { return byDepartment; }
    }
}

