package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.PartnerCompany;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PartnerCompanyDAO extends DBContext {

    private static final String SELECT_COLUMNS = "id, name, email, phone, address, status, createdAt";

    public List<PartnerCompany> getAll() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM Partner ORDER BY createdAt DESC";
        List<PartnerCompany> partners = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                partners.add(mapRow(resultSet));
            }
        }
        return partners;
    }

    public PartnerCompany getById(int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM Partner WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    public boolean insert(PartnerCompany partner) throws SQLException {
        String sql = "INSERT INTO Partner (name, email, phone, address, status, createdAt) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setPartnerParameters(statement, partner, false);
            if (statement.executeUpdate() == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    partner.setId(generatedKeys.getInt(1));
                }
            }
            return true;
        }
    }

    public boolean update(PartnerCompany partner) throws SQLException {
        String sql = "UPDATE Partner SET name = ?, email = ?, phone = ?, address = ?, "
                + "status = ?, createdAt = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setPartnerParameters(statement, partner, true);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM Partner WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void setPartnerParameters(PreparedStatement statement, PartnerCompany partner,
                                      boolean includeId) throws SQLException {
        int index = 1;
        statement.setString(index++, partner.getName());
        statement.setString(index++, partner.getEmail());
        statement.setString(index++, partner.getPhone());
        statement.setString(index++, partner.getAddress());
        statement.setString(index++, partner.getStatus());
        statement.setTimestamp(index++, partner.getCreatedAt());
        if (includeId) {
            statement.setInt(index, partner.getId());
        }
    }

    private PartnerCompany mapRow(ResultSet resultSet) throws SQLException {
        PartnerCompany partner = new PartnerCompany();
        partner.setId(resultSet.getInt("id"));
        partner.setName(resultSet.getString("name"));
        partner.setEmail(resultSet.getString("email"));
        partner.setPhone(resultSet.getString("phone"));
        partner.setAddress(resultSet.getString("address"));
        partner.setStatus(resultSet.getString("status"));
        partner.setCreatedAt(resultSet.getTimestamp("createdAt"));
        return partner;
    }
}