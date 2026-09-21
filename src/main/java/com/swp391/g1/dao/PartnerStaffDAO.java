package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.PartnerStaff;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PartnerStaffDAO extends DBContext {

    private static final String SELECT_COLUMNS = "id, fullName, email, phone, position, "
            + "isActive, partnerId, createdAt";

    public List<PartnerStaff> getAll() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM PartnerStaff ORDER BY createdAt DESC";
        List<PartnerStaff> staffList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                staffList.add(mapRow(resultSet));
            }
        }
        return staffList;
    }

    public List<PartnerStaff> getByPartnerId(int partnerId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM PartnerStaff WHERE partnerId = ? ORDER BY createdAt DESC";
        List<PartnerStaff> staffList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, partnerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    staffList.add(mapRow(resultSet));
                }
            }
        }
        return staffList;
    }

    public PartnerStaff getById(int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM PartnerStaff WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    public boolean insert(PartnerStaff staff) throws SQLException {
        String sql = "INSERT INTO PartnerStaff (fullName, email, phone, position, isActive, "
                + "partnerId, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setStaffParameters(statement, staff, false);
            if (statement.executeUpdate() == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    staff.setId(generatedKeys.getInt(1));
                }
            }
            return true;
        }
    }

    public boolean update(PartnerStaff staff) throws SQLException {
        String sql = "UPDATE PartnerStaff SET fullName = ?, email = ?, phone = ?, position = ?, "
                + "isActive = ?, partnerId = ?, createdAt = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setStaffParameters(statement, staff, true);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM PartnerStaff WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void setStaffParameters(PreparedStatement statement, PartnerStaff staff,
                                    boolean includeId) throws SQLException {
        int index = 1;
        statement.setString(index++, staff.getFullName());
        statement.setString(index++, staff.getEmail());
        statement.setString(index++, staff.getPhone());
        statement.setString(index++, staff.getPosition());
        statement.setBoolean(index++, staff.getIsActive());
        statement.setInt(index++, staff.getPartnerId());
        statement.setTimestamp(index++, staff.getCreatedAt());
        if (includeId) {
            statement.setInt(index, staff.getId());
        }
    }

    private PartnerStaff mapRow(ResultSet resultSet) throws SQLException {
        PartnerStaff staff = new PartnerStaff();
        staff.setId(resultSet.getInt("id"));
        staff.setFullName(resultSet.getString("fullName"));
        staff.setEmail(resultSet.getString("email"));
        staff.setPhone(resultSet.getString("phone"));
        staff.setPosition(resultSet.getString("position"));
        staff.setIsActive(resultSet.getBoolean("isActive"));
        staff.setPartnerId(resultSet.getInt("partnerId"));
        staff.setCreatedAt(resultSet.getTimestamp("createdAt"));
        return staff;
    }
}