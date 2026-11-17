package ru.mirea.project.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.project.model.Patient;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.model.StatisticsSummary;
import ru.mirea.project.repository.PatientRepository;
import ru.mirea.project.repository.ProcedureRepository;
import ru.mirea.project.service.ExportService;
import ru.mirea.project.service.PatientService;
import ru.mirea.project.service.ProcedureService;
import ru.mirea.project.service.StatisticsService;
import ru.mirea.project.service.ValidationService;
import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.util.DialogUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // --- Services ---
    private PatientService patientService;
    private ProcedureService procedureService;
    private StatisticsService statisticsService;
    private ExportService exportService;
    private ValidationService validationService;

    // --- Observable Lists ---
    private final ObservableList<Patient> patientData = FXCollections.observableArrayList();
    private final ObservableList<Procedure> procedureData = FXCollections.observableArrayList();
    private final ObservableList<Procedure> patientProceduresData = FXCollections.observableArrayList();

    // --- Tab 1: Patients UI ---
    @FXML private TextField patientSearchField;
    @FXML private TextField minAgeField;
    @FXML private TextField maxAgeField;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, Number> patientIdColumn;
    @FXML private TableColumn<Patient, String> patientLastNameColumn;
    @FXML private TableColumn<Patient, String> patientFirstNameColumn;
    @FXML private TableColumn<Patient, Number> patientAgeColumn;
    @FXML private TableColumn<Patient, String> patientBirthDateColumn;
    @FXML private TableColumn<Patient, String> patientPhoneColumn;
    @FXML private TableColumn<Patient, String> patientEmailColumn;

    @FXML private Label selectedPatientInfoLabel;
    @FXML private TableView<Procedure> patientProceduresTable;
    @FXML private TableColumn<Procedure, Number> pProcIdColumn;
    @FXML private TableColumn<Procedure, String> pProcNameColumn;
    @FXML private TableColumn<Procedure, String> pProcDateColumn;
    @FXML private TableColumn<Procedure, String> pProcStatusColumn;
    @FXML private TableColumn<Procedure, String> pProcDescColumn;

    // --- Tab 2: Procedures UI ---
    @FXML private TextField procedureSearchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private TableView<Procedure> procedureTable;
    @FXML private TableColumn<Procedure, Number> procIdColumn;
    @FXML private TableColumn<Procedure, String> procPatientColumn;
    @FXML private TableColumn<Procedure, String> procNameColumn;
    @FXML private TableColumn<Procedure, String> procDateColumn;
    @FXML private TableColumn<Procedure, String> procStatusColumn;
    @FXML private TableColumn<Procedure, String> procDescColumn;

    // --- Tab 3: Statistics UI ---
    @FXML private Label totalPatientsValueLabel;
    @FXML private Label totalProceduresValueLabel;
    @FXML private Label completionRateValueLabel;
    @FXML private Label avgAgeValueLabel;
    @FXML private Label monthProceduresValueLabel;
    @FXML private Label avgProceduresPerPatientLabel;
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> ageBarChart;

    // --- Status bar ---
    @FXML private Label dbStatusLabel;
    @FXML private Label totalRecordsLabel;

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void initialize() {
        initServices();
        initPatientTable();
        initPatientProceduresTable();
        initProcedureTable();
        initFilters();
        checkDatabaseStatus();
        loadAllData();
    }

    private void initServices() {
        PatientRepository patientRepository = new PatientRepository();
        ProcedureRepository procedureRepository = new ProcedureRepository();
        this.validationService = new ValidationService(patientRepository);
        this.patientService = new PatientService(patientRepository, validationService);
        this.procedureService = new ProcedureService(procedureRepository, validationService);
        this.statisticsService = new StatisticsService(patientRepository, procedureRepository);
        this.exportService = new ExportService();
    }

    private void checkDatabaseStatus() {
        boolean ok = DatabaseManager.checkConnection();
        if (ok) {
            dbStatusLabel.setText("Подключено: " + DatabaseManager.getDatabaseUrl());
            dbStatusLabel.setStyle("-fx-text-fill: #155724; -fx-font-weight: bold;");
        } else {
            dbStatusLabel.setText("Ошибка подключения к БД");
            dbStatusLabel.setStyle("-fx-text-fill: #721c24; -fx-font-weight: bold;");
        }
    }

    private void initPatientTable() {
        patientIdColumn.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getId() != null ? cellData.getValue().getId() : 0));
        patientLastNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastName()));
        patientFirstNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFirstName()));
        patientAgeColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getAge()));
        patientBirthDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedBirthDate()));
        patientPhoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        patientEmailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));

        patientTable.setItems(patientData);

        // Слушатель выбора пациента -> отображение его процедур
        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedPatient) -> {
            showSelectedPatientProcedures(selectedPatient);
        });

        // Поиск по нажатию Enter или изменению текста
        patientSearchField.textProperty().addListener((obs, oldVal, newVal) -> handlePatientSearch());
    }

    private void initPatientProceduresTable() {
        pProcIdColumn.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getId() != null ? cellData.getValue().getId() : 0));
        pProcNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        pProcDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedProcedureDate()));
        pProcStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatusTitle()));
        pProcDescColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));

        patientProceduresTable.setItems(patientProceduresData);
        setupStatusColumnColor(pProcStatusColumn);
    }

    private void initProcedureTable() {
        procIdColumn.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getId() != null ? cellData.getValue().getId() : 0));
        procPatientColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPatientFullName()));
        procNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        procDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedProcedureDate()));
        procStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatusTitle()));
        procDescColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));

        procedureTable.setItems(procedureData);
        setupStatusColumnColor(procStatusColumn);

        // Поиск процедур в реальном времени
        procedureSearchField.textProperty().addListener((obs, oldVal, newVal) -> handleProcedureSearch());
    }

    private void setupStatusColumnColor(TableColumn<Procedure, String> column) {
        column.setCellFactory(col -> new TableCell<Procedure, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Завершена")) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    } else if (item.contains("Выполняется")) {
                        setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;");
                    } else if (item.contains("Запланирована")) {
                        setStyle("-fx-text-fill: #0277bd; -fx-font-weight: bold;");
                    } else if (item.contains("Отменена")) {
                        setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void initFilters() {
        statusFilterComboBox.getItems().clear();
        statusFilterComboBox.getItems().add("Все статусы");
        for (ProcedureStatus status : ProcedureStatus.values()) {
            statusFilterComboBox.getItems().add(status.getTitle());
        }
        statusFilterComboBox.getSelectionModel().selectFirst();
    }

    public void loadAllData() {
        try {
            List<Patient> patients = patientService.getAllPatients();
            patientData.setAll(patients);

            List<Procedure> procedures = procedureService.getAllProcedures();
            procedureData.setAll(procedures);

            updateStatusBar();
            refreshStatistics();

            // Если был выбран пациент, обновим его процедуры
            Patient selected = patientTable.getSelectionModel().getSelectedItem();
            showSelectedPatientProcedures(selected);

        } catch (Exception ex) {
            logger.error("Ошибка загрузки данных из БД: {}", ex.getMessage());
            DialogUtils.showError(primaryStage, "Ошибка базы данных", "Не удалось загрузить данные из PostgreSQL", ex.getMessage());
        }
    }

    private void showSelectedPatientProcedures(Patient patient) {
        if (patient == null) {
            selectedPatientInfoLabel.setText("Выберите пациента в таблице выше для просмотра назначенных процедур");
            patientProceduresData.clear();
            return;
        }

        selectedPatientInfoLabel.setText(String.format("Процедуры пациента: %s (Тел: %s)", patient.getFullName(), patient.getPhone()));
        try {
            List<Procedure> procs = procedureService.getProceduresByPatientId(patient.getId());
            patientProceduresData.setAll(procs);
        } catch (Exception ex) {
            logger.error("Ошибка загрузки процедур пациента ID {}: {}", patient.getId(), ex.getMessage());
            DialogUtils.showError(primaryStage, "Ошибка", "Не удалось загрузить процедуры пациента", ex.getMessage());
        }
    }

    private void updateStatusBar() {
        totalRecordsLabel.setText(String.format("Пациентов: %d | Процедур: %d", patientData.size(), procedureData.size()));
    }

    // ==========================================
    // ДЕЙСТВИЯ С ПАЦИЕНТАМИ (CRUD, ПОИСК, ФИЛЬТР)
    // ==========================================

    @FXML
    private void handleAddPatient() {
        Patient newPatient = new Patient();
        boolean saved = showPatientEditDialog(newPatient, "Добавление нового пациента");
        if (saved) {
            try {
                Patient created = patientService.createPatient(newPatient);
                patientData.add(created);
                patientTable.getSelectionModel().select(created);
                updateStatusBar();
                refreshStatistics();
                DialogUtils.showInfo(primaryStage, "Успешно", "Пациент зарегистрирован", "Пациент " + created.getFullName() + " успешно сохранен в базе данных.");
            } catch (Exception ex) {
                logger.error("Ошибка добавления пациента: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка сохранения", "Не удалось сохранить пациента", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleEditPatient() {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(primaryStage, "Внимание", "Пациент не выбран", "Пожалуйста, выберите пациента из таблицы для редактирования.");
            return;
        }

        // Копия для редактирования
        Patient editCopy = new Patient(
                selected.getId(),
                selected.getFirstName(),
                selected.getLastName(),
                selected.getPhone(),
                selected.getEmail(),
                selected.getBirthDate()
        );

        boolean saved = showPatientEditDialog(editCopy, "Редактирование пациента #" + selected.getId());
        if (saved) {
            try {
                patientService.updatePatient(editCopy);
                // Обновляем в локальном списке
                int index = patientData.indexOf(selected);
                if (index >= 0) {
                    patientData.set(index, editCopy);
                    patientTable.getSelectionModel().select(editCopy);
                }
                loadAllData(); // Обновить также имена в процедурах
                DialogUtils.showInfo(primaryStage, "Успешно", "Данные обновлены", "Информация о пациенте успешно обновлена.");
            } catch (Exception ex) {
                logger.error("Ошибка обновления пациента: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка обновления", "Не удалось обновить данные пациента", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleDeletePatient() {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(primaryStage, "Внимание", "Пациент не выбран", "Пожалуйста, выберите пациента из таблицы для удаления.");
            return;
        }

        boolean confirmed = DialogUtils.showConfirmation(
                primaryStage,
                "Подтверждение удаления",
                "Удалить пациента " + selected.getFullName() + "?",
                "Внимание! При удалении пациента будут автоматически удалены все связанные процедуры (каскадное удаление). Продолжить?"
        );

        if (confirmed) {
            try {
                patientService.deletePatient(selected.getId());
                patientData.remove(selected);
                loadAllData();
                DialogUtils.showInfo(primaryStage, "Успешно", "Пациент удален", "Пациент и все связанные данные удалены из базы данных.");
            } catch (Exception ex) {
                logger.error("Ошибка удаления пациента: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка удаления", "Не удалось удалить пациента", ex.getMessage());
            }
        }
    }

    @FXML
    private void handlePatientSearch() {
        String query = patientSearchField.getText();
        try {
            List<Patient> found = patientService.searchPatients(query);
            patientData.setAll(found);
            applyAgeFilter();
        } catch (Exception ex) {
            logger.error("Ошибка поиска пациентов: {}", ex.getMessage());
        }
    }

    @FXML
    private void handleApplyAgeFilter() {
        applyAgeFilter();
    }

    private void applyAgeFilter() {
        Integer minAge = null;
        Integer maxAge = null;

        try {
            if (minAgeField.getText() != null && !minAgeField.getText().trim().isEmpty()) {
                minAge = Integer.parseInt(minAgeField.getText().trim());
            }
            if (maxAgeField.getText() != null && !maxAgeField.getText().trim().isEmpty()) {
                maxAge = Integer.parseInt(maxAgeField.getText().trim());
            }
        } catch (NumberFormatException e) {
            DialogUtils.showWarning(primaryStage, "Ошибка фильтра", "Некорректный возраст", "Пожалуйста, введите корректные целые числа для возраста.");
            return;
        }

        List<Patient> list = patientService.searchPatients(patientSearchField.getText());
        final Integer finalMin = minAge;
        final Integer finalMax = maxAge;
        List<Patient> filtered = list.stream()
                .filter(p -> (finalMin == null || p.getAge() >= finalMin))
                .filter(finalMax == null ? p -> true : p -> p.getAge() <= finalMax)
                .toList();

        patientData.setAll(filtered);
    }

    @FXML
    private void handleResetPatientFilters() {
        patientSearchField.clear();
        minAgeField.clear();
        maxAgeField.clear();
        loadAllData();
    }

    private boolean showPatientEditDialog(Patient patient, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/patient_dialog.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            PatientEditDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setValidationService(validationService);
            controller.setPatient(patient);

            dialogStage.showAndWait();
            return controller.isSaveClicked();
        } catch (IOException e) {
            logger.error("Ошибка открытия диалога пациента: {}", e.getMessage(), e);
            DialogUtils.showError(primaryStage, "Ошибка интерфейса", "Не удалось открыть диалоговое окно", e.getMessage());
            return false;
        }
    }

    // ==========================================
    // ДЕЙСТВИЯ С ПРОЦЕДУРАМИ (CRUD, ПОИСК, ФИЛЬТР)
    // ==========================================

    @FXML
    private void handleAddProcedure() {
        List<Patient> patients = patientService.getAllPatients();
        if (patients.isEmpty()) {
            DialogUtils.showWarning(primaryStage, "Предупреждение", "Нет доступных пациентов",
                    "В системе пока нет ни одного пациента. Сначала зарегистрируйте пациента, чтобы назначить ему процедуру.");
            return;
        }

        Procedure newProcedure = new Procedure();
        newProcedure.setProcedureDate(LocalDate.now());
        newProcedure.setStatus(ProcedureStatus.PLANNED);

        // Если на первой вкладке выбран пациент, предвыберем его
        Patient selectedPatient = patientTable.getSelectionModel().getSelectedItem();
        if (selectedPatient != null) {
            newProcedure.setPatientId(selectedPatient.getId());
        }

        boolean saved = showProcedureEditDialog(newProcedure, patients, "Назначение процедуры");
        if (saved) {
            try {
                Procedure created = procedureService.createProcedure(newProcedure);
                procedureData.add(created);
                updateStatusBar();
                refreshStatistics();
                loadAllData();
                DialogUtils.showInfo(primaryStage, "Успешно", "Процедура назначена", "Процедура '" + created.getName() + "' успешно сохранена.");
            } catch (Exception ex) {
                logger.error("Ошибка сохранения процедуры: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка сохранения", "Не удалось назначить процедуру", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleEditProcedure() {
        Procedure selected = procedureTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = patientProceduresTable.getSelectionModel().getSelectedItem();
        }
        if (selected == null) {
            DialogUtils.showWarning(primaryStage, "Внимание", "Процедура не выбрана", "Пожалуйста, выберите процедуру для редактирования.");
            return;
        }

        List<Patient> patients = patientService.getAllPatients();

        Procedure editCopy = new Procedure(
                selected.getId(),
                selected.getPatientId(),
                selected.getName(),
                selected.getDescription(),
                selected.getProcedureDate(),
                selected.getStatus()
        );
        editCopy.setPatientFullName(selected.getPatientFullName());

        boolean saved = showProcedureEditDialog(editCopy, patients, "Редактирование процедуры #" + selected.getId());
        if (saved) {
            try {
                procedureService.updateProcedure(editCopy);
                loadAllData();
                DialogUtils.showInfo(primaryStage, "Успешно", "Процедура обновлена", "Данные процедуры успешно обновлены.");
            } catch (Exception ex) {
                logger.error("Ошибка обновления процедуры: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка обновления", "Не удалось обновить процедуру", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteProcedure() {
        Procedure selected = procedureTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = patientProceduresTable.getSelectionModel().getSelectedItem();
        }
        if (selected == null) {
            DialogUtils.showWarning(primaryStage, "Внимание", "Процедура не выбрана", "Пожалуйста, выберите процедуру для удаления.");
            return;
        }

        boolean confirmed = DialogUtils.showConfirmation(
                primaryStage,
                "Подтверждение удаления",
                "Удалить процедуру '" + selected.getName() + "'?",
                "Вы действительно хотите удалить выбранную запись о процедуре?"
        );

        if (confirmed) {
            try {
                procedureService.deleteProcedure(selected.getId());
                loadAllData();
                DialogUtils.showInfo(primaryStage, "Успешно", "Процедура удалена", "Запись о процедуре успешно удалена из базы данных.");
            } catch (Exception ex) {
                logger.error("Ошибка удаления процедуры: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка удаления", "Не удалось удалить процедуру", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleProcedureSearch() {
        applyProcedureFilter();
    }

    @FXML
    private void handleApplyProcedureFilter() {
        applyProcedureFilter();
    }

    private void applyProcedureFilter() {
        String query = procedureSearchField.getText();

        String selectedStatusStr = statusFilterComboBox.getValue();
        ProcedureStatus status = null;
        if (selectedStatusStr != null && !selectedStatusStr.equals("Все статусы")) {
            status = ProcedureStatus.fromString(selectedStatusStr);
        }

        LocalDate fromDate = dateFromPicker.getValue();
        LocalDate toDate = dateToPicker.getValue();

        try {
            List<Procedure> filtered = procedureService.searchAndFilter(query, status, fromDate, toDate);
            procedureData.setAll(filtered);
        } catch (Exception ex) {
            logger.error("Ошибка применения фильтра процедур: {}", ex.getMessage());
        }
    }

    @FXML
    private void handleResetProcedureFilters() {
        procedureSearchField.clear();
        statusFilterComboBox.getSelectionModel().selectFirst();
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        loadAllData();
    }

    private boolean showProcedureEditDialog(Procedure procedure, List<Patient> patients, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/procedure_dialog.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            ProcedureEditDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setValidationService(validationService);
            controller.setProcedure(procedure);
            controller.setPatients(patients);

            dialogStage.showAndWait();
            return controller.isSaveClicked();
        } catch (IOException e) {
            logger.error("Ошибка открытия диалога процедуры: {}", e.getMessage(), e);
            DialogUtils.showError(primaryStage, "Ошибка интерфейса", "Не удалось открыть диалоговое окно", e.getMessage());
            return false;
        }
    }

    // ==========================================
    // СТАТИСТИКА И АНАЛИТИКА
    // ==========================================

    @FXML
    public void refreshStatistics() {
        try {
            StatisticsSummary summary = statisticsService.calculateStatistics();

            totalPatientsValueLabel.setText(String.valueOf(summary.getTotalPatients()));
            totalProceduresValueLabel.setText(String.valueOf(summary.getTotalProcedures()));
            completionRateValueLabel.setText(String.format("%.1f%%", summary.getCompletionRate()));
            avgAgeValueLabel.setText(String.format("%.1f лет (от %d до %d)",
                    summary.getAveragePatientAge(), summary.getMinPatientAge(), summary.getMaxPatientAge()));
            monthProceduresValueLabel.setText(String.valueOf(summary.getProceduresThisMonth()));
            avgProceduresPerPatientLabel.setText(String.format("%.1f", summary.getAverageProceduresPerPatient()));

            // Обновление круговой диаграммы (PieChart)
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            for (ProcedureStatus status : ProcedureStatus.values()) {
                int count = summary.getCountForStatus(status);
                if (count > 0) {
                    pieChartData.add(new PieChart.Data(status.getTitle() + " (" + count + ")", count));
                }
            }
            statusPieChart.setData(pieChartData);

            // Обновление столбчатой диаграммы по возрастам (BarChart)
            ageBarChart.getData().clear();
            XYChart.Series<String, Number> ageSeries = new XYChart.Series<>();
            ageSeries.setName("Количество пациентов");

            int under25 = 0;
            int between25and40 = 0;
            int between41and60 = 0;
            int over60 = 0;

            for (Patient p : patientData) {
                int age = p.getAge();
                if (age < 25) under25++;
                else if (age <= 40) between25and40++;
                else if (age <= 60) between41and60++;
                else over60++;
            }

            ageSeries.getData().add(new XYChart.Data<>("< 25 лет", under25));
            ageSeries.getData().add(new XYChart.Data<>("25-40 лет", between25and40));
            ageSeries.getData().add(new XYChart.Data<>("41-60 лет", between41and60));
            ageSeries.getData().add(new XYChart.Data<>("> 60 лет", over60));

            ageBarChart.getData().add(ageSeries);

        } catch (Exception ex) {
            logger.error("Ошибка расчета статистики: {}", ex.getMessage());
        }
    }

    // ==========================================
    // ЭКСПОРТ ДАННЫХ (.xlsx, .csv)
    // ==========================================

    @FXML
    private void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Экспорт всех данных в Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Книга Excel (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("rehabilitation_report_" + LocalDate.now() + ".xlsx");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                exportService.exportToExcel(file, patientData, procedureData);
                DialogUtils.showInfo(primaryStage, "Экспорт завершен", "Данные успешно выгружены",
                        "Файл Excel сохранен: " + file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Ошибка экспорта в Excel: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка экспорта", "Не удалось экспортировать данные в Excel", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleExportPatientsCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Экспорт списка пациентов в CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовый файл CSV (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("patients_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                exportService.exportPatientsToCsv(file, patientData);
                DialogUtils.showInfo(primaryStage, "Экспорт завершен", "Список пациентов выгружен",
                        "Файл CSV сохранен: " + file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Ошибка экспорта пациентов в CSV: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка экспорта", "Не удалось экспортировать пациентов в CSV", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleExportProceduresCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Экспорт процедур в CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовый файл CSV (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("procedures_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                exportService.exportProceduresToCsv(file, procedureData);
                DialogUtils.showInfo(primaryStage, "Экспорт завершен", "Список процедур выгружен",
                        "Файл CSV сохранен: " + file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Ошибка экспорта процедур в CSV: {}", ex.getMessage());
                DialogUtils.showError(primaryStage, "Ошибка экспорта", "Не удалось экспортировать процедуры в CSV", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleRefreshAll() {
        loadAllData();
        checkDatabaseStatus();
    }
}

