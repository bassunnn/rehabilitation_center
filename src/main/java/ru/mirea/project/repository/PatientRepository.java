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

    @Override
    public Optional<Patient> findById(Long id) {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date FROM patient WHERE id = ?";

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()){
                if (rs.next()) {
                    return Optional.of(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка поиска пациента по ID: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    @Override
    public List<Patient> findAll() {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date FROM patients ORDER BY id";
        List<Patient> patients = new ArrayList<>();

         try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

             while (rs.next()){
                patients.add(mapResultSetToPatient(rs));
             }
         } catch (SQLException ex) {
            throw new RuntimeException("Ошибка получения списка пациентов: " + ex.getMessage(), ex);
         }
        return patients;
    }

    @Override
    public void update(Patient patient) {
        String sql = "UPDATE patients SET first_name = ?, last_name = ?, phone = ?, email = ?, birth_date = ?" +
                "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patient.getFirstName());
            stmt.setString(2, patient.getLastName());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            stmt.setDate(5, patient.getBirthDate() != null ? Date.valueOf(patient.getBirthDate()) : null);
            stmt.setLong(6, patient.getId());

            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка обновления ланных пациента: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteById(Long id){
        String sql = "DELETE FROM patients WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка удаления пациента: " + ex.getMessage(), ex);
        }
    }

    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM patients WHERE id = ?";
         try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
         } catch (SQLException ex) {
             throw new RuntimeException("Ошибка проверки существования пациента: " + ex.getMessage(), ex);
         }
    }

    public boolean existsByPhone(String phone, Long excludeId){
        String sql = excludeId == null ?
                "SELECT 1 FROM patients WHERE phone = ?" :
                "SELECT 1 FROM patients WHERE phone = ? AND id <> ?";

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, phone);

            if (excludeId != null){
                stmt.setLong(2, excludeId);
            }

            try (ResultSet rs = stmt.executeQuery()){
                return rs.next();
            }
        } catch (SQLException ex){
            throw new RuntimeException("Ошибка проверки уникальности телефона: " + ex.getMessage(), ex);
        }
    }

    public List<Patient> searchNyName(String query){
        String sql = "SELECT first_name, last_name, phone, email, birth_date FROM patients" +
                "WHERE LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? ORDER BY id";
        List<Patient> patients = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            String wildcard = "%" + query + "%";
            stmt.setString(1, wildcard);
            stmt.setString(2, wildcard);

            try (ResultSet rs = stmt.executeQuery()){
                while (rs.next()){
                    patients.add(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка поиска пациента: " + ex.getMessage(), ex);
        }
        return  patients;
    }

    private Patient mapResultSetToPatient(ResultSet rs) throws SQLException {
        Date birthDateSql = rs.getDate("birth_date");
        return new Patient(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone"),
                rs.getString("email"),
                birthDateSql != null ? birthDateSql.toLocalDate() : null
        );
    }
}