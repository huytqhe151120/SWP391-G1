package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IStudentGradeDAO;
import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ParticipantStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class StudentGradeDAOImpl implements IStudentGradeDAO {

    private static final String SYNC_SEMESTER_RESULTS_SQL = """
            MERGE dbo.student_semester_result AS target
            USING (
                SELECT
                    ap.student_id,
                    ea.semester_id,
                    COUNT(ap.id) AS registered_activity_count,
                    COALESCE(SUM(CASE WHEN ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN 1 ELSE 0 END), 0) AS attended_activity_count,
                    COALESCE(SUM(COALESCE(sar.attendance_score, CAST(0.00 AS DECIMAL(10,2)))), CAST(0.00 AS DECIMAL(10,2))) AS attendance_score,
                    COALESCE(SUM(COALESCE(sar.participation_eval_score, CAST(0.00 AS DECIMAL(10,2)))), CAST(0.00 AS DECIMAL(10,2))) AS participation_eval_score,
                    COALESCE(SUM(COALESCE(sar.participation_score, CAST(0.00 AS DECIMAL(10,2)))), CAST(0.00 AS DECIMAL(10,2))) AS participation_score,
                    COALESCE(SUM(COALESCE(sar.summary_score, CAST(0.00 AS DECIMAL(10,2)))), CAST(0.00 AS DECIMAL(10,2))) AS summary_score,
                    COALESCE(SUM(COALESCE(sar.average_score, CAST(0.00 AS DECIMAL(10,2)))), CAST(0.00 AS DECIMAL(10,2))) AS average_score,
                    COALESCE(SUM(CASE WHEN sar.final_point > 0 THEN sar.final_point ELSE CAST(0.00 AS DECIMAL(10,2)) END), CAST(0.00 AS DECIMAL(10,2))) AS plus_points,
                    COALESCE(SUM(CASE WHEN sar.final_point < 0 THEN ABS(sar.final_point) ELSE CAST(0.00 AS DECIMAL(10,2)) END), CAST(0.00 AS DECIMAL(10,2))) AS minus_points,
                    COALESCE(SUM(COALESCE(sar.final_point, CAST(0.00 AS DECIMAL(10,2)))), CAST(0.00 AS DECIMAL(10,2))) AS final_point
                FROM dbo.activity_participant ap
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                LEFT JOIN dbo.student_activity_result sar ON sar.student_id = ap.student_id AND sar.activity_id = ap.activity_id
                WHERE ea.semester_id IS NOT NULL
                GROUP BY ap.student_id, ea.semester_id
            ) AS src
            ON (target.student_id = src.student_id AND target.semester_id = src.semester_id)
            WHEN MATCHED THEN
                UPDATE SET
                    registered_activity_count = src.registered_activity_count,
                    attended_activity_count   = src.attended_activity_count,
                    attendance_score          = src.attendance_score,
                    participation_eval_score  = src.participation_eval_score,
                    participation_score       = src.participation_score,
                    summary_score             = src.summary_score,
                    average_score             = src.average_score,
                    plus_points               = src.plus_points,
                    minus_points              = src.minus_points,
                    final_point               = src.final_point,
                    updated_at                = SYSUTCDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (
                    student_id, semester_id,
                    registered_activity_count, attended_activity_count,
                    attendance_score, participation_eval_score, participation_score,
                    summary_score, average_score, plus_points, minus_points, final_point, updated_at
                )
                VALUES (
                    src.student_id, src.semester_id,
                    src.registered_activity_count, src.attended_activity_count,
                    src.attendance_score, src.participation_eval_score, src.participation_score,
                    src.summary_score, src.average_score, src.plus_points, src.minus_points, src.final_point, SYSUTCDATETIME()
                );
            """;

    private static final String SUMMARY_BY_SEMESTER_SQL = """
            SELECT s.id AS student_id,
                   s.code AS student_code,
                   s.name AS student_name,
                   mc.code AS class_code,
                   p.name AS program_name,
                   m.name AS major_name,
                   d.name AS department_name,
                   sem.id AS semester_id,
                   sem.code AS semester_code,
                   sem.name AS semester_name,
                   COALESCE(ssr.registered_activity_count, 0) AS registered_count,
                   COALESCE(ssr.attended_activity_count, 0) AS attended_count,
                   COALESCE(ssr.attendance_score, CAST(0.00 AS DECIMAL(10,2))) AS attendance_score,
                   COALESCE(ssr.participation_eval_score, CAST(0.00 AS DECIMAL(10,2))) AS participation_eval_score,
                   COALESCE(ssr.participation_score, CAST(0.00 AS DECIMAL(10,2))) AS participation_score,
                   COALESCE(ssr.summary_score, CAST(0.00 AS DECIMAL(10,2))) AS summary_score,
                   COALESCE(ssr.average_score, CAST(0.00 AS DECIMAL(10,2))) AS average_score,
                   COALESCE(ssr.plus_points, CAST(0.00 AS DECIMAL(10,2))) AS plus_points,
                   COALESCE(ssr.minus_points, CAST(0.00 AS DECIMAL(10,2))) AS minus_points,
                   COALESCE(ssr.final_point, CAST(0.00 AS DECIMAL(10,2))) AS final_point
            FROM dbo.student s
            LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
            LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
            LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
            LEFT JOIN dbo.department d ON d.id = m.responsible_department_id
            LEFT JOIN dbo.semester sem ON sem.id = ?
            LEFT JOIN dbo.student_semester_result ssr ON ssr.student_id = s.id AND ssr.semester_id = ?
            """;

    private static final String SUMMARY_ALL_SEMESTERS_SQL = """
            SELECT s.id AS student_id,
                   s.code AS student_code,
                   s.name AS student_name,
                   mc.code AS class_code,
                   p.name AS program_name,
                   m.name AS major_name,
                   d.name AS department_name,
                   CAST(0 AS INT) AS semester_id,
                   CAST(NULL AS VARCHAR(50)) AS semester_code,
                   CAST(NULL AS NVARCHAR(200)) AS semester_name,
                   COALESCE(SUM(ssr.registered_activity_count), 0) AS registered_count,
                   COALESCE(SUM(ssr.attended_activity_count), 0) AS attended_count,
                   COALESCE(SUM(ssr.attendance_score), CAST(0.00 AS DECIMAL(10,2))) AS attendance_score,
                   COALESCE(SUM(ssr.participation_eval_score), CAST(0.00 AS DECIMAL(10,2))) AS participation_eval_score,
                   COALESCE(SUM(ssr.participation_score), CAST(0.00 AS DECIMAL(10,2))) AS participation_score,
                   COALESCE(SUM(ssr.summary_score), CAST(0.00 AS DECIMAL(10,2))) AS summary_score,
                   COALESCE(SUM(ssr.average_score), CAST(0.00 AS DECIMAL(10,2))) AS average_score,
                   COALESCE(SUM(ssr.plus_points), CAST(0.00 AS DECIMAL(10,2))) AS plus_points,
                   COALESCE(SUM(ssr.minus_points), CAST(0.00 AS DECIMAL(10,2))) AS minus_points,
                   COALESCE(SUM(ssr.final_point), CAST(0.00 AS DECIMAL(10,2))) AS final_point
            FROM dbo.student s
            LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
            LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
            LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
            LEFT JOIN dbo.department d ON d.id = m.responsible_department_id
            LEFT JOIN dbo.student_semester_result ssr ON ssr.student_id = s.id
            """;

    private static final String SUMMARY_GROUP_BY = """
            GROUP BY s.id, s.code, s.name, mc.code, p.name, m.name, d.name
            """;

    private static final String ACTIVITY_GRADES_SQL = """
            SELECT ap.id AS participant_id,
                   ap.student_id,
                   s.code AS student_code,
                   s.name AS student_name,
                   mc.code AS class_code,
                   p.name AS program_name,
                   m.name AS major_name,
                   d.name AS department_name,
                   ea.id AS activity_id,
                   ea.code AS activity_code,
                   ea.name AS activity_name,
                   sem.name AS semester_name,
                   ea.activity_status,
                   ea.start_time,
                   ea.end_time,
                   ea.is_summary_required,
                   ol.name AS org_level_name,
                   COALESCE(sar.org_level_coefficient, ol.coefficient, CAST(1.00 AS DECIMAL(4,2))) AS org_coeff,
                   ap.participation_role,
                   COALESCE(sar.organizer_coefficient,
                            CASE WHEN ap.participation_role = 'ORGANIZER' THEN CAST(1.50 AS DECIMAL(4,2))
                                 ELSE CAST(1.00 AS DECIMAL(4,2)) END) AS organizer_coeff,
                   ap.registration_status,
                   COALESCE(sar.early_reg_coefficient,
                            CASE WHEN ap.registration_status = 'REGISTERED' THEN CAST(1.10 AS DECIMAL(4,2)) ELSE CAST(1.00 AS DECIMAL(4,2)) END) AS reg_coeff,
                   ap.attendance_status,
                   COALESCE(NULLIF(ea.attendance_session_count, 0), 1) AS total_sessions,
                   COALESCE(sess_stats.attended_sessions, 0) AS attended_session_count,
                   COALESCE(sar.attendance_score, sess_stats.calc_avg_attendance_score) AS attendance_score,
                   att_hist.created_by AS att_marked_by,
                   att_hist.created_at AS att_marked_at,
                   ap.participation_eval_score,
                   ap.participation_feedback,
                   ap.participation_graded_by,
                   ap.participation_graded_at,
                   sar.participation_score,
                   ap.summary_score,
                   ap.summary_feedback,
                   ap.summary_graded_by,
                   ap.summary_graded_at,
                   sar.average_score,
                   sar.final_point
            FROM dbo.activity_participant ap
            JOIN dbo.student s ON s.id = ap.student_id
            LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
            LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
            LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
            LEFT JOIN dbo.department d ON d.id = m.responsible_department_id
            JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
            LEFT JOIN dbo.semester sem ON sem.id = ea.semester_id
            LEFT JOIN dbo.activity_organization_level ol ON ol.id = ea.organization_level_id
            OUTER APPLY (
                SELECT
                    SUM(CASE WHEN s_all.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN 1 ELSE 0 END) AS attended_sessions,
                    CAST(ROUND(
                        COALESCE(SUM(CASE s_all.attendance_status
                            WHEN 'ATTENDED'  THEN CAST(5.00 AS DECIMAL(10,4))
                            WHEN 'IN_LATE'   THEN CAST(3.00 AS DECIMAL(10,4))
                            WHEN 'OUT_EARLY' THEN CAST(3.00 AS DECIMAL(10,4))
                            WHEN 'ABSENT'    THEN CAST(-5.00 AS DECIMAL(10,4))
                            ELSE CAST(0.00 AS DECIMAL(10,4))
                        END), CAST(0.00 AS DECIMAL(10,4)))
                        / CAST(COALESCE(NULLIF(ea.attendance_session_count, 0), 1) AS DECIMAL(10,4)),
                        2
                    ) AS DECIMAL(5,2)) AS calc_avg_attendance_score
                FROM dbo.activity_participant_session_attendance s_all
                WHERE s_all.activity_participant_id = ap.id
                  AND s_all.session_number <= COALESCE(NULLIF(ea.attendance_session_count, 0), 1)
            ) sess_stats
            LEFT JOIN dbo.student_activity_result sar ON sar.student_id = ap.student_id AND sar.activity_id = ap.activity_id
            OUTER APPLY (
                SELECT TOP 1
                    COALESCE(st_att.name, ps_att.name, st_att_id.name, acc_att.username, CAST(h.created_by AS NVARCHAR(100))) AS created_by,
                    h.created_at
                FROM dbo.activity_attendance_history h
                LEFT JOIN dbo.account acc_att ON acc_att.id = TRY_CAST(h.created_by AS INT)
                LEFT JOIN dbo.staff st_att ON st_att.account_id = acc_att.id
                LEFT JOIN dbo.partner_staff ps_att ON ps_att.account_id = acc_att.id
                LEFT JOIN dbo.staff st_att_id ON st_att.id IS NULL AND ps_att.id IS NULL AND st_att_id.id = TRY_CAST(h.created_by AS INT)
                WHERE h.activity_participant_id = ap.id
                ORDER BY h.created_at DESC, h.id DESC
            ) att_hist
            """;

    private void syncSemesterResults(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SYNC_SEMESTER_RESULTS_SQL)) {
            ps.executeUpdate();
        }
    }

    @Override
    public List<StudentGradeSummaryDTO> findAllStudentGradeSummaries() {
        return findAllStudentGradeSummaries(0);
    }

    @Override
    public List<StudentGradeSummaryDTO> findAllStudentGradeSummaries(int semesterId) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        List<StudentGradeSummaryDTO> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection()) {
            syncSemesterResults(conn);
            if (semesterId > 0) {
                String sql = SUMMARY_BY_SEMESTER_SQL
                        + " WHERE s.status = 'ACTIVE' "
                        + " ORDER BY s.code ASC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, semesterId);
                    ps.setInt(2, semesterId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(mapSummary(rs));
                        }
                    }
                }
            } else {
                String sql = SUMMARY_ALL_SEMESTERS_SQL
                        + " WHERE s.status = 'ACTIVE' "
                        + SUMMARY_GROUP_BY
                        + " ORDER BY s.code ASC";
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapSummary(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách điểm hoạt động của sinh viên.", e);
        }
        return list;
    }

    @Override
    public StudentGradeSummaryDTO findStudentGradeSummaryById(int studentId) {
        return findStudentGradeSummaryById(studentId, 0);
    }

    @Override
    public StudentGradeSummaryDTO findStudentGradeSummaryById(int studentId, int semesterId) {
        if (studentId <= 0) {
            return null;
        }
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        try (Connection conn = DBContext.getConnection()) {
            syncSemesterResults(conn);
            if (semesterId > 0) {
                String sql = SUMMARY_BY_SEMESTER_SQL
                        + " WHERE s.id = ? ";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, semesterId);
                    ps.setInt(2, semesterId);
                    ps.setInt(3, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? mapSummary(rs) : null;
                    }
                }
            } else {
                String sql = SUMMARY_ALL_SEMESTERS_SQL
                        + " WHERE s.id = ? "
                        + SUMMARY_GROUP_BY;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? mapSummary(rs) : null;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải tổng hợp điểm của sinh viên.", e);
        }
    }

    @Override
    public List<StudentActivityGradeDTO> findActivityGradesByStudentId(int studentId) {
        return findActivityGradesByStudentId(studentId, 0);
    }

    @Override
    public List<StudentActivityGradeDTO> findActivityGradesByStudentId(int studentId, int semesterId) {
        List<StudentActivityGradeDTO> list = new ArrayList<>();
        if (studentId <= 0) {
            return list;
        }
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        RecruitmentSchema.ensure();
        String sql = ACTIVITY_GRADES_SQL
                + " WHERE ap.student_id = ? "
                + (semesterId > 0 ? " AND ea.semester_id = ? " : "")
                + " ORDER BY ea.start_time DESC, ea.id DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            if (semesterId > 0) {
                ps.setInt(2, semesterId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapActivityGrade(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải lịch sử điểm hoạt động của sinh viên.", e);
        }
        return list;
    }

    @Override
    public StudentActivityGradeDTO findActivityGradeDetail(int studentId, int activityId) {
        if (studentId <= 0 || activityId <= 0) {
            return null;
        }
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        RecruitmentSchema.ensure();
        String sql = ACTIVITY_GRADES_SQL
                + " WHERE ap.student_id = ? AND ap.activity_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapActivityGrade(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải chi tiết điểm của sinh viên trong hoạt động.", e);
        }
    }

    private StudentGradeSummaryDTO mapSummary(ResultSet rs) throws SQLException {
        StudentGradeSummaryDTO dto = new StudentGradeSummaryDTO();
        dto.setStudentId(rs.getInt("student_id"));
        dto.setStudentCode(rs.getString("student_code"));
        dto.setStudentName(rs.getString("student_name"));
        dto.setMainClassCode(rs.getString("class_code"));
        dto.setAcademicProgramName(rs.getString("program_name"));
        dto.setMajorName(rs.getString("major_name"));
        dto.setDepartmentName(rs.getString("department_name"));
        dto.setSemesterId(rs.getInt("semester_id"));
        dto.setSemesterCode(rs.getString("semester_code"));
        dto.setSemesterName(rs.getString("semester_name"));
        dto.setRegisteredActivityCount(rs.getInt("registered_count"));
        dto.setAttendedActivityCount(rs.getInt("attended_count"));

        dto.setTotalAttendanceScore(rs.getBigDecimal("attendance_score"));
        dto.setTotalParticipationEvalScore(rs.getBigDecimal("participation_eval_score"));
        dto.setTotalParticipationScore(rs.getBigDecimal("participation_score"));
        dto.setTotalSummaryScore(rs.getBigDecimal("summary_score"));
        dto.setTotalAverageScore(rs.getBigDecimal("average_score"));

        BigDecimal plus = rs.getBigDecimal("plus_points");
        BigDecimal minus = rs.getBigDecimal("minus_points");
        BigDecimal finalPt = rs.getBigDecimal("final_point");
        if (plus == null) plus = BigDecimal.ZERO;
        if (minus == null) minus = BigDecimal.ZERO;
        dto.setTotalPlusPoints(plus);
        dto.setTotalMinusPoints(minus);
        dto.setTotalAccumulatedPoints(finalPt != null ? finalPt : plus.subtract(minus));
        return dto;
    }

    private StudentActivityGradeDTO mapActivityGrade(ResultSet rs) throws SQLException {
        StudentActivityGradeDTO dto = new StudentActivityGradeDTO();
        dto.setParticipantId(rs.getInt("participant_id"));
        dto.setStudentId(rs.getInt("student_id"));
        dto.setStudentCode(rs.getString("student_code"));
        dto.setStudentName(rs.getString("student_name"));
        dto.setMainClassCode(rs.getString("class_code"));
        dto.setAcademicProgramName(rs.getString("program_name"));
        dto.setMajorName(rs.getString("major_name"));
        dto.setDepartmentName(rs.getString("department_name"));

        dto.setActivityId(rs.getInt("activity_id"));
        dto.setActivityCode(rs.getString("activity_code"));
        dto.setActivityName(rs.getString("activity_name"));
        dto.setSemesterName(rs.getString("semester_name"));
        String actSt = rs.getString("activity_status");
        if (actSt != null) {
            try {
                dto.setActivityStatus(ActivityStatus.valueOf(actSt));
            } catch (IllegalArgumentException ignored) {}
        }
        Timestamp stTime = rs.getTimestamp("start_time");
        dto.setStartTime(stTime == null ? null : stTime.toLocalDateTime());
        Timestamp endTime = rs.getTimestamp("end_time");
        dto.setEndTime(endTime == null ? null : endTime.toLocalDateTime());
        dto.setSummaryRequired(rs.getBoolean("is_summary_required"));
        dto.setOrganizationLevelName(rs.getString("org_level_name"));
        dto.setOrgLevelCoefficient(rs.getBigDecimal("org_coeff"));

        dto.setParticipationRole(rs.getString("participation_role"));
        dto.setOrganizerCoefficient(rs.getBigDecimal("organizer_coeff"));
        dto.setRegistrationStatus(rs.getString("registration_status"));
        dto.setEarlyRegCoefficient(rs.getBigDecimal("reg_coeff"));

        String attStr = rs.getString("attendance_status");
        ParticipantStatus pst = attStr == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(attStr);
        dto.setAttendanceStatus(pst);
        int totalSessions = Math.max(1, rs.getInt("total_sessions"));
        int attendedSessions = rs.getInt("attended_session_count");
        if (attendedSessions == 0 && dto.isPresent()) {
            attendedSessions = 1;
        }
        dto.setTotalSessions(totalSessions);
        dto.setAttendedSessionCount(attendedSessions);

        BigDecimal attScore = rs.getBigDecimal("attendance_score");
        if (attScore == null) {
            BigDecimal rawAtt = switch (pst) {
                case ATTENDED -> new BigDecimal("5.00");
                case IN_LATE, OUT_EARLY -> new BigDecimal("3.00");
                case ABSENT -> new BigDecimal("-5.00");
                default -> BigDecimal.ZERO;
            };
            attScore = rawAtt.divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        }
        dto.setAttendanceScore(attScore);
        dto.setAttendanceMarkedBy(rs.getString("att_marked_by"));
        Timestamp attAt = rs.getTimestamp("att_marked_at");
        dto.setAttendanceMarkedAt(attAt == null ? null : attAt.toLocalDateTime());

        BigDecimal evalScore = rs.getBigDecimal("participation_eval_score");
        dto.setParticipationEvalScore(evalScore);
        dto.setParticipationFeedback(rs.getString("participation_feedback"));
        dto.setParticipationGradedBy(rs.getString("participation_graded_by"));
        Timestamp evalAt = rs.getTimestamp("participation_graded_at");
        dto.setParticipationGradedAt(evalAt == null ? null : evalAt.toLocalDateTime());

        BigDecimal partScore = rs.getBigDecimal("participation_score");
        if (partScore == null) {
            if (dto.isPresent()) {
                partScore = attScore.add(evalScore == null ? BigDecimal.ZERO : evalScore);
            } else {
                partScore = attScore;
            }
        }
        dto.setParticipationScore(partScore);

        BigDecimal sumScore = rs.getBigDecimal("summary_score");
        dto.setSummaryScore(sumScore);
        dto.setSummaryFeedback(rs.getString("summary_feedback"));
        dto.setSummaryGradedBy(rs.getString("summary_graded_by"));
        Timestamp sumAt = rs.getTimestamp("summary_graded_at");
        dto.setSummaryGradedAt(sumAt == null ? null : sumAt.toLocalDateTime());

        BigDecimal avgScore = rs.getBigDecimal("average_score");
        if (avgScore == null) {
            if (dto.isPresent() && dto.isSummaryRequired() && sumScore != null) {
                avgScore = partScore.add(sumScore).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            } else {
                avgScore = partScore;
            }
        }
        dto.setAverageScore(avgScore);

        BigDecimal finalPt = rs.getBigDecimal("final_point");
        if (finalPt == null) {
            if (dto.isPresent()) {
                finalPt = avgScore
                        .multiply(dto.getOrgLevelCoefficient() == null ? BigDecimal.ONE : dto.getOrgLevelCoefficient())
                        .multiply(dto.getEarlyRegCoefficient() == null ? BigDecimal.ONE : dto.getEarlyRegCoefficient())
                        .multiply(dto.getOrganizerCoefficient())
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                finalPt = attScore;
            }
        }
        dto.setAccumulatedScore(finalPt);

        return dto;
    }
}
