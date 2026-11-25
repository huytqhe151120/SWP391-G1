package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityPartnerStaffDAO;
import com.swp391.g1.dto.response.ActivityPartnerStaffResponseDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai DAO bằng JDBC cho bảng phân công nhân sự đối tác {@code activity_partner_staff}.
 * Cung cấp các thao tác truy vấn, kiểm tra tồn tại và thêm/xóa phân công nhân sự đối tác theo điều kiện hoạt động chưa bắt đầu.
 */
public class ActivityPartnerStaffDAOImpl implements IActivityPartnerStaffDAO {

    /**
     * Lấy danh sách nhân sự đối tác đã được phân công vào hoạt động kèm thông tin cá nhân và công ty đối tác.
     *
     * @param activityId ID hoạt động cần tra cứu
     * @return Danh sách {@link ActivityPartnerStaffResponseDTO} sắp xếp theo thời điểm phân công mới nhất trước
     */
    @Override
    public List<ActivityPartnerStaffResponseDTO> findByActivityId(int activityId) {
        List<ActivityPartnerStaffResponseDTO> list = new ArrayList<>();
        // JOIN bảng partner_staff và LEFT JOIN partner_company để lấy đầy đủ thông tin nhân sự đối tác và tên công ty
        String sql = """
                SELECT aps.activity_id, aps.partner_staff_id, aps.assigned_on,
                       ps.code, ps.name, ps.status, ps.position, ps.company_id, pc.name AS company_name
                FROM activity_partner_staff aps
                JOIN partner_staff ps ON aps.partner_staff_id = ps.id
                LEFT JOIN partner_company pc ON ps.company_id = pc.id
                WHERE aps.activity_id = ?
                ORDER BY aps.assigned_on DESC
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả và ánh xạ sang ActivityPartnerStaffResponseDTO
                while (rs.next()) {
                    ActivityPartnerStaffResponseDTO dto = new ActivityPartnerStaffResponseDTO();
                    dto.setActivityId(rs.getInt("activity_id"));
                    dto.setPartnerStaffId(rs.getInt("partner_staff_id"));
                    Timestamp ts = rs.getTimestamp("assigned_on");
                    // Chuyển thời điểm phân công sang LocalDateTime nếu giá trị trong DB khác null
                    dto.setAssignedOn(ts == null ? null : ts.toLocalDateTime());
                    dto.setPartnerStaffCode(rs.getString("code"));
                    dto.setPartnerStaffName(rs.getString("name"));
                    dto.setStatus(rs.getString("status"));
                    dto.setPosition(rs.getString("position"));
                    dto.setCompanyId(rs.getInt("company_id"));
                    dto.setCompanyName(rs.getString("company_name"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn danh sách nhân sự đối tác của hoạt động
            throw new IllegalStateException("Failed to find assigned partner staffs for activity " + activityId, e);
        }
        return list;
    }

    /**
     * Phân công một nhân sự đối tác vào hoạt động với dấu thời gian theo múi giờ Việt Nam (+07:00).
     * Chỉ chèn khi hoạt động vẫn ở trạng thái {@code UPCOMING} và {@code start_time} lớn hơn thời điểm UTC hiện tại.
     *
     * @param activityId     ID hoạt động
     * @param partnerStaffId ID nhân sự đối tác
     * @return {@code true} nếu chèn thành công 1 bản ghi, {@code false} nếu hoạt động không còn thỏa điều kiện
     */
    @Override
    public boolean insert(int activityId, int partnerStaffId) {
        // Dùng INSERT ... SELECT ... WHERE EXISTS để kiểm tra trạng thái UPCOMING và thời gian bắt đầu ngay tại tầng DB
        String sql = """
                INSERT INTO activity_partner_staff
                    (activity_id, partner_staff_id, assigned_on)
                SELECT ?, ?, CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2)
                WHERE EXISTS (
                    SELECT 1
                    FROM extracurricular_activity
                    WHERE id = ?
                      AND activity_status = 'UPCOMING'
                      AND start_time > SYSUTCDATETIME()
                )
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, partnerStaffId);
            ps.setInt(3, activityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi phân công nhân sự đối tác vào hoạt động
            throw new IllegalStateException("Failed to assign partner staff to activity: " + e.getMessage(), e);
        }
    }

    /**
     * Gỡ phân công một nhân sự đối tác khỏi hoạt động, chỉ thực hiện khi hoạt động vẫn {@code UPCOMING} và chưa bắt đầu.
     *
     * @param activityId     ID hoạt động
     * @param partnerStaffId ID nhân sự đối tác cần gỡ
     * @return {@code true} nếu có bản ghi bị xóa, ngược lại {@code false}
     */
    @Override
    public boolean delete(int activityId, int partnerStaffId) {
        // JOIN với extracurricular_activity để ràng buộc chỉ cho phép xóa trước thời điểm bắt đầu (UTC)
        String sql = """
                DELETE aps
                FROM activity_partner_staff aps
                INNER JOIN extracurricular_activity ea ON ea.id = aps.activity_id
                WHERE aps.activity_id = ?
                  AND aps.partner_staff_id = ?
                  AND ea.activity_status = 'UPCOMING'
                  AND ea.start_time > SYSUTCDATETIME()
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, partnerStaffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi gỡ nhân sự đối tác khỏi hoạt động
            throw new IllegalStateException("Failed to remove partner staff from activity.", e);
        }
    }

    /**
     * Kiểm tra nhân sự đối tác đã được phân công vào hoạt động hay chưa.
     *
     * @param activityId     ID hoạt động
     * @param partnerStaffId ID nhân sự đối tác
     * @return {@code true} nếu bản ghi phân công đã tồn tại, ngược lại {@code false}
     */
    @Override
    public boolean exists(int activityId, int partnerStaffId) {
        String sql = "SELECT 1 FROM activity_partner_staff WHERE activity_id = ? AND partner_staff_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, partnerStaffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi kiểm tra tồn tại phân công nhân sự đối tác
            throw new IllegalStateException("Failed to check partner staff assignment.", e);
        }
    }
}
