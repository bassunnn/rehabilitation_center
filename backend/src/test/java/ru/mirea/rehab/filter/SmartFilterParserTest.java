package ru.mirea.rehab.filter;

import org.junit.jupiter.api.Test;
import ru.mirea.rehab.domain.PatientStatus;
import ru.mirea.rehab.domain.RiskLevel;
import ru.mirea.rehab.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет русские и английские команды умного фильтра. */
class SmartFilterParserTest {
    private final SmartFilterParser parser = new SmartFilterParser();

    @Test
    void parsesMixedSmartQuery() {
        PatientFilter result = parser.parse("Иванов отделение:НЕВРО балл<45,5 помощь:да риск:высокий тег:инсульт");
        assertThat(result.freeText()).isEqualTo("Иванов");
        assertThat(result.department()).isEqualTo("НЕВРО");
        assertThat(result.status()).isNull();
        assertThat(result.requiresAssistance()).isTrue();
        assertThat(result.risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.tag()).isEqualTo("инсульт");
        assertThat(result.archived()).isFalse();
        assertThat(result.scores()).hasSize(1);
    }

    @Test
    void supportsQuotedFreeTextAndStatus() {
        PatientFilter result = parser.parse("\"Иван Иванов\" status:in_rehabilitation archived:all");
        assertThat(result.freeText()).isEqualTo("Иван Иванов");
        assertThat(result.status()).isEqualTo(PatientStatus.IN_REHABILITATION);
        assertThat(result.archived()).isNull();
    }

    @Test
    void rejectsOutOfRangeScore() {
        assertThatThrownBy(() -> parser.parse("балл>105"))
                .isInstanceOf(BadRequestException.class);
    }
}

