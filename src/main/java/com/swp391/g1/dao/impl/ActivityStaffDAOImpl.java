package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityStaffDAO;
import com.swp391.g1.dto.response.ActivityStaffResponseDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ActivityStaffDAOImpl implements IActivityStaffDAO {

    @Override
    public List<ActivityStaffResponseDTO> findStaffsByActivityId(int activityId) {
        List<ActivityStaffResponseDTO> list = new ArrayList<>();
        String sql = """
                SELECT ast.activity_id, ast.staff_id, ast.assigned_on,
                       s.code AS staff_code, s.name AS staff_name, s.gender AS staff_gender, s.status AS staff_status,
                       sa.position AS staff_position, sa.department_id, d.name AS department_name
                FROM activity_staff ast
                 JOIN staff s ON ast.staff_id = s.id
                JOIN extracurricular_activity ea ON ast.activity_id = ea.id
                LEFT JOIN staff_assignment sa ON s.id = sa.staff_id AND sa.department_id = ea.responsible_department_id
                LEFT JOIN department d ON sa.department_id = d.id
                WHERE ast.activity_id = ?
                ORDER BY ast.assigned_on DESC
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityStaffResponseDTO dto = new ActivityStaffResponseDTO();
                    dto.setActivityId(rs.getInt("activity_id"));
                    dto.setStaffId(rs.getInt("staff_id"));
                    Timestamp ts = rs.getTimestamp("assigned_on");
                    dto.setAssignedOn(ts == null ? null : ts.toLocalDateTime());
                    dto.setStaffCode(rs.getString("staff_code"));
                    dto.setStaffName(rs.getString("staff_name"));
                    boolean gender = rs.getBoolean("staff_gender");
                    dto.setStaffGender(rs.wasNull() ? null : (gender ? "Nam" : "Nữ"));
                    dto.setStaffStatus(rs.getString("staff_status"));
                    dto.setPosition(rs.getString("staff_position"));
                    dto.setDepartmentId(rs.getInt("department_id"));
                    dto.setDepartmentName(rs.getString("department_name"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find assigned staffs for activity " + activityId, e);
        }
        return list;
    }

    @Override
    public boolean insert(int activityId, int staffId) {
        String sql = "INSERT INTO activity_staff (activity_id, staff_id, assigned_on) VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, staffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to assign staff to activity.", e);
        }
    }

    @Override
    public boolean delete(int activityId, int staffId) {
        String sql = "DELETE FROM activity_staff WHERE activity_id = ? AND staff_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, staffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete assigned staff from activity.", e);
        }
    }

    @Override
    public boolean exists(int activityId, int staffId) {
        String sql = "SELECT 1 FROM activity_staff WHERE activity_id = ? AND staff_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check if staff is assigned to activity.", e);
        }
    }

    @Override
    public boolean isStaffInDepartment(int staffId, int departmentId) {
        String sql = "SELECT 1 FROM staff_assignment WHERE staff_id = ? AND department_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setInt(2, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check if staff belongs to department.", e);
        }
    }
}
