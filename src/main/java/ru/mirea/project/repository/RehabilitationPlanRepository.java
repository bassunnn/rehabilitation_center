package ru.mirea.project.repository;

import ru.mirea.project.model.PlanStatus;
import ru.mirea.project.model.RehabilitationPlan;
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

public class RehabilitationPlanRepository implements CrudRepository<RehabilitationPlan, Long> {
    @Override
    public RehabilitationPlan save(RehabilitationPlan plan) {
        String sql = "INSERT INTO rehabilitation_plans (patient_id, therapist_id, title, goal, start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, plan.getPatientId());
            statement.setLong(2, plan.getTherapistId());
            statement.setString(3, plan.getTitle());
            statement.setString(4, plan.getGoal());
            statement.setDate(5, Date.valueOf(plan.getStartDate()));
            statement.setDate(6, Date.valueOf(plan.getEndDate()));
            statement.setString(7, plan.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    plan.setId(keys.getLong(1));
                }
            }
            return plan;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка сохранения плана: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<RehabilitationPlan> findById(Long id) {
        String sql = "SELECT id, patient_id, therapist_id, title, goal, start_date, end_date, status FROM rehabilitation_plans WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка поиска плана: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    @Override
    public List<RehabilitationPlan> findAll() {
        String sql = "SELECT id, patient_id, therapist_id, title, goal, start_date, end_date, status FROM rehabilitation_plans ORDER BY start_date, id";
        return findMany(sql, null, null);
    }

    @Override
    public void update(RehabilitationPlan plan) {
        String sql = "UPDATE rehabilitation_plans SET patient_id = ?, therapist_id = ?, title = ?, goal = ?, start_date = ?, end_date = ?, status = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, plan.getPatientId());
            statement.setLong(2, plan.getTherapistId());
            statement.setString(3, plan.getTitle());
            statement.setString(4, plan.getGoal());
            statement.setDate(5, Date.valueOf(plan.getStartDate()));
            statement.setDate(6, Date.valueOf(plan.getEndDate()));
            statement.setString(7, plan.getStatus().name());
            statement.setLong(8, plan.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка обновления плана: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM rehabilitation_plans WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка удаления плана: " + ex.getMessage(), ex);
        }
    }

    public List<RehabilitationPlan> findByPatientId(Long patientId) {
        String sql = "SELECT id, patient_id, therapist_id, title, goal, start_date, end_date, status FROM rehabilitation_plans WHERE patient_id = ? ORDER BY start_date";
        return findMany(sql, patientId, null);
    }

    public List<RehabilitationPlan> findByTherapistId(Long therapistId) {
        String sql = "SELECT id, patient_id, therapist_id, title, goal, start_date, end_date, status FROM rehabilitation_plans WHERE therapist_id = ? ORDER BY start_date";
        return findMany(sql, therapistId, null);
    }

    public List<RehabilitationPlan> findByStatus(PlanStatus status) {
        String sql = "SELECT id, patient_id, therapist_id, title, goal, start_date, end_date, status FROM rehabilitation_plans WHERE status = ? ORDER BY start_date";
        return findMany(sql, null, status.name());
    }

    public long countByStatus(PlanStatus status) {
        String sql = "SELECT COUNT(*) FROM rehabilitation_plans WHERE status = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка подсчета планов: " + ex.getMessage(), ex);
        }
    }

    private List<RehabilitationPlan> findMany(String sql, Long numericParameter, String textParameter) {
        List<RehabilitationPlan> plans = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (numericParameter != null) {
                statement.setLong(1, numericParameter);
            } else if (textParameter != null) {
                statement.setString(1, textParameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    plans.add(map(resultSet));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Ошибка фильтрации планов: " + ex.getMessage(), ex);
        }
        return plans;
    }

    private RehabilitationPlan map(ResultSet resultSet) throws SQLException {
        Date startDate = resultSet.getDate("start_date");
        Date endDate = resultSet.getDate("end_date");
        return new RehabilitationPlan(resultSet.getLong("id"), resultSet.getLong("patient_id"),
                resultSet.getLong("therapist_id"), resultSet.getString("title"),
                resultSet.getString("goal"), startDate.toLocalDate(), endDate.toLocalDate(),
                PlanStatus.valueOf(resultSet.getString("status")));
    }
}
