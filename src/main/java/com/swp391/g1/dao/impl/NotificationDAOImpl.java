package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.INotificationDAO;
import com.swp391.g1.dto.response.NotificationDTO;
import com.swp391.g1.util.DateTimeUtil;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lớp triển khai DAO bằng JDBC cho module thông báo ({@link INotificationDAO}),
 * thao tác trực tiếp với hai bảng {@code notifications} và {@code user_notifications}
 * cùng các bảng liên quan để xác định tập người nhận theo nghiệp vụ.
 */
public class NotificationDAOImpl implements INotificationDAO {

    /**
     * Tạo bản ghi thông báo đăng nhập thành công và gán cho tài khoản tương ứng trong cùng một transaction.
     *
     * @param accountId mã tài khoản vừa đăng nhập (phải lớn hơn 0)
     * @param loginAt   thời điểm đăng nhập (không được {@code null})
     * @return mã thông báo ({@code notifications.id}) vừa được sinh
     * @throws IllegalArgumentException nếu {@code accountId <= 0} hoặc {@code loginAt == null}
     * @throws IllegalStateException    nếu xảy ra lỗi JDBC khi chèn thông báo
     */
    @Override
    public int createLoginNotification(int accountId, Instant loginAt) {
        // Kiểm tra tham số đầu vào bắt buộc trước khi mở kết nối CSDL
        if (accountId <= 0 || loginAt == null) {
            throw new IllegalArgumentException("Login notification requires a valid account and timestamp.");
        }
        String timestamp = DateTimeUtil.format(loginAt);
        try (Connection connection = DBContext.getConnection()) {
            // Bắt đầu transaction để đảm bảo chèn cả notifications và user_notifications nguyên tử
            connection.setAutoCommit(false);
            try {
                int notificationId;
                // Bước 1: Chèn nội dung thông báo đăng nhập vào bảng notifications và lấy khóa chính tự tăng
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO notifications (title, content, type, event_type, target_url) "
                                + "VALUES (?, ?, 'IN_APP', 'USER_LOGIN', '/home')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, "Đăng nhập thành công");
                    statement.setString(2, "Bạn vừa đăng nhập lúc " + timestamp + ".");
                    // Xác nhận đúng 1 dòng thông báo được tạo thành công
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Login notification was not created.");
                    }
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        // Đảm bảo CSDL trả về ID tự tăng hợp lệ
                        if (!keys.next()) {
                            throw new SQLException("Login notification ID was not generated.");
                        }
                        notificationId = keys.getInt(1);
                    }
                }
                // Bước 2: Gán thông báo vừa tạo cho đúng tài khoản đăng nhập
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO user_notifications (notification_id, user_id) VALUES (?, ?)")) {
                    statement.setInt(1, notificationId);
                    statement.setInt(2, accountId);
                    statement.executeUpdate();
                }
                // Xác nhận hoàn tất transaction
                connection.commit();
                return notificationId;
            } catch (SQLException e) {
                // Hoàn tác toàn bộ thay đổi nếu bất kỳ bước nào trong transaction thất bại
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc ngoại lệ SQL sang IllegalStateException cho tầng service xử lý
            throw new IllegalStateException("Failed to create login notification.", e);
        }
    }

    /**
     * Sinh mệnh đề điều kiện SQL phụ (nối vào {@code WHERE}) tương ứng với bộ lọc trạng thái đọc.
     *
     * @param filter giá trị bộ lọc ({@code "unread"}, {@code "read"}, hoặc {@code "all"})
     * @return chuỗi SQL {@code AND ...} hoặc chuỗi rỗng nếu lấy tất cả
     */
    private static String filterClause(String filter) {
        // Lọc chỉ các thông báo chưa đọc
        if ("unread".equals(filter)) return " AND un.is_read = 0";
        // Lọc chỉ các thông báo đã đọc
        if ("read".equals(filter)) return " AND un.is_read = 1";
        return "";
    }

    /**
     * Tạo thông báo yêu cầu duyệt hoạt động mới và phát tới tất cả tài khoản có loại {@code ADMIN}
     * trong cùng một transaction.
     *
     * @param title     tiêu đề thông báo
     * @param content   nội dung thông báo
     * @param targetUrl đường dẫn điều hướng khi quản trị viên nhấn vào thông báo
     * @return mã thông báo vừa tạo
     * @throws IllegalStateException nếu thao tác CSDL thất bại
     */
    @Override
    public int createApprovalNotification(String title, String content, String targetUrl) {
        try (Connection c = DBContext.getConnection()) {
            // Bắt đầu transaction để đồng bộ hóa việc tạo thông báo và gán người nhận ADMIN
            c.setAutoCommit(false);
            try {
                int id;
                // Bước 1: Chèn bản ghi thông báo yêu cầu duyệt hoạt động và lấy ID tự tăng
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO notifications (title, content, type, event_type, target_url) "
                        + "VALUES (?, ?, 'IN_APP', 'ACTIVITY_APPROVAL_REQUEST', ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, content);
                    ps.setString(3, targetUrl);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        id = (int) rs.getLong(1);
                    }
                }
                // Bước 2: Phát thông báo đồng loạt cho mọi tài khoản có type = 'ADMIN'
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO user_notifications (notification_id, user_id) "
                        + "SELECT ?, id FROM account WHERE type = 'ADMIN'")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                // Xác nhận lưu transaction
                c.commit();
                return id;
            } catch (SQLException e) {
                // Rollback nếu có lỗi SQL trong quá trình chèn thông báo hoặc người nhận
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ unchecked cho tầng gọi
            throw new IllegalStateException("Failed to create notification.", e);
        }
    }

    /**
     * Tạo thông báo trạng thái hoạt động và gửi tới cán bộ phụ trách chính lẫn toàn bộ cán bộ hỗ trợ
     * được phân công cho hoạt động đó.
     *
     * @param activityId mã hoạt động ngoại khóa
     * @param eventType  mã loại sự kiện thông báo
     * @param title      tiêu đề thông báo
     * @param content    nội dung thông báo
     * @param targetUrl  đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu không có người nhận hợp lệ
     */
    @Override
    public int createActivityStatusNotification(
            int activityId, String eventType, String title, String content, String targetUrl) {
        // Hợp nhất (UNION) tài khoản của cán bộ phụ trách chính và các cán bộ tham gia hỗ trợ hoạt động
        String recipientsSql = """
                SELECT s.account_id
                FROM extracurricular_activity a
                JOIN staff s ON s.id = a.responsible_staff_id
                WHERE a.id = ? AND s.account_id IS NOT NULL
                UNION
                SELECT s.account_id
                FROM activity_staff ast
                JOIN staff s ON s.id = ast.staff_id
                WHERE ast.activity_id = ? AND s.account_id IS NOT NULL
                """;
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql,
                activityId, activityId);
    }

    /**
     * Tạo thông báo liên quan đến lời mời hợp tác và gửi tới nhân sự đối tác được chỉ định trực tiếp
     * cùng các trưởng nhóm/thư ký ({@code PARTNER_LEADER}, {@code PARTNER_SECRETARY}) của công ty đối tác.
     *
     * @param invitationId mã lời mời hợp tác
     * @param eventType    mã loại sự kiện thông báo
     * @param title        tiêu đề thông báo
     * @param content      nội dung thông báo
     * @param targetUrl    đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu không có tài khoản đối tác nào khớp
     */
    @Override
    public int createPartnerInvitationNotification(
            int invitationId, String eventType, String title, String content, String targetUrl) {
        // Hợp nhất (UNION) nhân sự đối tác gắn trực tiếp với lời mời và các tài khoản lãnh đạo/thư ký cùng công ty
        String recipientsSql = """
                SELECT ps.account_id
                FROM activity_collaboration_invitation i
                JOIN partner_staff ps ON ps.id = i.partner_staff_id
                WHERE i.id = ? AND ps.account_id IS NOT NULL
                UNION
                SELECT ps.account_id
                FROM activity_collaboration_invitation i
                JOIN partner_staff ps ON ps.company_id = i.partner_company_id
                JOIN account acc ON acc.id = ps.account_id
                WHERE i.id = ? AND acc.type = 'PARTNER_STAFF'
                  AND acc.role IN ('PARTNER_LEADER', 'PARTNER_SECRETARY')
                """;
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql,
                invitationId, invitationId);
    }

    /**
     * Tạo thông báo phản hồi về lời mời đối tác gửi tới cán bộ phụ trách chính của hoạt động tương ứng.
     *
     * @param invitationId mã lời mời hợp tác
     * @param eventType    mã loại sự kiện thông báo
     * @param title        tiêu đề thông báo
     * @param content      nội dung thông báo
     * @param targetUrl    đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu không tìm thấy cán bộ phụ trách có tài khoản
     */
    @Override
    public int createPartnerInvitationOwnerNotification(
            int invitationId, String eventType, String title, String content, String targetUrl) {
        // Tra cứu ngược từ lời mời hợp tác sang hoạt động và tài khoản cán bộ phụ trách chính
        String recipientsSql = """
                SELECT responsible.account_id
                FROM activity_collaboration_invitation i
                JOIN extracurricular_activity a ON a.id = i.activity_id
                JOIN staff responsible ON responsible.id = a.responsible_staff_id
                WHERE i.id = ? AND responsible.account_id IS NOT NULL
                """;
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql,
                invitationId);
    }

    /**
     * Tạo thông báo gửi tới một sinh viên cụ thể dựa theo {@code student.id}.
     *
     * @param studentId mã sinh viên
     * @param eventType mã loại sự kiện thông báo
     * @param title     tiêu đề thông báo
     * @param content   nội dung thông báo
     * @param targetUrl đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu sinh viên không có tài khoản liên kết
     */
    @Override
    public int createStudentNotification(int studentId, String eventType, String title, String content,
                                         String targetUrl) {
        String recipientsSql = "SELECT account_id FROM dbo.student WHERE id = ? AND account_id IS NOT NULL";
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql, studentId);
    }

    /**
     * Tạo thông báo gửi trực tiếp tới một tài khoản cụ thể dựa theo {@code account.id}.
     *
     * @param accountId mã tài khoản nhận thông báo
     * @param eventType mã loại sự kiện thông báo
     * @param title     tiêu đề thông báo
     * @param content   nội dung thông báo
     * @param targetUrl đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu tài khoản không tồn tại
     */
    @Override
    public int createAccountNotification(int accountId, String eventType, String title, String content,
                                         String targetUrl) {
        return createNotificationForRecipients(eventType, title, content, targetUrl,
                "SELECT id FROM dbo.account WHERE id = ?", accountId);
    }

    /**
     * Tạo thông báo gửi tới toàn bộ sinh viên đang nằm trong danh sách tham gia của một hoạt động.
     *
     * @param activityId mã hoạt động ngoại khóa
     * @param eventType  mã loại sự kiện thông báo
     * @param title      tiêu đề thông báo
     * @param content    nội dung thông báo
     * @param targetUrl  đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu không có người tham gia nào có tài khoản
     */
    @Override
    public int createActivityParticipantsNotification(
            int activityId, String eventType, String title, String content, String targetUrl) {
        // Lấy danh sách account_id không trùng lặp của các sinh viên tham gia hoạt động
        String recipientsSql = """
                SELECT DISTINCT s.account_id
                FROM dbo.activity_participant ap
                JOIN dbo.student s ON s.id = ap.student_id
                WHERE ap.activity_id = ? AND s.account_id IS NOT NULL
                """;
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql, activityId);
    }

    /**
     * Tạo và gửi thông báo nhắc nhở đánh giá (feedback) cho những sinh viên đã tham dự một hoạt động
     * đã kết thúc ({@code COMPLETED} và đã qua {@code end_time}) nhưng chưa gửi feedback.
     *
     * @param activityId mã hoạt động ngoại khóa cần gửi nhắc nhở
     * @return số lượng sinh viên nhận được thông báo nhắc nhở (hoặc {@code 0} nếu không có ai cần nhắc)
     * @throws IllegalStateException nếu xảy ra lỗi JDBC trong quá trình xử lý
     */
    @Override
    public int createActivityFeedbackReminder(int activityId) {
        // Truy vấn tìm sinh viên có tham dự thực tế (có mặt/đi muộn/về sớm) ở hoạt động đã hoàn thành nhưng chưa có feedback
        String pendingRecipientsSql = """
                SELECT s.account_id
                FROM dbo.activity_participant ap
                JOIN dbo.student s ON s.id = ap.student_id
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                WHERE ap.activity_id = ?
                  AND s.account_id IS NOT NULL
                  AND ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                  AND ea.activity_status = 'COMPLETED'
                  AND ea.end_time <= SYSUTCDATETIME()
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_feedback f
                      WHERE f.activity_participant_id = ap.id)
                """;
        try (Connection connection = DBContext.getConnection()) {
            // Mở transaction để đảm bảo chỉ tạo bản ghi notifications khi có sinh viên cần nhắc nhở
            connection.setAutoCommit(false);
            try {
                int pendingCount;
                // Bước 1: Đếm trước số sinh viên còn thiếu feedback để tránh tạo thông báo rỗng
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM (" + pendingRecipientsSql + ") pending_students")) {
                    statement.setInt(1, activityId);
                    try (ResultSet rs = statement.executeQuery()) {
                        rs.next();
                        pendingCount = rs.getInt(1);
                    }
                }
                // Không có sinh viên nào cần nhắc thì kết thúc sớm và trả về 0
                if (pendingCount == 0) {
                    connection.commit();
                    return 0;
                }

                int notificationId;
                // Bước 2: Chèn thông báo nhắc feedback bằng cách lấy tên hoạt động trực tiếp từ bảng extracurricular_activity
                String insertNotificationSql = """
                        INSERT INTO dbo.notifications (title, content, type, event_type, target_url)
                        SELECT N'Nhắc nhở feedback hoạt động',
                               CONCAT(N'Bạn chưa gửi feedback cho hoạt động "', ea.name,
                                      N'". Hãy chia sẻ cảm nhận của bạn.'),
                               'IN_APP', 'ACTIVITY_FEEDBACK_REMINDER', '/activity-feedback'
                        FROM dbo.extracurricular_activity ea
                        WHERE ea.id = ? AND ea.activity_status = 'COMPLETED'
                          AND ea.end_time <= SYSUTCDATETIME()
                        """;
                try (PreparedStatement statement = connection.prepareStatement(
                        insertNotificationSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, activityId);
                    // Nếu hoạt động không còn thỏa điều kiện COMPLETED/end_time thì rollback và trả về 0
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        return 0;
                    }
                    try (ResultSet rs = statement.getGeneratedKeys()) {
                        // Xác nhận lấy được ID thông báo vừa sinh
                        if (!rs.next()) {
                            throw new SQLException("Notification ID was not generated.");
                        }
                        notificationId = rs.getInt(1);
                    }
                }

                // Bước 3: Chèn danh sách sinh viên chưa gửi feedback vào user_notifications
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO dbo.user_notifications (notification_id, user_id) "
                                + pendingRecipientsSql)) {
                    statement.setInt(1, notificationId);
                    statement.setInt(2, activityId);
                    pendingCount = statement.executeUpdate();
                }
                // Xác nhận hoàn tất transaction
                connection.commit();
                return pendingCount;
            } catch (SQLException e) {
                // Hoàn tác transaction nếu có lỗi SQL ở bất kỳ bước nào
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành IllegalStateException để tầng trên ghi nhận
            throw new IllegalStateException("Failed to send activity feedback reminders.", e);
        }
    }

    // Câu truy vấn tái sử dụng: lọc các tài khoản sinh viên đang ACTIVE thỏa mãn đối tượng mục tiêu của hoạt động.
    // Nếu hoạt động không cấu hình bản ghi nào trong activity_target_participant (NOT EXISTS) thì áp dụng cho toàn bộ sinh viên ACTIVE;
    // ngược lại khớp theo phân cấp khoa -> khóa -> ngành -> chương trình đào tạo -> lớp chính quy (cột NULL nghĩa là không giới hạn cấp đó).
    private static final String ELIGIBLE_ACTIVE_STUDENTS_SQL = """
            SELECT DISTINCT s.account_id
            FROM dbo.student s
            LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
            LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
            LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
            WHERE s.status = 'ACTIVE'
              AND s.account_id IS NOT NULL
              AND (
                  NOT EXISTS (
                      SELECT 1 FROM dbo.activity_target_participant t0 WHERE t0.activity_id = ?
                  )
                  OR EXISTS (
                      SELECT 1
                      FROM dbo.activity_target_participant t
                      WHERE t.activity_id = ?
                        AND (t.department_id IS NULL OR t.department_id = m.responsible_department_id)
                        AND (t.cohort_id IS NULL OR t.cohort_id = mc.applicable_cohort_id)
                        AND (t.major_id IS NULL OR t.major_id = p.responsible_major_id)
                        AND (t.academic_program_id IS NULL OR t.academic_program_id = mc.responsible_academic_program_id)
                        AND (t.main_class_id IS NULL OR t.main_class_id = s.main_class_id)
                  )
              )
            """;

    /**
     * Tạo thông báo về hoạt động cho các sinh viên đang hoạt động thuộc đối tượng mục tiêu,
     * tùy chọn gộp thêm những sinh viên đã có trong danh sách tham gia ({@code activity_participant}).
     *
     * @param activityId          mã hoạt động ngoại khóa
     * @param includeParticipants {@code true} nếu cần UNION thêm sinh viên đã được chỉ định/tham gia hoạt động
     * @param eventType           mã loại sự kiện thông báo
     * @param title               tiêu đề thông báo
     * @param content             nội dung thông báo
     * @param targetUrl           đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu {@code activityId <= 0} hoặc không có người nhận
     */
    @Override
    public int createEligibleStudentsActivityNotification(
            int activityId, boolean includeParticipants, String eventType, String title, String content, String targetUrl) {
        // Không xử lý nếu mã hoạt động không hợp lệ
        if (activityId <= 0) {
            return 0;
        }
        // Chỉ gửi cho sinh viên đủ điều kiện mục tiêu khi không cần gộp danh sách đã tham gia
        if (!includeParticipants) {
            return createNotificationForRecipients(
                    eventType, title, content, targetUrl, ELIGIBLE_ACTIVE_STUDENTS_SQL, activityId, activityId);
        }
        // Hợp nhất (UNION) sinh viên đủ điều kiện mục tiêu với sinh viên đã nằm trong danh sách activity_participant
        String recipientsSql = ELIGIBLE_ACTIVE_STUDENTS_SQL
                + """
                UNION
                SELECT s.account_id
                FROM dbo.activity_participant ap
                JOIN dbo.student s ON s.id = ap.student_id
                WHERE ap.activity_id = ? AND s.account_id IS NOT NULL
                """;
        return createNotificationForRecipients(
                eventType, title, content, targetUrl, recipientsSql, activityId, activityId, activityId);
    }

    /**
     * Tạo thông báo thay đổi trạng thái đợt đăng ký (mở/đóng) cho sinh viên đủ điều kiện mục tiêu,
     * tùy chọn gộp thêm những sinh viên đã nộp đơn đăng ký (ngoại trừ đơn đã rút {@code WITHDRAWN}).
     *
     * @param activityId        mã hoạt động ngoại khóa
     * @param formId            mã biểu mẫu đăng ký
     * @param includeApplicants {@code true} nếu cần UNION thêm sinh viên đã đăng ký vào đợt này
     * @param eventType         mã loại sự kiện thông báo
     * @param title             tiêu đề thông báo
     * @param content           nội dung thông báo
     * @param targetUrl         đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu {@code activityId <= 0} hoặc không có người nhận
     */
    @Override
    public int createRegistrationFormStatusNotification(
            int activityId, int formId, boolean includeApplicants, String eventType, String title, String content, String targetUrl) {
        // Bỏ qua nếu ID hoạt động không hợp lệ
        if (activityId <= 0) {
            return 0;
        }
        // Nếu không gộp ứng viên hoặc formId không hợp lệ thì chỉ gửi tới tập sinh viên đủ điều kiện mục tiêu
        if (!includeApplicants || formId <= 0) {
            return createNotificationForRecipients(
                    eventType, title, content, targetUrl, ELIGIBLE_ACTIVE_STUDENTS_SQL, activityId, activityId);
        }
        // Hợp nhất (UNION) sinh viên đủ điều kiện mục tiêu với sinh viên đã nộp đơn (chưa rút) của biểu mẫu này
        String recipientsSql = ELIGIBLE_ACTIVE_STUDENTS_SQL
                + """
                UNION
                SELECT s.account_id
                FROM dbo.activity_registration r
                JOIN dbo.student s ON s.id = r.student_id
                WHERE r.form_id = ? AND r.status <> 'WITHDRAWN' AND s.account_id IS NOT NULL
                """;
        return createNotificationForRecipients(
                eventType, title, content, targetUrl, recipientsSql, activityId, activityId, formId);
    }

    /**
     * Tạo một thông báo chung và phân phát tới danh sách mã tài khoản sinh viên chỉ định trước
     * (tự động lọc bỏ giá trị null/không hợp lệ và loại trùng lặp) trong cùng một transaction.
     *
     * @param accountIds danh sách mã tài khoản sinh viên nhận thông báo
     * @param eventType  mã loại sự kiện thông báo
     * @param title      tiêu đề thông báo
     * @param content    nội dung thông báo
     * @param targetUrl  đường dẫn điều hướng
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu không có tài khoản hợp lệ nào tồn tại trong CSDL
     * @throws IllegalStateException nếu thao tác JDBC thất bại
     */
    @Override
    public int createStudentAccountsNotification(
            List<Integer> accountIds, String eventType, String title, String content, String targetUrl) {
        // Trả về 0 ngay nếu danh sách tài khoản đầu vào rỗng hoặc null
        if (accountIds == null || accountIds.isEmpty()) {
            return 0;
        }
        Set<Integer> distinctIds = new LinkedHashSet<>();
        // Duyệt danh sách đầu vào để lọc các ID dương hợp lệ và loại bỏ trùng lặp nhưng giữ nguyên thứ tự
        for (Integer accountId : accountIds) {
            // Chỉ giữ lại mã tài khoản khác null và lớn hơn 0
            if (accountId != null && accountId > 0) {
                distinctIds.add(accountId);
            }
        }
        // Dừng sớm nếu sau khi lọc không còn mã tài khoản hợp lệ nào
        if (distinctIds.isEmpty()) {
            return 0;
        }
        try (Connection c = DBContext.getConnection()) {
            // Bắt đầu transaction để tránh tạo bản ghi notifications mồ côi khi không có người nhận tồn tại
            c.setAutoCommit(false);
            try {
                int id;
                // Bước 1: Tạo bản ghi thông báo chính trong bảng notifications
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO notifications (title, content, type, event_type, target_url) "
                                + "VALUES (?, ?, 'IN_APP', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, content);
                    ps.setString(3, eventType);
                    ps.setString(4, targetUrl);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        // Kiểm tra khóa chính tự tăng đã được CSDL trả về
                        if (!rs.next()) {
                            throw new SQLException("Notification ID was not generated.");
                        }
                        id = (int) rs.getLong(1);
                    }
                }
                int insertedRecipients = 0;
                // Bước 2: Chèn từng người nhận có tồn tại thực tế trong dbo.account vào user_notifications
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO user_notifications (notification_id, user_id) "
                                + "SELECT ?, acc.id FROM dbo.account acc WHERE acc.id = ?")) {
                    // Lặp qua từng mã tài khoản duy nhất để thực thi chèn và cộng dồn số bản ghi thành công
                    for (int accountId : distinctIds) {
                        ps.setInt(1, id);
                        ps.setInt(2, accountId);
                        insertedRecipients += ps.executeUpdate();
                    }
                }
                // Nếu không có tài khoản nào tồn tại trong CSDL thì rollback để không lưu thông báo rỗng
                if (insertedRecipients == 0) {
                    c.rollback();
                    return 0;
                }
                // Xác nhận hoàn tất transaction
                c.commit();
                return id;
            } catch (SQLException e) {
                // Rollback toàn bộ transaction khi xảy ra lỗi SQL
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ unchecked cho tầng service
            throw new IllegalStateException("Failed to create student notification.", e);
        }
    }

    /**
     * Hàm lõi dùng chung để chèn một thông báo vào bảng {@code notifications} rồi phát hàng loạt
     * vào bảng {@code user_notifications} dựa trên câu truy vấn con {@code recipientsSql} xác định tập người nhận.
     * Nếu không chèn được người nhận nào thì tự động rollback transaction và trả về {@code 0}.
     *
     * @param eventType       mã loại sự kiện thông báo
     * @param title           tiêu đề thông báo
     * @param content         nội dung thông báo
     * @param targetUrl       đường dẫn điều hướng
     * @param recipientsSql   câu SQL trả về cột {@code account_id} của tập người nhận
     * @param recipientParams các tham số nguyên cần bind tương ứng với dấu {@code ?} trong {@code recipientsSql}
     * @return mã thông báo vừa tạo, hoặc {@code 0} nếu không có người nhận hợp lệ
     * @throws IllegalStateException nếu thao tác CSDL thất bại
     */
    private int createNotificationForRecipients(
            String eventType, String title, String content, String targetUrl,
            String recipientsSql, int... recipientParams) {
        try (Connection c = DBContext.getConnection()) {
            // Mở transaction nhằm đảm bảo tính toàn vẹn giữa bảng notifications và user_notifications
            c.setAutoCommit(false);
            try {
                int id;
                // Bước 1: Chèn bản ghi thông báo mới và lấy ID tự tăng
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO notifications (title, content, type, event_type, target_url) "
                                + "VALUES (?, ?, 'IN_APP', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, content);
                    ps.setString(3, eventType);
                    ps.setString(4, targetUrl);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        // Xác nhận đã lấy được khóa chính vừa sinh từ CSDL
                        if (!rs.next()) {
                            throw new SQLException("Notification ID was not generated.");
                        }
                        id = (int) rs.getLong(1);
                    }
                }
                int insertedRecipients;
                // Bước 2: Dùng subquery recipientsSql kết hợp JOIN dbo.account để lọc trùng (DISTINCT) và xác thực tài khoản tồn tại
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO user_notifications (notification_id, user_id) "
                                + "SELECT DISTINCT ?, recipients.account_id FROM (" + recipientsSql + ") recipients "
                                + "JOIN dbo.account acc ON acc.id = recipients.account_id")) {
                    ps.setInt(1, id);
                    // Gán lần lượt các tham số động của recipientsSql bắt đầu từ vị trí tham số số 2
                    for (int i = 0; i < recipientParams.length; i++) {
                        ps.setInt(i + 2, recipientParams[i]);
                    }
                    insertedRecipients = ps.executeUpdate();
                }
                // Không có người nhận nào được chèn thì hủy bỏ bản ghi notifications vừa tạo bằng rollback
                if (insertedRecipients == 0) {
                    c.rollback();
                    return 0;
                }
                // Xác nhận lưu toàn bộ transaction
                c.commit();
                return id;
            } catch (SQLException e) {
                // Hoàn tác transaction nếu bất kỳ câu lệnh SQL nào thất bại
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Chuyển lỗi SQL thành IllegalStateException cho tầng gọi
            throw new IllegalStateException("Failed to create notification.", e);
        }
    }

    /**
     * Truy vấn danh sách thông báo của một tài khoản có áp dụng lọc trạng thái đọc, sắp xếp mới nhất trước
     * và phân trang theo cú pháp {@code OFFSET ... FETCH NEXT} của SQL Server.
     *
     * @param accountId mã tài khoản người dùng
     * @param filter    bộ lọc trạng thái ({@code all}, {@code unread}, {@code read})
     * @param offset    số bản ghi bỏ qua
     * @param limit     số bản ghi tối đa cần lấy
     * @return danh sách {@link NotificationDTO} tương ứng với trang yêu cầu
     * @throws IllegalStateException nếu xảy ra lỗi JDBC khi tải danh sách thông báo
     */
    @Override
    public List<NotificationDTO> findByAccount(int accountId, String filter, int offset, int limit) {
        // Ghép mệnh đề lọc trạng thái đọc và sắp xếp giảm dần theo thời gian tạo, sau đó theo ID để phân trang ổn định
        String sql = "SELECT n.id, n.title, n.content, n.event_type, n.target_url, n.created_at, un.is_read "
                + "FROM user_notifications un JOIN notifications n ON n.id = un.notification_id "
                + "WHERE un.user_id = ?" + filterClause(filter)
                + " ORDER BY n.created_at DESC, n.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<NotificationDTO> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng bản ghi trong ResultSet và ánh xạ các cột sang đối tượng NotificationDTO
                while (rs.next()) {
                    NotificationDTO d = new NotificationDTO();
                    d.setId(rs.getInt("id"));
                    d.setTitle(rs.getString("title"));
                    d.setContent(rs.getString("content"));
                    d.setEventType(rs.getString("event_type"));
                    d.setLink(rs.getString("target_url"));
                    d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    d.setRead(rs.getBoolean("is_read"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành IllegalStateException để tầng controller trả HTTP 500
            throw new IllegalStateException("Failed to load notifications.", e);
        }
        return list;
    }

    /**
     * Đếm tổng số thông báo của một tài khoản theo bộ lọc trạng thái đọc.
     *
     * @param accountId mã tài khoản người dùng
     * @param filter    bộ lọc trạng thái ({@code all}, {@code unread}, {@code read})
     * @return tổng số bản ghi khớp điều kiện
     */
    @Override
    public int countByAccount(int accountId, String filter) {
        return count("SELECT COUNT(*) FROM user_notifications un WHERE un.user_id = ?"
                + filterClause(filter), accountId);
    }

    /**
     * Đếm số lượng thông báo chưa đọc của một tài khoản.
     *
     * @param accountId mã tài khoản người dùng
     * @return số thông báo chưa đọc
     */
    @Override
    public int countUnread(int accountId) {
        return countByAccount(accountId, "unread");
    }

    /**
     * Hàm hỗ trợ thực thi câu lệnh đếm {@code SELECT COUNT(*)} theo mã tài khoản.
     *
     * @param sql       câu truy vấn đếm có đúng 1 tham số {@code accountId}
     * @param accountId mã tài khoản người dùng
     * @return giá trị đếm trả về từ cột đầu tiên, hoặc {@code 0} nếu tập kết quả rỗng
     * @throws IllegalStateException nếu truy vấn đếm gặp lỗi SQL
     */
    private int count(String sql, int accountId) {
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                // Trả về giá trị COUNT(*) ở cột 1 nếu có dòng kết quả, ngược lại trả về 0
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ unchecked
            throw new IllegalStateException("Failed to count notifications.", e);
        }
    }

    /**
     * Đánh dấu một thông báo chưa đọc cụ thể của tài khoản là đã đọc và ghi nhận thời điểm đọc.
     *
     * @param accountId      mã tài khoản người dùng
     * @param notificationId mã thông báo cần đánh dấu
     * @return {@code true} nếu có bản ghi được cập nhật, ngược lại {@code false}
     */
    @Override
    public boolean markRead(int accountId, int notificationId) {
        return update("UPDATE user_notifications SET is_read = 1, read_at = SYSDATETIME() "
                + "WHERE user_id = ? AND notification_id = ? AND is_read = 0", accountId, notificationId) > 0;
    }

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của tài khoản thành đã đọc và ghi nhận thời điểm đọc.
     *
     * @param accountId mã tài khoản người dùng
     * @return số lượng bản ghi thông báo đã được cập nhật
     */
    @Override
    public int markAllRead(int accountId) {
        return update("UPDATE user_notifications SET is_read = 1, read_at = SYSDATETIME() "
                + "WHERE user_id = ? AND is_read = 0", accountId, null);
    }

    /**
     * Hàm hỗ trợ thực thi câu lệnh cập nhật trạng thái đọc trong bảng {@code user_notifications}.
     *
     * @param sql            câu lệnh SQL {@code UPDATE}
     * @param accountId      mã tài khoản người dùng (tham số vị trí 1)
     * @param notificationId mã thông báo (tham số vị trí 2, truyền {@code null} khi cập nhật tất cả)
     * @return số dòng bị ảnh hưởng bởi câu lệnh cập nhật
     * @throws IllegalStateException nếu cập nhật CSDL gặp lỗi SQL
     */
    private int update(String sql, int accountId, Integer notificationId) {
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            // Chỉ gán tham số notification_id khi thao tác đánh dấu đọc một thông báo riêng lẻ
            if (notificationId != null) ps.setInt(2, notificationId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            // Bọc lỗi SQL thành IllegalStateException cho tầng service/controller xử lý
            throw new IllegalStateException("Failed to update notification.", e);
        }
    }
}