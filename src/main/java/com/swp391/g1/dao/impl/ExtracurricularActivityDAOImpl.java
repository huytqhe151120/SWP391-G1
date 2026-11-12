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

public class ExtracurricularActivityDAOImpl implements IExtracurricularActivityDAO {
    private static final Logger LOGGER = Logger.getLogger(ExtracurricularActivityDAOImpl.class.getName());
    private static volatile boolean attendanceSessionSchemaEnsured = false;

    public static void ensureAttendanceSessionSchema() {
        if (attendanceSessionSchemaEnsured) {
            return;
        }
        synchronized (ExtracurricularActivityDAOImpl.class) {
            if (attendanceSessionSchemaEnsured) {
                return;
            }
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
                    """;
            try (Connection conn = DBContext.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(ddl);
                attendanceSessionSchemaEnsured = true;
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Could not auto-ensure attendance session schema.", e);
            }
        }
    }

    private int readAttendanceSessionCount(ResultSet rs) {
        try {
            int val = rs.getInt("attendance_session_count");
            return val > 0 ? val : 1;
        } catch (SQLException e) {
            return 1;
        }
    }

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
        String nature = rs.getString("nature");
        activity.setNature(nature == null ? null : Enum.ActivityNature.valueOf(nature));
        activity.setSummaryRequired(rs.getBoolean("is_summary_required"));
        activity.setMaxParticipants((Integer) rs.getObject("max_participants"));
        activity.setAttendanceSessionCount(readAttendanceSessionCount(rs));
        activity.setAddress(rs.getString("address"));
        activity.setDescription(rs.getString("description"));

        String activityStatus = rs.getString("activity_status");
        activity.setActivityStatus(activityStatus == null ? null : Enum.ActivityStatus.valueOf(activityStatus));
        String approvalStatus = rs.getString("approval_status");
        activity.setApprovalStatus(approvalStatus == null ? null : Enum.ApprovalStatus.valueOf(approvalStatus));

        Timestamp createdAt = rs.getTimestamp("created_at");
        activity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        activity.setActivityTypeId(rs.getInt("activity_type_id"));
        activity.setOrganizationLevelId(rs.getInt("organization_level_id"));

        Timestamp startTime = rs.getTimestamp("start_time");
        activity.setStartTime(startTime == null
                ? null : DateTimeUtil.fromUtc(startTime.toLocalDateTime()));
        Timestamp endTime = rs.getTimestamp("end_time");
        activity.setEndTime(endTime == null
                ? null : DateTimeUtil.fromUtc(endTime.toLocalDateTime()));
        activity.setRegistrationFormRequired(rs.getBoolean("registration_form_required"));
        // Activity Series linking fields (nullable for standalone activities)
        activity.setSeriesId((Integer) rs.getObject("series_id"));
        activity.setOccurrenceNo((Integer) rs.getObject("occurrence_no"));
        Timestamp occurrenceStartDate = rs.getTimestamp("occurrence_start_date");
        activity.setOccurrenceStartDate(occurrenceStartDate == null
                ? null : occurrenceStartDate.toLocalDateTime().toLocalDate());
        activity.setApprovedViaSeries(rs.getBoolean("approved_via_series"));
        activity.setPublic(rs.getBoolean("is_public"));
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
    public List<ExtracurricularActivityResponseDTO> findAllWithDetails() {
        return queryActivitiesWithDetails(null, null);
    }

    @Override
    public List<ExtracurricularActivityResponseDTO> findAllWithDetailsForStaff(int accountId, String role) {
        if (accountId <= 0) {
            return List.of();
        }
        String scope = staffScopePredicate(role);
        return scope == null ? List.of() : queryActivitiesWithDetails(scope, accountId);
    }

    @Override
    public boolean isInStaffActivityScope(int activityId, int accountId, String role) {
        if (activityId <= 0 || accountId <= 0) {
            return false;
        }
        String scope = staffScopePredicate(role);
        if (scope == null) {
            return false;
        }
        String sql = "SELECT 1 FROM extracurricular_activity a WHERE a.id = ? AND " + scope;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check staff activity access.", e);
        }
    }

    private String staffScopePredicate(String role) {
        if ("STAFF".equals(role)) {
            return """
                    EXISTS (SELECT 1 FROM staff scoped_staff
                            WHERE scoped_staff.account_id = ?
                              AND scoped_staff.id = a.responsible_staff_id)
                    """;
        }
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
        return null;
    }

    private List<ExtracurricularActivityResponseDTO> queryActivitiesWithDetails(String scope, Integer accountId) {
        ensureAttendanceSessionSchema();
        List<ExtracurricularActivityResponseDTO> list = new ArrayList<>();
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
            if (accountId != null) {
                stmt.setInt(1, accountId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
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
                    String nature = rs.getString("nature");
                    dto.setNature(nature == null ? null : Enum.ActivityNature.valueOf(nature));
                    dto.setSummaryRequired(rs.getBoolean("is_summary_required"));
                    dto.setMaxParticipants((Integer) rs.getObject("max_participants"));
                    dto.setAttendanceSessionCount(readAttendanceSessionCount(rs));
                    dto.setAddress(rs.getString("address"));
                    dto.setDescription(rs.getString("description"));
                    dto.setActivityStatus(Enum.ActivityStatus.valueOf(rs.getString("activity_status")));
                    dto.setApprovalStatus(Enum.ApprovalStatus.valueOf(rs.getString("approval_status")));

                    Timestamp startTime = rs.getTimestamp("start_time");
                    dto.setStartTime(startTime == null
                            ? null : DateTimeUtil.fromUtc(startTime.toLocalDateTime()));
                    Timestamp endTime = rs.getTimestamp("end_time");
                    dto.setEndTime(endTime == null
                            ? null : DateTimeUtil.fromUtc(endTime.toLocalDateTime()));
                    dto.setRegistrationFormRequired(rs.getBoolean("registration_form_required"));
                    dto.setPublic(rs.getBoolean("is_public"));

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
            LOGGER.log(Level.SEVERE, "Failed to query extracurricular activity list.", e);
            throw new IllegalStateException("Failed to find all activity details.", e);
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
    public int countReservedApplications(int activityId) {
        String sql = "SELECT COUNT(*) FROM dbo.activity_registration r "
                + "JOIN dbo.activity_registration_form f ON f.id=r.form_id "
                + "WHERE f.activity_id=? AND r.status IN ('SUBMITTED','APPROVED')";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count reserved activity registrations.", e);
        }
    }

    @Override
    public int countActiveApplications(int activityId) {
        String sql = "SELECT COUNT(*) FROM dbo.activity_registration r "
                + "JOIN dbo.activity_registration_form f ON f.id=r.form_id "
                + "WHERE f.activity_id=? AND r.status<>'WITHDRAWN'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count reserved activity registrations.", e);
        }
    }

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
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            if (isDuplicateCodeError(e)) {
                throw new IllegalArgumentException("Extracurricular activity with the same code already exists.", e);
            }
            throw new IllegalStateException("Failed to insert extracurricular activity.", e);
        }
    }

    @Override
    public boolean update(ExtracurricularActivity activity) {
        return update(activity, null);
    }

    @Override
    public boolean update(ExtracurricularActivity activity, Enum.ApprovalStatus newApprovalStatus) {
        if (activity == null || activity.getId() <= 0) {
            return false;
        }
        ensureAttendanceSessionSchema();

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
            ps.setString(8, activity.getNature() == null ? null : activity.getNature().name());
            ps.setBoolean(9, activity.isSummaryRequired());
            ps.setString(10, activity.getAddress());
            ps.setString(11, activity.getDescription());
            ps.setInt(12, activity.getActivityTypeId());
            ps.setInt(13, activity.getOrganizationLevelId());
            ps.setTimestamp(14, activity.getStartTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getStartTime())));
            ps.setTimestamp(15, activity.getEndTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getEndTime())));
            ps.setBoolean(16, activity.isRegistrationFormRequired());
            setMaxParticipants(ps, 17, activity);
            ps.setBoolean(18, activity.isPublic());
            ps.setInt(19, activity.getAttendanceSessionCount());
            int next = 20;
            if (newApprovalStatus != null) {
                ps.setString(next++, newApprovalStatus.name());
            }
            ps.setInt(next, activity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isDuplicateCodeError(e)) {
                throw new IllegalArgumentException("Mã hoạt động đã tồn tại. Vui lòng nhập mã khác.", e);
            }
            throw new IllegalStateException("Failed to update extracurricular activity: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateUpcoming(
            ExtracurricularActivity activity,
            Enum.ApprovalStatus expectedApprovalStatus,
            Enum.ApprovalStatus newApprovalStatus,
            LocalDateTime now) {
        if (activity == null || activity.getId() <= 0
                || expectedApprovalStatus == null || now == null) {
            return false;
        }
        ensureAttendanceSessionSchema();

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
            ps.setString(8, activity.getNature() == null ? null : activity.getNature().name());
            ps.setBoolean(9, activity.isSummaryRequired());
            ps.setString(10, activity.getAddress());
            ps.setString(11, activity.getDescription());
            ps.setInt(12, activity.getActivityTypeId());
            ps.setInt(13, activity.getOrganizationLevelId());
            ps.setTimestamp(14, activity.getStartTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getStartTime())));
            ps.setTimestamp(15, activity.getEndTime() == null
                    ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getEndTime())));
            ps.setBoolean(16, activity.isRegistrationFormRequired());
            setMaxParticipants(ps, 17, activity);
            ps.setBoolean(18, activity.isPublic());
            ps.setInt(19, activity.getAttendanceSessionCount());

            int next = 20;
            if (newApprovalStatus != null) {
                ps.setString(next++, newApprovalStatus.name());
            }
            ps.setInt(next++, activity.getId());
            ps.setTimestamp(next++, Timestamp.valueOf(now));
            ps.setString(next++, expectedApprovalStatus.name());
            setMaxParticipants(ps, next++, activity);
            setMaxParticipants(ps, next++, activity);
            setMaxParticipants(ps, next++, activity);
            setMaxParticipants(ps, next, activity);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isDuplicateCodeError(e)) {
                throw new IllegalArgumentException("Mã hoạt động đã tồn tại. Vui lòng nhập mã khác.", e);
            }
            throw new IllegalStateException("Failed to update upcoming extracurricular activity.", e);
        }
    }

    private void setMaxParticipants(PreparedStatement ps, int index, ExtracurricularActivity activity) throws SQLException {
        if (activity.getMaxParticipants() == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, activity.getMaxParticipants());
        }
    }

    private boolean isDuplicateCodeError(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            if (current.getErrorCode() == 2601 || current.getErrorCode() == 2627) {
                return true;
            }
        }
        return false;
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
        ps.setString(8, activity.getNature() == null ? null : activity.getNature().name());
        ps.setBoolean(9, activity.isSummaryRequired());
        ps.setString(10, activity.getAddress());
        ps.setString(11, activity.getDescription());
        ps.setString(12, activity.getActivityStatus() == null ? null : activity.getActivityStatus().name());
        ps.setString(13, activity.getApprovalStatus() == null ? null : activity.getApprovalStatus().name());
        ps.setInt(14, activity.getActivityTypeId());
        ps.setInt(15, activity.getOrganizationLevelId());
        ps.setTimestamp(16, activity.getStartTime() == null
                ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getStartTime())));
        ps.setTimestamp(17, activity.getEndTime() == null
                ? null : Timestamp.valueOf(DateTimeUtil.toUtc(activity.getEndTime())));
        ps.setBoolean(18, activity.isRegistrationFormRequired());
        setMaxParticipants(ps, 19, activity);
        ps.setBoolean(20, activity.isPublic());
        ps.setInt(21, activity.getAttendanceSessionCount());
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null) {
            return false;
        }

        String existsSql = "SELECT 1 FROM extracurricular_activity WHERE id = ?";
        // Remove the partner-company link before its invitation because it references that invitation.
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

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(existsSql)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                for (String sql : dependentDeletes) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps =
                             conn.prepareStatement("DELETE FROM extracurricular_activity WHERE id = ?")) {
                    ps.setInt(1, id);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete extracurricular activity.", e);
        }
    }

    @Override
    public boolean updateApprovalStatusIfUpcoming(
            int id,
            Enum.ApprovalStatus status,
            List<Enum.ApprovalStatus> allowedFrom,
            LocalDateTime now) {

        if (id <= 0 || status == null
                || allowedFrom == null || allowedFrom.isEmpty()
                || now == null) {
            return false;
        }

        StringBuilder sql = new StringBuilder(
                "UPDATE extracurricular_activity "
                        + "SET approval_status = ? "
                        + "WHERE id = ? "
                        + "AND activity_status = 'UPCOMING' "
                        + "AND start_time IS NOT NULL "
                        + "AND start_time > ? "
                        + "AND approval_status IN (");

        for (int i = 0; i < allowedFrom.size(); i++) {
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
            for (Enum.ApprovalStatus from : allowedFrom) {
                ps.setString(index++, from.name());
            }

            // Chỉ thành công nếu câu SQL cập nhật được ít nhất một dòng.
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to update approval status: " + e.getMessage(), e);
        }
    }

    /**
     * Đồng bộ trạng thái hoạt động bằng thời gian UTC.
     *
     * Xử lý cả trường hợp Tomcat tắt trong thời gian diễn ra hoạt động.
     */
    @Override
    public void synchronizeActivityStatuses(
            java.time.LocalDateTime now) {

        if (now == null) {
            throw new IllegalArgumentException(
                    "The synchronization time must not be null.");
        }

        // Transition newly rejected activities and their linked partner records atomically.
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

        String cancelInactiveInvitationsSql =
                "UPDATE activity_collaboration_invitation " +
                        "SET approval_status = 'CANCELLED' " +
                        "WHERE approval_status = 'PENDING' " +
                        "AND activity_id IN (" +
                        "SELECT id FROM extracurricular_activity " +
                        "WHERE activity_status IN ('ONGOING', 'COMPLETED', 'CANCELLED'))";

        try (Connection conn = DBContext.getConnection()) {

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
                    e.addSuppressed(rollbackException);
                }

                throw e;

            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                    // Connection sắp được đóng.
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to synchronize extracurricular activity statuses.",
                    e);
        }
    }

    @Override
    public boolean deleteIfNotOngoingOrCompleted(int id) {
        if (id <= 0) {
            return false;
        }

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

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String sql : dependentDeletes) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(deleteActivitySql)) {
                    ps.setInt(1, id);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to delete activity that has not started or was cancelled.", e);
        }
    }

    @Override
    public boolean refreshPublic(int activityId) {
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
            throw new IllegalStateException("Không thể cập nhật trạng thái công khai của hoạt động.", e);
        }
    }

}
