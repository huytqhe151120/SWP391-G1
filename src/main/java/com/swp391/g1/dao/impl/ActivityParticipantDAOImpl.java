package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityParticipantDAO;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
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
import java.util.List;
import java.util.Map;

public class ActivityParticipantDAOImpl implements IActivityParticipantDAO {

    private static final String SYNC_ACTIVITY_RESULTS_SQL = """
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
                    ap.student_id,
                    ap.activity_id,
                    CASE ap.attendance_status
                        WHEN 'ATTENDED' THEN CAST(5.00 AS DECIMAL(5,2))
                        WHEN 'IN_LATE'  THEN CAST(3.00 AS DECIMAL(5,2))
                        WHEN 'OUT_EARLY' THEN CAST(3.00 AS DECIMAL(5,2))
                        WHEN 'ABSENT'   THEN CAST(-5.00 AS DECIMAL(5,2))
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END AS calc_attendance_score,
                    CASE
                        WHEN ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                        THEN COALESCE(ap.participation_eval_score, CAST(0.00 AS DECIMAL(5,2)))
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END AS calc_eval_score,
                    CASE
                        WHEN ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                        THEN (
                            CASE ap.attendance_status
                                WHEN 'ATTENDED' THEN CAST(5.00 AS DECIMAL(5,2))
                                ELSE CAST(3.00 AS DECIMAL(5,2))
                            END
                            + COALESCE(ap.participation_eval_score, CAST(0.00 AS DECIMAL(5,2)))
                        )
                        WHEN ap.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(5,2))
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END AS calc_participation_score,
                    ap.summary_score AS calc_summary_score,
                    COALESCE(ol.coefficient, CAST(1.00 AS DECIMAL(4,2))) AS calc_org_coeff,
                    CASE WHEN ap.registration_status = 'REGISTERED'
                        THEN CAST(1.10 AS DECIMAL(4,2))
                        ELSE CAST(1.00 AS DECIMAL(4,2))
                    END AS calc_reg_coeff,
                    ea.is_summary_required,
                    ap.attendance_status
                FROM dbo.activity_participant ap
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                LEFT JOIN dbo.activity_organization_level ol ON ol.id = ea.organization_level_id
                WHERE ap.activity_id = ?
            ) AS src
            ON (target.student_id = src.student_id AND target.activity_id = src.activity_id)
            WHEN MATCHED THEN
                UPDATE SET
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
                        WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(5,2))
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END,
                    org_level_coefficient = src.calc_org_coeff,
                    early_reg_coefficient = src.calc_reg_coeff,
                    final_point = CASE
                        WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                            ROUND(
                                (CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                                    THEN (src.calc_participation_score + src.calc_summary_score) / 2.0
                                    ELSE src.calc_participation_score
                                END) * src.calc_org_coeff * src.calc_reg_coeff,
                                2
                            )
                        WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(6,2))
                        ELSE CAST(0.00 AS DECIMAL(6,2))
                    END,
                    updated_at = SYSUTCDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (
                    student_id, activity_id, attendance_score, participation_eval_score,
                    participation_score, summary_score, average_score,
                    org_level_coefficient, early_reg_coefficient, final_point, updated_at
                )
                VALUES (
                    src.student_id,
                    src.activity_id,
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
                        WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(5,2))
                        ELSE CAST(0.00 AS DECIMAL(5,2))
                    END,
                    src.calc_org_coeff,
                    src.calc_reg_coeff,
                    CASE
                        WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                            ROUND(
                                (CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                                    THEN (src.calc_participation_score + src.calc_summary_score) / 2.0
                                    ELSE src.calc_participation_score
                                END) * src.calc_org_coeff * src.calc_reg_coeff,
                                2
                            )
                        WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(6,2))
                        ELSE CAST(0.00 AS DECIMAL(6,2))
                    END,
                    SYSUTCDATETIME()
                );
            """;

    @Override
    public List<ActivityParticipantResponseDTO> findByActivityId(int activityId) {
        List<ActivityParticipantResponseDTO> list = new ArrayList<>();
        String sql = """
                SELECT ap.id, ap.student_id, ap.attendance_status, ap.registration_status, ap.registration_by, ap.created_at,
                       ap.participation_eval_score, ap.participation_feedback, ap.participation_graded_by, ap.participation_graded_at,
                       ap.summary_score,
                       sar.attendance_score, sar.participation_score, sar.average_score,
                       sar.org_level_coefficient, sar.early_reg_coefficient, sar.final_point,
                       COALESCE(ol.coefficient, CAST(1.00 AS DECIMAL(4,2))) AS default_org_coeff,
                       s.code AS student_code, s.name AS student_name, mc.code AS class_code,
                       p.name AS program_name, m.name AS major_name, d.name AS department_name,
                       CASE WHEN ap.attendance_status = 'NOT_YET' AND NOT EXISTS (
                                SELECT 1 FROM activity_attendance_history h
                                WHERE h.activity_participant_id = ap.id)
                            THEN 1 ELSE 0 END AS removable
                FROM activity_participant ap
                JOIN student s ON s.id = ap.student_id
                JOIN extracurricular_activity ea ON ea.id = ap.activity_id
                LEFT JOIN activity_organization_level ol ON ol.id = ea.organization_level_id
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
            ps.setInt(1, activityId);
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
                    dto.setRegistrationStatus(rs.getString("registration_status"));
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

                    BigDecimal attScore = rs.getBigDecimal("attendance_score");
                    if (attScore == null) {
                        attScore = computeAttendanceScore(pst);
                    }
                    dto.setAttendanceScore(attScore);

                    BigDecimal partScore = rs.getBigDecimal("participation_score");
                    if (partScore == null) {
                        if (dto.isPresentStatus()) {
                            partScore = attScore.add(evalScore == null ? BigDecimal.ZERO : evalScore);
                        } else {
                            partScore = attScore;
                        }
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

                    BigDecimal avgScore = rs.getBigDecimal("average_score");
                    dto.setAverageScore(avgScore == null ? partScore : avgScore);

                    BigDecimal finalPt = rs.getBigDecimal("final_point");
                    if (finalPt == null) {
                        if (dto.isPresentStatus()) {
                            finalPt = dto.getAverageScore()
                                    .multiply(dto.getOrgLevelCoefficient())
                                    .multiply(dto.getEarlyRegCoefficient())
                                    .setScale(2, RoundingMode.HALF_UP);
                        } else {
                            finalPt = attScore;
                        }
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
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT ap.student_id
                    FROM dbo.activity_participant ap
                    WHERE ap.activity_id = ?
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
        String deleteSql = """
                DELETE FROM activity_participant
                WHERE id = ? AND activity_id = ? AND attendance_status = 'NOT_YET'
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
                    WHERE ap.activity_id = ?
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
        String updateSql = """
                UPDATE activity_participant
                SET attendance_status = ?
                OUTPUT deleted.attendance_status
                WHERE id = ? AND activity_id = ? AND attendance_status <> ?
                """;
        String historySql = """
                INSERT INTO activity_attendance_history
                    (activity_participant_id, old_status, new_status, created_by)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement update = conn.prepareStatement(updateSql);
                 PreparedStatement history = conn.prepareStatement(historySql)) {
                int changed = 0;
                for (Map.Entry<Integer, ParticipantStatus> entry : statusByParticipantId.entrySet()) {
                    String newStatus = entry.getValue().name();
                    update.setString(1, newStatus);
                    update.setInt(2, entry.getKey());
                    update.setInt(3, activityId);
                    update.setString(4, newStatus);
                    try (ResultSet rs = update.executeQuery()) {
                        if (!rs.next()) {
                            continue;
                        }
                        history.setInt(1, entry.getKey());
                        history.setString(2, rs.getString(1));
                        history.setString(3, newStatus);
                        history.setString(4, changedBy);
                    }
                    history.executeUpdate();
                    changed++;
                }
                if (changed > 0) {
                    syncActivityResults(conn, activityId);
                }
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
        String updateSql = """
                UPDATE activity_participant
                SET participation_eval_score = ?,
                    participation_feedback = ?,
                    participation_graded_by = ?,
                    participation_graded_by_id = ?,
                    participation_graded_at = SYSUTCDATETIME()
                OUTPUT deleted.participation_eval_score
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
            try (PreparedStatement update = conn.prepareStatement(updateSql);
                 PreparedStatement history = conn.prepareStatement(historySql)) {
                int updatedCount = 0;
                BigDecimal normalizedScore = evalScore == null ? null : evalScore.setScale(2, RoundingMode.HALF_UP);
                String trimmedFeedback = (feedback == null || feedback.trim().isEmpty()) ? null : feedback.trim();
                for (Integer pid : participantIds) {
                    if (pid == null || pid <= 0) {
                        continue;
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

                    BigDecimal oldScore;
                    try (ResultSet rs = update.executeQuery()) {
                        if (!rs.next()) {
                            continue;
                        }
                        oldScore = rs.getBigDecimal(1);
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

    private void syncActivityResults(Connection conn, int activityId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SYNC_ACTIVITY_RESULTS_SQL)) {
            ps.setInt(1, activityId);
            ps.setInt(2, activityId);
            ps.executeUpdate();
        }
    }
}
