package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityCollaborationInvitationDAO;
import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ActivityCollaborationInvitationDAOImpl implements IActivityCollaborationInvitationDAO {

    // Câu truy vấn dùng chung JOIN lời mời với công ty đối tác, nhân sự phụ trách, tài khoản tạo, liên kết hợp tác và thông tin hoạt động
    private static final String SELECT_DETAILS = """
            SELECT i.id, i.activity_id, i.partner_company_id, i.partner_staff_id, i.approval_status,
                   i.invitation_type, i.message, i.created_by, i.created_at,
                   pc.code AS company_code, pc.name AS company_name,
                   ps.code AS staff_code, ps.name AS staff_name, ps.position AS staff_position,
                   acc.username AS created_by_name,
                   apc.collaboration_status AS collaboration_status,
                   ea.code AS activity_code, ea.name AS activity_name, ea.approval_status AS activity_approval_status,
                   ea.activity_status AS activity_status, ea.start_time AS activity_start_time,
                   ea.end_time AS activity_end_time,
                   atype.name AS activity_type_name
            FROM activity_collaboration_invitation i
            JOIN partner_company pc ON i.partner_company_id = pc.id
            LEFT JOIN partner_staff ps ON i.partner_staff_id = ps.id
            LEFT JOIN account acc ON acc.id = TRY_CAST(i.created_by AS INT)
            LEFT JOIN activity_partner_company apc ON apc.invitation_id = i.id
            JOIN extracurricular_activity ea ON ea.id = i.activity_id
            LEFT JOIN activity_type atype ON atype.id = ea.activity_type_id
            """;

    /**
     * Ánh xạ một dòng dữ liệu từ ResultSet sang đối tượng ActivityCollaborationInvitationDTO.
     *
     * @param rs con trỏ ResultSet đang đứng tại dòng dữ liệu hợp lệ
     * @return đối tượng DTO chứa đầy đủ thông tin lời mời, đối tác và hoạt động
     */
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
        dto.setInvitationType(rs.getString("invitation_type"));
        dto.setMessage(rs.getString("message"));
        dto.setCollaborationStatus(rs.getString("collaboration_status"));
        dto.setActivityCode(rs.getString("activity_code"));
        dto.setActivityName(rs.getString("activity_name"));
        dto.setActivityTypeName(rs.getString("activity_type_name"));
        dto.setActivityApprovalStatus(rs.getString("activity_approval_status"));
        dto.setActivityStatus(rs.getString("activity_status"));
        // Chuyển đổi an toàn các mốc thời gian từ SQL Timestamp sang LocalDateTime
        Timestamp start = rs.getTimestamp("activity_start_time");
        dto.setActivityStartTime(start == null ? null : start.toLocalDateTime());
        Timestamp end = rs.getTimestamp("activity_end_time");
        dto.setActivityEndTime(end == null ? null : end.toLocalDateTime());
        dto.setCreatedBy(rs.getString("created_by"));
        dto.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        dto.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return dto;
    }

    /**
     * Lấy danh sách tất cả lời mời phối hợp thuộc một hoạt động ngoại khóa, sắp xếp mới nhất trước.
     *
     * @param activityId ID hoạt động ngoại khóa
     * @return danh sách DTO lời mời phối hợp của hoạt động
     */
    @Override
    public List<ActivityCollaborationInvitationDTO> findByActivityId(int activityId) {
        List<ActivityCollaborationInvitationDTO> list = new ArrayList<>();
        String sql = SELECT_DETAILS + " WHERE i.activity_id = ? ORDER BY i.created_at DESC, i.id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt kết quả truy vấn và ánh xạ từng bản ghi sang DTO
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime kèm thông tin ID hoạt động
            throw new IllegalStateException("Failed to find collaboration invitations of activity " + activityId, e);
        }
        return list;
    }

    /**
     * Lấy danh sách lời mời phối hợp theo công ty đối tác, hoặc toàn bộ lời mời nếu partnerCompanyId là null.
     *
     * @param partnerCompanyId ID công ty đối tác cần lọc (null để lấy tất cả)
     * @return danh sách DTO lời mời sắp xếp mới nhất trước
     */
    @Override
    public List<ActivityCollaborationInvitationDTO> findByPartnerCompanyId(Integer partnerCompanyId) {
        List<ActivityCollaborationInvitationDTO> list = new ArrayList<>();
        // Ghép điều kiện WHERE động khi có truyền ID công ty đối tác
        String sql = SELECT_DETAILS + (partnerCompanyId == null ? "" : " WHERE i.partner_company_id = ?")
                + " ORDER BY i.created_at DESC, i.id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Gắn tham số công ty đối tác nếu có lọc theo công ty
            if (partnerCompanyId != null) {
                ps.setInt(1, partnerCompanyId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả và thêm vào danh sách trả về
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            // Ném ngoại lệ khi truy vấn danh sách lời mời thất bại
            throw new IllegalStateException("Failed to find collaboration invitations.", e);
        }
        return list;
    }

    /**
     * Lấy tập ID các hoạt động mà một công ty đối tác được phép xem
     * (gồm hoạt động có lời mời hợp tác PENDING/APPROVED hoặc đang hợp tác ACTIVE).
     *
     * @param partnerCompanyId ID công ty đối tác
     * @return tập ID hoạt động được phép hiển thị cho công ty
     */
    @Override
    public java.util.Set<Integer> findVisibleActivityIds(int partnerCompanyId) {
        // Hợp nhất (UNION) hoạt động từ lời mời hợp tác PENDING/APPROVED và bảng liên kết đối tác đang ACTIVE
        String sql = "SELECT activity_id FROM activity_collaboration_invitation "
                + "WHERE partner_company_id = ? AND invitation_type = ? AND approval_status IN (?, ?) "
                + "UNION SELECT activity_id FROM activity_partner_company "
                + "WHERE partner_company_id = ? AND collaboration_status = 'ACTIVE'";
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, partnerCompanyId);
            ps.setString(2, TYPE_COLLABORATION);
            ps.setString(3, PENDING);
            ps.setString(4, APPROVED);
            ps.setInt(5, partnerCompanyId);
            try (ResultSet rs = ps.executeQuery()) {
                // Thu thập tất cả ID hoạt động không trùng lặp vào tập kết quả
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            // Ném ngoại lệ khi truy vấn phạm vi hoạt động của công ty thất bại
            throw new IllegalStateException("Failed to find visible activities of partner company.", e);
        }
        return ids;
    }

    /**
     * Lấy tập ID các hoạt động mà nhân sự đối tác có quyền truy cập điểm danh
     * (toàn bộ hoạt động ACTIVE của công ty nếu companyWide = true, ngược lại chỉ hoạt động là đầu mối chính hoặc được phân công).
     *
     * @param partnerCompanyId ID công ty đối tác
     * @param partnerStaffId   ID nhân sự đối tác
     * @param companyWide      true nếu có phạm vi toàn công ty, false nếu giới hạn theo phân công cá nhân
     * @return tập ID hoạt động được phép điểm danh
     */
    @Override
    public java.util.Set<Integer> findAttendanceActivityIds(int partnerCompanyId, int partnerStaffId,
                                                            boolean companyWide) {
        // Truy vấn các hoạt động có liên kết ACTIVE của công ty và thỏa phạm vi toàn công ty, nhân sự chính hoặc bảng phân công nhân sự
        String sql = """
                SELECT DISTINCT apc.activity_id
                FROM activity_partner_company apc
                WHERE apc.partner_company_id = ?
                  AND apc.collaboration_status = 'ACTIVE'
                  AND (? = 1
                       OR apc.partner_staff_id = ?
                       OR EXISTS (
                           SELECT 1
                           FROM activity_partner_staff aps
                           WHERE aps.activity_id = apc.activity_id
                             AND aps.partner_staff_id = ?
                       ))
                """;
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, partnerCompanyId);
            // Truyền cờ 1/0 cho nhánh kiểm tra phạm vi toàn công ty trong SQL
            ps.setInt(2, companyWide ? 1 : 0);
            ps.setInt(3, partnerStaffId);
            ps.setInt(4, partnerStaffId);
            try (ResultSet rs = ps.executeQuery()) {
                // Đọc từng ID hoạt động đủ điều kiện điểm danh
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            // Ném ngoại lệ khi không thể truy vấn danh sách hoạt động điểm danh của nhân sự đối tác
            throw new IllegalStateException("Failed to find attendance activities of partner staff.", e);
        }
        return ids;
    }

    /**
     * Tự động hủy hàng loạt các lời mời đang PENDING thuộc những hoạt động không còn ở trạng thái UPCOMING
     * (đang diễn ra, đã hoàn thành hoặc đã hủy).
     *
     * @return số lượng bản ghi lời mời đã được chuyển sang CANCELLED
     */
    @Override
    public int cancelPendingForInactiveActivities() {
        // Cập nhật trạng thái PENDING -> CANCELLED với truy vấn con lọc các hoạt động đã ONGOING/COMPLETED/CANCELLED
        String sql = "UPDATE activity_collaboration_invitation SET approval_status = ? "
                + "WHERE approval_status = ? AND activity_id IN "
                + "(SELECT id FROM extracurricular_activity "
                + "WHERE activity_status IN ('ONGOING', 'COMPLETED', 'CANCELLED'))";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CANCELLED);
            ps.setString(2, PENDING);
            return ps.executeUpdate();
        } catch (SQLException e) {
            // Ném ngoại lệ khi thao tác đồng bộ hủy lời mời gặp lỗi SQL
            throw new IllegalStateException("Failed to cancel pending invitations for inactive activities.", e);
        }
    }

    /**
     * Tìm thông tin chi tiết một lời mời phối hợp theo ID.
     *
     * @param id ID lời mời cần tìm
     * @return DTO lời mời nếu tìm thấy, ngược lại trả về null
     */
    @Override
    public ActivityCollaborationInvitationDTO findById(int id) {
        String sql = SELECT_DETAILS + " WHERE i.id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Trả về bản ghi đã ánh xạ nếu tồn tại, ngược lại trả về null
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            // Ném ngoại lệ khi truy vấn lời mời theo ID thất bại
            throw new IllegalStateException("Failed to find collaboration invitation " + id, e);
        }
    }

    /**
     * Kiểm tra xem giữa hoạt động và công ty đối tác có đang tồn tại lời mời ở trạng thái PENDING hay không.
     *
     * @param activityId       ID hoạt động
     * @param partnerCompanyId ID công ty đối tác
     * @return true nếu đã có lời mời đang chờ phản hồi
     */
    @Override
    public boolean existsPendingInvitation(int activityId, int partnerCompanyId) {
        String sql = "SELECT 1 FROM activity_collaboration_invitation "
                + "WHERE activity_id = ? AND partner_company_id = ? AND approval_status = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, partnerCompanyId);
            ps.setString(3, PENDING);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Ném ngoại lệ khi kiểm tra lời mời PENDING thất bại
            throw new IllegalStateException("Failed to check pending collaboration invitation.", e);
        }
    }

    /**
     * Kiểm tra xem công ty đối tác đã có lời mời mở (đang PENDING) hoặc đang hợp tác ACTIVE trong hoạt động chưa.
     *
     * @param activityId       ID hoạt động
     * @param partnerCompanyId ID công ty đối tác
     * @return true nếu đang có lời mời chờ phản hồi hoặc đang hợp tác ACTIVE
     */
    @Override
    public boolean existsOpenInvitation(int activityId, int partnerCompanyId) {
        // Trả về true ngay nếu đang có lời mời ở trạng thái PENDING
        if (existsPendingInvitation(activityId, partnerCompanyId)) {
            return true;
        }
        String sql = "SELECT 1 FROM activity_partner_company "
                + "WHERE activity_id = ? AND partner_company_id = ? AND collaboration_status = 'ACTIVE'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, partnerCompanyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Ném ngoại lệ khi kiểm tra trạng thái hợp tác hiện hành thất bại
            throw new IllegalStateException("Failed to check existing collaboration.", e);
        }
    }

    /**
     * Chèn mới một lời mời (hợp tác hoặc ngừng hợp tác) ở trạng thái PENDING trong transaction,
     * chỉ thành công khi hoạt động vẫn đang UPCOMING và chưa đến giờ bắt đầu tại thời điểm ghi CSDL.
     *
     * @param activityId       ID hoạt động
     * @param partnerCompanyId ID công ty đối tác
     * @param partnerStaffId   ID nhân sự đối tác phụ trách chính
     * @param createdBy        ID tài khoản tạo lời mời
     * @param invitationType   loại lời mời (COLLABORATION hoặc TERMINATION)
     * @param message          nội dung lời nhắn đính kèm
     * @return ID lời mời vừa tạo, hoặc 0 nếu điều kiện hoạt động UPCOMING không còn thỏa mãn
     */
    @Override
    public int insert(int activityId, int partnerCompanyId, int partnerStaffId, String createdBy,
                      String invitationType, String message) {
        // Dùng INSERT ... SELECT WHERE EXISTS để đảm bảo kiểm tra hoạt động UPCOMING & chưa quá start_time ngay ở tầng CSDL
        String insertSql = """
                INSERT INTO activity_collaboration_invitation
                    (activity_id, partner_company_id, partner_staff_id, approval_status,
                     invitation_type, message, created_by)
                SELECT ?, ?, ?, ?, ?, ?, ?
                WHERE EXISTS (
                    SELECT 1
                    FROM extracurricular_activity
                    WHERE id = ?
                      AND activity_status = 'UPCOMING'
                      AND start_time > SYSUTCDATETIME()
                )
                """;
        try (Connection conn = DBContext.getConnection()) {
            // Mở transaction thủ công để kiểm soát rollback khi chèn không sinh được khóa chính
            conn.setAutoCommit(false);
            try {
                int invitationId;
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, activityId);
                    ps.setInt(2, partnerCompanyId);
                    ps.setInt(3, partnerStaffId);
                    ps.setString(4, PENDING);
                    ps.setString(5, invitationType);
                    ps.setString(6, message);
                    ps.setString(7, createdBy);
                    ps.setInt(8, activityId);
                    // Nếu không có dòng nào được chèn (do hoạt động không còn UPCOMING hoặc đã bắt đầu) thì rollback và trả về 0
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return 0;
                    }
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        // Nếu không lấy được ID tự tăng vừa tạo thì rollback và trả về 0
                        if (!keys.next()) {
                            conn.rollback();
                            return 0;
                        }
                        invitationId = keys.getInt(1);
                    }
                }
                conn.commit();
                return invitationId;
            } catch (SQLException e) {
                // Rollback toàn bộ transaction khi có lỗi SQL trong quá trình chèn
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime để tầng trên xử lý
            throw new IllegalStateException(
                    "Failed to create collaboration invitation: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Chấp nhận lời mời đang PENDING (chuyển sang APPROVED và cập nhật trạng thái hợp tác tương ứng).
     *
     * @param id ID lời mời
     * @return true nếu cập nhật thành công
     */
    @Override
    public boolean accept(int id) {
        return changeStatus(id, PENDING, APPROVED);
    }

    /**
     * Từ chối lời mời đang PENDING (chuyển sang REJECTED).
     *
     * @param id ID lời mời
     * @return true nếu cập nhật thành công
     */
    @Override
    public boolean reject(int id) {
        return changeStatus(id, PENDING, REJECTED);
    }

    /**
     * Hủy lời mời đang PENDING (chuyển sang CANCELLED).
     *
     * @param id ID lời mời
     * @return true nếu cập nhật thành công
     */
    @Override
    public boolean cancel(int id) {
        return changeStatus(id, PENDING, CANCELLED);
    }

    /**
     * Thu hồi quyết định chấp nhận (chuyển từ APPROVED về PENDING và vô hiệu hóa liên kết hợp tác/nhân sự liên quan).
     *
     * @param id ID lời mời
     * @return true nếu cập nhật thành công
     */
    @Override
    public boolean revokeApproval(int id) {
        return changeStatus(id, APPROVED, PENDING);
    }

    /**
     * Thu hồi quyết định từ chối (chuyển từ REJECTED về PENDING).
     *
     * @param id ID lời mời
     * @return true nếu cập nhật thành công
     */
    @Override
    public boolean revokeRejection(int id) {
        return changeStatus(id, REJECTED, PENDING);
    }

    /**
     * Chuyển trạng thái lời mời từ {@code fromStatus} sang {@code toStatus} và đồng bộ hệ quả lên
     * bảng {@code activity_partner_company} và {@code activity_partner_staff} trong cùng một transaction.
     * - Lời mời COLLABORATION điều khiển trạng thái hợp tác của bản ghi đối tác tương ứng.
     * - Yêu cầu TERMINATION chỉ tác động chấm dứt hợp tác khi được chấp nhận (APPROVED).
     *
     * @param id         ID lời mời cần chuyển trạng thái
     * @param fromStatus trạng thái nguồn yêu cầu khớp trước khi cập nhật
     * @param toStatus   trạng thái đích cần chuyển tới
     * @return true nếu chuyển trạng thái và đồng bộ dữ liệu thành công, ngược lại false
     */
    private boolean changeStatus(int id, String fromStatus, String toStatus) {
        // Cập nhật trạng thái lời mời có điều kiện ràng buộc: khớp trạng thái cũ và hoạt động vẫn UPCOMING, chưa bắt đầu
        String updateInvitation = """
                UPDATE i
                SET approval_status = ?
                FROM activity_collaboration_invitation i
                JOIN extracurricular_activity a ON a.id = i.activity_id
                WHERE i.id = ?
                  AND i.approval_status = ?
                  AND a.activity_status = 'UPCOMING'
                  AND a.start_time > SYSUTCDATETIME()
                """;
        // Lấy loại lời mời để quyết định logic đồng bộ bảng liên kết đối tác
        String selectType = "SELECT invitation_type FROM activity_collaboration_invitation WHERE id = ?";
        // Cập nhật trạng thái hợp tác của bản ghi đối tác gắn trực tiếp với lời mời này
        String updateOwnLink = "UPDATE activity_partner_company SET collaboration_status = ? WHERE invitation_id = ?";
        // Tái sử dụng bản ghi activity_partner_company đã tồn tại (nếu đối tác từng tham gia trước đó) và kích hoạt lại ACTIVE
        String reuseLink = """
                UPDATE apc SET partner_staff_id = i.partner_staff_id, invitation_id = i.id,
                       collaboration_status = 'ACTIVE'
                FROM activity_partner_company apc
                JOIN activity_collaboration_invitation i ON i.activity_id = apc.activity_id
                     AND i.partner_company_id = apc.partner_company_id
                WHERE i.id = ?
                """;
        // Tạo mới bản ghi liên kết đối tác ACTIVE khi công ty lần đầu hợp tác trong hoạt động này
        String createLink = "INSERT INTO activity_partner_company "
                + "(activity_id, partner_company_id, partner_staff_id, invitation_id, collaboration_status) "
                + "SELECT activity_id, partner_company_id, partner_staff_id, id, 'ACTIVE' "
                + "FROM activity_collaboration_invitation WHERE id = ? AND partner_staff_id IS NOT NULL";
        // Vô hiệu hóa (INACTIVE) liên kết hợp tác của công ty đối tác trong hoạt động
        String deactivateCompanyLink = """
                UPDATE apc SET collaboration_status = 'INACTIVE'
                FROM activity_partner_company apc
                JOIN activity_collaboration_invitation i ON i.activity_id = apc.activity_id
                     AND i.partner_company_id = apc.partner_company_id
                WHERE i.id = ?
                """;
        // Xóa toàn bộ phân công nhân sự của công ty đối tác trong hoạt động khi chấm dứt/hủy chấp nhận hợp tác
        String deleteStaff = """
                DELETE aps FROM activity_partner_staff aps
                JOIN partner_staff ps ON aps.partner_staff_id = ps.id
                JOIN activity_collaboration_invitation i ON i.activity_id = aps.activity_id
                     AND i.partner_company_id = ps.company_id
                WHERE i.id = ?
                """;
        try (Connection conn = DBContext.getConnection()) {
            // Bắt đầu transaction đảm bảo tính nhất quán giữa bảng lời mời, bảng đối tác và bảng phân công nhân sự
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(updateInvitation)) {
                    ps.setString(1, toStatus);
                    ps.setInt(2, id);
                    ps.setString(3, fromStatus);
                    // Nếu không có dòng nào được cập nhật (sai trạng thái hoặc hoạt động đã bắt đầu) thì rollback và dừng
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                String type;
                try (PreparedStatement ps = conn.prepareStatement(selectType)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Mặc định coi là COLLABORATION nếu không đọc được giá trị loại lời mời
                        type = rs.next() ? rs.getString(1) : TYPE_COLLABORATION;
                    }
                }

                // Nhánh xử lý cho yêu cầu ngừng hợp tác (TERMINATION)
                if (TYPE_TERMINATION.equals(type)) {
                    // Chỉ gỡ phân công nhân sự và chuyển liên kết công ty sang INACTIVE khi yêu cầu ngừng hợp tác được chấp nhận
                    if (APPROVED.equals(toStatus)) {
                        executeById(conn, deleteStaff, id);
                        executeById(conn, deactivateCompanyLink, id);
                    }
                } else {
                    // Nhánh xử lý cho lời mời hợp tác (COLLABORATION)
                    // Nếu đang thu hồi từ trạng thái APPROVED thì xóa phân công nhân sự đối tác đã gán cho hoạt động
                    if (APPROVED.equals(fromStatus)) {
                        executeById(conn, deleteStaff, id);
                    }
                    // Nếu lời mời hợp tác chuyển sang APPROVED thì kích hoạt hoặc tạo mới bản ghi đối tác ACTIVE
                    if (APPROVED.equals(toStatus)) {
                        // Chỉ thêm đối tác vào hoạt động sau khi lời mời được duyệt (ưu tiên tái sử dụng bản ghi cũ nếu có)
                        int linked;
                        try (PreparedStatement ps = conn.prepareStatement(reuseLink)) {
                            ps.setInt(1, id);
                            linked = ps.executeUpdate();
                        }
                        // Nếu chưa từng có bản ghi liên kết trước đó thì thực hiện INSERT mới
                        if (linked == 0) {
                            try (PreparedStatement ps = conn.prepareStatement(createLink)) {
                                ps.setInt(1, id);
                                // Nếu không tạo được liên kết (ví dụ thiếu partner_staff_id) thì rollback toàn bộ
                                if (ps.executeUpdate() == 0) {
                                    conn.rollback();
                                    return false;
                                }
                            }
                        }
                    } else {
                        // Các chuyển trạng thái khác (từ chối, hủy, hoặc thu hồi duyệt về PENDING) đều chuyển liên kết của lời mời sang INACTIVE
                        try (PreparedStatement ps = conn.prepareStatement(updateOwnLink)) {
                            ps.setString(1, "INACTIVE");
                            ps.setInt(2, id);
                            ps.executeUpdate();
                        }
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                // Rollback transaction khi có lỗi SQL phát sinh giữa chừng
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime kèm thông điệp chi tiết
            throw new IllegalStateException(
                    "Failed to update collaboration invitation: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Thực thi nhanh một câu lệnh SQL cập nhật/xóa nhận duy nhất một tham số ID nguyên trên kết nối transaction hiện tại.
     *
     * @param conn kết nối JDBC đang trong transaction
     * @param sql  câu lệnh SQL có 1 tham số dấu hỏi (?)
     * @param id   giá trị ID truyền vào tham số thứ nhất
     */
    private void executeById(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
