package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityOrganizationLevelDAO;
import com.swp391.g1.model.ActivityOrganizationLevel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityOrganizationLevelDAOImpl implements IActivityOrganizationLevelDAO {

    private ActivityOrganizationLevel mapOrganizationLevel(ResultSet rs) throws SQLException {
        ActivityOrganizationLevel level = new ActivityOrganizationLevel();
        level.setId(rs.getInt("id"));
        level.setCode(rs.getString("code"));
        level.setName(rs.getString("name"));
        level.setCoefficient(rs.getBigDecimal("coefficient"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            level.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            level.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return level;
    }

    @Override
    public List<ActivityOrganizationLevel> findAll() {
        List<ActivityOrganizationLevel> list = new ArrayList<>();
        String sql = "SELECT id, code, name, coefficient, created_at, updated_at "
                + "FROM dbo.activity_organization_level ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapOrganizationLevel(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách cấp tổ chức hoạt động.", e);
        }
        return list;
    }

    @Override
    public ActivityOrganizationLevel findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT id, code, name, coefficient, created_at, updated_at "
                + "FROM dbo.activity_organization_level WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapOrganizationLevel(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tìm cấp tổ chức hoạt động theo ID.", e);
        }
    }

    @Override
    public List<ActivityOrganizationLevel> findByCode(String code) {
        List<ActivityOrganizationLevel> list = new ArrayList<>();
        String sql = "SELECT id, code, name, coefficient, created_at, updated_at "
                + "FROM dbo.activity_organization_level WHERE code = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrganizationLevel(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tìm cấp tổ chức hoạt động theo mã.", e);
        }
        return list;
    }

    @Override
    public Integer insert(ActivityOrganizationLevel level) {
        String sql = "INSERT INTO dbo.activity_organization_level (code, name, coefficient) VALUES (?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, level.getCode());
            ps.setString(2, level.getName());
            ps.setBigDecimal(3, level.getCoefficient() == null
                    ? null
                    : level.getCoefficient().setScale(2, java.math.RoundingMode.HALF_UP));
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể thêm mới cấp tổ chức hoạt động.", e);
        }
    }

    @Override
    public boolean update(ActivityOrganizationLevel level) {
        if (level == null || level.getId() <= 0) {
            return false;
        }
        String sql = "UPDATE dbo.activity_organization_level "
                + "SET code = ?, name = ?, coefficient = ?, updated_at = SYSUTCDATETIME() "
                + "WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, level.getCode());
            ps.setString(2, level.getName());
            ps.setBigDecimal(3, level.getCoefficient() == null
                    ? null
                    : level.getCoefficient().setScale(2, java.math.RoundingMode.HALF_UP));
            ps.setInt(4, level.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật cấp tổ chức hoạt động.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM dbo.activity_organization_level WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể xóa cấp tổ chức hoạt động.", e);
        }
    }
}
