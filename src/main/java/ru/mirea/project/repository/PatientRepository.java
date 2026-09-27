package ru.mirea.project.repository;

import ru.mirea.project.model.Patient;
import ru.mirea.project.util.DatabaseManager;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import  java.util.Optional;

public class PatientRepository implements CrudRepository<Patient, Long> {

    @Override
    public Patient save(Patient patient) {
        String sql = "INSERT INTO patients (first_name, last_name, phone, email, birth_date)" +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {

            stmt.setString(1, patient.getFirstName());
            stmt.setString(2, patient.getLastName());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            stmt.setDate(5, patient.getBirthDate() != null ? Date.valueOf(patient.getBirthDate()) : null);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()){
                    patient.setId(generatedKeys.getLong(1));
                }
            }
            return patient;

        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка сохранения пациента в БД: " + ex.getMessage(), ex);
        }
    }
}
