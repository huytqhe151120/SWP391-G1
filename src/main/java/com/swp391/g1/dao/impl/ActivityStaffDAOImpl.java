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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp triển khai DAO bằng JDBC cho bảng phân công nhân sự nội bộ {@code activity_staff}.
 * Hỗ trợ truy vấn danh sách nhân sự phân công, nhãn đơn vị theo nhân sự, kiểm tra thuộc đơn vị và thêm/xóa phân công an toàn theo trạng thái hoạt động.
 */
public class ActivityStaffDAOImpl implements IActivityStaffDAO {

    /**
     * Lấy danh sách nhân sự đã được phân công vào hoạt động kèm thông tin cá nhân, chức vụ và đơn vị ưu tiên.
     *
     * @param activityId ID hoạt động cần tra cứu
     * @return Danh sách {@link ActivityStaffResponseDTO} sắp xếp theo thời điểm phân công mới nhất trước
     */
    @Override
    public List<ActivityStaffResponseDTO> findStaffsByActivityId(int activityId) {
        List<ActivityStaffResponseDTO> list = new ArrayList<>();
        // Truy vấn kết hợp OUTER APPLY để lấy 1 chức vụ/đơn vị đại diện của nhân sự trong phạm vi các đơn vị của hoạt động,
        // ưu tiên đơn vị phụ trách chính (priority = 0) trước các đơn vị đồng phụ trách (priority = 1)
        String sql = """
                SELECT ast.activity_id, ast.staff_id, ast.assigned_on,
                       s.code AS staff_code, s.name AS staff_name, s.gender AS staff_gender, s.status AS staff_status,
                       sa.position AS staff_position, sa.department_id, d.name AS department_name
                FROM activity_staff ast
                 JOIN staff s ON ast.staff_id = s.id
                JOIN extracurricular_activity ea ON ast.activity_id = ea.id
                OUTER APPLY (
                    SELECT TOP 1 x.position, x.department_id
                    FROM staff_assignment x
                    WHERE x.staff_id = s.id
                      AND (x.department_id = ea.responsible_department_id
                           OR x.department_id IN (SELECT ad.department_id FROM activity_department ad
                                                  WHERE ad.activity_id = ea.id))
                    ORDER BY CASE WHEN x.department_id = ea.responsible_department_id THEN 0 ELSE 1 END
                ) sa
                LEFT JOIN department d ON sa.department_id = d.id
                WHERE ast.activity_id = ?
                ORDER BY ast.assigned_on DESC
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả và ánh xạ sang ActivityStaffResponseDTO
                while (rs.next()) {
                    ActivityStaffResponseDTO dto = new ActivityStaffResponseDTO();
                    dto.setActivityId(rs.getInt("activity_id"));
                    dto.setStaffId(rs.getInt("staff_id"));
                    Timestamp ts = rs.getTimestamp("assigned_on");
                    // Chuyển thời điểm phân công sang LocalDateTime nếu khác null
                    dto.setAssignedOn(ts == null ? null : ts.toLocalDateTime());
                    dto.setStaffCode(rs.getString("staff_code"));
                    dto.setStaffName(rs.getString("staff_name"));
                    boolean gender = rs.getBoolean("staff_gender");
                    // Nếu cột gender bị NULL trong DB thì để null; ngược lại true -> "Nam", false -> "Nữ"
                    dto.setStaffGender(rs.wasNull() ? null : (gender ? "Nam" : "Nữ"));
                    dto.setStaffStatus(rs.getString("staff_status"));
                    dto.setPosition(rs.getString("staff_position"));
                    dto.setDepartmentId(rs.getInt("department_id"));
                    dto.setDepartmentName(rs.getString("department_name"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn danh sách nhân sự của hoạt động
            throw new IllegalStateException("Failed to find assigned staffs for activity " + activityId, e);
        }
        return list;
    }

    /**
     * Lấy bản đồ nhãn đơn vị hiển thị cho từng nhân sự thuộc phạm vi các đơn vị của hoạt động.
     * Nếu một nhân sự thuộc nhiều đơn vị tham gia hoạt động, các nhãn được nối bằng dấu phẩy theo thứ tự ưu tiên đơn vị phụ trách chính trước.
     *
     * @param activityId ID hoạt động
     * @return Map ánh xạ {@code staffId -> "CODE - Name, ..."}
     */
    @Override
    public Map<Integer, String> findDepartmentLabelsByStaff(int activityId) {
        Map<Integer, String> labels = new LinkedHashMap<>();
        // Lấy tất cả cặp (nhân sự, đơn vị) thuộc đơn vị phụ trách chính hoặc đồng phụ trách, sắp xếp ưu tiên đơn vị chính rồi theo mã đơn vị
        String sql = """
                SELECT DISTINCT sa.staff_id, d.code, d.name,
                       CASE WHEN d.id = ea.responsible_department_id THEN 0 ELSE 1 END AS priority
                FROM extracurricular_activity ea
                JOIN staff_assignment sa ON sa.department_id = ea.responsible_department_id
                     OR sa.department_id IN (SELECT ad.department_id FROM activity_department ad
                                             WHERE ad.activity_id = ea.id)
                JOIN department d ON d.id = sa.department_id
                WHERE ea.id = ?
                ORDER BY sa.staff_id, priority, d.code
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng và gộp nhãn đơn vị theo staffId bằng phương thức merge
                while (rs.next()) {
                    String label = rs.getString("code") + " - " + rs.getString("name");
                    labels.merge(rs.getInt("staff_id"), label, (a, b) -> a + ", " + b);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tải nhãn đơn vị theo nhân sự
            throw new IllegalStateException("Failed to find department labels for activity " + activityId, e);
        }
        return labels;
    }

    /**
     * Thêm bản ghi phân công nhân sự vào hoạt động với thời gian phân công theo múi giờ Việt Nam (GMT+7).
     * Câu lệnh SQL kiểm tra nguyên tử (atomic) điều kiện hoạt động vẫn ở trạng thái {@code UPCOMING} và chưa tới {@code start_time} (UTC).
     *
     * @param activityId ID hoạt động
     * @param staffId    ID nhân sự được phân công
     * @return {@code true} nếu chèn thành công 1 dòng, {@code false} nếu hoạt động không còn thỏa điều kiện
     */
    @Override
    public boolean insert(int activityId, int staffId) {
        // Dùng INSERT ... SELECT ... WHERE EXISTS để đảm bảo ràng buộc thời gian ngay tại tầng DB, tránh race condition
        String sql = """
                INSERT INTO activity_staff (activity_id, staff_id, assigned_on)
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
            ps.setInt(2, staffId);
            ps.setInt(3, activityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi phân công nhân sự vào hoạt động
            throw new IllegalStateException("Failed to assign staff to activity.", e);
        }
    }

    /**
     * Xóa bản ghi phân công nhân sự khỏi hoạt động, chỉ thực thi khi hoạt động vẫn {@code UPCOMING} và chưa bắt đầu.
     *
     * @param activityId ID hoạt động
     * @param staffId    ID nhân sự cần gỡ
     * @return {@code true} nếu có bản ghi bị xóa, ngược lại {@code false}
     */
    @Override
    public boolean delete(int activityId, int staffId) {
        // JOIN với extracurricular_activity để ràng buộc chỉ cho xóa trước thời điểm bắt đầu (so sánh UTC)
        String sql = """
                DELETE ast
                FROM activity_staff ast
                INNER JOIN extracurricular_activity ea ON ea.id = ast.activity_id
                WHERE ast.activity_id = ?
                  AND ast.staff_id = ?
                  AND ea.activity_status = 'UPCOMING'
                  AND ea.start_time > SYSUTCDATETIME()
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, staffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi gỡ nhân sự khỏi hoạt động
            throw new IllegalStateException("Failed to delete assigned staff from activity.", e);
        }
    }

    /**
     * Kiểm tra nhân sự đã được phân công vào hoạt động hay chưa.
     *
     * @param activityId ID hoạt động
     * @param staffId    ID nhân sự
     * @return {@code true} nếu đã tồn tại bản ghi phân công, ngược lại {@code false}
     */
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
            // Bọc lỗi SQL khi kiểm tra tồn tại phân công
            throw new IllegalStateException("Failed to check if staff is assigned to activity.", e);
        }
    }

    /**
     * Kiểm tra nhân sự có đang ở trạng thái {@code ACTIVE} và thuộc ít nhất một đơn vị đang {@code ACTIVE}
     * là đơn vị phụ trách chính hoặc đơn vị đồng phụ trách của hoạt động hay không.
     *
     * @param staffId    ID nhân sự
     * @param activityId ID hoạt động
     * @return {@code true} nếu nhân sự thỏa điều kiện thuộc đơn vị của hoạt động
     */
    @Override
    public boolean isStaffInActivityDepartments(int staffId, int activityId) {
        // Kiểm tra chéo giữa staff, staff_assignment, department và danh sách đơn vị của hoạt động
        String sql = """
                SELECT 1
                FROM staff s
                JOIN staff_assignment sa ON sa.staff_id = s.id
                JOIN department d ON d.id = sa.department_id
                JOIN extracurricular_activity ea ON ea.id = ?
                WHERE s.id = ? AND s.status = 'ACTIVE' AND d.status = 'ACTIVE'
                  AND (sa.department_id = ea.responsible_department_id
                       OR sa.department_id IN (SELECT ad.department_id FROM activity_department ad
                                               WHERE ad.activity_id = ea.id))
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi kiểm tra nhân sự thuộc đơn vị hoạt động
            throw new IllegalStateException("Failed to check if staff belongs to activity departments.", e);
        }
    }

    /**
     * Kiểm tra nhân sự có thuộc một đơn vị cụ thể hay không thông qua bảng {@code staff_assignment}.
     *
     * @param staffId      ID nhân sự
     * @param departmentId ID đơn vị
     * @return {@code true} nếu có phân công nhân sự tại đơn vị đó
     */
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
            // Bọc lỗi SQL khi kiểm tra nhân sự thuộc đơn vị
            throw new IllegalStateException("Failed to check if staff belongs to department.", e);
        }
    }
}
