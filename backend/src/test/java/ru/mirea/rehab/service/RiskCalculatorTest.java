package ru.mirea.rehab.service;

import org.junit.jupiter.api.Test;
import ru.mirea.rehab.domain.PatientStatus;
import ru.mirea.rehab.domain.RiskLevel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Фиксирует бизнес-правила расчета риска/приоритета пациента. */
class RiskCalculatorTest {
    private final RiskCalculator calculator = new RiskCalculator();

    @Test
    void lowScoreAndAssistanceAreCritical() {
        assertThat(calculator.calculate(PatientStatus.IN_REHABILITATION, new BigDecimal("35.00"), true))
                .isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void assistanceAloneIsHigh() {
        assertThat(calculator.calculate(PatientStatus.IN_REHABILITATION, new BigDecimal("65.00"), true))
                .isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void goodScoreWithoutAssistanceIsLow() {
        assertThat(calculator.calculate(PatientStatus.IN_REHABILITATION, new BigDecimal("85.00"), false))
                .isEqualTo(RiskLevel.LOW);
    }

    @Test
    void dischargedPatientIsNotApplicable() {
        assertThat(calculator.calculate(PatientStatus.DISCHARGED, new BigDecimal("30.00"), true))
                .isEqualTo(RiskLevel.NOT_APPLICABLE);
    }
}

