package ru.mirea.project.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.Patient;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.service.ValidationService;

import java.util.List;

public class ProcedureEditDialogController {

    @FXML
    private ComboBox<Patient> patientComboBox;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private DatePicker procedureDatePicker;
    @FXML
    private ComboBox<ProcedureStatus> statusComboBox;
    @FXML
    private Label errorLabel;

    private Stage dialogStage;
    private Procedure procedure;
    private boolean saveClicked = false;
    private ValidationService validationService;

    @FXML
    private void initialize() {
        errorLabel.setText("");

        statusComboBox.setItems(FXCollections.observableArrayList(ProcedureStatus.values()));
        statusComboBox.getSelectionModel().select(ProcedureStatus.PLANNED);

        patientComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Patient patient) {
                if (patient == null) return "";
                return patient.getFullName() + " (тел: " + patient.getPhone() + ")";
            }

            @Override
            public Patient fromString(String string) {
                return null;
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    public void setPatients(List<Patient> patients) {
        patientComboBox.setItems(FXCollections.observableArrayList(patients));
        if (procedure != null && procedure.getPatientId() != null) {
            for (Patient p : patients) {
                if (p.getId().equals(procedure.getPatientId())) {
                    patientComboBox.getSelectionModel().select(p);
                    break;
                }
            }
        }
    }

    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
        if (procedure != null) {
            nameField.setText(procedure.getName());
            descriptionArea.setText(procedure.getDescription());
            procedureDatePicker.setValue(procedure.getProcedureDate());
            if (procedure.getStatus() != null) {
                statusComboBox.setValue(procedure.getStatus());
            }
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Procedure getProcedure() {
        return procedure;
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");

        if (procedure == null) {
            procedure = new Procedure();
        }

        Patient selectedPatient = patientComboBox.getValue();
        if (selectedPatient != null) {
            procedure.setPatientId(selectedPatient.getId());
            procedure.setPatientFullName(selectedPatient.getFullName());
        } else {
            procedure.setPatientId(null);
            procedure.setPatientFullName(null);
        }

        procedure.setName(nameField.getText() != null ? nameField.getText().trim() : "");
        procedure.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");
        procedure.setProcedureDate(procedureDatePicker.getValue());
        procedure.setStatus(statusComboBox.getValue());

        if (validationService != null) {
            try {
                validationService.validateProcedure(procedure);
            } catch (BusinessException ex) {
                errorLabel.setText(ex.getMessage());
                return;
            }
        }

        saveClicked = true;
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}

