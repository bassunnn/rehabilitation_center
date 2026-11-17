package ru.mirea.project.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.project.model.Procedure;
import ru.mirea.project.model.ProcedureStatus;
import ru.mirea.project.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProcedureRepository implements CrudRepository<Procedure, Long> {
    private static final Logger logger = LoggerFactory.getLogger(ProcedureRepository.class);

    @Override
    public Procedure save(Procedure procedure) {
        String sql = "INSERT INTO procedures (patient_id, name, description, procedure_date, status) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, procedure.getPatientId());
            stmt.setString(2, procedure.getName());
            stmt.setString(3, procedure.getDescription());
            stmt.setDate(4, procedure.getProcedureDate() != null ? Date.valueOf(procedure.getProcedureDate()) : null);
            stmt.setString(5, procedure.getStatus() != null ? procedure.getStatus().name() : ProcedureStatus.PLANNED.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Создание процедуры не удалось, нет затронутых строк.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    procedure.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Создание процедуры не удалось, идентификатор не получен.");
                }
            }
            logger.info("Процедура успешно сохранена с ID: {}", procedure.getId());
            return procedure;

        } catch (SQLException ex) {
            logger.error("Ошибка сохранения процедуры в БД: {}", ex.getMessage());
            throw new RuntimeException("Ошибка сохранения процедуры в БД: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Procedure> findById(Long id) {
        String sql = "SELECT p.id, p.patient_id, p.name, p.description, p.procedure_date, p.status, " +
                "pt.last_name || ' ' || pt.first_name AS patient_full_name " +
                "FROM procedures p " +
                "LEFT JOIN patients pt ON p.patient_id = pt.id " +
                "WHERE p.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProcedure(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            logger.error("Ошибка поиска процедуры по ID {}: {}", id, ex.getMessage());
            throw new RuntimeException("Ошибка поиска процедуры по ID: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Procedure> findAll() {
        return findAllWithPatientDetails();
    }

    public List<Procedure> findAllWithPatientDetails() {
        String sql = "SELECT p.id, p.patient_id, p.name, p.description, p.procedure_date, p.status, " +
                "pt.last_name || ' ' || pt.first_name AS patient_full_name " +
                "FROM procedures p " +
                "LEFT JOIN patients pt ON p.patient_id = pt.id " +
                "ORDER BY p.id ASC";

        List<Procedure> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToProcedure(rs));
            }
            return list;
        } catch (SQLException ex) {
            logger.error("Ошибка получения списка процедур: {}", ex.getMessage());
            throw new RuntimeException("Ошибка получения списка процедур: " + ex.getMessage(), ex);
        }
    }

    public List<Procedure> findByPatientId(Long patientId) {
        String sql = "SELECT p.id, p.patient_id, p.name, p.description, p.procedure_date, p.status, " +
                "pt.last_name || ' ' || pt.first_name AS patient_full_name " +
                "FROM procedures p " +
                "LEFT JOIN patients pt ON p.patient_id = pt.id " +
                "WHERE p.patient_id = ? " +
                "ORDER BY p.procedure_date DESC, p.id DESC";

        List<Procedure> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToProcedure(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            logger.error("Ошибка получения процедур для пациента ID {}: {}", patientId, ex.getMessage());
            throw new RuntimeException("Ошибка получения процедур пациента: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void update(Procedure procedure) {
        String sql = "UPDATE procedures SET patient_id = ?, name = ?, description = ?, procedure_date = ?, status = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, procedure.getPatientId());
            stmt.setString(2, procedure.getName());
            stmt.setString(3, procedure.getDescription());
            stmt.setDate(4, procedure.getProcedureDate() != null ? Date.valueOf(procedure.getProcedureDate()) : null);
            stmt.setString(5, procedure.getStatus() != null ? procedure.getStatus().name() : ProcedureStatus.PLANNED.name());
            stmt.setLong(6, procedure.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Процедура с ID " + procedure.getId() + " не найдена для обновления.");
            }
            logger.info("Процедура с ID {} успешно обновлена", procedure.getId());
        } catch (SQLException ex) {
            logger.error("Ошибка обновления процедуры с ID {}: {}", procedure.getId(), ex.getMessage());
            throw new RuntimeException("Ошибка обновления процедуры: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM procedures WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            logger.info("Удаление процедуры с ID {}: затронуто строк {}", id, rows);
            return rows > 0;
        } catch (SQLException ex) {
            logger.error("Ошибка удаления процедуры с ID {}: {}", id, ex.getMessage());
            throw new RuntimeException("Ошибка удаления процедуры: " + ex.getMessage(), ex);
        }
    }

    public List<Procedure> searchAndFilter(String query, ProcedureStatus status, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.patient_id, p.name, p.description, p.procedure_date, p.status, " +
                        "pt.last_name || ' ' || pt.first_name AS patient_full_name " +
                        "FROM procedures p " +
                        "LEFT JOIN patients pt ON p.patient_id = pt.id " +
                        "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ? OR LOWER(pt.last_name || ' ' || pt.first_name) LIKE ?) ");
            String p = "%" + query.trim().toLowerCase() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
        }

        if (status != null) {
            sql.append("AND p.status = ? ");
            params.add(status.name());
        }

        if (fromDate != null) {
            sql.append("AND p.procedure_date >= ? ");
            params.add(Date.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append("AND p.procedure_date <= ? ");
            params.add(Date.valueOf(toDate));
        }

        sql.append("ORDER BY p.procedure_date DESC, p.id DESC");

        List<Procedure> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToProcedure(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            logger.error("Ошибка фильтрации процедур: {}", ex.getMessage());
            throw new RuntimeException("Ошибка фильтрации процедур: " + ex.getMessage(), ex);
        }
    }

    private Procedure mapRowToProcedure(ResultSet rs) throws SQLException {
        Date procDateSql = rs.getDate("procedure_date");
        String statusStr = rs.getString("status");
        Procedure procedure = new Procedure(
                rs.getLong("id"),
                rs.getLong("patient_id"),
                rs.getString("name"),
                rs.getString("description"),
                procDateSql != null ? procDateSql.toLocalDate() : null,
                statusStr != null ? ProcedureStatus.valueOf(statusStr) : null
        );

        try {
            procedure.setPatientFullName(rs.getString("patient_full_name"));
        } catch (SQLException ignored) {
            // patient_full_name might not be in query in some cases
        }

        return procedure;
    }
}
