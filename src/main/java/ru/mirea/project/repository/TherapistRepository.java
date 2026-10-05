package ru.mirea.project.repository;

import ru.mirea.project.model.Therapist;
import ru.mirea.project.model.TherapistSpecialization;
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

public class TherapistRepository implements CrudRepository<Therapist, Long> {
    @Override
    public Therapist save(Therapist therapist) {
        String sql = "INSERT INTO therapists (full_name, specialization, phone, email, hire_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, therapist.getFullName());
            statement.setString(2, therapist.getSpecialization().name());
            statement.setString(3, therapist.getPhone());
            statement.setString(4, therapist.getEmail());
            statement.setDate(5, Date.valueOf(therapist.getHireDate()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    therapist.setId(keys.getLong(1));
                }
            }
            return therapist;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка сохранения специалиста: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Therapist> findById(Long id) {
        String sql = "SELECT id, full_name, specialization, phone, email, hire_date FROM therapists WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка поиска специалиста: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    @Override
    public List<Therapist> findAll() {
        String sql = "SELECT id, full_name, specialization, phone, email, hire_date FROM therapists ORDER BY id";
        List<Therapist> therapists = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                therapists.add(map(resultSet));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка получения специалистов: " + ex.getMessage(), ex);
        }
        return therapists;
    }

    @Override
    public void update(Therapist therapist) {
        String sql = "UPDATE therapists SET full_name = ?, specialization = ?, phone = ?, email = ?, hire_date = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, therapist.getFullName());
            statement.setString(2, therapist.getSpecialization().name());
            statement.setString(3, therapist.getPhone());
            statement.setString(4, therapist.getEmail());
            statement.setDate(5, Date.valueOf(therapist.getHireDate()));
            statement.setLong(6, therapist.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка обновления специалиста: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM therapists WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка удаления специалиста: " + ex.getMessage(), ex);
        }
    }

    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM therapists WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка проверки специалиста: " + ex.getMessage(), ex);
        }
    }

    public boolean existsByPhoneOrEmail(String phone, String email, Long excludedId) {
        String sql = excludedId == null
                ? "SELECT 1 FROM therapists WHERE phone = ? OR email = ?"
                : "SELECT 1 FROM therapists WHERE (phone = ? OR email = ?) AND id <> ?";
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
            throw new RuntimeException("Ошибка проверки уникальности специалиста: " + ex.getMessage(), ex);
        }
    }

    public List<Therapist> findBySpecialization(TherapistSpecialization specialization) {
        String sql = "SELECT id, full_name, specialization, phone, email, hire_date FROM therapists WHERE specialization = ? ORDER BY full_name";
        List<Therapist> therapists = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, specialization.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    therapists.add(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка фильтрации специалистов: " + ex.getMessage(), ex);
        }
        return therapists;
    }

    public boolean hasLinkedRecords(Long therapistId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM procedures WHERE therapist_id = ?) OR EXISTS (SELECT 1 FROM rehabilitation_plans WHERE therapist_id = ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, therapistId);
            statement.setLong(2, therapistId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка проверки связанных записей специалиста: " + ex.getMessage(), ex);
        }
    }

    public long countBySpecialization(TherapistSpecialization specialization) {
        String sql = "SELECT COUNT(*) FROM therapists WHERE specialization = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, specialization.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка подсчета специалистов: " + ex.getMessage(), ex);
        }
    }

    private Therapist map(ResultSet resultSet) throws SQLException {
        Date hireDate = resultSet.getDate("hire_date");
        return new Therapist(resultSet.getLong("id"), resultSet.getString("full_name"),
                TherapistSpecialization.valueOf(resultSet.getString("specialization")),
                resultSet.getString("phone"), resultSet.getString("email"),
                hireDate.toLocalDate());
    }
}
