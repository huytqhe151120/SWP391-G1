package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.PartnerCompany;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartnerCompanyDAOImpl implements IPartnerCompanyDAO {

    private PartnerCompany mapPartnerCompany(ResultSet rs) throws SQLException {
        PartnerCompany pc = new PartnerCompany();
        pc.setId(rs.getInt("id"));
        pc.setCode(rs.getString("code"));
        pc.setName(rs.getString("name"));
        pc.setWebsite(rs.getString("website"));
        pc.setEmail(rs.getString("email"));
        pc.setPhoneNumber(rs.getString("phone_number"));
        pc.setAddress(rs.getString("address"));
        pc.setDescription(rs.getString("description"));
        String status = rs.getString("status");
        pc.setStatus(status == null ? null : Enum.CommonStatus.valueOf(status));
        return pc;
    }

    @Override
    public List<PartnerCompany> findByCode(String code) {
        List<PartnerCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_company WHERE code = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPartnerCompany(rs));
                }
            }
            if(list.size() > 1){
                System.out.println("Warning: Multiple partner companies found with the same code: " + code);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find partner companies by code.", e);
        }
        return list;
    }

    @Override
    public List<PartnerCompany> findByCommonStatus(Enum.CommonStatus status) {
        List<PartnerCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_company WHERE status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPartnerCompany(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find partner companies by status.", e);
        }
        return list;
    }

    @Override
    public List<PartnerCompany> findAll() {
        List<PartnerCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_company";

        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPartnerCompany(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all partner companies.", e);
        }
        return list;
    }

    @Override
    public PartnerCompany findById(Integer id) {
        PartnerCompany pc = null;
        String sql = "SELECT * FROM partner_company WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pc = mapPartnerCompany(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find partner company by ID.", e);
        }
        return pc;
    }

    @Override
    public Integer insert(PartnerCompany partnerCompany) {
        String sql = "INSERT INTO partner_company "
                + "(code, name, website, email, phone_number, address, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, partnerCompany.getCode());
            ps.setString(2, partnerCompany.getName());
            ps.setString(3, partnerCompany.getWebsite());
            ps.setString(4, partnerCompany.getEmail());
            ps.setString(5, partnerCompany.getPhoneNumber());
            ps.setString(6, partnerCompany.getAddress());
            ps.setString(7, partnerCompany.getDescription());
            if (partnerCompany.getStatus() == null) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, partnerCompany.getStatus().name());
            }

            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert partner company.", e);
        }
    }

    @Override
    public boolean update(PartnerCompany partnerCompany) {
        if (partnerCompany == null || partnerCompany.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE partner_company "
                + "SET code = ?, name = ?, website = ?, email = ?, phone_number = ?, address = ?, description = ?, status = ? "
                + "WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partnerCompany.getCode());
            ps.setString(2, partnerCompany.getName());
            ps.setString(3, partnerCompany.getWebsite());
            ps.setString(4, partnerCompany.getEmail());
            ps.setString(5, partnerCompany.getPhoneNumber());
            ps.setString(6, partnerCompany.getAddress());
            ps.setString(7, partnerCompany.getDescription());
            if (partnerCompany.getStatus() == null) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, partnerCompany.getStatus().name());
            }
            ps.setInt(9, partnerCompany.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update partner company.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM partner_company WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete partner company.", e);
        }
    }
}
