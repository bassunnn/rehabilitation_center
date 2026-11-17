package ru.mirea.project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.Patient;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.repository.PatientRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        // Mocking repo for unit test
        PatientRepository patientRepository = new PatientRepository() {
            @Override
            public boolean existsByEmail(String email, Long excludeId) {
                return "exists@example.com".equalsIgnoreCase(email);
            }

            @Override
            public boolean existsByPhone(String phone, Long excludeId) {
                return "+79990000000".equals(phone);
            }
        };

        validationService = new ValidationService(patientRepository);
    }

    @Test
    @DisplayName("Валидация корректного пациента проходит успешно")
    void testValidPatient() {
        Patient patient = new Patient("Иван", "Петров", "+79161112233", "ivan.petrov@test.ru", LocalDate.of(1990, 5, 15));
        validationService.validatePatient(patient);
        assertThat(patient.getAge()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Ошибка валидации: пустая фамилия")
    void testEmptyLastName() {
        Patient patient = new Patient("Иван", "  ", "+79161112233", "ivan@test.ru", LocalDate.of(1990, 5, 15));
        assertThatThrownBy(() -> validationService.validatePatient(patient))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Фамилия");
    }

    @Test
    @DisplayName("Ошибка валидации: некорректный email")
    void testInvalidEmail() {
        Patient patient = new Patient("Иван", "Петров", "+79161112233", "invalid-email", LocalDate.of(1990, 5, 15));
        assertThatThrownBy(() -> validationService.validatePatient(patient))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("почта");
    }

    @Test
    @DisplayName("Ошибка валидации: дубликат email")
    void testDuplicateEmail() {
        Patient patient = new Patient("Иван", "Петров", "+79161112233", "exists@example.com", LocalDate.of(1990, 5, 15));
        assertThatThrownBy(() -> validationService.validatePatient(patient))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    @DisplayName("Ошибка валидации: дата рождения в будущем")
    void testFutureBirthDate() {
        Patient patient = new Patient("Иван", "Петров", "+79161112233", "ivan@test.ru", LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> validationService.validatePatient(patient))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("будущем");
    }

    @Test
    @DisplayName("Валидация корректной процедуры")
    void testValidProcedure() {
        Procedure procedure = new Procedure(1L, "Массаж", "Лечебный массаж", LocalDate.now(), ProcedureStatus.PLANNED);
        validationService.validateProcedure(procedure);
    }

    @Test
    @DisplayName("Ошибка валидации процедуры: пациент не выбран")
    void testProcedureWithoutPatient() {
        Procedure procedure = new Procedure(null, "Массаж", "Лечебный массаж", LocalDate.now(), ProcedureStatus.PLANNED);
        assertThatThrownBy(() -> validationService.validateProcedure(procedure))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("пациента");
    }
}

