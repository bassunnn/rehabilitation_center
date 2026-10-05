package ru.mirea.project.repository;

import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProcedureRepository implements CrudRepository<Procedure, Long> {
    @Override
    public Procedure save(Procedure procedure) {
        String sql = "INSERT INTO procedures (patient_id, therapist_id, name, description, procedure_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, procedure.getPatientId());
            statement.setLong(2, procedure.getTherapistId());
            statement.setString(3, procedure.getName());
            statement.setString(4, procedure.getDescription());
            statement.setTimestamp(5, Timestamp.valueOf(procedure.getProcedureDate()));
            statement.setString(6, procedure.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    procedure.setId(keys.getLong(1));
                }
            }
            return procedure;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка сохранения процедуры: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Procedure> findById(Long id) {
        String sql = "SELECT id, patient_id, therapist_id, name, description, procedure_date, status FROM procedures WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка поиска процедуры: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    @Override
    public List<Procedure> findAll() {
        String sql = "SELECT id, patient_id, therapist_id, name, description, procedure_date, status FROM procedures ORDER BY procedure_date, id";
        return findMany(sql, null, null);
    }

    @Override
    public void update(Procedure procedure) {
        String sql = "UPDATE procedures SET patient_id = ?, therapist_id = ?, name = ?, description = ?, procedure_date = ?, status = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, procedure.getPatientId());
            statement.setLong(2, procedure.getTherapistId());
            statement.setString(3, procedure.getName());
            statement.setString(4, procedure.getDescription());
            statement.setTimestamp(5, Timestamp.valueOf(procedure.getProcedureDate()));
            statement.setString(6, procedure.getStatus().name());
            statement.setLong(7, procedure.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка обновления процедуры: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM procedures WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка удаления процедуры: " + ex.getMessage(), ex);
        }
    }

    public List<Procedure> findByPatientId(Long patientId) {
        String sql = "SELECT id, patient_id, therapist_id, name, description, procedure_date, status FROM procedures WHERE patient_id = ? ORDER BY procedure_date";
        return findMany(sql, patientId, null);
    }

    public List<Procedure> findByTherapistId(Long therapistId) {
        String sql = "SELECT id, patient_id, therapist_id, name, description, procedure_date, status FROM procedures WHERE therapist_id = ? ORDER BY procedure_date";
        return findMany(sql, therapistId, null);
    }

    public List<Procedure> findByStatus(ProcedureStatus status) {
        String sql = "SELECT id, patient_id, therapist_id, name, description, procedure_date, status FROM procedures WHERE status = ? ORDER BY procedure_date";
        return findMany(sql, null, status.name());
    }

    public long countByStatus(ProcedureStatus status) {
        String sql = "SELECT COUNT(*) FROM procedures WHERE status = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка подсчета процедур: " + ex.getMessage(), ex);
        }
    }

    private List<Procedure> findMany(String sql, Long numericParameter, String textParameter) {
        List<Procedure> procedures = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (numericParameter != null) {
                statement.setLong(1, numericParameter);
            } else if (textParameter != null) {
                statement.setString(1, textParameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    procedures.add(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка фильтрации процедур: " + ex.getMessage(), ex);
        }
        return procedures;
    }

    private Procedure map(ResultSet resultSet) throws SQLException {
        Timestamp procedureDate = resultSet.getTimestamp("procedure_date");
        return new Procedure(resultSet.getLong("id"), resultSet.getLong("patient_id"),
                resultSet.getLong("therapist_id"), resultSet.getString("name"),
                resultSet.getString("description"), procedureDate.toLocalDateTime(),
                ProcedureStatus.valueOf(resultSet.getString("status")));
    }
}
