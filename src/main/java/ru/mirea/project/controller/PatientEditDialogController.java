package ru.mirea.project.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.Patient;
import ru.mirea.project.service.ValidationService;

public class PatientEditDialogController {

    @FXML
    private TextField lastNameField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private Label errorLabel;

    private Stage dialogStage;
    private Patient patient;
    private boolean saveClicked = false;
    private ValidationService validationService;

    @FXML
    private void initialize() {
        errorLabel.setText("");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        if (patient != null) {
            lastNameField.setText(patient.getLastName());
            firstNameField.setText(patient.getFirstName());
            phoneField.setText(patient.getPhone());
            emailField.setText(patient.getEmail());
            birthDatePicker.setValue(patient.getBirthDate());
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Patient getPatient() {
        return patient;
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");

        if (patient == null) {
            patient = new Patient();
        }

        patient.setLastName(lastNameField.getText() != null ? lastNameField.getText().trim() : "");
        patient.setFirstName(firstNameField.getText() != null ? firstNameField.getText().trim() : "");
        patient.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : "");
        patient.setEmail(emailField.getText() != null ? emailField.getText().trim() : "");
        patient.setBirthDate(birthDatePicker.getValue());

        if (validationService != null) {
            try {
                validationService.validatePatient(patient);
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

