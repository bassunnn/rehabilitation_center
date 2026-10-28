package ru.mirea.project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.project.model.Patient;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.model.StatisticsSummary;
import ru.mirea.project.repository.PatientRepository;
import ru.mirea.project.repository.ProcedureRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsAndExportServiceTest {

    private StatisticsService statisticsService;
    private ExportService exportService;
    private List<Patient> testPatients;
    private List<Procedure> testProcedures;

    @BeforeEach
    void setUp() {
        testPatients = List.of(
                new Patient(1L, "Иван", "Иванов", "+79161111111", "ivan@example.com", LocalDate.of(1990, 1, 1)),
                new Patient(2L, "Ольга", "Смирнова", "+79162222222", "olga@example.com", LocalDate.of(1980, 5, 20))
        );

        testProcedures = List.of(
                new Procedure(1L, 1L, "Массаж", "Массаж спины", LocalDate.now(), ProcedureStatus.COMPLETED),
                new Procedure(2L, 1L, "ЛФК", "Лечебная физкультура", LocalDate.now(), ProcedureStatus.PLANNED),
                new Procedure(3L, 2L, "Электрофорез", "Физиотерапия", LocalDate.now(), ProcedureStatus.IN_PROGRESS)
        );

        PatientRepository patientRepository = new PatientRepository() {
            @Override
            public List<Patient> findAll() {
                return testPatients;
            }
        };

        ProcedureRepository procedureRepository = new ProcedureRepository() {
            @Override
            public List<Procedure> findAll() {
                return testProcedures;
            }
        };

        statisticsService = new StatisticsService(patientRepository, procedureRepository);
        exportService = new ExportService();
    }

    @Test
    @DisplayName("Корректный расчет 5+ статистических показателей")
    void testStatisticsCalculation() {
        StatisticsSummary summary = statisticsService.calculateStatistics();

        // 1. Общее количество пациентов
        assertThat(summary.getTotalPatients()).isEqualTo(2);

        // 2. Общее количество процедур
        assertThat(summary.getTotalProcedures()).isEqualTo(3);

        // 3. Распределение по статусам
        assertThat(summary.getCountForStatus(ProcedureStatus.COMPLETED)).isEqualTo(1);
        assertThat(summary.getCountForStatus(ProcedureStatus.PLANNED)).isEqualTo(1);
        assertThat(summary.getCountForStatus(ProcedureStatus.IN_PROGRESS)).isEqualTo(1);
        assertThat(summary.getCountForStatus(ProcedureStatus.CANCELLED)).isEqualTo(0);

        // 4. Доля завершенных процедур
        assertThat(summary.getCompletionRate()).isGreaterThan(33.0).isLessThan(34.0);

        // 5. Средний возраст пациентов
        assertThat(summary.getAveragePatientAge()).isGreaterThan(30.0);
        assertThat(summary.getMinPatientAge()).isLessThanOrEqualTo(summary.getMaxPatientAge());

        // 6. Процедур в текущем месяце
        assertThat(summary.getProceduresThisMonth()).isEqualTo(3);
    }

    @Test
    @DisplayName("Экспорт данных в Excel (.xlsx)")
    void testExportExcel(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test_export.xlsx").toFile();
        exportService.exportToExcel(file, testPatients, testProcedures);

        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Экспорт пациентов в CSV")
    void testExportPatientsCsv(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test_patients.csv").toFile();
        exportService.exportPatientsToCsv(file, testPatients);

        assertThat(file).exists();
        String content = Files.readString(file.toPath());
        assertThat(content).contains("Иванов").contains("Смирнова");
    }

    @Test
    @DisplayName("Экспорт процедур в CSV")
    void testExportProceduresCsv(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test_procedures.csv").toFile();
        exportService.exportProceduresToCsv(file, testProcedures);

        assertThat(file).exists();
        String content = Files.readString(file.toPath());
        assertThat(content).contains("Массаж").contains("Электрофорез");
    }
}

