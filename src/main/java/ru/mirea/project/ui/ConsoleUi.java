package ru.mirea.project.ui;

import ru.mirea.project.model.Patient;
import ru.mirea.project.model.PatientStatus;
import ru.mirea.project.model.PlanStatus;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.model.RehabilitationPlan;
import ru.mirea.project.model.Therapist;
import ru.mirea.project.model.TherapistSpecialization;
import ru.mirea.project.service.PatientService;
import ru.mirea.project.service.ProcedureService;
import ru.mirea.project.service.RehabilitationPlanService;
import ru.mirea.project.service.TherapistService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUi {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PatientService patientService;
    private final TherapistService therapistService;
    private final ProcedureService procedureService;
    private final RehabilitationPlanService planService;

    public ConsoleUi(PatientService patientService, TherapistService therapistService,
                     ProcedureService procedureService, RehabilitationPlanService planService) {
        this.patientService = patientService;
        this.therapistService = therapistService;
        this.procedureService = procedureService;
        this.planService = planService;
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMainMenu();
                Integer choice = readInt(scanner, "Выберите пункт: ");
                if (choice == null) {
                    continue;
                }
                switch (choice) {
                    case 1 -> patientMenu(scanner);
                    case 2 -> therapistMenu(scanner);
                    case 3 -> procedureMenu(scanner);
                    case 4 -> planMenu(scanner);
                    case 5 -> showStatistics();
                    case 0 -> running = false;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            }
        }
        System.out.println("Работа программы завершена.");
    }

    private void printMainMenu() {
        System.out.println("\n=== Реабилитационный центр ===");
        System.out.println("1. Пациенты");
        System.out.println("2. Специалисты");
        System.out.println("3. Процедуры");
        System.out.println("4. Планы реабилитации");
        System.out.println("5. Сводная статистика");
        System.out.println("0. Выход");
    }

    private void patientMenu(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Пациенты ---");
            System.out.println("1. Показать всех");
            System.out.println("2. Найти по ID");
            System.out.println("3. Добавить");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по имени (ASC)");
            System.out.println("7. Сортировка по имени (DESC)");
            System.out.println("8. Фильтрация");
            System.out.println("9. Экспорт CSV");
            System.out.println("0. Назад");
            Integer choice = readInt(scanner, "Выберите пункт: ");
            if (choice == null) {
                continue;
            }
            try {
                switch (choice) {
                    case 1 -> printList(patientService.findAll());
                    case 2 -> findPatient(scanner);
                    case 3 -> createPatient(scanner);
                    case 4 -> updatePatient(scanner);
                    case 5 -> deletePatient(scanner);
                    case 6 -> printList(patientService.sortByName(true));
                    case 7 -> printList(patientService.sortByName(false));
                    case 8 -> filterPatients(scanner);
                    case 9 -> exportPatients(scanner);
                    case 0 -> back = true;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (RuntimeException ex) {
                printError(ex);
            }
        }
    }

    private void findPatient(Scanner scanner) {
        Long id = readLong(scanner, "ID пациента: ");
        if (id != null) {
            System.out.println(patientService.findById(id));
        }
    }

    private void createPatient(Scanner scanner) {
        Patient patient = new Patient(readText(scanner, "Имя: "), readText(scanner, "Фамилия: "),
                readText(scanner, "Телефон: "), readText(scanner, "Email: "),
                readDate(scanner, "Дата рождения (гггг-мм-дд): "),
                readEnum(scanner, PatientStatus.values(), "Статус"));
        System.out.println("Создано: " + patientService.create(patient));
    }

    private void updatePatient(Scanner scanner) {
        Long id = readLong(scanner, "ID пациента: ");
        if (id == null) {
            return;
        }
        Patient patient = new Patient(id, readText(scanner, "Имя: "), readText(scanner, "Фамилия: "),
                readText(scanner, "Телефон: "), readText(scanner, "Email: "),
                readDate(scanner, "Дата рождения (гггг-мм-дд): "),
                readEnum(scanner, PatientStatus.values(), "Статус"));
        patientService.update(patient);
        System.out.println("Пациент обновлен.");
    }

    private void deletePatient(Scanner scanner) {
        Long id = readLong(scanner, "ID пациента: ");
        if (id != null) {
            patientService.delete(id);
            System.out.println("Пациент удален.");
        }
    }

    private void filterPatients(Scanner scanner) {
        System.out.println("1. По статусу");
        System.out.println("2. По диапазону даты рождения");
        Integer choice = readInt(scanner, "Вид фильтра: ");
        if (choice == null) {
            return;
        }
        if (choice == 1) {
            printList(patientService.filterByStatus(readEnum(scanner, PatientStatus.values(), "Статус")));
        } else if (choice == 2) {
            printList(patientService.filterByBirthDate(readDate(scanner, "Дата от: "),
                    readDate(scanner, "Дата до: ")));
        } else {
            System.out.println("Нет такого фильтра.");
        }
    }

    private void exportPatients(Scanner scanner) {
        String fileName = readText(scanner, "Имя CSV-файла: ");
        patientService.exportToCsv(patientService.findAll(), fileName);
        System.out.println("Пациенты экспортированы.");
    }

    private void therapistMenu(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Специалисты ---");
            System.out.println("1. Показать всех");
            System.out.println("2. Найти по ID");
            System.out.println("3. Добавить");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по ФИО (ASC)");
            System.out.println("7. Сортировка по ФИО (DESC)");
            System.out.println("8. Фильтрация");
            System.out.println("9. Экспорт CSV");
            System.out.println("0. Назад");
            Integer choice = readInt(scanner, "Выберите пункт: ");
            if (choice == null) {
                continue;
            }
            try {
                switch (choice) {
                    case 1 -> printList(therapistService.findAll());
                    case 2 -> findTherapist(scanner);
                    case 3 -> createTherapist(scanner);
                    case 4 -> updateTherapist(scanner);
                    case 5 -> deleteTherapist(scanner);
                    case 6 -> printList(therapistService.sortByName(true));
                    case 7 -> printList(therapistService.sortByName(false));
                    case 8 -> filterTherapists(scanner);
                    case 9 -> exportTherapists(scanner);
                    case 0 -> back = true;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (RuntimeException ex) {
                printError(ex);
            }
        }
    }

    private void findTherapist(Scanner scanner) {
        Long id = readLong(scanner, "ID специалиста: ");
        if (id != null) {
            System.out.println(therapistService.findById(id));
        }
    }

    private void createTherapist(Scanner scanner) {
        Therapist therapist = new Therapist(readText(scanner, "ФИО: "),
                readEnum(scanner, TherapistSpecialization.values(), "Специализация"),
                readText(scanner, "Телефон: "), readText(scanner, "Email: "),
                readDate(scanner, "Дата приема (гггг-мм-дд): "));
        System.out.println("Создано: " + therapistService.create(therapist));
    }

    private void updateTherapist(Scanner scanner) {
        Long id = readLong(scanner, "ID специалиста: ");
        if (id == null) {
            return;
        }
        Therapist therapist = new Therapist(id, readText(scanner, "ФИО: "),
                readEnum(scanner, TherapistSpecialization.values(), "Специализация"),
                readText(scanner, "Телефон: "), readText(scanner, "Email: "),
                readDate(scanner, "Дата приема (гггг-мм-дд): "));
        therapistService.update(therapist);
        System.out.println("Специалист обновлен.");
    }

    private void deleteTherapist(Scanner scanner) {
        Long id = readLong(scanner, "ID специалиста: ");
        if (id != null) {
            therapistService.delete(id);
            System.out.println("Специалист удален.");
        }
    }

    private void filterTherapists(Scanner scanner) {
        System.out.println("1. По специализации");
        System.out.println("2. По диапазону даты приема");
        Integer choice = readInt(scanner, "Вид фильтра: ");
        if (choice == null) {
            return;
        }
        if (choice == 1) {
            printList(therapistService.filterBySpecialization(
                    readEnum(scanner, TherapistSpecialization.values(), "Специализация")));
        } else if (choice == 2) {
            printList(therapistService.filterByHireDate(readDate(scanner, "Дата от: "),
                    readDate(scanner, "Дата до: ")));
        } else {
            System.out.println("Нет такого фильтра.");
        }
    }

    private void exportTherapists(Scanner scanner) {
        String fileName = readText(scanner, "Имя CSV-файла: ");
        therapistService.exportToCsv(therapistService.findAll(), fileName);
        System.out.println("Специалисты экспортированы.");
    }

    private void procedureMenu(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Процедуры ---");
            System.out.println("1. Показать все");
            System.out.println("2. Найти по ID");
            System.out.println("3. Добавить");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по дате (ASC)");
            System.out.println("7. Сортировка по дате (DESC)");
            System.out.println("8. Фильтрация");
            System.out.println("9. Экспорт CSV");
            System.out.println("0. Назад");
            Integer choice = readInt(scanner, "Выберите пункт: ");
            if (choice == null) {
                continue;
            }
            try {
                switch (choice) {
                    case 1 -> printList(procedureService.findAll());
                    case 2 -> findProcedure(scanner);
                    case 3 -> createProcedure(scanner);
                    case 4 -> updateProcedure(scanner);
                    case 5 -> deleteProcedure(scanner);
                    case 6 -> printList(procedureService.sortByDate(true));
                    case 7 -> printList(procedureService.sortByDate(false));
                    case 8 -> filterProcedures(scanner);
                    case 9 -> exportProcedures(scanner);
                    case 0 -> back = true;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (RuntimeException ex) {
                printError(ex);
            }
        }
    }

    private void findProcedure(Scanner scanner) {
        Long id = readLong(scanner, "ID процедуры: ");
        if (id != null) {
            System.out.println(procedureService.findById(id));
        }
    }

    private void createProcedure(Scanner scanner) {
        Procedure procedure = new Procedure(readLong(scanner, "ID пациента: "),
                readLong(scanner, "ID специалиста: "), readText(scanner, "Название: "),
                readText(scanner, "Описание: "), readDateTime(scanner, "Дата и время (гггг-мм-дд ЧЧ:мм): "),
                readEnum(scanner, ProcedureStatus.values(), "Статус"));
        System.out.println("Создано: " + procedureService.create(procedure));
    }

    private void updateProcedure(Scanner scanner) {
        Long id = readLong(scanner, "ID процедуры: ");
        if (id == null) {
            return;
        }
        Procedure procedure = new Procedure(id, readLong(scanner, "ID пациента: "),
                readLong(scanner, "ID специалиста: "), readText(scanner, "Название: "),
                readText(scanner, "Описание: "), readDateTime(scanner, "Дата и время (гггг-мм-дд ЧЧ:мм): "),
                readEnum(scanner, ProcedureStatus.values(), "Статус"));
        procedureService.update(procedure);
        System.out.println("Процедура обновлена.");
    }

    private void deleteProcedure(Scanner scanner) {
        Long id = readLong(scanner, "ID процедуры: ");
        if (id != null) {
            procedureService.delete(id);
            System.out.println("Процедура удалена.");
        }
    }

    private void filterProcedures(Scanner scanner) {
        System.out.println("1. По статусу");
        System.out.println("2. По пациенту");
        System.out.println("3. По диапазону дат");
        Integer choice = readInt(scanner, "Вид фильтра: ");
        if (choice == null) {
            return;
        }
        if (choice == 1) {
            printList(procedureService.filterByStatus(readEnum(scanner, ProcedureStatus.values(), "Статус")));
        } else if (choice == 2) {
            printList(procedureService.filterByPatient(readLong(scanner, "ID пациента: ")));
        } else if (choice == 3) {
            printList(procedureService.filterByDate(readDate(scanner, "Дата от: "),
                    readDate(scanner, "Дата до: ")));
        } else {
            System.out.println("Нет такого фильтра.");
        }
    }

    private void exportProcedures(Scanner scanner) {
        String fileName = readText(scanner, "Имя CSV-файла: ");
        procedureService.exportToCsv(procedureService.findAll(), fileName);
        System.out.println("Процедуры экспортированы.");
    }

    private void planMenu(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Планы реабилитации ---");
            System.out.println("1. Показать все");
            System.out.println("2. Найти по ID");
            System.out.println("3. Добавить");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по дате начала (ASC)");
            System.out.println("7. Сортировка по дате начала (DESC)");
            System.out.println("8. Фильтрация");
            System.out.println("9. Экспорт CSV");
            System.out.println("0. Назад");
            Integer choice = readInt(scanner, "Выберите пункт: ");
            if (choice == null) {
                continue;
            }
            try {
                switch (choice) {
                    case 1 -> printList(planService.findAll());
                    case 2 -> findPlan(scanner);
                    case 3 -> createPlan(scanner);
                    case 4 -> updatePlan(scanner);
                    case 5 -> deletePlan(scanner);
                    case 6 -> printList(planService.sortByStartDate(true));
                    case 7 -> printList(planService.sortByStartDate(false));
                    case 8 -> filterPlans(scanner);
                    case 9 -> exportPlans(scanner);
                    case 0 -> back = true;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (RuntimeException ex) {
                printError(ex);
            }
        }
    }

    private void findPlan(Scanner scanner) {
        Long id = readLong(scanner, "ID плана: ");
        if (id != null) {
            System.out.println(planService.findById(id));
        }
    }

    private void createPlan(Scanner scanner) {
        RehabilitationPlan plan = new RehabilitationPlan(readLong(scanner, "ID пациента: "),
                readLong(scanner, "ID специалиста: "), readText(scanner, "Название: "),
                readText(scanner, "Цель: "), readDate(scanner, "Дата начала (гггг-мм-дд): "),
                readDate(scanner, "Дата окончания (гггг-мм-дд): "),
                readEnum(scanner, PlanStatus.values(), "Статус"));
        System.out.println("Создано: " + planService.create(plan));
    }

    private void updatePlan(Scanner scanner) {
        Long id = readLong(scanner, "ID плана: ");
        if (id == null) {
            return;
        }
        RehabilitationPlan plan = new RehabilitationPlan(id, readLong(scanner, "ID пациента: "),
                readLong(scanner, "ID специалиста: "), readText(scanner, "Название: "),
                readText(scanner, "Цель: "), readDate(scanner, "Дата начала (гггг-мм-дд): "),
                readDate(scanner, "Дата окончания (гггг-мм-дд): "),
                readEnum(scanner, PlanStatus.values(), "Статус"));
        planService.update(plan);
        System.out.println("План обновлен.");
    }

    private void deletePlan(Scanner scanner) {
        Long id = readLong(scanner, "ID плана: ");
        if (id != null) {
            planService.delete(id);
            System.out.println("План удален.");
        }
    }

    private void filterPlans(Scanner scanner) {
        System.out.println("1. По статусу");
        System.out.println("2. По пациенту");
        System.out.println("3. По пересечению диапазона дат");
        Integer choice = readInt(scanner, "Вид фильтра: ");
        if (choice == null) {
            return;
        }
        if (choice == 1) {
            printList(planService.filterByStatus(readEnum(scanner, PlanStatus.values(), "Статус")));
        } else if (choice == 2) {
            printList(planService.filterByPatient(readLong(scanner, "ID пациента: ")));
        } else if (choice == 3) {
            printList(planService.filterByDate(readDate(scanner, "Дата от: "),
                    readDate(scanner, "Дата до: ")));
        } else {
            System.out.println("Нет такого фильтра.");
        }
    }

    private void exportPlans(Scanner scanner) {
        String fileName = readText(scanner, "Имя CSV-файла: ");
        planService.exportToCsv(planService.findAll(), fileName);
        System.out.println("Планы экспортированы.");
    }

    private void showStatistics() {
        try {
            List<Patient> patients = patientService.findAll();
            List<Therapist> therapists = therapistService.findAll();
            List<Procedure> procedures = procedureService.findAll();
            List<RehabilitationPlan> plans = planService.findAll();
            long activePatients = patients.stream().filter(item -> item.getStatus() == PatientStatus.ACTIVE).count();
            long completedProcedures = procedures.stream()
                    .filter(item -> item.getStatus() == ProcedureStatus.COMPLETED).count();
            double averagePlanDays = plans.stream()
                    .mapToLong(item -> item.getEndDate().toEpochDay() - item.getStartDate().toEpochDay() + 1)
                    .average().orElse(0.0);

            System.out.println("\n--- Сводная статистика ---");
            System.out.println("Всего пациентов: " + patients.size());
            System.out.println("Активных пациентов: " + activePatients);
            System.out.println("Всего специалистов: " + therapists.size());
            System.out.println("Всего процедур: " + procedures.size());
            System.out.println("Завершенных процедур: " + completedProcedures);
            System.out.println("Всего планов реабилитации: " + plans.size());
            System.out.println("Пациенты по статусам:");
            for (PatientStatus status : PatientStatus.values()) {
                long count = patients.stream().filter(item -> item.getStatus() == status).count();
                System.out.println("  " + status.getTitle() + ": " + count);
            }
            System.out.println("Процедуры по статусам:");
            for (ProcedureStatus status : ProcedureStatus.values()) {
                long count = procedures.stream().filter(item -> item.getStatus() == status).count();
                System.out.println("  " + status.getTitle() + ": " + count);
            }
            System.out.println("Планы по статусам:");
            for (PlanStatus status : PlanStatus.values()) {
                long count = plans.stream().filter(item -> item.getStatus() == status).count();
                System.out.println("  " + status.getTitle() + ": " + count);
            }
            System.out.printf("Средняя длительность плана: %.1f дней%n", averagePlanDays);
        } catch (RuntimeException ex) {
            printError(ex);
        }
    }

    private <T> void printList(List<T> items) {
        if (items.isEmpty()) {
            System.out.println("Записей не найдено.");
            return;
        }
        items.forEach(System.out::println);
    }

    private String readText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private Integer readInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            System.out.println("Ожидалось целое число. Попробуйте еще раз.");
            return null;
        }
    }

    private Long readLong(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            System.out.println("Ожидался числовой ID. Попробуйте еще раз.");
            return null;
        }
    }

    private LocalDate readDate(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            System.out.println("Неверная дата. Используйте формат гггг-мм-дд.");
            return null;
        }
    }

    private LocalDateTime readDateTime(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            System.out.println("Неверная дата и время. Используйте формат гггг-мм-дд ЧЧ:мм.");
            return null;
        }
    }

    private <T extends Enum<T>> T readEnum(Scanner scanner, T[] values, String label) {
        System.out.println(label + ":");
        for (int i = 0; i < values.length; i++) {
            System.out.println((i + 1) + ". " + values[i]);
        }
        Integer choice = readInt(scanner, "Номер: ");
        if (choice == null || choice < 1 || choice > values.length) {
            System.out.println("Нет такого значения.");
            return null;
        }
        return values[choice - 1];
    }

    private void printError(RuntimeException ex) {
        String message = ex.getMessage();
        System.out.println("Ошибка: " + (message == null ? ex.getClass().getSimpleName() : message));
    }
}
