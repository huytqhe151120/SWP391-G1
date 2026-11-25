package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityPartnerCompanyDAO;
import com.swp391.g1.dto.response.ActivityPartnerCompanyDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai DAO bằng JDBC cho bảng liên kết công ty đối tác tham gia hoạt động {@code activity_partner_company}.
 * Cung cấp các truy vấn chi tiết thông tin công ty đối tác và nhân sự đối tác phụ trách chính của hoạt động.
 */
public class ActivityPartnerCompanyDAOImpl implements IActivityPartnerCompanyDAO {

    // Câu truy vấn nền JOIN 3 bảng: activity_partner_company, partner_company và partner_staff
    private static final String SELECT_DETAILS = """
            SELECT apc.id, apc.activity_id, apc.partner_company_id, apc.partner_staff_id, apc.created_at, apc.collaboration_status,
                   pc.code AS company_code, pc.name AS company_name,
                   ps.code AS staff_code, ps.name AS staff_name, ps.position AS staff_position
            FROM activity_partner_company apc
            JOIN partner_company pc ON apc.partner_company_id = pc.id
            JOIN partner_staff ps ON apc.partner_staff_id = ps.id
            """;

    /**
     * Ánh xạ dòng hiện tại của {@link ResultSet} sang đối tượng {@link ActivityPartnerCompanyDTO}.
     *
     * @param rs ResultSet đang trỏ tới bản ghi cần đọc
     * @return Đối tượng {@link ActivityPartnerCompanyDTO} chứa đầy đủ thông tin công ty và nhân sự đại diện
     * @throws SQLException Nếu có lỗi khi đọc cột từ ResultSet
     */
    private ActivityPartnerCompanyDTO map(ResultSet rs) throws SQLException {
        ActivityPartnerCompanyDTO dto = new ActivityPartnerCompanyDTO();
        dto.setId(rs.getInt("id"));
        dto.setActivityId(rs.getInt("activity_id"));
        dto.setPartnerCompanyId(rs.getInt("partner_company_id"));
        dto.setPartnerCompanyCode(rs.getString("company_code"));
        dto.setPartnerCompanyName(rs.getString("company_name"));
        dto.setPartnerStaffId(rs.getInt("partner_staff_id"));
        dto.setCollaborationStatus(rs.getString("collaboration_status"));
        dto.setPartnerStaffCode(rs.getString("staff_code"));
        dto.setPartnerStaffName(rs.getString("staff_name"));
        dto.setPartnerStaffPosition(rs.getString("staff_position"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        // Chuyển created_at sang LocalDateTime nếu khác null, ngược lại gán null
        dto.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return dto;
    }

    /**
     * Lấy danh sách các công ty đối tác tham gia hợp tác trong một hoạt động, sắp xếp theo thời gian tạo và ID tăng dần.
     *
     * @param activityId ID hoạt động cần tra cứu
     * @return Danh sách {@link ActivityPartnerCompanyDTO} (rỗng nếu không có đối tác tham gia)
     */
    @Override
    public List<ActivityPartnerCompanyDTO> findByActivityId(int activityId) {
        List<ActivityPartnerCompanyDTO> list = new ArrayList<>();
        String sql = SELECT_DETAILS + " WHERE apc.activity_id = ? ORDER BY apc.created_at, apc.id";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng bản ghi công ty đối tác của hoạt động và ánh xạ vào danh sách
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn danh sách công ty đối tác theo activityId
            throw new IllegalStateException("Failed to find partner companies of activity " + activityId, e);
        }
        return list;
    }

    /**
     * Tìm thông tin chi tiết một bản ghi hợp tác công ty đối tác theo ID khóa chính.
     *
     * @param id ID của bản ghi trong bảng {@code activity_partner_company}
     * @return Đối tượng {@link ActivityPartnerCompanyDTO} nếu tìm thấy, ngược lại {@code null}
     */
    @Override
    public ActivityPartnerCompanyDTO findById(int id) {
        String sql = SELECT_DETAILS + " WHERE apc.id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Ánh xạ bản ghi nếu tồn tại, ngược lại trả về null
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn bản ghi hợp tác công ty đối tác theo ID
            throw new IllegalStateException("Failed to find activity partner company " + id, e);
        }
    }
}
