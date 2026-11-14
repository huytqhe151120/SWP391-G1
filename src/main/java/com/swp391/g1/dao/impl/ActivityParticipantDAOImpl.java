package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityParticipantDAO;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.AttendanceHistoryDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
import com.swp391.g1.dto.response.ParticipantSessionAttendanceDTO;
import com.swp391.g1.model.Enum.ParticipantStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityParticipantDAOImpl implements IActivityParticipantDAO {

    public ActivityParticipantDAOImpl() {
        RecruitmentSchema.ensure();
    }

    private static final String SYNC_ACTIVITY_RESULTS_SQL = """
            INSERT INTO dbo.activity_participant_session_attendance (activity_participant_id, session_number, attendance_status)
            SELECT ap.id, 1, ap.attendance_status
            FROM dbo.activity_participant ap
            WHERE ap.activity_id = ?
              AND ap.attendance_status <> 'NOT_YET'
              AND NOT EXISTS (
                  SELECT 1 FROM dbo.activity_participant_session_attendance s
                  WHERE s.activity_participant_id = ap.id
              );

            DELETE apsa
            FROM dbo.activity_participant_session_attendance apsa
            JOIN dbo.activity_participant ap ON ap.id = apsa.activity_participant_id
            JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
            WHERE ap.activity_id = ?
              AND apsa.session_number > COALESCE(NULLIF(ea.attendance_session_count, 0), 1);

            UPDATE ap
            SET ap.attendance_status = CASE
                WHEN EXISTS (
                    SELECT 1 FROM dbo.activity_participant_session_attendance s
                    WHERE s.activity_participant_id = ap.id
                      AND s.session_number <= COALESCE(NULLIF(ea.attendance_session_count, 0), 1)
                      AND s.attendance_status = 'ATTENDED'
                ) THEN 'ATTENDED'
                WHEN EXISTS (
                    SELECT 1 FROM dbo.activity_participant_session_attendance s
                    WHERE s.activity_participant_id = ap.id
                      AND s.session_number <= COALESCE(NULLIF(ea.attendance_session_count, 0), 1)
                      AND s.attendance_status = 'IN_LATE'
                ) THEN 'IN_LATE'
                WHEN EXISTS (
                    SELECT 1 FROM dbo.activity_participant_session_attendance s
                    WHERE s.activity_participant_id = ap.id
                      AND s.session_number <= COALESCE(NULLIF(ea.attendance_session_count, 0), 1)
                      AND s.attendance_status = 'OUT_EARLY'
                ) THEN 'OUT_EARLY'
                WHEN EXISTS (
                    SELECT 1 FROM dbo.activity_participant_session_attendance s
                    WHERE s.activity_participant_id = ap.id
                      AND s.session_number <= COALESCE(NULLIF(ea.attendance_session_count, 0), 1)
                      AND s.attendance_status = 'ABSENT'
                ) THEN 'ABSENT'
                ELSE 'NOT_YET'
            END
            FROM dbo.activity_participant ap
            JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
            WHERE ap.activity_id = ?;

            DELETE sar
            FROM dbo.student_activity_result sar
            WHERE sar.activity_id = ?
              AND NOT EXISTS (
                  SELECT 1 FROM dbo.activity_participant ap
                  WHERE ap.activity_id = sar.activity_id AND ap.student_id = sar.student_id
              );

            MERGE dbo.student_activity_result AS target
            USING (
                SELECT
                    s_base.student_id,
                    s_base.activity_id,
                    s_base.semester_id,
                    s_base.participation_role,
                    s_base.calc_attendance_score,
                    s_base.calc_eval_score,
                    CASE
                        WHEN s_base.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                        THEN CAST(s_base.calc_attendance_score + s_base.calc_eval_score AS DECIMAL(5,2))
                        WHEN s_base.attendance_status = 'ABSENT' THEN s_base.calc_attendance_score
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END AS calc_participation_score,
                    s_base.calc_summary_score,
                    s_base.calc_org_coeff,
                    s_base.calc_reg_coeff,
                    s_base.calc_organizer_coeff,
                    s_base.is_summary_required,
                    s_base.attendance_status
                FROM (
                    SELECT
                        ap.student_id,
                        ap.activity_id,
                        ea.semester_id,
                        ap.participation_role,
                        CAST(ROUND(
                            COALESCE((
                                SELECT SUM(CASE s.attendance_status
                                    WHEN 'ATTENDED'  THEN CAST(5.00 AS DECIMAL(10,4))
                                    WHEN 'IN_LATE'   THEN CAST(3.00 AS DECIMAL(10,4))
                                    WHEN 'OUT_EARLY' THEN CAST(3.00 AS DECIMAL(10,4))
                                    WHEN 'ABSENT'    THEN CAST(-5.00 AS DECIMAL(10,4))
                                    ELSE CAST(0.00 AS DECIMAL(10,4))
                                END)
                                FROM dbo.activity_participant_session_attendance s
                                WHERE s.activity_participant_id = ap.id
                                  AND s.session_number <= COALESCE(NULLIF(ea.attendance_session_count, 0), 1)
                            ), CAST(0.00 AS DECIMAL(10,4)))
                            / CAST(COALESCE(NULLIF(ea.attendance_session_count, 0), 1) AS DECIMAL(10,4)),
                            2
                        ) AS DECIMAL(5,2)) AS calc_attendance_score,
                        CASE
                            WHEN ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                            THEN COALESCE(ap.participation_eval_score, CAST(0.00 AS DECIMAL(5,2)))
                            ELSE CAST(0.00 AS DECIMAL(5,2))
                        END AS calc_eval_score,
                        ap.summary_score AS calc_summary_score,
                        COALESCE(ol.coefficient, CAST(1.00 AS DECIMAL(4,2))) AS calc_org_coeff,
                        CASE WHEN ap.registration_status = 'REGISTERED'
                            THEN CAST(1.10 AS DECIMAL(4,2))
                            ELSE CAST(1.00 AS DECIMAL(4,2))
                        END AS calc_reg_coeff,
                        CASE WHEN ap.participation_role = 'ORGANIZER'
                            THEN CAST(1.50 AS DECIMAL(4,2))
                            ELSE CAST(1.00 AS DECIMAL(4,2))
                        END AS calc_organizer_coeff,
                        ea.is_summary_required,
                        ap.attendance_status
                    FROM dbo.activity_participant ap
                    JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                    LEFT JOIN dbo.activity_organization_level ol ON ol.id = ea.organization_level_id
                    WHERE ap.activity_id = ?
                ) s_base
            ) AS src
            ON (target.student_id = src.student_id AND target.activity_id = src.activity_id)
            WHEN MATCHED THEN
                UPDATE SET
                    semester_id = src.semester_id,
                    participation_role = src.participation_role,
                    attendance_score = src.calc_attendance_score,
                    participation_eval_score = src.calc_eval_score,
                    participation_score = src.calc_participation_score,
                    summary_score = src.calc_summary_score,
                    average_score = CASE
                        WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                            CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                                THEN ROUND((src.calc_participation_score + src.calc_summary_score) / 2.0, 2)
                                ELSE src.calc_participation_score
                            END
                        WHEN src.attendance_status = 'ABSENT' THEN src.calc_attendance_score
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END,
                    org_level_coefficient = src.calc_org_coeff,
                    early_reg_coefficient = src.calc_reg_coeff,
                    organizer_coefficient = src.calc_organizer_coeff,
                    final_point = CASE
                        WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                            ROUND(
                                (CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                                    THEN (src.calc_participation_score + src.calc_summary_score) / 2.0
                                    ELSE src.calc_participation_score
                                END
                                ) * src.calc_org_coeff * src.calc_reg_coeff * src.calc_organizer_coeff,
                                2
                            )
                        WHEN src.attendance_status = 'ABSENT' THEN CAST(src.calc_attendance_score AS DECIMAL(6,2))
                        ELSE CAST(0.00 AS DECIMAL(6,2))
                    END,
                    updated_at = SYSUTCDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (
                    student_id, activity_id, semester_id, participation_role, attendance_score, participation_eval_score,
                    participation_score, summary_score, average_score,
                    org_level_coefficient, early_reg_coefficient, organizer_coefficient, final_point, updated_at
                )
                VALUES (
                    src.student_id,
                    src.activity_id,
                    src.semester_id,
                    src.participation_role,
                    src.calc_attendance_score,
                    src.calc_eval_score,
                    src.calc_participation_score,
                    src.calc_summary_score,
                    CASE
                        WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                            CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                                THEN ROUND((src.calc_participation_score + src.calc_summary_score) / 2.0, 2)
                                ELSE src.calc_participation_score
                            END
                        WHEN src.attendance_status = 'ABSENT' THEN src.calc_attendance_score
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END,
                    src.calc_org_coeff,
                    src.calc_reg_coeff,
                    src.calc_organizer_coeff,
                    CASE
                        WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                            ROUND(
                                (CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                                    THEN (src.calc_participation_score + src.calc_summary_score) / 2.0
                                    ELSE src.calc_participation_score
                                END
                                ) * src.calc_org_coeff * src.calc_reg_coeff * src.calc_organizer_coeff,
                                2
                            )
                        WHEN src.attendance_status = 'ABSENT' THEN CAST(src.calc_attendance_score AS DECIMAL(6,2))
                        ELSE CAST(0.00 AS DECIMAL(6,2))
                    END,
                    SYSUTCDATETIME()
                );
            """;

    @Override
    public List<ActivityParticipantResponseDTO> findByActivityId(int activityId) {
        return findByActivityId(activityId, 1);
    }

    @Override
    public List<ActivityParticipantResponseDTO> findByActivityId(int activityId, int sessionNumber) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        RecruitmentSchema.ensure();
        int safeSessionNumber = Math.max(1, sessionNumber);
        List<ActivityParticipantResponseDTO> list = new ArrayList<>();
        String sql = """
                SELECT ap.id, ap.student_id, ap.attendance_status, ap.registration_status, ap.registration_by, ap.created_at,
                       ap.participation_role,
                       ap.participation_eval_score, ap.participation_feedback, ap.participation_graded_by, ap.participation_graded_at,
                       ap.summary_score,
                       COALESCE(NULLIF(ea.attendance_session_count, 0), 1) AS total_sessions,
                       COALESCE(curr_s.attendance_status,
                                CASE WHEN ? = 1 AND NOT EXISTS (
                                    SELECT 1 FROM dbo.activity_participant_session_attendance s0
                                    WHERE s0.activity_participant_id = ap.id
                                ) THEN ap.attendance_status ELSE 'NOT_YET' END) AS session_attendance_status,
                       COALESCE(sess_stats.attended_sessions, 0) AS attended_session_count,
                       COALESCE(sess_stats.absent_sessions, 0) AS absent_session_count,
                       COALESCE(sess_stats.marked_sessions, 0) AS marked_session_count,
                       sess_stats.calc_avg_attendance_score,
                       sar.attendance_score, sar.participation_score, sar.average_score,
                       sar.org_level_coefficient, sar.early_reg_coefficient, sar.organizer_coefficient, sar.final_point,
                       COALESCE(ol.coefficient, CAST(1.00 AS DECIMAL(4,2))) AS default_org_coeff,
                       s.code AS student_code, s.name AS student_name, mc.code AS class_code,
                       p.name AS program_name, m.name AS major_name, d.name AS department_name,
                       CASE WHEN ap.participation_role <> 'ORGANIZER' AND ap.attendance_status = 'NOT_YET'
                             AND COALESCE(sess_stats.marked_sessions, 0) = 0
                             AND NOT EXISTS (
                                 SELECT 1 FROM activity_attendance_history h
                                 WHERE h.activity_participant_id = ap.id)
                            THEN 1 ELSE 0 END AS removable
                FROM activity_participant ap
                JOIN student s ON s.id = ap.student_id
                JOIN extracurricular_activity ea ON ea.id = ap.activity_id
                LEFT JOIN activity_organization_level ol ON ol.id = ea.organization_level_id
                LEFT JOIN dbo.activity_participant_session_attendance curr_s
                       ON curr_s.activity_participant_id = ap.id AND curr_s.session_number = ?
                OUTER APPLY (
                    SELECT
                        SUM(CASE WHEN s_all.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN 1 ELSE 0 END) AS attended_sessions,
                        SUM(CASE WHEN s_all.attendance_status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_sessions,
                        SUM(CASE WHEN s_all.attendance_status <> 'NOT_YET' THEN 1 ELSE 0 END) AS marked_sessions,
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
                LEFT JOIN student_activity_result sar ON sar.student_id = ap.student_id AND sar.activity_id = ap.activity_id
                LEFT JOIN main_class mc ON mc.id = s.main_class_id
                LEFT JOIN academic_program p ON p.id = mc.responsible_academic_program_id
                LEFT JOIN major m ON m.id = p.responsible_major_id
                LEFT JOIN department d ON d.id = m.responsible_department_id
                WHERE ap.activity_id = ?
                ORDER BY s.name ASC, s.code ASC
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, safeSessionNumber);
            ps.setInt(2, safeSessionNumber);
            ps.setInt(3, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityParticipantResponseDTO dto = new ActivityParticipantResponseDTO();
                    dto.setParticipantId(rs.getInt("id"));
                    dto.setStudentId(rs.getInt("student_id"));
                    dto.setStudentCode(rs.getString("student_code"));
                    dto.setStudentName(rs.getString("student_name"));
                    dto.setMainClassCode(rs.getString("class_code"));
                    dto.setAcademicProgramName(rs.getString("program_name"));
                    dto.setMajorName(rs.getString("major_name"));
                    dto.setDepartmentName(rs.getString("department_name"));
                    String status = rs.getString("attendance_status");
                    ParticipantStatus pst = status == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(status);
                    dto.setAttendanceStatus(pst);

                    int totalSessions = Math.max(1, rs.getInt("total_sessions"));
                    int clampedSession = Math.min(safeSessionNumber, totalSessions);
                    dto.setTotalSessions(totalSessions);
                    dto.setSessionNumber(clampedSession);

                    String sessStatusStr = rs.getString("session_attendance_status");
                    ParticipantStatus sessPst = sessStatusStr == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(sessStatusStr);
                    dto.setSessionAttendanceStatus(sessPst);
                    dto.setSessionAttendanceScore(computeAttendanceScore(sessPst));

                    int attendedSessions = rs.getInt("attended_session_count");
                    int absentSessions = rs.getInt("absent_session_count");
                    int markedSessions = rs.getInt("marked_session_count");
                    if (markedSessions == 0 && pst != ParticipantStatus.NOT_YET) {
                        markedSessions = 1;
                        if (pst == ParticipantStatus.ABSENT) {
                            absentSessions = 1;
                        } else {
                            attendedSessions = 1;
                        }
                    }
                    dto.setAttendedSessionCount(attendedSessions);
                    dto.setAbsentSessionCount(absentSessions);
                    dto.setMarkedSessionCount(markedSessions);

                    dto.setRegistrationStatus(rs.getString("registration_status"));
                    dto.setParticipationRole(rs.getString("participation_role"));
                    long regBy = rs.getLong("registration_by");
                    dto.setRegistrationBy(rs.wasNull() ? null : regBy);
                    Timestamp ts = rs.getTimestamp("created_at");
                    dto.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    dto.setRemovable(rs.getInt("removable") == 1);

                    BigDecimal evalScore = rs.getBigDecimal("participation_eval_score");
                    dto.setParticipationEvalScore(evalScore);
                    dto.setParticipationFeedback(rs.getString("participation_feedback"));
                    dto.setParticipationGradedBy(rs.getString("participation_graded_by"));
                    Timestamp gradedAt = rs.getTimestamp("participation_graded_at");
                    dto.setParticipationGradedAt(gradedAt == null ? null : gradedAt.toLocalDateTime());
                    dto.setSummaryScore(rs.getBigDecimal("summary_score"));

                    BigDecimal attScore = rs.getBigDecimal("calc_avg_attendance_score");
                    if (attScore == null || (markedSessions == 0 && pst != ParticipantStatus.NOT_YET)) {
                        attScore = computeAttendanceScore(pst)
                                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
                    }
                    dto.setAttendanceScore(attScore);

                    BigDecimal partScore;
                    if (dto.isPresentStatus()) {
                        partScore = attScore.add(evalScore == null ? BigDecimal.ZERO : evalScore);
                    } else if (pst == ParticipantStatus.ABSENT) {
                        partScore = attScore;
                    } else {
                        partScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }
                    dto.setParticipationScore(partScore);

                    BigDecimal orgCoeff = rs.getBigDecimal("org_level_coefficient");
                    if (orgCoeff == null) {
                        orgCoeff = rs.getBigDecimal("default_org_coeff");
                    }
                    dto.setOrgLevelCoefficient(orgCoeff == null ? BigDecimal.ONE : orgCoeff);

                    BigDecimal regCoeff = rs.getBigDecimal("early_reg_coefficient");
                    if (regCoeff == null) {
                        regCoeff = "REGISTERED".equals(dto.getRegistrationStatus())
                                ? new BigDecimal("1.10")
                                : BigDecimal.ONE;
                    }
                    dto.setEarlyRegCoefficient(regCoeff);

                    BigDecimal organizerCoeff = rs.getBigDecimal("organizer_coefficient");
                    if (organizerCoeff == null) {
                        organizerCoeff = dto.isOrganizer() ? new BigDecimal("1.50") : BigDecimal.ONE;
                    }

                    BigDecimal avgScore = rs.getBigDecimal("average_score");
                    dto.setAverageScore(avgScore == null ? partScore : avgScore);

                    BigDecimal finalPt;
                    if (dto.isPresentStatus()) {
                        finalPt = dto.getAverageScore()
                                .multiply(dto.getOrgLevelCoefficient())
                                .multiply(dto.getEarlyRegCoefficient())
                                .multiply(organizerCoeff)
                                .setScale(2, RoundingMode.HALF_UP);
                    } else if (pst == ParticipantStatus.ABSENT) {
                        finalPt = attScore;
                    } else {
                        finalPt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }
                    dto.setFinalPoint(finalPt);

                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find participants for activity " + activityId, e);
        }
        return list;
    }

    private static BigDecimal computeAttendanceScore(ParticipantStatus status) {
        if (status == null) {
            return BigDecimal.ZERO;
        }
        return switch (status) {
            case ATTENDED -> new BigDecimal("5.00");
            case IN_LATE, OUT_EARLY -> new BigDecimal("3.00");
            case ABSENT -> new BigDecimal("-5.00");
            default -> BigDecimal.ZERO;
        };
    }

    @Override
    public List<ParticipantCandidateDTO> findCandidates(int activityId) {
        List<ParticipantCandidateDTO> list = new ArrayList<>();
        String sql = """
                SELECT s.id, s.code, s.name, mc.id AS class_id, mc.code AS class_code,
                       p.id AS program_id, p.name AS program_name, m.id AS major_id, m.name AS major_name,
                       d.id AS department_id, d.name AS department_name
                FROM student s
                LEFT JOIN main_class mc ON mc.id = s.main_class_id
                LEFT JOIN academic_program p ON p.id = mc.responsible_academic_program_id
                LEFT JOIN major m ON m.id = p.responsible_major_id
                LEFT JOIN department d ON d.id = m.responsible_department_id
                WHERE s.status = 'ACTIVE'
                  AND NOT EXISTS (SELECT 1 FROM activity_participant ap
                                  WHERE ap.activity_id = ? AND ap.student_id = s.id)
                ORDER BY s.code ASC
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ParticipantCandidateDTO dto = new ParticipantCandidateDTO();
                    dto.setStudentId(rs.getInt("id"));
                    dto.setStudentCode(rs.getString("code"));
                    dto.setStudentName(rs.getString("name"));
                    dto.setMainClassId(rs.getInt("class_id"));
                    dto.setMainClassCode(rs.getString("class_code"));
                    dto.setAcademicProgramId(rs.getInt("program_id"));
                    dto.setAcademicProgramName(rs.getString("program_name"));
                    dto.setMajorId(rs.getInt("major_id"));
                    dto.setMajorName(rs.getString("major_name"));
                    dto.setDepartmentId(rs.getInt("department_id"));
                    dto.setDepartmentName(rs.getString("department_name"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find participant candidates for activity " + activityId, e);
        }
        return list;
    }

    @Override
    public int countByActivityId(int activityId) {
        try (Connection conn = DBContext.getConnection()) {
            return count(conn, activityId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count participants for activity " + activityId, e);
        }
    }

    @Override
    public int countOccupiedByActivityId(int activityId) {
        RecruitmentSchema.ensure();
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT ap.student_id
                    FROM dbo.activity_participant ap
                    WHERE ap.activity_id = ? AND ap.participation_role = 'PARTICIPANT'
                    UNION
                    SELECT r.student_id
                    FROM dbo.activity_registration_form f
                    JOIN dbo.activity_registration r ON r.form_id = f.id
                    WHERE f.activity_id = ? AND r.status IN ('SUBMITTED', 'APPROVED')
                ) occupied
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count occupied seats for activity " + activityId, e);
        }
    }

    @Override
    public int insertParticipants(int activityId, List<Integer> studentIds, Integer maxParticipants) {
        return insertParticipants(activityId, studentIds, maxParticipants, 1);
    }

    @Override
    public int insertParticipants(int activityId, List<Integer> studentIds, Integer maxParticipants, Integer registrationBy) {
        String lockSql = "SELECT id FROM extracurricular_activity WITH (UPDLOCK, ROWLOCK) WHERE id = ?";
        String insertSql = """
                INSERT INTO activity_participant (activity_id, student_id, registration_status, registration_by)
                SELECT ?, s.id, 'STAFF_ENROLLED', ? FROM student s
                WHERE s.id = ? AND s.status = 'ACTIVE'
                  AND NOT EXISTS (SELECT 1 FROM activity_participant ap
                                  WHERE ap.activity_id = ? AND ap.student_id = s.id)
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement lock = conn.prepareStatement(lockSql);
                 PreparedStatement insert = conn.prepareStatement(insertSql)) {
                lock.setInt(1, activityId);
                lock.executeQuery().close();
                int before = count(conn, activityId);
                long byId = (registrationBy != null && registrationBy > 0) ? registrationBy.longValue() : 1L;
                for (Integer studentId : studentIds) {
                    insert.setInt(1, activityId);
                    insert.setLong(2, byId);
                    insert.setInt(3, studentId);
                    insert.setInt(4, activityId);
                    insert.addBatch();
                }
                insert.executeBatch();
                int after = count(conn, activityId);
                if (maxParticipants != null && countOccupied(conn, activityId) > maxParticipants) {
                    conn.rollback();
                    return -1;
                }
                syncActivityResults(conn, activityId);
                conn.commit();
                return after - before;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add participants to activity " + activityId, e);
        }
    }

    @Override
    public int deleteRemovable(int activityId, List<Integer> participantIds) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        String deleteSql = """
                DELETE FROM activity_participant
                WHERE id = ? AND activity_id = ? AND participation_role <> 'ORGANIZER' AND attendance_status = 'NOT_YET'
                  AND NOT EXISTS (SELECT 1 FROM dbo.activity_participant_session_attendance s
                                  WHERE s.activity_participant_id = activity_participant.id
                                    AND s.attendance_status <> 'NOT_YET')
                  AND NOT EXISTS (SELECT 1 FROM activity_attendance_history h
                                  WHERE h.activity_participant_id = activity_participant.id)
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
                for (Integer participantId : participantIds) {
                    delete.setInt(1, participantId);
                    delete.setInt(2, activityId);
                    delete.addBatch();
                }
                int deleted = 0;
                for (int rows : delete.executeBatch()) {
                    deleted += Math.max(rows, 0);
                }
                syncActivityResults(conn, activityId);
                conn.commit();
                return deleted;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove participants from activity " + activityId, e);
        }
    }

    private int count(Connection conn, int activityId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM activity_participant WHERE activity_id = ?")) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countOccupied(Connection conn, int activityId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT ap.student_id
                    FROM dbo.activity_participant ap
                    WHERE ap.activity_id = ? AND ap.participation_role = 'PARTICIPANT'
                    UNION
                    SELECT r.student_id
                    FROM dbo.activity_registration_form f
                    JOIN dbo.activity_registration r ON r.form_id = f.id
                    WHERE f.activity_id = ? AND r.status IN ('SUBMITTED', 'APPROVED')
                ) occupied
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int updateStatuses(int activityId, Map<Integer, ParticipantStatus> statusByParticipantId,
            String changedBy) {
        return updateStatuses(activityId, 1, statusByParticipantId, changedBy);
    }

    @Override
    public int updateStatuses(int activityId, int sessionNumber, Map<Integer, ParticipantStatus> statusByParticipantId,
            String changedBy) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        int safeSessionNumber = Math.max(1, sessionNumber);
        String selectOldSql = """
                SELECT ap.id,
                       COALESCE(s.attendance_status,
                                CASE WHEN ? = 1 AND NOT EXISTS (
                                    SELECT 1 FROM dbo.activity_participant_session_attendance s0
                                    WHERE s0.activity_participant_id = ap.id
                                ) THEN ap.attendance_status ELSE 'NOT_YET' END) AS current_status
                FROM dbo.activity_participant ap
                LEFT JOIN dbo.activity_participant_session_attendance s
                       ON s.activity_participant_id = ap.id AND s.session_number = ?
                WHERE ap.id = ? AND ap.activity_id = ?
                """;
        String upsertSessionSql = """
                MERGE dbo.activity_participant_session_attendance AS target
                USING (SELECT ? AS activity_participant_id, ? AS session_number) AS src
                ON (target.activity_participant_id = src.activity_participant_id
                    AND target.session_number = src.session_number)
                WHEN MATCHED THEN
                    UPDATE SET attendance_status = ?, updated_by = ?, updated_at = SYSUTCDATETIME()
                WHEN NOT MATCHED THEN
                    INSERT (activity_participant_id, session_number, attendance_status, updated_by, updated_at)
                    VALUES (src.activity_participant_id, src.session_number, ?, ?, SYSUTCDATETIME());
                """;
        String historySql = """
                INSERT INTO activity_attendance_history
                    (activity_participant_id, session_number, old_status, new_status, created_by)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement selectOld = conn.prepareStatement(selectOldSql);
                 PreparedStatement upsertSession = conn.prepareStatement(upsertSessionSql);
                 PreparedStatement history = conn.prepareStatement(historySql)) {
                int changed = 0;
                for (Map.Entry<Integer, ParticipantStatus> entry : statusByParticipantId.entrySet()) {
                    int participantId = entry.getKey();
                    String newStatus = entry.getValue().name();

                    selectOld.setInt(1, safeSessionNumber);
                    selectOld.setInt(2, safeSessionNumber);
                    selectOld.setInt(3, participantId);
                    selectOld.setInt(4, activityId);

                    String oldStatus;
                    try (ResultSet rs = selectOld.executeQuery()) {
                        if (!rs.next()) {
                            continue;
                        }
                        oldStatus = rs.getString("current_status");
                    }
                    if (oldStatus == null) {
                        oldStatus = ParticipantStatus.NOT_YET.name();
                    }
                    if (oldStatus.equals(newStatus)) {
                        continue;
                    }

                    int actorId = parseActorId(changedBy);

                    upsertSession.setInt(1, participantId);
                    upsertSession.setInt(2, safeSessionNumber);
                    upsertSession.setString(3, newStatus);
                    upsertSession.setInt(4, actorId);
                    upsertSession.setString(5, newStatus);
                    upsertSession.setInt(6, actorId);
                    upsertSession.executeUpdate();

                    history.setInt(1, participantId);
                    history.setInt(2, safeSessionNumber);
                    history.setString(3, oldStatus);
                    history.setString(4, newStatus);
                    history.setInt(5, actorId);
                    history.executeUpdate();
                    changed++;
                }
                syncActivityResults(conn, activityId);
                conn.commit();
                return changed;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update attendance for activity " + activityId, e);
        }
    }

    @Override
    public int updateParticipationGrades(int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, String gradedBy, Integer gradedById) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        String selectOldSql = """
                SELECT participation_eval_score
                FROM activity_participant
                WHERE id = ? AND activity_id = ?
                  AND attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                """;
        String updateSql = """
                UPDATE activity_participant
                SET participation_eval_score = ?,
                    participation_feedback = ?,
                    participation_graded_by = ?,
                    participation_graded_by_id = ?,
                    participation_graded_at = SYSUTCDATETIME()
                WHERE id = ? AND activity_id = ?
                  AND attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                """;
        String historySql = """
                INSERT INTO activity_grade_history
                    (activity_participant_id, grade_type, old_score, new_score, feedback, created_by, created_by_id)
                VALUES (?, 'PARTICIPATION_EVAL', ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement selectOld = conn.prepareStatement(selectOldSql);
                 PreparedStatement update = conn.prepareStatement(updateSql);
                 PreparedStatement history = conn.prepareStatement(historySql)) {
                int updatedCount = 0;
                BigDecimal normalizedScore = evalScore == null ? null : evalScore.setScale(2, RoundingMode.HALF_UP);
                String trimmedFeedback = (feedback == null || feedback.trim().isEmpty()) ? null : feedback.trim();
                for (Integer pid : participantIds) {
                    if (pid == null || pid <= 0) {
                        continue;
                    }
                    selectOld.setInt(1, pid);
                    selectOld.setInt(2, activityId);
                    BigDecimal oldScore;
                    try (ResultSet rs = selectOld.executeQuery()) {
                        if (!rs.next()) {
                            continue;
                        }
                        oldScore = rs.getBigDecimal(1);
                    }

                    if (normalizedScore == null) {
                        update.setNull(1, Types.DECIMAL);
                    } else {
                        update.setBigDecimal(1, normalizedScore);
                    }
                    if (trimmedFeedback == null) {
                        update.setNull(2, Types.NVARCHAR);
                    } else {
                        update.setString(2, trimmedFeedback);
                    }
                    update.setString(3, gradedBy);
                    if (gradedById == null) {
                        update.setNull(4, Types.INTEGER);
                    } else {
                        update.setInt(4, gradedById);
                    }
                    update.setInt(5, pid);
                    update.setInt(6, activityId);

                    if (update.executeUpdate() <= 0) {
                        continue;
                    }

                    history.setInt(1, pid);
                    if (oldScore == null) {
                        history.setNull(2, Types.DECIMAL);
                    } else {
                        history.setBigDecimal(2, oldScore);
                    }
                    if (normalizedScore == null) {
                        history.setNull(3, Types.DECIMAL);
                    } else {
                        history.setBigDecimal(3, normalizedScore);
                    }
                    if (trimmedFeedback == null) {
                        history.setNull(4, Types.NVARCHAR);
                    } else {
                        history.setString(4, trimmedFeedback);
                    }
                    history.setString(5, gradedBy);
                    if (gradedById == null) {
                        history.setNull(6, Types.INTEGER);
                    } else {
                        history.setInt(6, gradedById);
                    }
                    history.executeUpdate();
                    updatedCount++;
                }
                if (updatedCount > 0) {
                    syncActivityResults(conn, activityId);
                }
                conn.commit();
                return updatedCount;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update participation grades for activity " + activityId, e);
        }
    }

    private static int parseActorId(String changedBy) {
        if (changedBy != null && !changedBy.isBlank()) {
            try {
                int parsed = Integer.parseInt(changedBy.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 767;
    }

    @Override
    public List<ParticipantSessionAttendanceDTO> findSessionAttendances(int activityId, int participantId, int totalSessions) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        int safeTotalSessions = Math.max(1, totalSessions);
        Map<Integer, ParticipantSessionAttendanceDTO> bySession = new HashMap<>();

        String sql = """
                SELECT s.session_number,
                       s.attendance_status,
                       COALESCE(TRY_CAST(s.updated_by AS INT), TRY_CAST(hist.created_by AS INT)) AS actor_id,
                       COALESCE(hist.created_at, s.updated_at) AS effective_updated_at,
                       actor_info.actor_code,
                       actor_info.actor_name,
                       actor_info.actor_unit
                FROM dbo.activity_participant_session_attendance s
                JOIN dbo.activity_participant ap ON ap.id = s.activity_participant_id
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                OUTER APPLY (
                    SELECT TOP 1 h.created_by, h.created_at
                    FROM dbo.activity_attendance_history h
                    WHERE h.activity_participant_id = s.activity_participant_id
                      AND COALESCE(h.session_number, 1) = s.session_number
                    ORDER BY h.created_at DESC, h.id DESC
                ) hist
                OUTER APPLY (
                    SELECT TOP 1
                        COALESCE(st.code, ps.code, st_by_id.code) AS actor_code,
                        COALESCE(st.name, ps.name, st_by_id.name, acc.username) AS actor_name,
                        COALESCE(dept.name, pc.name, dept_by_id.name) AS actor_unit
                    FROM (SELECT COALESCE(TRY_CAST(s.updated_by AS INT), TRY_CAST(hist.created_by AS INT)) AS resolved_id) x
                    LEFT JOIN dbo.account acc ON acc.id = x.resolved_id
                    LEFT JOIN dbo.staff st ON st.account_id = acc.id
                    OUTER APPLY (
                        SELECT TOP 1 d.name
                        FROM dbo.staff_assignment sa
                        JOIN dbo.department d ON d.id = sa.department_id
                        WHERE sa.staff_id = st.id
                        ORDER BY
                            CASE
                                WHEN sa.department_id = ea.responsible_department_id THEN 0
                                WHEN sa.department_id IN (SELECT ad.department_id FROM dbo.activity_department ad WHERE ad.activity_id = ea.id) THEN 1
                                ELSE 2
                            END,
                            sa.id ASC
                    ) dept
                    LEFT JOIN dbo.partner_staff ps ON ps.account_id = acc.id
                    LEFT JOIN dbo.partner_company pc ON pc.id = ps.company_id
                    LEFT JOIN dbo.staff st_by_id ON st.id IS NULL AND ps.id IS NULL AND st_by_id.id = x.resolved_id
                    OUTER APPLY (
                        SELECT TOP 1 d.name
                        FROM dbo.staff_assignment sa
                        JOIN dbo.department d ON d.id = sa.department_id
                        WHERE sa.staff_id = st_by_id.id
                        ORDER BY
                            CASE
                                WHEN sa.department_id = ea.responsible_department_id THEN 0
                                WHEN sa.department_id IN (SELECT ad.department_id FROM dbo.activity_department ad WHERE ad.activity_id = ea.id) THEN 1
                                ELSE 2
                            END,
                            sa.id ASC
                    ) dept_by_id
                    WHERE x.resolved_id IS NOT NULL
                ) actor_info
                WHERE ap.activity_id = ? AND s.activity_participant_id = ?
                  AND s.session_number <= ?
                ORDER BY s.session_number ASC
                """;
        String fallbackSql = """
                SELECT ap.attendance_status,
                       TRY_CAST(h.created_by AS INT) AS actor_id,
                       h.created_at,
                       actor_info.actor_code,
                       actor_info.actor_name,
                       actor_info.actor_unit
                FROM dbo.activity_participant ap
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                OUTER APPLY (
                    SELECT TOP 1 created_by, created_at
                    FROM dbo.activity_attendance_history
                    WHERE activity_participant_id = ap.id
                    ORDER BY created_at DESC, id DESC
                ) h
                OUTER APPLY (
                    SELECT TOP 1
                        COALESCE(st.code, ps.code, st_by_id.code) AS actor_code,
                        COALESCE(st.name, ps.name, st_by_id.name, acc.username) AS actor_name,
                        COALESCE(dept.name, pc.name, dept_by_id.name) AS actor_unit
                    FROM (SELECT TRY_CAST(h.created_by AS INT) AS resolved_id) x
                    LEFT JOIN dbo.account acc ON acc.id = x.resolved_id
                    LEFT JOIN dbo.staff st ON st.account_id = acc.id
                    OUTER APPLY (
                        SELECT TOP 1 d.name
                        FROM dbo.staff_assignment sa
                        JOIN dbo.department d ON d.id = sa.department_id
                        WHERE sa.staff_id = st.id
                        ORDER BY
                            CASE
                                WHEN sa.department_id = ea.responsible_department_id THEN 0
                                WHEN sa.department_id IN (SELECT ad.department_id FROM dbo.activity_department ad WHERE ad.activity_id = ea.id) THEN 1
                                ELSE 2
                            END,
                            sa.id ASC
                    ) dept
                    LEFT JOIN dbo.partner_staff ps ON ps.account_id = acc.id
                    LEFT JOIN dbo.partner_company pc ON pc.id = ps.company_id
                    LEFT JOIN dbo.staff st_by_id ON st.id IS NULL AND ps.id IS NULL AND st_by_id.id = x.resolved_id
                    OUTER APPLY (
                        SELECT TOP 1 d.name
                        FROM dbo.staff_assignment sa
                        JOIN dbo.department d ON d.id = sa.department_id
                        WHERE sa.staff_id = st_by_id.id
                        ORDER BY
                            CASE
                                WHEN sa.department_id = ea.responsible_department_id THEN 0
                                WHEN sa.department_id IN (SELECT ad.department_id FROM dbo.activity_department ad WHERE ad.activity_id = ea.id) THEN 1
                                ELSE 2
                            END,
                            sa.id ASC
                    ) dept_by_id
                    WHERE x.resolved_id IS NOT NULL
                ) actor_info
                WHERE ap.activity_id = ? AND ap.id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, participantId);
            ps.setInt(3, safeTotalSessions);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int sessNo = rs.getInt("session_number");
                    String stStr = rs.getString("attendance_status");
                    ParticipantStatus pst = stStr == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(stStr);
                    ParticipantSessionAttendanceDTO dto = new ParticipantSessionAttendanceDTO();
                    dto.setSessionNumber(sessNo);
                    dto.setAttendanceStatus(pst);
                    dto.setSessionScore(computeAttendanceScore(pst));
                    int actorId = rs.getInt("actor_id");
                    dto.setUpdatedById(rs.wasNull() ? null : actorId);
                    dto.setUpdatedByCode(rs.getString("actor_code"));
                    dto.setUpdatedByName(rs.getString("actor_name"));
                    dto.setUpdatedByUnit(rs.getString("actor_unit"));
                    dto.setUpdatedBy(rs.getString("actor_name"));
                    Timestamp ts = rs.getTimestamp("effective_updated_at");
                    dto.setUpdatedAt(ts == null ? null : ts.toLocalDateTime());
                    bySession.put(sessNo, dto);
                }
            }
            if (bySession.isEmpty()) {
                try (PreparedStatement fb = conn.prepareStatement(fallbackSql)) {
                    fb.setInt(1, activityId);
                    fb.setInt(2, participantId);
                    try (ResultSet rs = fb.executeQuery()) {
                        if (rs.next()) {
                            String stStr = rs.getString("attendance_status");
                            ParticipantStatus pst = stStr == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(stStr);
                            if (pst != ParticipantStatus.NOT_YET) {
                                ParticipantSessionAttendanceDTO dto = new ParticipantSessionAttendanceDTO();
                                dto.setSessionNumber(1);
                                dto.setAttendanceStatus(pst);
                                dto.setSessionScore(computeAttendanceScore(pst));
                                int actorId = rs.getInt("actor_id");
                                dto.setUpdatedById(rs.wasNull() ? null : actorId);
                                dto.setUpdatedByCode(rs.getString("actor_code"));
                                dto.setUpdatedByName(rs.getString("actor_name"));
                                dto.setUpdatedByUnit(rs.getString("actor_unit"));
                                dto.setUpdatedBy(rs.getString("actor_name"));
                                Timestamp ts = rs.getTimestamp("created_at");
                                dto.setUpdatedAt(ts == null ? null : ts.toLocalDateTime());
                                bySession.put(1, dto);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load session attendances for participant " + participantId, e);
        }

        List<ParticipantSessionAttendanceDTO> result = new ArrayList<>(safeTotalSessions);
        for (int i = 1; i <= safeTotalSessions; i++) {
            ParticipantSessionAttendanceDTO existing = bySession.get(i);
            if (existing != null) {
                result.add(existing);
            } else {
                ParticipantSessionAttendanceDTO emptyDto = new ParticipantSessionAttendanceDTO();
                emptyDto.setSessionNumber(i);
                emptyDto.setAttendanceStatus(ParticipantStatus.NOT_YET);
                emptyDto.setSessionScore(BigDecimal.ZERO);
                result.add(emptyDto);
            }
        }
        return result;
    }

    @Override
    public List<AttendanceHistoryDTO> findAttendanceHistory(int activityId, int participantId) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        List<AttendanceHistoryDTO> list = new ArrayList<>();
        String sql = """
                SELECT h.id,
                       h.activity_participant_id,
                       COALESCE(h.session_number, 1) AS session_number,
                       h.old_status,
                       h.new_status,
                       TRY_CAST(h.created_by AS INT) AS actor_id,
                       h.created_by,
                       h.created_at,
                       actor_info.actor_code,
                       actor_info.actor_name,
                       actor_info.actor_unit
                FROM dbo.activity_attendance_history h
                JOIN dbo.activity_participant ap ON ap.id = h.activity_participant_id
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                OUTER APPLY (
                    SELECT TOP 1
                        COALESCE(st.code, ps.code, st_by_id.code) AS actor_code,
                        COALESCE(st.name, ps.name, st_by_id.name, acc.username) AS actor_name,
                        COALESCE(dept.name, pc.name, dept_by_id.name) AS actor_unit
                    FROM (SELECT TRY_CAST(h.created_by AS INT) AS resolved_id) x
                    LEFT JOIN dbo.account acc ON acc.id = x.resolved_id
                    LEFT JOIN dbo.staff st ON st.account_id = acc.id
                    OUTER APPLY (
                        SELECT TOP 1 d.name
                        FROM dbo.staff_assignment sa
                        JOIN dbo.department d ON d.id = sa.department_id
                        WHERE sa.staff_id = st.id
                        ORDER BY
                            CASE
                                WHEN sa.department_id = ea.responsible_department_id THEN 0
                                WHEN sa.department_id IN (SELECT ad.department_id FROM dbo.activity_department ad WHERE ad.activity_id = ea.id) THEN 1
                                ELSE 2
                            END,
                            sa.id ASC
                    ) dept
                    LEFT JOIN dbo.partner_staff ps ON ps.account_id = acc.id
                    LEFT JOIN dbo.partner_company pc ON pc.id = ps.company_id
                    LEFT JOIN dbo.staff st_by_id ON st.id IS NULL AND ps.id IS NULL AND st_by_id.id = x.resolved_id
                    OUTER APPLY (
                        SELECT TOP 1 d.name
                        FROM dbo.staff_assignment sa
                        JOIN dbo.department d ON d.id = sa.department_id
                        WHERE sa.staff_id = st_by_id.id
                        ORDER BY
                            CASE
                                WHEN sa.department_id = ea.responsible_department_id THEN 0
                                WHEN sa.department_id IN (SELECT ad.department_id FROM dbo.activity_department ad WHERE ad.activity_id = ea.id) THEN 1
                                ELSE 2
                            END,
                            sa.id ASC
                    ) dept_by_id
                    WHERE x.resolved_id IS NOT NULL
                ) actor_info
                WHERE ap.activity_id = ? AND h.activity_participant_id = ?
                ORDER BY h.created_at DESC, h.id DESC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, participantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceHistoryDTO dto = new AttendanceHistoryDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setParticipantId(rs.getInt("activity_participant_id"));
                    dto.setSessionNumber(rs.getInt("session_number"));
                    String oldSt = rs.getString("old_status");
                    String newSt = rs.getString("new_status");
                    dto.setOldStatus(oldSt == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(oldSt));
                    dto.setNewStatus(newSt == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(newSt));
                    int actorId = rs.getInt("actor_id");
                    dto.setCreatedById(rs.wasNull() ? null : actorId);
                    dto.setCreatedByCode(rs.getString("actor_code"));
                    dto.setCreatedByName(rs.getString("actor_name"));
                    dto.setCreatedByUnit(rs.getString("actor_unit"));
                    String fallbackCreatedBy = rs.getString("actor_name");
                    dto.setCreatedBy(fallbackCreatedBy != null ? fallbackCreatedBy : rs.getString("created_by"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    dto.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load attendance history for participant " + participantId, e);
        }
        return list;
    }

    @Override
    public void syncActivityResults(int activityId) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        try (Connection conn = DBContext.getConnection()) {
            syncActivityResults(conn, activityId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to sync activity results for activity " + activityId, e);
        }
    }

    private void syncActivityResults(Connection conn, int activityId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SYNC_ACTIVITY_RESULTS_SQL)) {
            ps.setInt(1, activityId);
            ps.setInt(2, activityId);
            ps.setInt(3, activityId);
            ps.setInt(4, activityId);
            ps.setInt(5, activityId);
            ps.executeUpdate();
        }
    }
}
