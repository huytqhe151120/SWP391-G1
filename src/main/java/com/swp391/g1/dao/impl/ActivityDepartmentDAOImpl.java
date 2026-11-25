package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityDepartmentDAO;
import com.swp391.g1.dto.response.ActivityDepartmentResponseDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai DAO bằng JDBC cho bảng đơn vị đồng phụ trách {@code activity_department}.
 * Quản lý việc truy vấn, kiểm tra tồn tại, thêm mới và xóa đơn vị đồng phụ trách (kèm dọn dẹp nhân sự thuộc đơn vị bị gỡ trong transaction).
 */
public class ActivityDepartmentDAOImpl implements IActivityDepartmentDAO {

    /**
     * Lấy danh sách các đơn vị đồng phụ trách của một hoạt động kèm mã, tên và trạng thái đơn vị.
     *
     * @param activityId ID hoạt động
     * @return Danh sách {@link ActivityDepartmentResponseDTO} sắp xếp tăng dần theo tên đơn vị
     */
    @Override
    public List<ActivityDepartmentResponseDTO> findByActivityId(int activityId) {
        List<ActivityDepartmentResponseDTO> list = new ArrayList<>();
        String sql = """
                SELECT ad.activity_id, ad.department_id, ad.created_at,
                       d.code AS department_code, d.name AS department_name, d.status AS department_status
                FROM activity_department ad
                JOIN department d ON d.id = ad.department_id
                WHERE ad.activity_id = ?
                ORDER BY d.name ASC
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả và ánh xạ sang ActivityDepartmentResponseDTO
                while (rs.next()) {
                    ActivityDepartmentResponseDTO dto = new ActivityDepartmentResponseDTO();
                    dto.setActivityId(rs.getInt("activity_id"));
                    dto.setDepartmentId(rs.getInt("department_id"));
                    dto.setDepartmentCode(rs.getString("department_code"));
                    dto.setDepartmentName(rs.getString("department_name"));
                    dto.setDepartmentStatus(rs.getString("department_status"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    // Chuyển created_at sang LocalDateTime nếu giá trị khác null
                    dto.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn đơn vị đồng phụ trách của hoạt động
            throw new IllegalStateException("Failed to find co-responsible departments for activity " + activityId, e);
        }
        return list;
    }

    /**
     * Thêm một đơn vị đồng phụ trách cho hoạt động, chỉ thực hiện khi hoạt động vẫn ở trạng thái {@code UPCOMING}
     * và chưa tới thời điểm bắt đầu (so sánh UTC).
     *
     * @param activityId   ID hoạt động
     * @param departmentId ID đơn vị đồng phụ trách cần thêm
     * @return {@code true} nếu chèn thành công, {@code false} nếu hoạt động không còn thỏa điều kiện
     */
    @Override
    public boolean insert(int activityId, int departmentId) {
        // Dùng INSERT ... SELECT ... WHERE EXISTS để đảm bảo ràng buộc hoạt động chưa bắt đầu ngay ở mức SQL
        String sql = """
                INSERT INTO activity_department (activity_id, department_id)
                SELECT ?, ?
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
            ps.setInt(2, departmentId);
            ps.setInt(3, activityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi thêm đơn vị đồng phụ trách
            throw new IllegalStateException("Failed to add co-responsible department to activity.", e);
        }
    }

    /**
     * Gỡ một đơn vị đồng phụ trách khỏi hoạt động trong cùng một transaction.
     * Sau khi xóa đơn vị đồng phụ trách, tự động dọn dẹp các nhân sự đã phân công không còn thuộc
     * đơn vị phụ trách chính hay bất kỳ đơn vị đồng phụ trách nào còn lại.
     *
     * @param activityId   ID hoạt động
     * @param departmentId ID đơn vị cần gỡ
     * @return {@code true} nếu xóa và commit thành công, {@code false} nếu không có bản ghi nào thỏa điều kiện xóa
     */
    @Override
    public boolean delete(int activityId, int departmentId) {
        // Khóa dòng hoạt động bằng gợi ý UPDLOCK, HOLDLOCK để tránh tranh chấp đồng thời khi xóa đơn vị và dọn nhân sự
        String deleteDepartmentSql = """
                DELETE ad
                FROM activity_department ad
                INNER JOIN extracurricular_activity ea WITH (UPDLOCK, HOLDLOCK)
                    ON ea.id = ad.activity_id
                WHERE ad.activity_id = ?
                  AND ad.department_id = ?
                  AND ea.activity_status = 'UPCOMING'
                  AND ea.start_time > SYSUTCDATETIME()
                """;
        try (Connection conn = DBContext.getConnection()) {
            // Tắt auto-commit để gộp thao tác xóa đơn vị và xóa nhân sự ngoài phạm vi vào cùng một transaction
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(deleteDepartmentSql)) {
                    ps.setInt(1, activityId);
                    ps.setInt(2, departmentId);
                    // Nếu không có dòng đơn vị đồng phụ trách nào bị xóa thì rollback và trả về false
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                // Dọn các nhân sự đã phân công nhưng không còn thuộc đơn vị phụ trách chính hoặc đơn vị đồng phụ trách còn lại
                deleteStaffOutsideDepartments(conn, activityId);
                // Xác nhận hoàn tất toàn bộ transaction
                conn.commit();
                return true;
            } catch (SQLException e) {
                // Rollback transaction khi có bất kỳ lỗi SQL nào trong quá trình xóa
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    // Gắn lỗi rollback làm suppressed exception để không che mất ngoại lệ gốc
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành IllegalStateException cho tầng service
            throw new IllegalStateException("Failed to remove co-responsible department from activity.", e);
        }
    }

    /**
     * Kiểm tra đơn vị đã được gán làm đơn vị đồng phụ trách cho hoạt động hay chưa.
     *
     * @param activityId   ID hoạt động
     * @param departmentId ID đơn vị
     * @return {@code true} nếu bản ghi đã tồn tại, ngược lại {@code false}
     */
    @Override
    public boolean exists(int activityId, int departmentId) {
        String sql = "SELECT 1 FROM activity_department WHERE activity_id = ? AND department_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi kiểm tra tồn tại đơn vị đồng phụ trách
            throw new IllegalStateException("Failed to check co-responsible department.", e);
        }
    }

    /**
     * Xóa các nhân sự đã phân công vào hoạt động nhưng không còn thuộc đơn vị phụ trách chính
     * hay bất kỳ đơn vị đồng phụ trách nào còn lại (chạy trên cùng {@link Connection} của transaction).
     *
     * @param conn       Kết nối JDBC đang mở transaction
     * @param activityId ID hoạt động cần dọn dẹp nhân sự
     * @return Số lượng bản ghi phân công nhân sự đã bị xóa
     * @throws SQLException Nếu có lỗi SQL khi thực thi câu lệnh xóa
     */
    private int deleteStaffOutsideDepartments(Connection conn, int activityId) throws SQLException {
        // Dùng NOT EXISTS để tìm và xóa những nhân sự trong activity_staff không còn bản ghi staff_assignment hợp lệ với hoạt động
        String sql = """
                DELETE FROM activity_staff
                WHERE activity_id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM staff_assignment sa
                      WHERE sa.staff_id = activity_staff.staff_id
                        AND (sa.department_id = (SELECT responsible_department_id
                                                 FROM extracurricular_activity WHERE id = ?)
                             OR sa.department_id IN (SELECT department_id FROM activity_department
                                                     WHERE activity_id = ?))
                  )
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, activityId);
            ps.setInt(3, activityId);
            return ps.executeUpdate();
        }
    }
}
