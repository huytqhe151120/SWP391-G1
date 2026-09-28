package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.PartnerStaff;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartnerStaffDAOImpl implements IPartnerStaffDAO {

    private PartnerStaff mapPartnerStaff(ResultSet rs) throws SQLException {
        PartnerStaff partnerStaff = new PartnerStaff();
        partnerStaff.setId(rs.getInt("id"));
        partnerStaff.setCode(rs.getString("code"));
        partnerStaff.setName(rs.getString("name"));
        partnerStaff.setCompanyId(rs.getInt("company_id"));
        partnerStaff.setDob(rs.getDate("dob"));
        partnerStaff.setGender(rs.getBoolean("gender"));
        partnerStaff.setPosition(rs.getString("position"));
        String status = rs.getString("status");
        partnerStaff.setStatus(status == null ? null : Enum.CommonStatus.valueOf(status));
        partnerStaff.setAccountId(rs.getInt("account_id"));
        return partnerStaff;
    }

    @Override
    public List<PartnerStaff> findByCode(String code) {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff WHERE code = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPartnerStaff(rs));
                }
            }
            if(list.size() > 1){
                System.out.println("Warning: Multiple partner staff found with the same code: " + code);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find partner staff by code.", e);
        }
        return list;
    }

    @Override
    public List<PartnerStaff> findByCommonStatus(Enum.CommonStatus status) {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff WHERE status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPartnerStaff(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find partner staff by status.", e);
        }
        return list;
    }

    @Override
    public List<PartnerStaff> findByCompany(int companyId) {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff WHERE company_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPartnerStaff(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find partner staff by company.", e);
        }
        return list;
    }

    @Override
    public List<PartnerStaff> findAll() {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapPartnerStaff(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all partner staff.", e);
        }
        return list;
    }

    @Override
    public PartnerStaff findById(Integer id) {
        PartnerStaff partnerStaff = null;
        String sql = "SELECT * FROM partner_staff WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    partnerStaff = mapPartnerStaff(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find partner staff by ID.", e);
        }
        return partnerStaff;
    }

    @Override
    public Integer insert(PartnerStaff partnerStaff) {
        String sql = "INSERT INTO partner_staff "
                + "(company_id, code, name, dob, gender, position, status, account_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, partnerStaff.getCompanyId());
            ps.setString(2, partnerStaff.getCode());
            ps.setString(3, partnerStaff.getName());
            ps.setDate(4, partnerStaff.getDob());
            ps.setBoolean(5, partnerStaff.getGender());
            ps.setString(6, partnerStaff.getPosition());
            ps.setString(7, partnerStaff.getStatus().name());
            ps.setInt(8, partnerStaff.getAccountId());

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
    public boolean update(PartnerStaff entity) {
        String sql = "UPDATE partner_staff SET company_id = ?, code = ?, name = ?, "
                + "dob = ?, gender = ?, position = ?, status = ?, account_id = ? WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCompanyId());
            ps.setString(2, entity.getCode());
            ps.setString(3, entity.getName());
            ps.setDate(4, entity.getDob());
            ps.setBoolean(5, entity.getGender());
            ps.setString(6, entity.getPosition());
            ps.setString(7, entity.getStatus().name());
            ps.setInt(8, entity.getAccountId());
            ps.setInt(9, entity.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update partner staff.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM partner_staff WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete partner staff.", e);
        }
    }
}
