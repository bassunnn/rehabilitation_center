package ru.mirea.rehab.service;

import org.springframework.stereotype.Component;
import ru.mirea.rehab.domain.PatientStatus;
import ru.mirea.rehab.domain.RiskLevel;

import java.math.BigDecimal;

/**
 * Единообразно рассчитывает уровень риска / приоритет ухода для всех пациентов.
 */
@Component
public class RiskCalculator {

    public RiskLevel calculate(PatientStatus status, BigDecimal rehabilitationScore, boolean requiresAssistance) {
        if (status == PatientStatus.COMPLETED || status == PatientStatus.DISCHARGED) {
            return RiskLevel.NOT_APPLICABLE;
        }

        /*
         * Используется 100-балльная шкала восстановления:
         * - Критический: маломобильный пациент (требует помощи) с низким баллом (<40)
         * - Высокий: требует посторонней помощи либо балл < 50
         * - Средний: балл < 75 или еще не оценен
         * - Низкий: балл >= 75 и не требует постоянной посторонней помощи
         */
        if (requiresAssistance && (rehabilitationScore == null || rehabilitationScore.compareTo(new BigDecimal("40.00")) < 0)) {
            return RiskLevel.CRITICAL;
        }
        if (requiresAssistance || (rehabilitationScore != null && rehabilitationScore.compareTo(new BigDecimal("50.00")) < 0)) {
            return RiskLevel.HIGH;
        }
        if (rehabilitationScore == null || rehabilitationScore.compareTo(new BigDecimal("75.00")) < 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}

