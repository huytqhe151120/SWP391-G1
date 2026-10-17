package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityCollaborationInvitationDAO;
import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ActivityCollaborationInvitationDAOImpl implements IActivityCollaborationInvitationDAO {

    private static final String SELECT_DETAILS = """
            SELECT i.id, i.activity_id, i.partner_company_id, i.partner_staff_id, i.approval_status,
                   i.created_by, i.created_at,
                   pc.code AS company_code, pc.name AS company_name,
                   ps.code AS staff_code, ps.name AS staff_name, ps.position AS staff_position,
                   acc.username AS created_by_name
            FROM activity_collaboration_invitation i
            JOIN partner_company pc ON i.partner_company_id = pc.id
            LEFT JOIN partner_staff ps ON i.partner_staff_id = ps.id
            LEFT JOIN account acc ON acc.id = TRY_CAST(i.created_by AS INT)
            """;

    private ActivityCollaborationInvitationDTO map(ResultSet rs) throws SQLException {
        ActivityCollaborationInvitationDTO dto = new ActivityCollaborationInvitationDTO();
        dto.setId(rs.getInt("id"));
        dto.setActivityId(rs.getInt("activity_id"));
        dto.setPartnerCompanyId(rs.getInt("partner_company_id"));
        dto.setPartnerCompanyCode(rs.getString("company_code"));
        dto.setPartnerCompanyName(rs.getString("company_name"));
        dto.setPartnerStaffId((Integer) rs.getObject("partner_staff_id"));
        dto.setPartnerStaffCode(rs.getString("staff_code"));
        dto.setPartnerStaffName(rs.getString("staff_name"));
        dto.setPartnerStaffPosition(rs.getString("staff_position"));
        dto.setApprovalStatus(rs.getString("approval_status"));
        dto.setCreatedBy(rs.getString("created_by"));
        dto.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        dto.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return dto;
    }

    @Override
    public List<ActivityCollaborationInvitationDTO> findByActivityId(int activityId) {
        List<ActivityCollaborationInvitationDTO> list = new ArrayList<>();
        String sql = SELECT_DETAILS + " WHERE i.activity_id = ? ORDER BY i.created_at DESC, i.id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find collaboration invitations of activity " + activityId, e);
        }
        return list;
    }

    @Override
    public ActivityCollaborationInvitationDTO findById(int id) {
        String sql = SELECT_DETAILS + " WHERE i.id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find collaboration invitation " + id, e);
        }
    }

    @Override
    public boolean existsOpenInvitation(int activityId, int partnerCompanyId) {
        String sql = "SELECT 1 FROM activity_collaboration_invitation "
                + "WHERE activity_id = ? AND partner_company_id = ? AND approval_status IN (?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, partnerCompanyId);
            ps.setString(3, PENDING);
            ps.setString(4, APPROVED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check existing collaboration invitation.", e);
        }
    }

    @Override
    public boolean insert(int activityId, int partnerCompanyId,
                          int partnerStaffId, String createdBy) {

        String sql = """
            INSERT INTO activity_collaboration_invitation
                (activity_id, partner_company_id, partner_staff_id,
                 approval_status, created_by)
            SELECT
                a.id, ?, ?, ?, ?
            FROM extracurricular_activity a
            WHERE a.id = ?
              AND a.activity_status = 'UPCOMING'
              AND a.start_time > SYSUTCDATETIME()
            """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, partnerCompanyId);
            ps.setInt(2, partnerStaffId);
            ps.setString(3, PENDING);
            ps.setString(4, createdBy);
            ps.setInt(5, activityId);

            // Chỉ thành công nếu SQL thực sự chèn được một bản ghi.
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to create collaboration invitation: "
                            + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(int id, String expectedStatus,
                                String newStatus) {

        String sql = """
            UPDATE i
            SET i.approval_status = ?
            FROM activity_collaboration_invitation i
            INNER JOIN extracurricular_activity a
                ON a.id = i.activity_id
            WHERE i.id = ?
              AND i.approval_status = ?
              AND a.activity_status = 'UPCOMING'
              AND a.start_time > SYSUTCDATETIME()
            """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, id);
            ps.setString(3, expectedStatus);

            // Chỉ thành công nếu cập nhật được một bản ghi.
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to update collaboration invitation: "
                            + e.getMessage(), e);
        }
    }
}
