package ru.mirea.project.repository;

import ru.mirea.project.model.Patient;
import ru.mirea.project.model.PatientStatus;
import ru.mirea.project.util.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientRepository implements CrudRepository<Patient, Long> {
    @Override
    public Patient save(Patient patient) {
        String sql = "INSERT INTO patients (first_name, last_name, phone, email, birth_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, patient.getFirstName());
            statement.setString(2, patient.getLastName());
            statement.setString(3, patient.getPhone());
            statement.setString(4, patient.getEmail());
            statement.setDate(5, Date.valueOf(patient.getBirthDate()));
            statement.setString(6, patient.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    patient.setId(keys.getLong(1));
                }
            }
            return patient;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка сохранения пациента: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Patient> findById(Long id) {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date, status FROM patients WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка поиска пациента: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    @Override
    public List<Patient> findAll() {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date, status FROM patients ORDER BY id";
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                patients.add(map(resultSet));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка получения пациентов: " + ex.getMessage(), ex);
        }
        return patients;
    }

    @Override
    public void update(Patient patient) {
        String sql = "UPDATE patients SET first_name = ?, last_name = ?, phone = ?, email = ?, birth_date = ?, status = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patient.getFirstName());
            statement.setString(2, patient.getLastName());
            statement.setString(3, patient.getPhone());
            statement.setString(4, patient.getEmail());
            statement.setDate(5, Date.valueOf(patient.getBirthDate()));
            statement.setString(6, patient.getStatus().name());
            statement.setLong(7, patient.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка обновления пациента: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM patients WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка удаления пациента: " + ex.getMessage(), ex);
        }
    }

    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM patients WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка проверки пациента: " + ex.getMessage(), ex);
        }
    }

    public boolean existsByPhoneOrEmail(String phone, String email, Long excludedId) {
        String sql = excludedId == null
                ? "SELECT 1 FROM patients WHERE phone = ? OR email = ?"
                : "SELECT 1 FROM patients WHERE (phone = ? OR email = ?) AND id <> ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            statement.setString(2, email);
            if (excludedId != null) {
                statement.setLong(3, excludedId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка проверки уникальности пациента: " + ex.getMessage(), ex);
        }
    }

    public List<Patient> findByStatus(PatientStatus status) {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date, status FROM patients WHERE status = ? ORDER BY last_name, first_name";
        return findByOneString(sql, status.name());
    }

    public List<Patient> searchByName(String query) {
        String sql = "SELECT id, first_name, last_name, phone, email, birth_date, status FROM patients WHERE LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? ORDER BY last_name, first_name";
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String wildcard = "%" + query.toLowerCase() + "%";
            statement.setString(1, wildcard);
            statement.setString(2, wildcard);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patients.add(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка поиска пациента по имени: " + ex.getMessage(), ex);
        }
        return patients;
    }

    public long countByStatus(PatientStatus status) {
        String sql = "SELECT COUNT(*) FROM patients WHERE status = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка подсчета пациентов: " + ex.getMessage(), ex);
        }
    }

    private List<Patient> findByOneString(String sql, String value) {
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patients.add(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка фильтрации пациентов: " + ex.getMessage(), ex);
        }
        return patients;
    }

    private Patient map(ResultSet resultSet) throws SQLException {
        Date birthDate = resultSet.getDate("birth_date");
        return new Patient(resultSet.getLong("id"), resultSet.getString("first_name"),
                resultSet.getString("last_name"), resultSet.getString("phone"),
                resultSet.getString("email"), birthDate.toLocalDate(),
                PatientStatus.valueOf(resultSet.getString("status")));
    }
}
