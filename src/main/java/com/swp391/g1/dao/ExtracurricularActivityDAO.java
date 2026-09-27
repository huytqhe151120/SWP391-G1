package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.ExtracurricularActivity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ExtracurricularActivityDAO extends DBContext {

    /*
     * The model used by HuyTQ2 is kept for compatibility.
     * SQL column names below follow the real swp391g1 database schema.
     */
    private static final String SELECT_COLUMNS = """
            ea.id,
            ea.semester_id AS semesterId,
            CAST(ea.activity_type_id AS varchar(50)) AS activityType,
            ea.code,
            ea.name,
            ea.responsible_department_id AS responsibleDepartmentId,
            ea.responsible_staff_id AS responsibleStaffId,
            ISNULL(ea.partner_company_id, 0) AS partnerId,
            ISNULL(ea.partner_staff_id, 0) AS partnerStaffId,
            ea.bonus_point AS bonusPoint,
            ea.penalty_point AS penaltyPoint,
            ea.address,
            ea.description,
            ea.activity_status AS activityStatus,
            ea.approval_status AS approvalStatus,
            ea.created_at AS createdAt
            """;

    public List<ExtracurricularActivity> getAll() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM extracurricular_activity ea"
                + " ORDER BY ea.created_at DESC";

        List<ExtracurricularActivity> activities = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                activities.add(mapRow(resultSet));
            }
        }

        return activities;
    }

    public ExtracurricularActivity getById(int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM extracurricular_activity ea"
                + " WHERE ea.id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    public boolean insert(ExtracurricularActivity activity) throws SQLException {
        String sql = """
                INSERT INTO extracurricular_activity (
                    semester_id,
                    activity_type_id,
                    code,
                    name,
                    responsible_department_id,
                    responsible_staff_id,
                    partner_company_id,
                    partner_staff_id,
                    bonus_point,
                    penalty_point,
                    address,
                    description,
                    activity_status,
                    approval_status,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setActivityParameters(statement, activity, false);

            if (statement.executeUpdate() == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    activity.setId(generatedKeys.getInt(1));
                }
            }

            return true;
        }
    }

    public boolean update(ExtracurricularActivity activity) throws SQLException {
        String sql = """
                UPDATE extracurricular_activity
                SET semester_id = ?,
                    activity_type_id = ?,
                    code = ?,
                    name = ?,
                    responsible_department_id = ?,
                    responsible_staff_id = ?,
                    partner_company_id = ?,
                    partner_staff_id = ?,
                    bonus_point = ?,
                    penalty_point = ?,
                    address = ?,
                    description = ?,
                    activity_status = ?,
                    approval_status = ?,
                    created_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setActivityParameters(statement, activity, true);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM extracurricular_activity WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean approve(int activityId, Integer actorAccountId, String note)
            throws SQLException {
        return changeApprovalStatus(
                activityId,
                "APPROVED",
                "APPROVE",
                actorAccountId,
                note
        );
    }

    public boolean reject(int activityId, Integer actorAccountId, String note)
            throws SQLException {
        return changeApprovalStatus(
                activityId,
                "REJECTED",
                "REJECT",
                actorAccountId,
                note
        );
    }

    private boolean changeApprovalStatus(
            int activityId,
            String newStatus,
            String actionType,
            Integer actorAccountId,
            String note) throws SQLException {

        String selectSql = """
                SELECT approval_status
                FROM extracurricular_activity
                WHERE id = ?
                """;

        String updateSql = """
                UPDATE extracurricular_activity
                SET approval_status = ?,
                    activity_status = 'UPCOMING'
                WHERE id = ?
                  AND approval_status = 'PENDING'
                """;

        String historySql = """
                INSERT INTO activity_approval_history (
                    activity_id,
                    old_approval_status,
                    new_approval_status,
                    action_type,
                    actor_account_id,
                    note
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        boolean oldAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            String oldStatus;

            try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                statement.setInt(1, activityId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return false;
                    }

                    oldStatus = resultSet.getString("approval_status");
                }
            }

            if (!"PENDING".equals(oldStatus)) {
                connection.rollback();
                return false;
            }

            int affectedRows;

            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, newStatus);
                statement.setInt(2, activityId);
                affectedRows = statement.executeUpdate();
            }

            if (affectedRows == 0) {
                connection.rollback();
                return false;
            }

            try (PreparedStatement statement = connection.prepareStatement(historySql)) {
                statement.setInt(1, activityId);
                statement.setString(2, oldStatus);
                statement.setString(3, newStatus);
                statement.setString(4, actionType);

                if (actorAccountId == null) {
                    statement.setNull(5, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(5, actorAccountId);
                }

                statement.setString(6, note);
                statement.executeUpdate();
            }

            connection.commit();
            return true;

        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private void setActivityParameters(
            PreparedStatement statement,
            ExtracurricularActivity activity,
            boolean includeId) throws SQLException {

        int index = 1;

        statement.setInt(index++, activity.getSemesterId());

        try {
            statement.setInt(index++, Integer.parseInt(activity.getActivityType()));
        } catch (NumberFormatException exception) {
            throw new SQLException("Activity type must be a valid activity_type.id.", exception);
        }

        statement.setString(index++, activity.getCode());
        statement.setString(index++, activity.getName());
        statement.setInt(index++, activity.getResponsibleDepartmentId());
        statement.setInt(index++, activity.getResponsibleStaffId());

        if (activity.getPartnerId() > 0) {
            statement.setInt(index++, activity.getPartnerId());
        } else {
            statement.setNull(index++, java.sql.Types.INTEGER);
        }

        if (activity.getPartnerStaffId() > 0) {
            statement.setInt(index++, activity.getPartnerStaffId());
        } else {
            statement.setNull(index++, java.sql.Types.INTEGER);
        }

        statement.setDouble(index++, activity.getBonusPoint());
        statement.setDouble(index++, activity.getPenaltyPoint());
        statement.setString(index++, activity.getAddress());
        statement.setString(index++, activity.getDescription());
        statement.setString(index++, activity.getActivityStatus());
        statement.setString(index++, activity.getApprovalStatus());

        Timestamp createdAt = activity.getCreatedAt();
        if (createdAt == null) {
            statement.setTimestamp(index++, new Timestamp(System.currentTimeMillis()));
        } else {
            statement.setTimestamp(index++, createdAt);
        }

        if (includeId) {
            statement.setInt(index, activity.getId());
        }
    }

    private ExtracurricularActivity mapRow(ResultSet resultSet) throws SQLException {
        ExtracurricularActivity activity = new ExtracurricularActivity();

        activity.setId(resultSet.getInt("id"));
        activity.setSemesterId(resultSet.getInt("semesterId"));
        activity.setActivityType(resultSet.getString("activityType"));
        activity.setCode(resultSet.getString("code"));
        activity.setName(resultSet.getString("name"));
        activity.setResponsibleDepartmentId(resultSet.getInt("responsibleDepartmentId"));
        activity.setResponsibleStaffId(resultSet.getInt("responsibleStaffId"));
        activity.setPartnerId(resultSet.getInt("partnerId"));
        activity.setPartnerStaffId(resultSet.getInt("partnerStaffId"));
        activity.setBonusPoint(resultSet.getDouble("bonusPoint"));
        activity.setPenaltyPoint(resultSet.getDouble("penaltyPoint"));
        activity.setAddress(resultSet.getString("address"));
        activity.setDescription(resultSet.getString("description"));
        activity.setActivityStatus(resultSet.getString("activityStatus"));
        activity.setApprovalStatus(resultSet.getString("approvalStatus"));
        activity.setCreatedAt(resultSet.getTimestamp("createdAt"));

        return activity;
    }
}
