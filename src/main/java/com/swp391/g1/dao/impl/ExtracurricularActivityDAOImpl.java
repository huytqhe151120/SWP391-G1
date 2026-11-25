package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.util.DateTimeUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp DAO triển khai JDBC quản lý dữ liệu hoạt động ngoại khóa ({@code extracurricular_activity}),
 * bao gồm truy vấn chi tiết đa bảng, kiểm soát phạm vi truy cập theo vai trò cán bộ,
 * cập nhật có điều kiện chống tranh chấp, xóa an toàn theo transaction và đồng bộ trạng thái theo thời gian UTC.
 */
public class ExtracurricularActivityDAOImpl implements IExtracurricularActivityDAO {
    private static final Logger LOGGER = Logger.getLogger(ExtracurricularActivityDAOImpl.class.getName());
    // Cờ đánh dấu schema điểm danh nhiều buổi và bảng kết quả học kỳ đã được kiểm tra/khởi tạo (dùng cho double-checked locking).
    private static volatile boolean attendanceSessionSchemaEnsured = false;

    /**
     * Đảm bảo lược đồ CSDL cho tính năng điểm danh nhiều buổi, chuẩn hóa khóa ngoại người cập nhật
     * sang ID tài khoản (INT), bảng tổng hợp kết quả theo học kỳ và cờ công khai ({@code is_public})
     * đã tồn tại và nhất quán. Thực thi tối đa một lần cho mỗi vòng đời ứng dụng nhờ cơ chế double-checked locking.
     */
    public static void ensureAttendanceSessionSchema() {
        // Bỏ qua nhanh nếu schema đã được đảm bảo ở lần gọi trước đó.
        if (attendanceSessionSchemaEnsured) {
            return;
        }
        // Khóa đồng bộ mức lớp để tránh nhiều luồng chạy kịch bản DDL/migration cùng lúc.
        synchronized (ExtracurricularActivityDAOImpl.class) {
            // Kiểm tra lại bên trong khối synchronized phòng trường hợp luồng khác vừa hoàn tất khởi tạo.
            if (attendanceSessionSchemaEnsured) {
                return;
            }
            // Kịch bản SQL idempotent: bổ sung cột số buổi điểm danh, tạo bảng điểm danh theo buổi,
            // chuyển đổi dữ liệu cũ của created_by/updated_by từ chuỗi tên/mã sang account_id kiểu INT,
            // đồng bộ dữ liệu điểm danh buổi 1, tạo bảng kết quả học kỳ và chuẩn hóa lại cột is_public.
            String ddl = """
                    IF COL_LENGTH('dbo.extracurricular_activity', 'attendance_session_count') IS NULL
                    BEGIN
                        ALTER TABLE dbo.extracurricular_activity
                        ADD attendance_session_count INT NOT NULL
                            CONSTRAINT DF_extracurricular_activity_attendance_session_count DEFAULT (1);
                    END;
                    IF OBJECT_ID(N'dbo.activity_participant_session_attendance', N'U') IS NULL
                    BEGIN
                        CREATE TABLE dbo.activity_participant_session_attendance (
                            id                      INT IDENTITY(1,1) NOT NULL,
                            activity_participant_id INT               NOT NULL,
                            session_number          INT               NOT NULL,
                            attendance_status       VARCHAR(30)       NOT NULL CONSTRAINT DF_apsa_status DEFAULT ('NOT_YET'),
                            updated_by              INT               NULL,
                            updated_at              DATETIME2         NOT NULL CONSTRAINT DF_apsa_updated_at DEFAULT (SYSUTCDATETIME()),
                            CONSTRAINT PK_activity_participant_session_attendance PRIMARY KEY CLUSTERED (id ASC),
                            CONSTRAINT UQ_apsa_participant_session UNIQUE (activity_participant_id, session_number),
                            CONSTRAINT FK_apsa_participant FOREIGN KEY (activity_participant_id)
                                REFERENCES dbo.activity_participant (id) ON DELETE CASCADE,
                            CONSTRAINT CK_apsa_session_number CHECK (session_number >= 1 AND session_number <= 50)
                        );
                    END;
                    IF OBJECT_ID(N'dbo.activity_attendance_history', N'U') IS NOT NULL
                       AND COL_LENGTH('dbo.activity_attendance_history', 'session_number') IS NULL
                    BEGIN
                        ALTER TABLE dbo.activity_attendance_history
                        ADD session_number INT NOT NULL
                            CONSTRAINT DF_activity_attendance_history_session DEFAULT (1);
                    END;
                    IF EXISTS (
                        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = 'dbo'
                          AND TABLE_NAME = 'activity_attendance_history'
                          AND COLUMN_NAME = 'created_by'
                          AND DATA_TYPE <> 'int'
                    )
                    BEGIN
                        UPDATE h
                        SET h.created_by = CAST(a.id AS NVARCHAR(100))
                        FROM dbo.activity_attendance_history h
                        JOIN dbo.account a ON LOWER(LTRIM(RTRIM(h.created_by))) = LOWER(a.username)
                        WHERE TRY_CAST(h.created_by AS INT) IS NULL;

                        UPDATE h
                        SET h.created_by = CAST(st.account_id AS NVARCHAR(100))
                        FROM dbo.activity_attendance_history h
                        JOIN dbo.staff st ON (
                            LOWER(LTRIM(RTRIM(h.created_by))) = LOWER(st.code)
                            OR LTRIM(RTRIM(h.created_by)) = st.name
                            OR LTRIM(RTRIM(h.created_by)) LIKE st.name + N' (%'
                        )
                        WHERE TRY_CAST(h.created_by AS INT) IS NULL
                          AND st.account_id IS NOT NULL;

                        UPDATE h
                        SET h.created_by = CAST(ps.account_id AS NVARCHAR(100))
                        FROM dbo.activity_attendance_history h
                        JOIN dbo.partner_staff ps ON (
                            LOWER(LTRIM(RTRIM(h.created_by))) = LOWER(ps.code)
                            OR LTRIM(RTRIM(h.created_by)) = ps.name
                            OR LTRIM(RTRIM(h.created_by)) LIKE ps.name + N' (%'
                        )
                        WHERE TRY_CAST(h.created_by AS INT) IS NULL
                          AND ps.account_id IS NOT NULL;

                        UPDATE dbo.activity_attendance_history
                        SET created_by = N'767'
                        WHERE TRY_CAST(created_by AS INT) IS NULL;

                        ALTER TABLE dbo.activity_attendance_history
                        ALTER COLUMN created_by INT NOT NULL;
                    END;
                    IF EXISTS (
                        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = 'dbo'
                          AND TABLE_NAME = 'activity_participant_session_attendance'
                          AND COLUMN_NAME = 'updated_by'
                          AND DATA_TYPE <> 'int'
                    )
                    BEGIN
                        UPDATE s
                        SET s.updated_by = CAST(a.id AS NVARCHAR(200))
                        FROM dbo.activity_participant_session_attendance s
                        JOIN dbo.account a ON LOWER(LTRIM(RTRIM(s.updated_by))) = LOWER(a.username)
                        WHERE s.updated_by IS NOT NULL AND TRY_CAST(s.updated_by AS INT) IS NULL;

                        UPDATE s
                        SET s.updated_by = CAST(st.account_id AS NVARCHAR(200))
                        FROM dbo.activity_participant_session_attendance s
                        JOIN dbo.staff st ON (
                            LOWER(LTRIM(RTRIM(s.updated_by))) = LOWER(st.code)
                            OR LTRIM(RTRIM(s.updated_by)) = st.name
                            OR LTRIM(RTRIM(s.updated_by)) LIKE st.name + N' (%'
                        )
                        WHERE s.updated_by IS NOT NULL AND TRY_CAST(s.updated_by AS INT) IS NULL
                          AND st.account_id IS NOT NULL;

                        UPDATE s
                        SET s.updated_by = CAST(ps.account_id AS NVARCHAR(200))
                        FROM dbo.activity_participant_session_attendance s
                        JOIN dbo.partner_staff ps ON (
                            LOWER(LTRIM(RTRIM(s.updated_by))) = LOWER(ps.code)
                            OR LTRIM(RTRIM(s.updated_by)) = ps.name
                            OR LTRIM(RTRIM(s.updated_by)) LIKE ps.name + N' (%'
                        )
                        WHERE s.updated_by IS NOT NULL AND TRY_CAST(s.updated_by AS INT) IS NULL
                          AND ps.account_id IS NOT NULL;

                        UPDATE dbo.activity_participant_session_attendance
                        SET updated_by = NULL
                        WHERE updated_by IS NOT NULL AND TRY_CAST(updated_by AS INT) IS NULL;

                        ALTER TABLE dbo.activity_participant_session_attendance
                        ALTER COLUMN updated_by INT NULL;
                    END;
                    INSERT INTO dbo.activity_participant_session_attendance
                        (activity_participant_id, session_number, attendance_status, updated_by, updated_at)
                    SELECT ap.id, 1, ap.attendance_status, h.created_by, COALESCE(h.created_at, SYSUTCDATETIME())
                    FROM dbo.activity_participant ap
                    OUTER APPLY (
                        SELECT TOP 1 TRY_CAST(created_by AS INT) AS created_by, created_at
                        FROM dbo.activity_attendance_history
                        WHERE activity_participant_id = ap.id
                        ORDER BY created_at DESC, id DESC
                    ) h
                    WHERE ap.attendance_status IS NOT NULL
                      AND ap.attendance_status <> 'NOT_YET'
                      AND NOT EXISTS (
                          SELECT 1
                          FROM dbo.activity_participant_session_attendance s
                          WHERE s.activity_participant_id = ap.id
                            AND s.session_number = 1
                      );
                    UPDATE s
                    SET s.updated_by = COALESCE(h.created_by, st_resp.account_id, 767),
                        s.updated_at = COALESCE(h.created_at, s.updated_at)
                    FROM dbo.activity_participant_session_attendance s
                    JOIN dbo.activity_participant ap ON ap.id = s.activity_participant_id
                    JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                    LEFT JOIN dbo.staff st_resp ON st_resp.id = ea.responsible_staff_id
                    OUTER APPLY (
                        SELECT TOP 1 TRY_CAST(created_by AS INT) AS created_by, created_at
                        FROM dbo.activity_attendance_history h0
                        WHERE h0.activity_participant_id = s.activity_participant_id
                          AND COALESCE(h0.session_number, 1) = s.session_number
                        ORDER BY h0.created_at DESC, h0.id DESC
                    ) h
                    WHERE s.updated_by IS NULL AND s.attendance_status <> 'NOT_YET';
                    IF COL_LENGTH('dbo.student_activity_result', 'semester_id') IS NULL
                    BEGIN
                        ALTER TABLE dbo.student_activity_result ADD semester_id INT NULL;
                    END;
                    IF OBJECT_ID(N'dbo.student_semester_result', N'U') IS NULL
                    BEGIN
                        CREATE TABLE dbo.student_semester_result (
                            student_id                INT           NOT NULL,
                            semester_id               INT           NOT NULL,
                            registered_activity_count INT           NOT NULL CONSTRAINT DF_ssr_registered_count DEFAULT (0),
                            attended_activity_count   INT           NOT NULL CONSTRAINT DF_ssr_attended_count DEFAULT (0),
                            attendance_score          DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_attendance_score DEFAULT (0.00),
                            participation_eval_score  DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_eval_score DEFAULT (0.00),
                            participation_score       DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_participation_score DEFAULT (0.00),
                            summary_score             DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_summary_score DEFAULT (0.00),
                            average_score             DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_average_score DEFAULT (0.00),
                            plus_points               DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_plus_points DEFAULT (0.00),
                            minus_points              DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_minus_points DEFAULT (0.00),
                            final_point               DECIMAL(10,2) NOT NULL CONSTRAINT DF_ssr_final_point DEFAULT (0.00),
                            updated_at                DATETIME2     NOT NULL CONSTRAINT DF_ssr_updated_at DEFAULT (SYSUTCDATETIME()),
                            CONSTRAINT PK_StudentSemesterResult PRIMARY KEY CLUSTERED (student_id ASC, semester_id ASC),
                            CONSTRAINT FK_SemesterResult_Student FOREIGN KEY (student_id)
                                REFERENCES dbo.student (id) ON DELETE CASCADE,
                            CONSTRAINT FK_SemesterResult_Semester FOREIGN KEY (semester_id)
                                REFERENCES dbo.semester (id) ON DELETE CASCADE
                        );
                    END;
                    UPDATE dbo.extracurricular_activity
                    SET is_public = CASE
                        WHEN nature <> 'REQUIRE_REGISTRATION' THEN 1
                        WHEN registration_form_required = 0 THEN 1
                        WHEN EXISTS (
                            SELECT 1 FROM dbo.activity_registration_form f
                            WHERE f.activity_id = dbo.extracurricular_activity.id
                        ) THEN 1
                        ELSE 0
                    END
                    WHERE is_public <> CASE
                        WHEN nature <> 'REQUIRE_REGISTRATION' THEN 1
                        WHEN registration_form_required = 0 THEN 1
                        WHEN EXISTS (
                            SELECT 1 FROM dbo.activity_registration_form f
                            WHERE f.activity_id = dbo.extracurricular_activity.id
                        ) THEN 1
                        ELSE 0
                    END;
                    """;
            try (Connection conn = DBContext.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(ddl);
                attendanceSessionSchemaEnsured = true;
            } catch (SQLException e) {
                // Chỉ ghi cảnh báo nếu tài khoản DB thiếu quyền DDL để không làm gián đoạn luồng đọc dữ liệu chính.
                LOGGER.log(Level.WARNING, "Could not auto-ensure attendance session schema.", e);
            }
        }
    }

    /**
     * Đọc an toàn số buổi điểm danh ({@code attendance_session_count}) từ {@link ResultSet},
     * tự động fallback về 1 nếu cột chưa tồn tại hoặc có giá trị không dương.
     *
     * @param rs tập kết quả JDBC hiện tại
     * @return số buổi điểm danh hợp lệ (>= 1)
     */
    private int readAttendanceSessionCount(ResultSet rs) {
        try {
            int val = rs.getInt("attendance_session_count");
            // Nếu giá trị đọc được <= 0 thì chuẩn hóa về tối thiểu 1 buổi điểm danh.
            return val > 0 ? val : 1;
        } catch (SQLException e) {
            // Fallback về 1 nếu truy vấn không chứa cột attendance_session_count (tương thích ngược schema cũ).
            return 1;
        }
    }

    /**
     * Ánh xạ dòng hiện tại của {@link ResultSet} thành thực thể {@link ExtracurricularActivity},
     * bao gồm chuyển đổi enum, quy đổi thời gian bắt đầu/kết thúc từ UTC về múi giờ hệ thống
     * và tính lại trạng thái công khai hiệu lực.
     *
     * @param rs tập kết quả JDBC
     * @return đối tượng {@link ExtracurricularActivity} đã điền đầy đủ thuộc tính
     * @throws SQLException nếu xảy ra lỗi đọc dữ liệu từ cột JDBC
     */
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
        // Chuyển chuỗi tính chất hoạt động sang enum tương ứng (cho phép null).
        String nature = rs.getString("nature");
        activity.setNature(nature == null ? null : Enum.ActivityNature.valueOf(nature));
        activity.setSummaryRequired(rs.getBoolean("is_summary_required"));
        activity.setMaxParticipants((Integer) rs.getObject("max_participants"));
        activity.setAttendanceSessionCount(readAttendanceSessionCount(rs));
        activity.setAddress(rs.getString("address"));
        activity.setDescription(rs.getString("description"));

        // Chuyển đổi trạng thái hoạt động và trạng thái duyệt từ chuỗi DB sang enum (giữ null nếu DB rỗng).
        String activityStatus = rs.getString("activity_status");
        activity.setActivityStatus(activityStatus == null ? null : Enum.ActivityStatus.valueOf(activityStatus));
        String approvalStatus = rs.getString("approval_status");
        activity.setApprovalStatus(approvalStatus == null ? null : Enum.ApprovalStatus.valueOf(approvalStatus));

        // Đọc thời điểm tạo bản ghi nếu không null.
        Timestamp createdAt = rs.getTimestamp("created_at");
        activity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        activity.setActivityTypeId(rs.getInt("activity_type_id"));
        activity.setOrganizationLevelId(rs.getInt("organization_level_id"));

        // Quy đổi start_time và end_time lưu theo UTC trong DB sang múi giờ địa phương hiển thị cho người dùng.
        Timestamp startTime = rs.getTimestamp("start_time");
        activity.setStartTime(startTime == null
                ? null : DateTimeUtil.fromUtc(startTime.toLocalDateTime()));
        Timestamp endTime = rs.getTimestamp("end_time");
        activity.setEndTime(endTime == null
                ? null : DateTimeUtil.fromUtc(endTime.toLocalDateTime()));
        // Hoạt động được coi là công khai nếu is_public = 1, hoặc không yêu cầu form đăng ký, hoặc không phải loại yêu cầu đăng ký.
        boolean requiresForm = rs.getBoolean("registration_form_required");
        activity.setRegistrationFormRequired(requiresForm);
        activity.setPublic(rs.getBoolean("is_public")
                || !requiresForm
                || activity.getNature() != Enum.ActivityNature.REQUIRE_REGISTRATION);
        return activity;
    }

    /**
     * Tìm danh sách hoạt động ngoại khóa theo mã hoạt động ({@code code}).
     *
     * @param code mã hoạt động cần tra cứu
     * @return danh sách {@link ExtracurricularActivity} trùng mã
     */
    @Override
    public List<ExtracurricularActivity> findByCode(String code) {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity WHERE code = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả và ánh xạ sang model ExtracurricularActivity.
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to find extracurricular activities by code.", e);
        }
        return list;
    }

    /**
     * Tìm danh sách hoạt động ngoại khóa theo trạng thái hoạt động ({@code UPCOMING}, {@code ONGOING}, ...).
     *
     * @param status trạng thái hoạt động cần lọc
     * @return danh sách {@link ExtracurricularActivity} khớp trạng thái
     */
    @Override
    public List<ExtracurricularActivity> findByActivityStatus(Enum.ActivityStatus status) {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity WHERE activity_status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt tập kết quả để ánh xạ từng hoạt động khớp trạng thái.
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to find extracurricular activities by status.", e);
        }
        return list;
    }

    /**
     * Tìm danh sách hoạt động ngoại khóa theo trạng thái phê duyệt ({@code PENDING}, {@code APPROVED}, ...).
     *
     * @param status trạng thái phê duyệt cần lọc
     * @return danh sách {@link ExtracurricularActivity} khớp trạng thái phê duyệt
     */
    @Override
    public List<ExtracurricularActivity> findByApprovalStatus(Enum.ApprovalStatus status) {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity WHERE approval_status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt tập kết quả để ánh xạ từng hoạt động khớp trạng thái duyệt.
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to find extracurricular activities by approval status.", e);
        }
        return list;
    }

    /**
     * Lấy toàn bộ bản ghi hoạt động ngoại khóa từ bảng {@code extracurricular_activity}.
     *
     * @return danh sách tất cả {@link ExtracurricularActivity}
     */
    @Override
    public List<ExtracurricularActivity> findAll() {
        List<ExtracurricularActivity> list = new ArrayList<>();
        String sql = "SELECT * FROM extracurricular_activity";

        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt từng dòng trong bảng và ánh xạ thành thực thể ExtracurricularActivity.
            while (rs.next()) {
                list.add(mapActivity(rs));
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to find all extracurricular activities.", e);
        }
        return list;
    }

    /**
     * Truy vấn toàn bộ danh sách hoạt động ngoại khóa kèm đầy đủ thông tin JOIN từ các bảng danh mục
     * (học kỳ, loại hoạt động, cấp tổ chức, đơn vị phụ trách, cán bộ, đối tác).
     *
     * @return danh sách {@link ExtracurricularActivityResponseDTO} sắp xếp mới nhất trước
     */
    @Override
    public List<ExtracurricularActivityResponseDTO> findAllWithDetails() {
        return queryActivitiesWithDetails(null, null);
    }

    /**
     * Truy vấn danh sách hoạt động chi tiết nằm trong phạm vi quản lý của một tài khoản cán bộ theo vai trò.
     *
     * @param accountId ID tài khoản cán bộ đang đăng nhập
     * @param role      mã vai trò (ví dụ {@code STAFF}, {@code UNIT_HEAD}, {@code UNIT_DEPUTY_HEAD}, {@code UNIT_SECRETARY})
     * @return danh sách hoạt động chi tiết thuộc phạm vi, hoặc danh sách rỗng nếu tài khoản/vai trò không hợp lệ
     */
    @Override
    public List<ExtracurricularActivityResponseDTO> findAllWithDetailsForStaff(int accountId, String role) {
        // Trả về danh sách rỗng nếu ID tài khoản không hợp lệ.
        if (accountId <= 0) {
            return List.of();
        }
        // Xác định điều kiện WHERE theo vai trò; trả về rỗng nếu vai trò không được hỗ trợ phân phạm vi.
        String scope = staffScopePredicate(role);
        return scope == null ? List.of() : queryActivitiesWithDetails(scope, accountId);
    }

    /**
     * Kiểm tra một hoạt động cụ thể có nằm trong phạm vi quyền truy cập của tài khoản cán bộ theo vai trò hay không.
     *
     * @param activityId ID hoạt động cần kiểm tra
     * @param accountId  ID tài khoản cán bộ
     * @param role       mã vai trò của cán bộ
     * @return {@code true} nếu hoạt động thuộc phạm vi quản lý của cán bộ, ngược lại {@code false}
     */
    @Override
    public boolean isInStaffActivityScope(int activityId, int accountId, String role) {
        // Từ chối ngay nếu ID hoạt động hoặc ID tài khoản không hợp lệ.
        if (activityId <= 0 || accountId <= 0) {
            return false;
        }
        String scope = staffScopePredicate(role);
        // Nếu vai trò không có quy tắc phạm vi tương ứng thì không có quyền truy cập.
        if (scope == null) {
            return false;
        }
        // Ghép điều kiện kiểm tra phạm vi theo vai trò vào mệnh đề WHERE để xác thực trực tiếp trên DB.
        String sql = "SELECT 1 FROM extracurricular_activity a WHERE a.id = ? AND " + scope;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to check staff activity access.", e);
        }
    }

    /**
     * Sinh biểu thức điều kiện SQL (predicate) giới hạn phạm vi hoạt động dựa trên vai trò cán bộ.
     *
     * @param role mã vai trò người dùng
     * @return đoạn điều kiện SQL {@code EXISTS (...)} sử dụng 1 tham số {@code account_id}, hoặc {@code null} nếu vai trò không khớp
     */
    private String staffScopePredicate(String role) {
        // Với vai trò STAFF: chỉ được xem/thao tác các hoạt động mà bản thân là cán bộ phụ trách trực tiếp.
        if ("STAFF".equals(role)) {
            return """
                    EXISTS (SELECT 1 FROM staff scoped_staff
                            WHERE scoped_staff.account_id = ?
                              AND scoped_staff.id = a.responsible_staff_id)
                    """;
        }
        // Với lãnh đạo/thư ký đơn vị: được quản lý mọi hoạt động thuộc các đơn vị mà cán bộ được phân công.
        if ("UNIT_DEPUTY_HEAD".equals(role) || "UNIT_HEAD".equals(role)
                || "UNIT_SECRETARY".equals(role)) {
            return """
                    EXISTS (SELECT 1
                            FROM staff scoped_staff
                            JOIN staff_assignment sa ON sa.staff_id = scoped_staff.id
                            WHERE scoped_staff.account_id = ?
                              AND sa.department_id = a.responsible_department_id)
                    """;
        }
        // Các vai trò khác không có predicate phạm vi cán bộ.
        return null;
    }

    /**
     * Thực thi truy vấn lấy danh sách hoạt động ngoại khóa kèm đầy đủ thông tin JOIN từ các bảng danh mục,
     * có thể lọc thêm theo điều kiện phạm vi ({@code scope}) của tài khoản cán bộ.
     *
     * @param scope     điều kiện WHERE bổ sung theo vai trò (null nếu lấy toàn bộ)
     * @param accountId ID tài khoản để gán cho tham số trong {@code scope} (null nếu không lọc)
     * @return danh sách {@link ExtracurricularActivityResponseDTO} sắp xếp theo ID giảm dần
     */
    private List<ExtracurricularActivityResponseDTO> queryActivitiesWithDetails(String scope, Integer accountId) {
        ensureAttendanceSessionSchema();
        List<ExtracurricularActivityResponseDTO> list = new ArrayList<>();
        // Truy vấn LEFT JOIN 7 bảng liên quan (học kỳ, loại, cấp tổ chức, đơn vị, cán bộ, đối tác) và ghép động mệnh đề WHERE nếu có phạm vi lọc.
        String sql = """
    SELECT
        a.*,
        s.code AS semester_code, s.name AS semester_name,
        t.code AS activity_type_code, t.name AS activity_type_name,
        ol.code AS org_level_code, ol.name AS org_level_name, ol.coefficient AS org_level_coefficient,
        d.code AS dept_code, d.name AS dept_name,
        st.code AS staff_code, st.name AS staff_name,
        pc.code AS company_code, pc.name AS company_name,
        ps.code AS partner_staff_code, ps.name AS partner_staff_name
    FROM extracurricular_activity a
    LEFT JOIN semester s ON a.semester_id = s.id
    LEFT JOIN activity_type t ON a.activity_type_id = t.id
    LEFT JOIN activity_organization_level ol ON a.organization_level_id = ol.id
    LEFT JOIN department d ON a.responsible_department_id = d.id
    LEFT JOIN staff st ON a.responsible_staff_id = st.id
    LEFT JOIN partner_company pc ON a.partner_company_id = pc.id
    LEFT JOIN partner_staff ps ON a.partner_staff_id = ps.id
    """ + (scope == null ? "" : "WHERE " + scope) + """
    ORDER BY a.id DESC
    """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Chỉ gán tham số accountId khi truy vấn có mệnh đề WHERE lọc theo phạm vi cán bộ.
            if (accountId != null) {
                stmt.setInt(1, accountId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                // Duyệt từng dòng kết quả và ánh xạ toàn bộ thuộc tính chính cùng các cột JOIN sang DTO phản hồi.
                while (rs.next()) {
                    ExtracurricularActivityResponseDTO dto = new ExtracurricularActivityResponseDTO();
                    dto.setId(rs.getInt("id"));
                    dto.setCode(rs.getString("code"));
                    dto.setName(rs.getString("name"));
                    dto.setSemesterId(rs.getInt("semester_id"));
                    dto.setActivityTypeId(rs.getInt("activity_type_id"));
                    dto.setOrganizationLevelId(rs.getInt("organization_level_id"));
                    dto.setResponsibleDepartmentId(rs.getInt("responsible_department_id"));
                    dto.setResponsibleStaffId(rs.getInt("responsible_staff_id"));
                    dto.setPartnerCompanyId((Integer) rs.getObject("partner_company_id"));
                    dto.setPartnerStaffId((Integer) rs.getObject("partner_staff_id"));
                    // Chuyển đổi tính chất hoạt động từ chuỗi DB sang enum (cho phép null).
                    String nature = rs.getString("nature");
                    dto.setNature(nature == null ? null : Enum.ActivityNature.valueOf(nature));
                    dto.setSummaryRequired(rs.getBoolean("is_summary_required"));
                    dto.setMaxParticipants((Integer) rs.getObject("max_participants"));
                    dto.setAttendanceSessionCount(readAttendanceSessionCount(rs));
                    dto.setAddress(rs.getString("address"));
                    dto.setDescription(rs.getString("description"));
                    dto.setActivityStatus(Enum.ActivityStatus.valueOf(rs.getString("activity_status")));
                    dto.setApprovalStatus(Enum.ApprovalStatus.valueOf(rs.getString("approval_status")));

                    // Quy đổi mốc thời gian bắt đầu và kết thúc từ UTC về múi giờ hệ thống.
                    Timestamp startTime = rs.getTimestamp("start_time");
                    dto.setStartTime(startTime == null
                            ? null : DateTimeUtil.fromUtc(startTime.toLocalDateTime()));
                    Timestamp endTime = rs.getTimestamp("end_time");
                    dto.setEndTime(endTime == null
                            ? null : DateTimeUtil.fromUtc(endTime.toLocalDateTime()));
                    // Xác định trạng thái công khai hiệu lực dựa trên cờ is_public, yêu cầu form và tính chất hoạt động.
                    boolean requiresForm = rs.getBoolean("registration_form_required");
                    dto.setRegistrationFormRequired(requiresForm);
                    dto.setPublic(rs.getBoolean("is_public")
                            || !requiresForm
                            || dto.getNature() != Enum.ActivityNature.REQUIRE_REGISTRATION);

                    // Thông tin chi tiết JOIN từ các bảng liên quan
                    dto.setSemesterCode(rs.getString("semester_code"));
                    dto.setSemesterName(rs.getString("semester_name"));
                    dto.setActivityTypeCode(rs.getString("activity_type_code"));
                    dto.setActivityTypeName(rs.getString("activity_type_name"));
                    dto.setOrganizationLevelCode(rs.getString("org_level_code"));
                    dto.setOrganizationLevelName(rs.getString("org_level_name"));
                    dto.setOrganizationLevelCoefficient(rs.getBigDecimal("org_level_coefficient"));
                    dto.setResponsibleDepartmentCode(rs.getString("dept_code"));
                    dto.setResponsibleDepartmentName(rs.getString("dept_name"));
                    dto.setResponsibleStaffCode(rs.getString("staff_code"));
                    dto.setResponsibleStaffName(rs.getString("staff_name"));
                    dto.setPartnerCompanyCode(rs.getString("company_code"));
                    dto.setPartnerCompanyName(rs.getString("company_name"));
                    dto.setPartnerStaffCode(rs.getString("partner_staff_code"));
                    dto.setPartnerStaffName(rs.getString("partner_staff_name"));

                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            // Ghi log mức SEVERE và ném ngoại lệ runtime nếu truy vấn danh sách chi tiết thất bại.
            LOGGER.log(Level.SEVERE, "Failed to query extracurricular activity list.", e);
            throw new IllegalStateException("Failed to find all activity details.", e);
        }
        return list;
    }

    /**
     * Tìm một hoạt động ngoại khóa theo khóa chính ({@code id}).
     *
     * @param id ID hoạt động cần tìm
     * @return đối tượng {@link ExtracurricularActivity} hoặc {@code null} nếu không tồn tại
     */
    @Override
    public ExtracurricularActivity findById(Integer id) {
        String sql = "SELECT * FROM extracurricular_activity WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Nếu có bản ghi khớp thì ánh xạ sang model, ngược lại trả về null.
                return rs.next() ? mapActivity(rs) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to find extracurricular activity by ID.", e);
        }
    }

    /**
     * Đếm số đơn đăng ký đang giữ chỗ ({@code SUBMITTED} hoặc {@code APPROVED}) của một hoạt động
     * để kiểm tra sức chứa tối đa ({@code max_participants}).
     *
     * @param activityId ID hoạt động cần đếm
     * @return số đơn đăng ký đang chiếm chỗ
     */
    @Override
    public int countReservedApplications(int activityId) {
        // JOIN bảng đơn đăng ký với biểu mẫu đăng ký của hoạt động và chỉ đếm các đơn chưa bị hủy/từ chối.
        String sql = "SELECT COUNT(*) FROM dbo.activity_registration r "
                + "JOIN dbo.activity_registration_form f ON f.id=r.form_id "
                + "WHERE f.activity_id=? AND r.status IN ('SUBMITTED','APPROVED')";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Đọc giá trị COUNT(*) ở cột đầu tiên (mặc định 0 nếu không có dòng trả về).
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to count reserved activity registrations.", e);
        }
    }

    /**
     * Đếm tổng số đơn đăng ký chưa bị sinh viên rút ({@code status <> 'WITHDRAWN'}) của một hoạt động.
     *
     * @param activityId ID hoạt động cần đếm
     * @return số đơn đăng ký đang hiệu lực
     */
    @Override
    public int countActiveApplications(int activityId) {
        // Đếm mọi đơn đăng ký gắn với form của hoạt động ngoại trừ các đơn đã rút (WITHDRAWN).
        String sql = "SELECT COUNT(*) FROM dbo.activity_registration r "
                + "JOIN dbo.activity_registration_form f ON f.id=r.form_id "
                + "WHERE f.activity_id=? AND r.status<>'WITHDRAWN'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Đọc kết quả COUNT(*) ở cột đầu tiên (mặc định 0 nếu không có dòng trả về).
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to count reserved activity registrations.", e);
        }
    }

    /**
     * Thêm mới một hoạt động ngoại khóa vào CSDL và trả về ID tự sinh.
     *
     * @param activity đối tượng {@link ExtracurricularActivity} chứa thông tin cần chèn
     * @return ID mới được sinh ra, hoặc {@code null} nếu không chèn được bản ghi nào
     * @throws IllegalArgumentException nếu mã hoạt động ({@code code}) bị trùng khóa duy nhất
     */
    @Override
    public Integer insert(ExtracurricularActivity activity) {
        ensureAttendanceSessionSchema();
        String sql = "INSERT INTO extracurricular_activity "
                + "(semester_id, code, name, responsible_department_id, responsible_staff_id, "
                + "partner_company_id, partner_staff_id, nature, is_summary_required, address, description, "
                + "activity_status, approval_status, activity_type_id, organization_level_id, start_time, end_time, registration_form_required, max_participants, is_public, attendance_session_count) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindActivity(ps, activity);
            // Nếu lệnh INSERT không tác động dòng nào thì trả về null.
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Trả về khóa tự tăng vừa được sinh nếu có, ngược lại null.
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            // Phát hiện lỗi trùng khóa duy nhất (mã hoạt động) để ném ngoại lệ nghiệp vụ thân thiện.
            if (isDuplicateCodeError(e)) {
                throw new IllegalArgumentException("Extracurricular activity with the same code already exists.", e);
            }
            throw new IllegalStateException("Failed to insert extracurricular activity.", e);
        }
    }

    /**
     * Cập nhật thông tin hoạt động ngoại khóa mà không thay đổi trạng thái phê duyệt hiện tại.
     *
     * @param activity đối tượng hoạt động chứa dữ liệu mới
     * @return {@code true} nếu cập nhật thành công ít nhất 1 dòng, ngược lại {@code false}
     */
    @Override
    public boolean update(ExtracurricularActivity activity) {
        return update(activity, null);
    }

    /**
     * Cập nhật thông tin hoạt động ngoại khóa và tùy chọn cập nhật kèm trạng thái phê duyệt mới.
     *
     * @param activity          đối tượng hoạt động cần cập nhật (phải có {@code id > 0})
     * @param newApprovalStatus trạng thái phê duyệt mới (truyền {@code null} nếu giữ nguyên)
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}
     * @throws IllegalArgumentException nếu mã hoạt động mới bị trùng với bản ghi khác
     */
    @Override
    public boolean update(ExtracurricularActivity activity, Enum.ApprovalStatus newApprovalStatus) {
        // Không thực hiện cập nhật nếu đối tượng null hoặc ID không hợp lệ.
        if (activity == null || activity.getId() <= 0) {
            return false;
        }
        ensureAttendanceSessionSchema();

        // Ghép động cột approval_status vào câu lệnh UPDATE chỉ khi caller yêu cầu đổi trạng thái duyệt.
        String sql = "UPDATE extracurricular_activity SET "
                + "semester_id = ?, code = ?, name = ?, responsible_department_id = ?, responsible_staff_id = ?, "
                + "partner_company_id = ?, partner_staff_id = ?, nature = ?, is_summary_required = ?, "
                + "address = ?, description = ?, activity_type_id = ?, organization_level_id = ?, start_time = ?, end_time = ?, "
                + "registration_form_required = ?, max_participants = ?, is_public = ?, attendance_session_count = ?"
                + (newApprovalStatus == null ? "" : ", approval_status = ?") + " WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activity.getSemesterId());
            ps.setString(2, activity.getCode());
            ps.setString(3, activity.getName());
            ps.setInt(4, activity.getResponsibleDepartmentId());
            ps.setInt(5, activity.getResponsibleStaffId());
            // Gán NULL nếu hoạt động không gắn doanh nghiệp đối tác, ngược lại gán ID doanh nghiệp.
            if (activity.getPartnerCompanyId() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, activity.getPartnerCompanyId());
            }
            // Gán NULL nếu không có cán bộ đối tác đi kèm, ngược lại gán ID cán bộ đối tác.
            if (activity.getPartnerStaffId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, activity.getPartnerStaffId());
            }
            // Gán tên enum tính chất hoạt động (hoặc null nếu chưa thiết lập).
            ps.setString(8, activity.getNature() == null ? null : activity.getNature().name());
            ps.setBoolean(9, activity.isSummaryRequired());
            ps.setString(10, activity.getAddress());
            ps.setString(11, activity.getDescription());
            ps.setInt(12, activity.getActivityTypeId());
            ps.setInt(13, activity.getOrganizationLevelId());
            // Quy đổi thời gian bắt đầu và kết thúc từ múi giờ ứng dụng sang UTC trước khi lưu DB.
            ps.setTimestamp(14, activity.getStartTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getStartTime())));
            ps.setTimestamp(15, activity.getEndTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getEndTime())));
            ps.setBoolean(16, activity.isRegistrationFormRequired());
            setMaxParticipants(ps, 17, activity);
            ps.setBoolean(18, activity.isPublic());
            ps.setInt(19, activity.getAttendanceSessionCount());
            int next = 20;
            // Nếu có truyền trạng thái phê duyệt mới thì gán tham số động tương ứng trước tham số WHERE id.
            if (newApprovalStatus != null) {
                ps.setString(next++, newApprovalStatus.name());
            }
            ps.setInt(next, activity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Kiểm tra mã lỗi SQL trùng unique index/constraint cho mã hoạt động.
            if (isDuplicateCodeError(e)) {
                throw new IllegalArgumentException("Mã hoạt động đã tồn tại. Vui lòng nhập mã khác.", e);
            }
            throw new IllegalStateException("Failed to update extracurricular activity: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật có điều kiện và khóa chống tranh chấp ({@code UPDLOCK, HOLDLOCK}) cho một hoạt động
     * đang ở trạng thái sắp diễn ra ({@code UPCOMING}), chưa tới giờ bắt đầu ({@code start_time > now}),
     * còn giữ đúng trạng thái duyệt kỳ vọng và không giảm {@code max_participants} xuống thấp hơn
     * số lượng đơn đăng ký đã giữ chỗ.
     *
     * @param activity               dữ liệu hoạt động mới cần lưu
     * @param expectedApprovalStatus trạng thái phê duyệt kỳ vọng hiện tại để chống ghi đè lạc quan
     * @param newApprovalStatus      trạng thái phê duyệt mới (null nếu giữ nguyên)
     * @param now                    thời điểm hiện tại theo UTC để đối chiếu với {@code start_time}
     * @return {@code true} nếu thỏa mọi điều kiện và cập nhật thành công, ngược lại {@code false}
     * @throws IllegalArgumentException nếu mã hoạt động mới bị trùng
     */
    @Override
    public boolean updateUpcoming(
            ExtracurricularActivity activity,
            Enum.ApprovalStatus expectedApprovalStatus,
            Enum.ApprovalStatus newApprovalStatus,
            LocalDateTime now) {
        // Từ chối sớm nếu thiếu bất kỳ tham số bắt buộc nào cho điều kiện cập nhật an toàn.
        if (activity == null || activity.getId() <= 0
                || expectedApprovalStatus == null || now == null) {
            return false;
        }
        ensureAttendanceSessionSchema();

        // Sử dụng gợi ý khóa UPDLOCK + HOLDLOCK trên cả hoạt động lẫn đơn đăng ký để đảm bảo kiểm tra
        // trạng thái UPCOMING, giờ bắt đầu, trạng thái duyệt kỳ vọng và sức chứa tối đa diễn ra nguyên tử.
        String sql = "UPDATE a WITH (UPDLOCK,HOLDLOCK) SET "
                + "semester_id = ?, code = ?, name = ?, responsible_department_id = ?, responsible_staff_id = ?, "
                + "partner_company_id = ?, partner_staff_id = ?, nature = ?, is_summary_required = ?, "
                + "address = ?, description = ?, activity_type_id = ?, organization_level_id = ?, start_time = ?, end_time = ?, "
                + "registration_form_required = ?, max_participants = ?, is_public = ?, attendance_session_count = ?"
                + (newApprovalStatus == null ? "" : ", approval_status = ?")
                + " FROM dbo.extracurricular_activity a "
                + " WHERE a.id = ? AND a.activity_status = 'UPCOMING' "
                + "AND a.start_time IS NOT NULL AND a.start_time > ? "
                + "AND a.approval_status = ? "
                + "AND (? IS NULL OR ? >= (SELECT COUNT(*) FROM dbo.activity_registration r WITH (UPDLOCK,HOLDLOCK) "
                + "JOIN dbo.activity_registration_form f ON f.id=r.form_id "
                + "WHERE f.activity_id=a.id "
                + "AND r.status IN ('SUBMITTED','APPROVED')) "
                + "OR (? IS NOT NULL AND a.max_participants = ?))";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activity.getSemesterId());
            ps.setString(2, activity.getCode());
            ps.setString(3, activity.getName());
            ps.setInt(4, activity.getResponsibleDepartmentId());
            ps.setInt(5, activity.getResponsibleStaffId());
            // Gán NULL nếu không có doanh nghiệp đối tác, ngược lại gán ID doanh nghiệp.
            if (activity.getPartnerCompanyId() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, activity.getPartnerCompanyId());
            }
            // Gán NULL nếu không có cán bộ đối tác, ngược lại gán ID cán bộ đối tác.
            if (activity.getPartnerStaffId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, activity.getPartnerStaffId());
            }
            // Chuyển enum tính chất sang chuỗi hoặc null.
            ps.setString(8, activity.getNature() == null ? null : activity.getNature().name());
            ps.setBoolean(9, activity.isSummaryRequired());
            ps.setString(10, activity.getAddress());
            ps.setString(11, activity.getDescription());
            ps.setInt(12, activity.getActivityTypeId());
            ps.setInt(13, activity.getOrganizationLevelId());
            // Quy đổi thời gian bắt đầu và kết thúc sang UTC trước khi lưu DB.
            ps.setTimestamp(14, activity.getStartTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getStartTime())));
            ps.setTimestamp(15, activity.getEndTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getEndTime())));
            ps.setBoolean(16, activity.isRegistrationFormRequired());
            setMaxParticipants(ps, 17, activity);
            ps.setBoolean(18, activity.isPublic());
            ps.setInt(19, activity.getAttendanceSessionCount());

            int next = 20;
            // Gán tham số approval_status mới nếu có yêu cầu chuyển trạng thái duyệt.
            if (newApprovalStatus != null) {
                ps.setString(next++, newApprovalStatus.name());
            }
            // Gán các tham số điều kiện WHERE: id, thời điểm hiện tại UTC, trạng thái duyệt kỳ vọng và kiểm tra sức chứa.
            ps.setInt(next++, activity.getId());
            ps.setTimestamp(next++, Timestamp.valueOf(now));
            ps.setString(next++, expectedApprovalStatus.name());
            setMaxParticipants(ps, next++, activity);
            setMaxParticipants(ps, next++, activity);
            setMaxParticipants(ps, next++, activity);
            setMaxParticipants(ps, next, activity);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Chuyển lỗi trùng mã hoạt động thành IllegalArgumentException hiển thị cho người dùng.
            if (isDuplicateCodeError(e)) {
                throw new IllegalArgumentException("Mã hoạt động đã tồn tại. Vui lòng nhập mã khác.", e);
            }
            throw new IllegalStateException("Failed to update upcoming extracurricular activity.", e);
        }
    }

    /**
     * Gán giá trị giới hạn số lượng người tham gia ({@code max_participants}) vào {@link PreparedStatement},
     * lưu thành SQL NULL nếu hoạt động không giới hạn số lượng.
     *
     * @param ps       câu lệnh chuẩn bị JDBC
     * @param index    vị trí tham số trong câu lệnh SQL
     * @param activity hoạt động nguồn chứa giá trị {@code maxParticipants}
     * @throws SQLException nếu lỗi gán tham số JDBC
     */
    private void setMaxParticipants(PreparedStatement ps, int index, ExtracurricularActivity activity) throws SQLException {
        // Nếu không thiết lập giới hạn người tham gia thì gán SQL NULL, ngược lại gán giá trị số nguyên.
        if (activity.getMaxParticipants() == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, activity.getMaxParticipants());
        }
    }

    /**
     * Kiểm tra chuỗi ngoại lệ {@link SQLException} có chứa mã lỗi vi phạm khóa duy nhất của SQL Server
     * ({@code 2601} - trùng unique index hoặc {@code 2627} - trùng primary/unique constraint) hay không.
     *
     * @param exception ngoại lệ SQL gốc
     * @return {@code true} nếu là lỗi trùng mã, ngược lại {@code false}
     */
    private boolean isDuplicateCodeError(SQLException exception) {
        // Duyệt toàn bộ chuỗi ngoại lệ liên kết (getNextException) vì driver JDBC có thể bọc lỗi thực bên dưới.
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            // Mã 2601 (Unique Index) và 2627 (Unique/Primary Key Constraint) là hai mã lỗi trùng khóa của SQL Server.
            if (current.getErrorCode() == 2601 || current.getErrorCode() == 2627) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gán toàn bộ 21 tham số của một {@link ExtracurricularActivity} vào câu lệnh {@code INSERT}.
     *
     * @param ps       câu lệnh {@link PreparedStatement} cho lệnh chèn
     * @param activity đối tượng hoạt động nguồn
     * @throws SQLException nếu lỗi gán tham số JDBC
     */
    private void bindActivity(PreparedStatement ps, ExtracurricularActivity activity) throws SQLException {
        ps.setInt(1, activity.getSemesterId());
        ps.setString(2, activity.getCode());
        ps.setString(3, activity.getName());
        ps.setInt(4, activity.getResponsibleDepartmentId());
        ps.setInt(5, activity.getResponsibleStaffId());
        // Gán NULL cho partner_company_id nếu không chọn doanh nghiệp đối tác.
        if (activity.getPartnerCompanyId() == null) {
            ps.setNull(6, Types.INTEGER);
        } else {
            ps.setInt(6, activity.getPartnerCompanyId());
        }
        // Gán NULL cho partner_staff_id nếu không chọn cán bộ đối tác.
        if (activity.getPartnerStaffId() == null) {
            ps.setNull(7, Types.INTEGER);
        } else {
            ps.setInt(7, activity.getPartnerStaffId());
        }
        // Gán tên enum cho các cột nature, activity_status, approval_status (giữ null nếu chưa thiết lập).
        ps.setString(8, activity.getNature() == null ? null : activity.getNature().name());
        ps.setBoolean(9, activity.isSummaryRequired());
        ps.setString(10, activity.getAddress());
        ps.setString(11, activity.getDescription());
        ps.setString(12, activity.getActivityStatus() == null ? null : activity.getActivityStatus().name());
        ps.setString(13, activity.getApprovalStatus() == null ? null : activity.getApprovalStatus().name());
        ps.setInt(14, activity.getActivityTypeId());
        ps.setInt(15, activity.getOrganizationLevelId());
        // Quy đổi start_time và end_time sang UTC trước khi lưu vào DB.
        ps.setTimestamp(16, activity.getStartTime() == null
                ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getStartTime())));
        ps.setTimestamp(17, activity.getEndTime() == null
                ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getEndTime())));
        ps.setBoolean(18, activity.isRegistrationFormRequired());
        setMaxParticipants(ps, 19, activity);
        ps.setBoolean(20, activity.isPublic());
        ps.setInt(21, activity.getAttendanceSessionCount());
    }

    /**
     * Xóa hoàn toàn một hoạt động ngoại khóa cùng toàn bộ dữ liệu phụ thuộc (lịch sử điểm danh,
     * danh sách tham gia, phân công cán bộ/đơn vị, đối tác và lời mời hợp tác) trong cùng một transaction.
     *
     * @param id ID hoạt động cần xóa
     * @return {@code true} nếu tìm thấy và xóa thành công, {@code false} nếu {@code id} null hoặc không tồn tại
     */
    @Override
    public boolean delete(Integer id) {
        // Không thực hiện nếu ID truyền vào là null.
        if (id == null) {
            return false;
        }

        String existsSql = "SELECT 1 FROM extracurricular_activity WHERE id = ?";
        // Xóa các bảng con theo thứ tự ràng buộc khóa ngoại (xóa lịch sử điểm danh trước participant,
        // và xóa liên kết doanh nghiệp đối tác trước lời mời hợp tác vì có khóa ngoại tham chiếu).
        String[] dependentDeletes = {
                "DELETE h FROM activity_attendance_history h "
                        + "JOIN activity_participant ap ON ap.id = h.activity_participant_id "
                        + "WHERE ap.activity_id = ?",
                "DELETE FROM activity_participant WHERE activity_id = ?",
                "DELETE FROM activity_staff WHERE activity_id = ?",
                "DELETE FROM activity_department WHERE activity_id = ?",
                "DELETE FROM activity_partner_staff WHERE activity_id = ?",
                "DELETE FROM activity_partner_company WHERE activity_id = ?",
                "DELETE FROM activity_collaboration_invitation WHERE activity_id = ?"
        };

        // Mở transaction để đảm bảo toàn bộ các bước xóa bảng phụ thuộc và bảng chính diễn ra nguyên tử.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(existsSql)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Nếu hoạt động không tồn tại thì hoàn tác và kết thúc sớm.
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                // Duyệt lần lượt các câu lệnh xóa dữ liệu phụ thuộc của hoạt động trước khi xóa bảng chính.
                for (String sql : dependentDeletes) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps =
                             conn.prepareStatement("DELETE FROM extracurricular_activity WHERE id = ?")) {
                    ps.setInt(1, id);
                    // Nếu bản ghi chính không còn để xóa (ví dụ bị xóa đồng thời) thì rollback.
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                // Xác nhận lưu toàn bộ thao tác xóa khi tất cả các bước đều thành công.
                conn.commit();
                return true;
            } catch (SQLException e) {
                // Có lỗi SQL trong transaction thì hoàn tác mọi thay đổi trước khi ném lại ngoại lệ.
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    // Đính kèm lỗi rollback vào ngoại lệ gốc để không làm mất nguyên nhân ban đầu.
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Failed to delete extracurricular activity.", e);
        }
    }

    /**
     * Cập nhật trạng thái phê duyệt của một hoạt động chỉ khi hoạt động đang ở trạng thái {@code UPCOMING},
     * chưa tới thời điểm bắt đầu ({@code start_time > now}) và trạng thái duyệt hiện tại nằm trong danh sách cho phép.
     *
     * @param id          ID hoạt động cần chuyển trạng thái duyệt
     * @param status      trạng thái phê duyệt đích
     * @param allowedFrom danh sách các trạng thái phê duyệt hiện tại được phép chuyển đổi
     * @param now         thời điểm hiện tại theo UTC
     * @return {@code true} nếu thỏa điều kiện và cập nhật thành công, ngược lại {@code false}
     */
    @Override
    public boolean updateApprovalStatusIfUpcoming(
            int id,
            Enum.ApprovalStatus status,
            List<Enum.ApprovalStatus> allowedFrom,
            LocalDateTime now) {

        // Kiểm tra tính hợp lệ của tất cả tham số đầu vào trước khi dựng câu lệnh SQL.
        if (id <= 0 || status == null
                || allowedFrom == null || allowedFrom.isEmpty()
                || now == null) {
            return false;
        }

        // Xây dựng câu lệnh SQL động với mệnh đề IN (?, ?, ...) tương ứng số phần tử của allowedFrom.
        StringBuilder sql = new StringBuilder(
                "UPDATE extracurricular_activity "
                        + "SET approval_status = ? "
                        + "WHERE id = ? "
                        + "AND activity_status = 'UPCOMING' "
                        + "AND start_time IS NOT NULL "
                        + "AND start_time > ? "
                        + "AND approval_status IN (");

        // Duyệt số lượng trạng thái cho phép để nối đủ placeholder "?" vào mệnh đề IN.
        for (int i = 0; i < allowedFrom.size(); i++) {
            // Thêm dấu phẩy ngăn cách từ phần tử thứ hai trở đi.
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }

        sql.append(")");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.setTimestamp(3, Timestamp.valueOf(now));

            int index = 4;
            // Gán lần lượt tên enum của các trạng thái duyệt nguồn hợp lệ vào các placeholder trong mệnh đề IN.
            for (Enum.ApprovalStatus from : allowedFrom) {
                ps.setString(index++, from.name());
            }

            // Chỉ thành công nếu câu SQL cập nhật được ít nhất một dòng.
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException(
                    "Failed to update approval status: " + e.getMessage(), e);
        }
    }

    /**
     * Đồng bộ trạng thái hoạt động ngoại khóa và lời mời hợp tác liên quan dựa trên mốc thời gian UTC hiện tại
     * trong một transaction duy nhất.
     * Xử lý đầy đủ các chuyển trạng thái quá giờ chưa duyệt (REJECTED/CANCELLED), bắt kịp hoạt động đã qua giờ
     * kết thúc khi máy chủ Tomcat tạm ngưng, chuyển sang ONGOING và COMPLETED, đồng thời hủy lời mời PENDING.
     *
     * @param now thời điểm đồng bộ theo UTC (không được {@code null})
     */
    @Override
    public void synchronizeActivityStatuses(
            java.time.LocalDateTime now) {

        // Bắt buộc phải có mốc thời gian đồng bộ hợp lệ.
        if (now == null) {
            throw new IllegalArgumentException(
                    "The synchronization time must not be null.");
        }

        // Bước 1: Tự động từ chối & hủy các hoạt động quá giờ bắt đầu nhưng chưa được duyệt,
        // đồng thời dùng bảng tạm OUTPUT để hủy lời mời hợp tác và vô hiệu hóa liên kết đối tác tương ứng.
        String rejectUnapprovedSql = """
                DECLARE @RejectedActivities TABLE (id INT PRIMARY KEY);

                UPDATE extracurricular_activity
                SET approval_status = 'REJECTED',
                    activity_status = 'CANCELLED'
                OUTPUT inserted.id INTO @RejectedActivities (id)
                WHERE activity_status = 'UPCOMING'
                  AND (approval_status IS NULL OR approval_status <> 'APPROVED')
                  AND start_time IS NOT NULL
                  AND start_time <= ?;

                UPDATE i
                SET approval_status = 'CANCELLED'
                FROM activity_collaboration_invitation i
                JOIN @RejectedActivities r ON r.id = i.activity_id;

                UPDATE apc
                SET collaboration_status = 'INACTIVE'
                FROM activity_partner_company apc
                JOIN @RejectedActivities r ON r.id = apc.activity_id;

                UPDATE aps
                SET collaboration_status = 'INACTIVE'
                FROM activity_partner_staff aps
                JOIN @RejectedActivities r ON r.id = aps.activity_id;
                """;

        // 2. Đã duyệt nhưng đã qua cả giờ kết thúc: chuyển thẳng từ UPCOMING sang COMPLETED.
        String completeMissedWindowSql =
                "UPDATE extracurricular_activity " +
                        "SET activity_status = 'COMPLETED' " +
                        "WHERE activity_status = 'UPCOMING' " +
                        "AND approval_status = 'APPROVED' " +
                        "AND end_time IS NOT NULL AND end_time <= ?";

        // 3. Đã duyệt, đã bắt đầu nhưng chưa kết thúc.
        String startApprovedSql =
                "UPDATE extracurricular_activity " +
                        "SET activity_status = 'ONGOING' " +
                        "WHERE activity_status = 'UPCOMING' " +
                        "AND approval_status = 'APPROVED' " +
                        "AND start_time IS NOT NULL AND start_time <= ? " +
                        "AND end_time IS NOT NULL AND end_time > ?";

        // 4. Đang diễn ra và đã đến giờ kết thúc.
        String completeOngoingSql =
                "UPDATE extracurricular_activity " +
                        "SET activity_status = 'COMPLETED' " +
                        "WHERE activity_status = 'ONGOING' " +
                        "AND approval_status = 'APPROVED' " +
                        "AND end_time IS NOT NULL AND end_time <= ?";

        // 5. Hủy mọi lời mời hợp tác đang PENDING thuộc các hoạt động không còn ở trạng thái UPCOMING.
        String cancelInactiveInvitationsSql =
                "UPDATE activity_collaboration_invitation " +
                        "SET approval_status = 'CANCELLED' " +
                        "WHERE approval_status = 'PENDING' " +
                        "AND activity_id IN (" +
                        "SELECT id FROM extracurricular_activity " +
                        "WHERE activity_status IN ('ONGOING', 'COMPLETED', 'CANCELLED'))";

        try (Connection conn = DBContext.getConnection()) {

            // Lưu lại chế độ autoCommit gốc và bắt đầu transaction để 5 bước đồng bộ diễn ra nhất quán.
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                // Bước 1: Từ chối hoạt động chưa được duyệt và đồng bộ dữ liệu liên quan.
                try (PreparedStatement ps =
                             conn.prepareStatement(rejectUnapprovedSql)) {

                    ps.setTimestamp(1, Timestamp.valueOf(now));
                    ps.execute();
                }

                // Bước 2: Bắt kịp hoạt động đã kết thúc khi Tomcat tắt.
                try (PreparedStatement ps =
                             conn.prepareStatement(completeMissedWindowSql)) {

                    ps.setTimestamp(1, Timestamp.valueOf(now));
                    ps.executeUpdate();
                }

                // Bước 3: Chuyển hoạt động đã duyệt sang ONGOING.
                try (PreparedStatement ps =
                             conn.prepareStatement(startApprovedSql)) {

                    ps.setTimestamp(1, Timestamp.valueOf(now));
                    ps.setTimestamp(2, Timestamp.valueOf(now));
                    ps.executeUpdate();
                }

                // Bước 4: Hoàn thành hoạt động đang diễn ra đã hết giờ.
                try (PreparedStatement ps =
                             conn.prepareStatement(completeOngoingSql)) {

                    ps.setTimestamp(1, Timestamp.valueOf(now));
                    ps.executeUpdate();
                }

                // Hủy lời mời còn chờ khi hoạt động không còn ở trạng thái UPCOMING.
                try (PreparedStatement ps =
                             conn.prepareStatement(cancelInactiveInvitationsSql)) {
                    ps.executeUpdate();
                }

                // Lưu tất cả thay đổi.
                conn.commit();

            } catch (SQLException e) {

                // Có lỗi thì hoàn tác toàn bộ thay đổi trong transaction.
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    // Gắn lỗi rollback làm suppressed exception để giữ nguyên ngoại lệ nguyên nhân.
                    e.addSuppressed(rollbackException);
                }

                throw e;

            } finally {
                // Khôi phục lại chế độ autoCommit ban đầu trước khi trả kết nối về pool/đóng kết nối.
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                    // Connection sắp được đóng.
                }
            }

        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException(
                    "Failed to synchronize extracurricular activity statuses.",
                    e);
        }
    }

    /**
     * Xóa hoạt động và các bảng phụ thuộc trong một transaction chỉ khi hoạt động chưa bắt đầu
     * ({@code UPCOMING} và {@code start_time > SYSUTCDATETIME()}) hoặc đã bị hủy ({@code CANCELLED}).
     * Nếu hoạt động đang diễn ra ({@code ONGOING}) hoặc đã hoàn thành ({@code COMPLETED}), toàn bộ thao tác sẽ rollback.
     *
     * @param id ID hoạt động cần xóa có điều kiện
     * @return {@code true} nếu đủ điều kiện và xóa thành công, ngược lại {@code false}
     */
    @Override
    public boolean deleteIfNotOngoingOrCompleted(int id) {
        // Bỏ qua ngay nếu ID hoạt động không hợp lệ.
        if (id <= 0) {
            return false;
        }

        // Danh sách câu lệnh xóa các bảng con theo đúng thứ tự phụ thuộc khóa ngoại.
        String[] dependentDeletes = {
                "DELETE h FROM activity_attendance_history h "
                        + "JOIN activity_participant ap ON ap.id = h.activity_participant_id "
                        + "WHERE ap.activity_id = ?",
                "DELETE FROM activity_participant WHERE activity_id = ?",
                "DELETE FROM activity_staff WHERE activity_id = ?",
                "DELETE FROM activity_department WHERE activity_id = ?",
                "DELETE FROM activity_partner_staff WHERE activity_id = ?",
                "DELETE FROM activity_partner_company WHERE activity_id = ?",
                "DELETE FROM activity_collaboration_invitation WHERE activity_id = ?"
        };
        // Chỉ cho phép xóa bản ghi chính nếu trạng thái là CANCELLED hoặc UPCOMING chưa tới giờ bắt đầu UTC.
        String deleteActivitySql = """
                DELETE FROM extracurricular_activity
                WHERE id = ?
                  AND (
                      activity_status = 'CANCELLED'
                      OR (activity_status = 'UPCOMING'
                          AND start_time IS NOT NULL
                          AND start_time > SYSUTCDATETIME())
                  )
                """;

        // Thực thi xóa bảng con và xóa có điều kiện bảng chính trong cùng một transaction để dễ rollback nếu điều kiện không thỏa.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Duyệt và xóa tạm các dữ liệu phụ thuộc của hoạt động trong phạm vi transaction.
                for (String sql : dependentDeletes) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(deleteActivitySql)) {
                    ps.setInt(1, id);
                    // Nếu lệnh xóa chính không xóa được dòng nào (đang ONGOING/COMPLETED hoặc không tồn tại) thì rollback cả dữ liệu con.
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                // Xác nhận xóa thành công sau khi bản ghi chính thỏa điều kiện và đã được xóa.
                conn.commit();
                return true;
            } catch (SQLException e) {
                // Hoàn tác toàn bộ transaction nếu xảy ra lỗi SQL ở bất kỳ bước nào.
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    // Giữ lại lỗi rollback dưới dạng suppressed exception.
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException(
                    "Failed to delete activity that has not started or was cancelled.", e);
        }
    }

    /**
     * Tính toán lại và cập nhật cột {@code is_public} của một hoạt động dựa trên tính chất,
     * cấu hình yêu cầu biểu mẫu đăng ký và sự tồn tại của biểu mẫu đăng ký trong CSDL.
     *
     * @param activityId ID hoạt động cần đồng bộ cờ công khai
     * @return {@code true} nếu cập nhật được bản ghi hoạt động, ngược lại {@code false}
     */
    @Override
    public boolean refreshPublic(int activityId) {
        // Hoạt động được công khai (is_public = 1) nếu không yêu cầu đăng ký, không yêu cầu form đăng ký, hoặc đã tạo form đăng ký.
        String sql = """
                UPDATE dbo.extracurricular_activity
                SET is_public = CASE
                    WHEN nature <> 'REQUIRE_REGISTRATION' THEN 1
                    WHEN registration_form_required = 0 THEN 1
                    WHEN EXISTS (
                        SELECT 1
                        FROM dbo.activity_registration_form
                        WHERE activity_id = ?
                    ) THEN 1
                    ELSE 0
                END
                WHERE id = ?
                """;
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            p.setInt(2, activityId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime kèm thông điệp tiếng Việt.
            throw new IllegalStateException("Không thể cập nhật trạng thái công khai của hoạt động.", e);
        }
    }

}
