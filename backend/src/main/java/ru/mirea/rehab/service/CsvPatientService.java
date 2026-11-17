package ru.mirea.rehab.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.rehab.config.AppProperties;
import ru.mirea.rehab.domain.Department;
import ru.mirea.rehab.domain.Patient;
import ru.mirea.rehab.domain.PatientStatus;
import ru.mirea.rehab.domain.UserAccount;
import ru.mirea.rehab.dto.ImportDtos.ImportMode;
import ru.mirea.rehab.dto.ImportDtos.ImportPreview;
import ru.mirea.rehab.dto.ImportDtos.ImportResult;
import ru.mirea.rehab.dto.ImportDtos.RowIssue;
import ru.mirea.rehab.dto.PatientDtos.PatientRequest;
import ru.mirea.rehab.dto.PatientDtos.PatientResponse;
import ru.mirea.rehab.error.BadRequestException;
import ru.mirea.rehab.filter.PatientSpecifications;
import ru.mirea.rehab.filter.SmartFilterParser;
import ru.mirea.rehab.repository.DepartmentRepository;
import ru.mirea.rehab.repository.PatientRepository;
import ru.mirea.rehab.security.CurrentUserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Защищенный CSV import/export.
 *
 * <p>Предварительная проверка отделена от импорта: ошибки видны до записи в базу.
 * При экспорте нейтрализуются значения, которые Excel может воспринять как формулы.</p>
 */
@Service
public class CsvPatientService {
    private static final String[] HEADERS = {
            "externalId", "lastName", "firstName", "middleName", "email", "phone",
            "birthDate", "department", "status", "rehabilitationScore", "requiresAssistance", "tags"
    };

    private final PatientRepository patients;
    private final DepartmentRepository departments;
    private final PatientService patientService;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final PatientMapper mapper;
    private final Validator validator;
    private final AppProperties properties;
    private final AuditService audit;

    public CsvPatientService(PatientRepository patients, DepartmentRepository departments,
                             PatientService patientService, CurrentUserService currentUser,
                             SmartFilterParser parser, PatientMapper mapper, Validator validator,
                             AppProperties properties, AuditService audit) {
        this.patients = patients;
        this.departments = departments;
        this.patientService = patientService;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.validator = validator;
        this.properties = properties;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        ParsedFile parsed = parse(file, mode == null ? ImportMode.CREATE_ONLY : mode);
        return new ImportPreview(parsed.totalRows(), parsed.rows().size(), parsed.issues().size(), parsed.issues());
    }

    @Transactional
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        ImportMode effectiveMode = mode == null ? ImportMode.CREATE_ONLY : mode;
        ParsedFile parsed = parse(file, effectiveMode);
        UserAccount actor = currentUser.requireEntity();
        int created = 0;
        int updated = 0;
        int skipped = parsed.issues().size();
        List<RowIssue> runtimeIssues = new ArrayList<>(parsed.issues());

        for (ParsedRow row : parsed.rows()) {
            try {
                PatientRequest req = row.request();
                Patient existing = req.externalId() == null ? null
                        : patients.findByExternalIdIgnoreCase(req.externalId()).orElse(null);
                if (existing != null) {
                    if (effectiveMode == ImportMode.UPSERT_BY_EXTERNAL_ID) {
                        existing.setFirstName(req.firstName());
                        existing.setLastName(req.lastName());
                        patientService.apply(existing, req, actor);
                        updated++;
                    } else {
                        skipped++;
                        runtimeIssues.add(new RowIssue(row.rowNumber(), "externalId", "Пациент уже существует"));
                    }
                } else {
                    Patient patient = new Patient(UUID.randomUUID(), req.firstName(), req.lastName(), actor);
                    patientService.apply(patient, req, actor);
                    patients.save(patient);
                    created++;
                }
            } catch (RuntimeException ex) {
                skipped++;
                runtimeIssues.add(new RowIssue(row.rowNumber(), "general", safeMessage(ex)));
            }
        }
        audit.record("CSV_IMPORTED", "PATIENT", null,
                "Импорт CSV: создано " + created + ", обновлено " + updated + ", пропущено " + skipped,
                Map.of("created", created, "updated", updated, "skipped", skipped));
        return new ImportResult(parsed.totalRows(), created, updated, skipped, runtimeIssues);
    }

    @Transactional(readOnly = true)
    public void export(String query, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build());
        int page = 0;
        int size = 500;
        while (true) {
            List<PatientResponse> batch = patients.findAll(
                            PatientSpecifications.from(parser.parse(query)),
                            PageRequest.of(page, size, Sort.by("lastName").ascending()))
                    .map(mapper::toResponse).getContent();
            if (batch.isEmpty()) break;
            for (PatientResponse s : batch) {
                printer.printRecord(
                        safeCell(s.externalId()),
                        safeCell(s.lastName()),
                        safeCell(s.firstName()),
                        safeCell(s.middleName()),
                        safeCell(s.email()),
                        safeCell(s.phone()),
                        safeCell(s.birthDate()),
                        safeCell(s.departmentCode()),
                        safeCell(s.status()),
                        safeCell(s.rehabilitationScore()),
                        safeCell(s.requiresAssistance()),
                        safeCell(s.tags() == null ? "" : String.join("|", s.tags()))
                );
            }
            if (batch.size() < size) break;
            page++;
        }
        printer.flush();
    }

    private ParsedFile parse(MultipartFile file, ImportMode mode) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Файл не передан или пуст");
        if (file.getSize() > 5 * 1024 * 1024) throw new BadRequestException("Размер CSV превышает 5 МБ");
        List<ParsedRow> rows = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        int count = 0;
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(true).build();
            try (CSVParser csv = format.parse(reader)) {
                validateHeaders(csv.getHeaderMap());
                for (CSVRecord record : csv) {
                    count++;
                    if (count > properties.importLimits().maxRows()) {
                        throw new BadRequestException("Превышен лимит строк CSV: " + properties.importLimits().maxRows());
                    }
                    try {
                        ParsedRow row = parseRow(record, count);
                        Set<ConstraintViolation<PatientRequest>> violations = validator.validate(row.request());
                        if (violations.isEmpty()) {
                            rows.add(row);
                        } else {
                            long rowNum = row.rowNumber();
                            violations.forEach(v -> issues.add(new RowIssue(rowNum, v.getPropertyPath().toString(), v.getMessage())));
                        }
                    } catch (RuntimeException ex) {
                        issues.add(new RowIssue(count, "format", safeMessage(ex)));
                    }
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось прочитать CSV-файл");
        }
        return new ParsedFile(count, rows, issues);
    }

    private ParsedRow parseRow(CSVRecord record, long rowNumber) {
        String externalId = nullable(record, "externalId");
        String deptCode = getAnyField(record, "department", "group");
        Department dept = deptCode == null ? null : departments.findByCodeIgnoreCase(deptCode)
                .orElseThrow(() -> new BadRequestException("Отделение не найдено: " + deptCode));
        if (dept != null && !dept.isActive()) throw new BadRequestException("Отделение неактивно: " + deptCode);

        PatientRequest request = new PatientRequest(
                externalId,
                required(record, "firstName"),
                required(record, "lastName"),
                nullable(record, "middleName"),
                nullable(record, "email"),
                nullable(record, "phone"),
                parseDate(nullable(record, "birthDate")),
                dept == null ? null : dept.getId(),
                parseStatus(required(record, "status")),
                parseScore(getAnyField(record, "rehabilitationScore", "averageGrade")),
                parseBoolean(getAnyField(record, "requiresAssistance", "hasDebt")),
                parseTags(nullable(record, "tags")),
                null);
        return new ParsedRow(rowNumber, request);
    }

    private static void validateHeaders(Map<String, Integer> actual) {
        for (String header : HEADERS) {
            if (!actual.containsKey(header)) {
                // Compatibility fallback: group / averageGrade / hasDebt
                if (header.equals("department") && actual.containsKey("group")) continue;
                if (header.equals("rehabilitationScore") && actual.containsKey("averageGrade")) continue;
                if (header.equals("requiresAssistance") && actual.containsKey("hasDebt")) continue;
                throw new BadRequestException("Отсутствует обязательная колонка: " + header);
            }
        }
    }

    private static String getAnyField(CSVRecord record, String primary, String fallback) {
        if (record.isMapped(primary)) {
            String val = nullable(record, primary);
            if (val != null) return val;
        }
        if (record.isMapped(fallback)) {
            return nullable(record, fallback);
        }
        return null;
    }

    private static String required(CSVRecord record, String field) {
        String value = nullable(record, field);
        if (value == null) throw new BadRequestException("Не заполнено поле " + field);
        return value;
    }

    private static String nullable(CSVRecord record, String field) {
        if (!record.isMapped(field)) return null;
        String value = record.get(field);
        if (value == null) return null;
        value = value.replace("\ufeff", "").trim();
        return value.isEmpty() ? null : value;
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new BadRequestException("Дата должна иметь формат YYYY-MM-DD"); }
    }

    private static BigDecimal parseScore(String value) {
        if (value == null) return null;
        try { return new BigDecimal(value.replace(',', '.')); }
        catch (NumberFormatException ex) { throw new BadRequestException("Некорректный показатель rehabilitationScore"); }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "да" -> true;
            case "false", "0", "no", "нет" -> false;
            default -> throw new BadRequestException("Поле принимает true/false или да/нет");
        };
    }

    private static PatientStatus parseStatus(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "under_observation", "наблюдение", "на_обследовании", "enrolled" -> PatientStatus.UNDER_OBSERVATION;
            case "in_rehabilitation", "реабилитация", "проходит_реабилитацию", "academic_leave" -> PatientStatus.IN_REHABILITATION;
            case "completed", "завершена", "курс_завершен", "graduated" -> PatientStatus.COMPLETED;
            case "discharged", "выписан", "expelled" -> PatientStatus.DISCHARGED;
            default -> throw new BadRequestException("Неизвестный статус: " + value);
        };
    }

    private static Set<String> parseTags(String value) {
        if (value == null) return Set.of();
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Arrays.stream(value.split("\\|")).map(String::trim).filter(v -> !v.isEmpty())
                .map(v -> v.toLowerCase(Locale.ROOT)).forEach(tags::add);
        return tags;
    }

    private static String safeCell(Object value) {
        if (value == null) return "";
        String cell = String.valueOf(value);
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) return "'" + cell;
        return cell;
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Некорректная строка" : message;
    }

    private record ParsedRow(long rowNumber, PatientRequest request) { }
    private record ParsedFile(int totalRows, List<ParsedRow> rows, List<RowIssue> issues) { }
}

