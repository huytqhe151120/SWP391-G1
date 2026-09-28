package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.ExtracurricularActivity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExtracurricularDAOImpl implements IExtracurricularActivityDAO {

    private ExtracurricularActivity mapActivity(ResultSet rs) throws SQLException {
        ExtracurricularActivity activity = new ExtracurricularActivity();
        activity.setId(rs.getInt("id"));
        activity.setSemesterId(rs.getInt("semester_id"));
        activity.setCode(rs.getString("code"));
        activity.setName(rs.getString("name"));
        activity.setResponsibleDepartmentId(rs.getInt("responsible_department_id"));
        activity.setResponsibleStaffId(rs.getInt("responsible_staff_id"));
        activity.setPartnerCompanyId((Integer) rs.getObject("partner_company_id"));
        activity.setPartnerStaffId((Integer) rs.getObject("partner_staff_id"));
        activity.setBonusPoint(rs.getBigDecimal("bonus_point"));
        activity.setPenaltyPoint(rs.getBigDecimal("penalty_point"));
        activity.setAddress(rs.getString("address"));
        activity.setDescription(rs.getString("description"));

        String activityStatus = rs.getString("activity_status");
        activity.setActivityStatus(activityStatus == null ? null : Enum.ActivityStatus.valueOf(activityStatus));
        String approvalStatus = rs.getString("approval_status");
        activity.setApprovalStatus(approvalStatus == null ? null : Enum.ApprovalStatus.valueOf(approvalStatus));

        Timestamp createdAt = rs.getTimestamp("created_at");
        activity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        activity.setActivityTypeId(rs.getInt("activity_type_id"));

        Timestamp startTime = rs.getTimestamp("start_time");
        activity.setStartTime(startTime == null ? null : startTime.toLocalDateTime());
        Timestamp endTime = rs.getTimestamp("end_time");
        activity.setEndTime(endTime == null ? null : endTime.toLocalDateTime());
        return activity;
    }

    @Override
    public List<ExtracurricularActivity> findByCode(String code) {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity WHERE code = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find extracurricular activities by code.", e);
        }
        return list;
    }

    @Override
    public List<ExtracurricularActivity> findByActivityStatus(Enum.ActivityStatus status) {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity WHERE activity_status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find extracurricular activities by status.", e);
        }
        return list;
    }

    @Override
    public List<ExtracurricularActivity> findByApprovalStatus(Enum.ApprovalStatus status) {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity WHERE approval_status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find extracurricular activities by approval status.", e);
        }
        return list;
    }

    @Override
    public List<ExtracurricularActivity> findAll() {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity";

        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapActivity(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all extracurricular activities.", e);
        }
        return list;
    }

    @Override
    public ExtracurricularActivity findById(Integer id) {
        String sql = "SELECT * FROM extracurricular_activity WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapActivity(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find extracurricular activity by ID.", e);
        }
    }

    @Override
    public Integer insert(ExtracurricularActivity activity) {
        String sql = "INSERT INTO extracurricular_activity "
                + "(semester_id, code, name, responsible_department_id, responsible_staff_id, "
                + "partner_company_id, partner_staff_id, bonus_point, penalty_point, address, description, "
                + "activity_status, approval_status, activity_type_id, start_time, end_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindActivity(ps, activity);
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert extracurricular activity.", e);
        }
    }

    @Override
    public boolean update(ExtracurricularActivity activity) {
        if (activity == null || activity.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE extracurricular_activity SET "
                + "semester_id = ?, code = ?, name = ?, responsible_department_id = ?, responsible_staff_id = ?, "
                + "partner_company_id = ?, partner_staff_id = ?, bonus_point = ?, penalty_point = ?, "
                + "address = ?, description = ?, activity_status = ?, approval_status = ?, activity_type_id = ?, "
                + "start_time = ?, end_time = ? WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindActivity(ps, activity);
            ps.setInt(17, activity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update extracurricular activity.", e);
        }
    }

    private void bindActivity(PreparedStatement ps, ExtracurricularActivity activity) throws SQLException {
        ps.setInt(1, activity.getSemesterId());
        ps.setString(2, activity.getCode());
        ps.setString(3, activity.getName());
        ps.setInt(4, activity.getResponsibleDepartmentId());
        ps.setInt(5, activity.getResponsibleStaffId());
        if (activity.getPartnerCompanyId() == null) {
            ps.setNull(6, Types.INTEGER);
        } else {
            ps.setInt(6, activity.getPartnerCompanyId());
        }
        if (activity.getPartnerStaffId() == null) {
            ps.setNull(7, Types.INTEGER);
        } else {
            ps.setInt(7, activity.getPartnerStaffId());
        }
        ps.setBigDecimal(8, activity.getBonusPoint());
        ps.setBigDecimal(9, activity.getPenaltyPoint());
        ps.setString(10, activity.getAddress());
        ps.setString(11, activity.getDescription());
        ps.setString(12, activity.getActivityStatus() == null ? null : activity.getActivityStatus().name());
        ps.setString(13, activity.getApprovalStatus() == null ? null : activity.getApprovalStatus().name());
        ps.setInt(14, activity.getActivityTypeId());
        ps.setTimestamp(15, activity.getStartTime() == null ? null : Timestamp.valueOf(activity.getStartTime()));
        ps.setTimestamp(16, activity.getEndTime() == null ? null : Timestamp.valueOf(activity.getEndTime()));
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM extracurricular_activity WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete extracurricular activity.", e);
        }
    }
}
