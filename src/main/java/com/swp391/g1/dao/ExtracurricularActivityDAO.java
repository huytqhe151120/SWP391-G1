package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.ExtracurricularActivity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ExtracurricularActivityDAO extends DBContext {

    private static final String SELECT_COLUMNS = "id, semesterId, activityType, code, name, "
            + "responsibleDepartmentId, responsibleStaffId, partnerId, partnerStaffId, "
            + "bonusPoint, penaltyPoint, address, description, activityStatus, approvalStatus, createdAt";

    public List<ExtracurricularActivity> getAll() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM ExtracurricularActivity ORDER BY createdAt DESC";
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
                + " FROM ExtracurricularActivity WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    public boolean insert(ExtracurricularActivity activity) throws SQLException {
        String sql = "INSERT INTO ExtracurricularActivity ("
                + "semesterId, activityType, code, name, responsibleDepartmentId, "
                + "responsibleStaffId, partnerId, partnerStaffId, bonusPoint, penaltyPoint, "
                + "address, description, activityStatus, approvalStatus, createdAt"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setActivityParameters(statement, activity, false);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
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
        String sql = "UPDATE ExtracurricularActivity SET semesterId = ?, activityType = ?, "
                + "code = ?, name = ?, responsibleDepartmentId = ?, responsibleStaffId = ?, "
                + "partnerId = ?, partnerStaffId = ?, bonusPoint = ?, penaltyPoint = ?, "
                + "address = ?, description = ?, activityStatus = ?, approvalStatus = ?, createdAt = ? "
                + "WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setActivityParameters(statement, activity, true);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM ExtracurricularActivity WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void setActivityParameters(PreparedStatement statement,
                                       ExtracurricularActivity activity,
                                       boolean includeId) throws SQLException {
        int index = 1;
        statement.setInt(index++, activity.getSemesterId());
        statement.setString(index++, activity.getActivityType());
        statement.setString(index++, activity.getCode());
        statement.setString(index++, activity.getName());
        statement.setInt(index++, activity.getResponsibleDepartmentId());
        statement.setInt(index++, activity.getResponsibleStaffId());
        statement.setInt(index++, activity.getPartnerId());
        statement.setInt(index++, activity.getPartnerStaffId());
        statement.setDouble(index++, activity.getBonusPoint());
        statement.setDouble(index++, activity.getPenaltyPoint());
        statement.setString(index++, activity.getAddress());
        statement.setString(index++, activity.getDescription());
        statement.setString(index++, activity.getActivityStatus());
        statement.setString(index++, activity.getApprovalStatus());
        statement.setTimestamp(index++, activity.getCreatedAt());

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