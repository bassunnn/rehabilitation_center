package ru.mirea.rehab.filter;

import ru.mirea.rehab.domain.PatientStatus;
import ru.mirea.rehab.domain.RiskLevel;

import java.math.BigDecimal;
import java.util.List;

/** Результат безопасного разбора строки умного фильтра пациентов. */
public record PatientFilter(
        String freeText,
        String department,
        PatientStatus status,
        Boolean requiresAssistance,
        RiskLevel risk,
        String tag,
        Boolean archived,
        List<ScoreConstraint> scores) {

    public enum Operator { LT, LTE, EQ, GTE, GT }
    public record ScoreConstraint(Operator operator, BigDecimal value) { }
}

