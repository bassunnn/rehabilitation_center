package ru.mirea.rehab.filter;

import org.springframework.stereotype.Component;
import ru.mirea.rehab.domain.PatientStatus;
import ru.mirea.rehab.domain.RiskLevel;
import ru.mirea.rehab.error.BadRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает человекочитаемый фильтр в типобезопасную модель.
 *
 * <p>Исходная строка фильтра не объединяется с SQL/JPQL. Парсер принимает только
 * известные команды, а Criteria API передает значения через параметры.</p>
 */
@Component
public class SmartFilterParser {
    private static final Pattern TOKEN = Pattern.compile("\\\"([^\\\"]*)\\\"|(\\S+)");
    private static final Pattern SCORE = Pattern.compile("^(?:балл|score|progress|grade)(<=|>=|=|<|>)([0-9]+(?:[.,][0-9]{1,2})?)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public PatientFilter parse(String source) {
        List<String> free = new ArrayList<>();
        String department = null;
        PatientStatus status = null;
        Boolean requiresAssistance = null;
        RiskLevel risk = null;
        String tag = null;
        Boolean archived = false;
        List<PatientFilter.ScoreConstraint> scores = new ArrayList<>();

        if (source == null || source.isBlank()) {
            return new PatientFilter("", null, null, null, null, null, false, List.of());
        }
        if (source.length() > 500) throw new BadRequestException("Строка фильтра слишком длинная");

        Matcher matcher = TOKEN.matcher(source.trim());
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String token = raw.trim();
            if (token.isEmpty()) continue;
            String lower = token.toLowerCase(Locale.ROOT);

            Matcher scoreMatcher = SCORE.matcher(lower);
            if (scoreMatcher.matches()) {
                BigDecimal value = new BigDecimal(scoreMatcher.group(2).replace(',', '.'));
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100.00")) > 0) {
                    throw new BadRequestException("Значение показателя должно быть от 0 до 100");
                }
                scores.add(new PatientFilter.ScoreConstraint(operator(scoreMatcher.group(1)), value));
                continue;
            }

            int colon = token.indexOf(':');
            if (colon > 0) {
                String key = lower.substring(0, colon);
                String value = stripQuotes(token.substring(colon + 1).trim());
                String normalized = value.toLowerCase(Locale.ROOT);
                switch (key) {
                    case "отделение", "department", "группа", "group" -> department = value;
                    case "статус", "status" -> status = status(normalized);
                    case "помощь", "assistance", "долг", "debt" -> requiresAssistance = booleanValue(normalized, "помощь");
                    case "риск", "risk", "приоритет", "priority" -> risk = risk(normalized);
                    case "тег", "tag" -> tag = value;
                    case "архив", "archived" -> archived = archivedValue(normalized);
                    default -> free.add(token);
                }
            } else {
                free.add(token);
            }
        }
        return new PatientFilter(String.join(" ", free), blankToNull(department), status, requiresAssistance, risk,
                blankToNull(tag), archived, List.copyOf(scores));
    }

    private static PatientFilter.Operator operator(String value) {
        return switch (value) {
            case "<" -> PatientFilter.Operator.LT;
            case "<=" -> PatientFilter.Operator.LTE;
            case "=" -> PatientFilter.Operator.EQ;
            case ">=" -> PatientFilter.Operator.GTE;
            case ">" -> PatientFilter.Operator.GT;
            default -> throw new BadRequestException("Неизвестный оператор сравнения");
        };
    }

    private static PatientStatus status(String value) {
        return switch (value.replace('-', '_')) {
            case "наблюдение", "на_обследовании", "under_observation", "enrolled" -> PatientStatus.UNDER_OBSERVATION;
            case "реабилитация", "проходит_реабилитацию", "in_rehabilitation", "academic_leave" -> PatientStatus.IN_REHABILITATION;
            case "завершена", "курс_завершен", "completed", "graduated" -> PatientStatus.COMPLETED;
            case "выписан", "discharged", "expelled" -> PatientStatus.DISCHARGED;
            default -> throw new BadRequestException("Неизвестный статус: " + value);
        };
    }

    private static RiskLevel risk(String value) {
        return switch (value) {
            case "не_применяется", "неприменимо", "not_applicable", "n/a" -> RiskLevel.NOT_APPLICABLE;
            case "низкий", "low" -> RiskLevel.LOW;
            case "средний", "medium" -> RiskLevel.MEDIUM;
            case "высокий", "high" -> RiskLevel.HIGH;
            case "критический", "critical" -> RiskLevel.CRITICAL;
            default -> throw new BadRequestException("Неизвестный уровень риска: " + value);
        };
    }

    private static Boolean booleanValue(String value, String field) {
        return switch (value) {
            case "да", "yes", "true", "1" -> true;
            case "нет", "no", "false", "0" -> false;
            default -> throw new BadRequestException("Поле «" + field + "» принимает да/нет");
        };
    }

    private static Boolean archivedValue(String value) {
        if (value.equals("все") || value.equals("all")) return null;
        return booleanValue(value, "архив");
    }

    private static String stripQuotes(String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

