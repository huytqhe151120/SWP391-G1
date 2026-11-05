package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityParticipantDAO;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
import com.swp391.g1.model.Enum.ParticipantStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActivityParticipantDAOImpl implements IActivityParticipantDAO {

    @Override
    public List<ActivityParticipantResponseDTO> findByActivityId(int activityId) {
        List<ActivityParticipantResponseDTO> list = new ArrayList<>();
        String sql = """
                SELECT ap.id, ap.student_id, ap.attendance_status, ap.created_at,
                       s.code AS student_code, s.name AS student_name, mc.code AS class_code,
                       p.name AS program_name, m.name AS major_name, d.name AS department_name,
                       CASE WHEN ap.attendance_status = 'NOT_YET' AND NOT EXISTS (
                                SELECT 1 FROM activity_attendance_history h
                                WHERE h.activity_participant_id = ap.id)
                            THEN 1 ELSE 0 END AS removable
                FROM activity_participant ap
                JOIN student s ON s.id = ap.student_id
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
                    dto.setAttendanceStatus(status == null ? ParticipantStatus.NOT_YET : ParticipantStatus.valueOf(status));
                    Timestamp ts = rs.getTimestamp("created_at");
                    dto.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    dto.setRemovable(rs.getInt("removable") == 1);
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find participants for activity " + activityId, e);
        }
        return list;
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
        // Locks the activity row so concurrent additions to the same activity cannot both pass the limit.
        String lockSql = "SELECT id FROM extracurricular_activity WITH (UPDLOCK, ROWLOCK) WHERE id = ?";
        String insertSql = """
                INSERT INTO activity_participant (activity_id, student_id)
                SELECT ?, s.id FROM student s
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
                for (Integer studentId : studentIds) {
                    insert.setInt(1, activityId);
                    insert.setInt(2, studentId);
                    insert.setInt(3, activityId);
                    insert.addBatch();
                }
                insert.executeBatch();
                int after = count(conn, activityId);
                if (maxParticipants != null && countOccupied(conn, activityId) > maxParticipants) {
                    conn.rollback();
                    return -1;
                }
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
}
