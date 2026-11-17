package ru.mirea.project.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.project.model.Patient;
import ru.mirea.project.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientRepository implements CrudRepository<Patient, Long> {
    private static final Logger logger = LoggerFactory.getLogger(PatientRepository.class);

    @Override
    public Patient save(Patient patient) {
        String sql = "INSERT INTO patients (first_name, last_name, phone, email, birth_date) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, patient.getFirstName());
            stmt.setString(2, patient.getLastName());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            stmt.setDate(5, patient.getBirthDate() != null ? Date.valueOf(patient.getBirthDate()) : null);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Создание пациента не удалось, нет затронутых строк.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    patient.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Создание пациента не удалось, идентификатор не получен.");
                }
            }
            logger.info("Пациент успешно сохранен с ID: {}", patient.getId());
            return patient;

        } catch (SQLException ex) {
            logger.error("Ошибка сохранения пациента в БД: {}", ex.getMessage());
            throw new RuntimeException("Ошибка сохранения пациента в БД: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Patient> findById(Long id) {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date FROM patients WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPatient(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            logger.error("Ошибка поиска пациента по ID {}: {}", id, ex.getMessage());
            throw new RuntimeException("Ошибка поиска пациента по ID: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Patient> findAll() {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date FROM patients ORDER BY id ASC";
        List<Patient> patients = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                patients.add(mapRowToPatient(rs));
            }
            return patients;
        } catch (SQLException ex) {
            logger.error("Ошибка получения списка пациентов: {}", ex.getMessage());
            throw new RuntimeException("Ошибка получения списка пациентов: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void update(Patient patient) {
        String sql = "UPDATE patients SET first_name = ?, last_name = ?, phone = ?, email = ?, birth_date = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, patient.getFirstName());
            stmt.setString(2, patient.getLastName());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            stmt.setDate(5, patient.getBirthDate() != null ? Date.valueOf(patient.getBirthDate()) : null);
            stmt.setLong(6, patient.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Пациент с ID " + patient.getId() + " не найден для обновления.");
            }
            logger.info("Пациент с ID {} успешно обновлен", patient.getId());
        } catch (SQLException ex) {
            logger.error("Ошибка обновления пациента с ID {}: {}", patient.getId(), ex.getMessage());
            throw new RuntimeException("Ошибка обновления пациента: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM patients WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            logger.info("Удаление пациента с ID {}: затронуто строк {}", id, rows);
            return rows > 0;
        } catch (SQLException ex) {
            logger.error("Ошибка удаления пациента с ID {}: {}", id, ex.getMessage());
            throw new RuntimeException("Ошибка удаления пациента: " + ex.getMessage(), ex);
        }
    }

    public boolean existsByEmail(String email, Long excludeId) {
        String sql = "SELECT 1 FROM patients WHERE LOWER(email) = LOWER(?) AND (? IS NULL OR id <> ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            if (excludeId != null) {
                stmt.setLong(2, excludeId);
                stmt.setLong(3, excludeId);
            } else {
                stmt.setNull(2, Types.BIGINT);
                stmt.setNull(3, Types.BIGINT);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            logger.error("Ошибка проверки уникальности email: {}", ex.getMessage());
            throw new RuntimeException("Ошибка проверки уникальности email: " + ex.getMessage(), ex);
        }
    }

    public boolean existsByPhone(String phone, Long excludeId) {
        String sql = "SELECT 1 FROM patients WHERE phone = ? AND (? IS NULL OR id <> ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            if (excludeId != null) {
                stmt.setLong(2, excludeId);
                stmt.setLong(3, excludeId);
            } else {
                stmt.setNull(2, Types.BIGINT);
                stmt.setNull(3, Types.BIGINT);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            logger.error("Ошибка проверки уникальности телефона: {}", ex.getMessage());
            throw new RuntimeException("Ошибка проверки уникальности телефона: " + ex.getMessage(), ex);
        }
    }

    public List<Patient> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date FROM patients " +
                "WHERE LOWER(first_name) LIKE ? " +
                "   OR LOWER(last_name) LIKE ? " +
                "   OR LOWER(phone) LIKE ? " +
                "   OR LOWER(email) LIKE ? " +
                "   OR LOWER(last_name || ' ' || first_name) LIKE ? " +
                "ORDER BY id ASC";

        List<Patient> patients = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 5; i++) {
                stmt.setString(i, pattern);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapRowToPatient(rs));
                }
            }
            return patients;
        } catch (SQLException ex) {
            logger.error("Ошибка поиска пациентов по запросу '{}': {}", query, ex.getMessage());
            throw new RuntimeException("Ошибка поиска пациентов: " + ex.getMessage(), ex);
        }
    }

    private Patient mapRowToPatient(ResultSet rs) throws SQLException {
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
