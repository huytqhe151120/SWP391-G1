package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.dao.IRecruitmentApplicationDAO;
import com.swp391.g1.model.RecruitmentAnswer;
import com.swp391.g1.model.RecruitmentApplication;
import com.swp391.g1.model.RecruitmentInterview;
import com.swp391.g1.model.RecruitmentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecruitmentApplicationDAOImpl implements IRecruitmentApplicationDAO {

    private static final String SELECT_APP = "SELECT oa.*, s.code AS student_code, s.name AS student_name, "
            + "mc.code AS class_code, r.title AS round_title, r.activity_id, a.name AS activity_name, "
            + "DATEADD(HOUR,7,a.start_time) AS activity_start, "
            + "p1.name AS choice1_name, p2.name AS choice2_name, pa.name AS accepted_name "
            + "FROM dbo.organizer_application oa "
            + "JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id "
            + "JOIN dbo.extracurricular_activity a ON a.id=r.activity_id "
            + "JOIN dbo.student s ON s.id=oa.student_id "
            + "LEFT JOIN dbo.main_class mc ON mc.id=s.main_class_id "
            + "JOIN dbo.organizer_recruitment_position p1 ON p1.id=oa.choice1_position_id "
            + "LEFT JOIN dbo.organizer_recruitment_position p2 ON p2.id=oa.choice2_position_id "
            + "LEFT JOIN dbo.organizer_recruitment_position pa ON pa.id=oa.accepted_position_id ";

    private static final String SELECT_INTERVIEW = "SELECT id,application_id,scheduled_at,format,location,note,"
            + "confirm_deadline,confirmed_at,reminder_sent_at,status,evaluation,created_by "
            + "FROM dbo.organizer_interview ";

    public RecruitmentApplicationDAOImpl() {
        RecruitmentSchema.ensure();
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
    }

    @Override
    public RecruitmentApplication findById(int id) {
        return findOne(SELECT_APP + "WHERE oa.id=?", id, null);
    }

    @Override
    public RecruitmentApplication findByRoundAndStudent(int roundId, int studentId) {
        return findOne(SELECT_APP + "WHERE oa.round_id=? AND oa.student_id=?", roundId, studentId);
    }

    private RecruitmentApplication findOne(String sql, int first, Integer second) {
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, first);
            if (second != null) {
                p.setInt(2, second);
            }
            RecruitmentApplication app;
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                app = mapApplication(rs);
            }
            app.setAnswers(loadAnswers(c, app.getId()));
            app.setInterview(loadInterview(c, app.getId()));
            return app;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải hồ sơ ứng tuyển.", e);
        }
    }

    @Override
    public List<RecruitmentApplication> findByRound(int roundId, String statusFilter, Integer positionId) {
        StringBuilder sql = new StringBuilder(SELECT_APP).append("WHERE oa.round_id=? ");
        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append("AND oa.status=? ");
        }
        if (positionId != null) {
            sql.append("AND (oa.choice1_position_id=? OR oa.choice2_position_id=? OR oa.accepted_position_id=?) ");
        }
        sql.append("ORDER BY oa.submitted_at, oa.id");
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql.toString())) {
            int i = 1;
            p.setInt(i++, roundId);
            if (statusFilter != null && !statusFilter.isBlank()) {
                p.setString(i++, statusFilter);
            }
            if (positionId != null) {
                p.setInt(i++, positionId);
                p.setInt(i++, positionId);
                p.setInt(i, positionId);
            }
            List<RecruitmentApplication> list = readList(p);
            for (RecruitmentApplication app : list) {
                app.setInterview(loadInterview(c, app.getId()));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải danh sách hồ sơ.", e);
        }
    }

    @Override
    public List<RecruitmentApplication> findByStudent(int studentId) {
        String sql = SELECT_APP + "WHERE oa.student_id=? ORDER BY oa.submitted_at DESC, oa.id DESC";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            List<RecruitmentApplication> list = readList(p);
            for (RecruitmentApplication app : list) {
                app.setInterview(loadInterview(c, app.getId()));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải hồ sơ của sinh viên.", e);
        }
    }

    @Override
    public boolean hasAcceptedApplication(int activityId, int studentId) {
        String sql = "SELECT 1 FROM dbo.organizer_application oa "
                + "JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id "
                + "WHERE r.activity_id=? AND oa.student_id=? AND oa.status='ACCEPTED'";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            p.setInt(2, studentId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể kiểm tra kết quả tuyển ban tổ chức.", e);
        }
    }

    @Override
    public boolean hasActiveParticipation(int activityId, int studentId) {
        String sql = "SELECT 1 FROM dbo.activity_registration r "
                + "JOIN dbo.activity_registration_form f ON f.id=r.form_id "
                + "WHERE f.activity_id=? AND r.student_id=? AND r.status IN ('SUBMITTED','APPROVED')";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            p.setInt(2, studentId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể kiểm tra đăng ký tham gia.", e);
        }
    }

    @Override
    public boolean deleteWithdrawn(int id) {
        return executeById("DELETE FROM dbo.organizer_application WHERE id=? AND status='WITHDRAWN'", id,
                "Không thể xóa hồ sơ đã rút.");
    }

    @Override
    public int insert(RecruitmentApplication app, LocalDateTime roundUpdatedAt) {
        String sql = "INSERT INTO dbo.organizer_application"
                + "(round_id,student_id,choice1_position_id,choice2_position_id,note) "
                + "SELECT ?,?,?,?,? FROM dbo.organizer_recruitment_round r "
                + "JOIN dbo.extracurricular_activity a ON a.id=r.activity_id "
                + "WHERE r.id=? AND r.status='OPEN' AND (r.open_at IS NULL OR r.open_at<=SYSDATETIME()) "
                + "AND (r.close_at IS NULL OR r.close_at>SYSDATETIME()) "
                + "AND a.approval_status='APPROVED' AND a.activity_status='UPCOMING' AND a.start_time>SYSUTCDATETIME()";
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                String eligible = "SELECT a.id FROM dbo.organizer_recruitment_round r "
                        + "JOIN dbo.extracurricular_activity a WITH (UPDLOCK,HOLDLOCK) ON a.id=r.activity_id "
                        + "WHERE r.id=? AND a.approval_status='APPROVED' AND a.activity_status='UPCOMING' "
                        + "AND a.start_time>SYSUTCDATETIME()";
                try (PreparedStatement p = c.prepareStatement(eligible)) {
                    p.setInt(1, app.getRoundId());
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Hoạt động không còn hợp lệ.");
                    }
                }
                String open = "SELECT 1 FROM dbo.organizer_recruitment_round WITH (UPDLOCK,HOLDLOCK) "
                        + "WHERE id=? AND updated_at=? AND status='OPEN' AND (open_at IS NULL OR open_at<=SYSDATETIME()) "
                        + "AND (close_at IS NULL OR close_at>SYSDATETIME())";
                try (PreparedStatement p = c.prepareStatement(open)) {
                    p.setInt(1, app.getRoundId());
                    p.setTimestamp(2, Timestamp.valueOf(roundUpdatedAt));
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Đợt tuyển đã đóng.");
                    }
                }
                List<Integer> choices = new ArrayList<>();
                choices.add(app.getChoice1PositionId());
                if (app.getChoice2PositionId() != null) choices.add(app.getChoice2PositionId());
                Collections.sort(choices);
                for (Integer positionId : choices) {
                    String available = "SELECT p.quota, (SELECT COUNT(*) FROM dbo.organizer_application oa "
                            + "WHERE oa.accepted_position_id=p.id AND oa.status='ACCEPTED') accepted "
                            + "FROM dbo.organizer_recruitment_position p WITH (UPDLOCK,HOLDLOCK) "
                            + "WHERE p.id=? AND p.round_id=?";
                    try (PreparedStatement p = c.prepareStatement(available)) {
                        p.setInt(1, positionId);
                        p.setInt(2, app.getRoundId());
                        try (ResultSet rs = p.executeQuery()) {
                            if (!rs.next() || rs.getInt("accepted") >= rs.getInt("quota")) {
                                throw new SQLException("Vị trí đã chọn không còn chỉ tiêu.");
                            }
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "DELETE FROM dbo.organizer_application WHERE round_id=? AND student_id=? AND status='WITHDRAWN'")) {
                    p.setInt(1, app.getRoundId());
                    p.setInt(2, app.getStudentId());
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, app.getRoundId());
                p.setInt(2, app.getStudentId());
                p.setInt(3, app.getChoice1PositionId());
                if (app.getChoice2PositionId() == null) {
                    p.setNull(4, Types.INTEGER);
                } else {
                    p.setInt(4, app.getChoice2PositionId());
                }
                p.setString(5, app.getNote());
                p.setInt(6, app.getRoundId());
                if (p.executeUpdate() != 1) throw new SQLException("Đợt tuyển đã đóng.");
                int id;
                try (ResultSet keys = p.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Không lấy được id hồ sơ.");
                    }
                    id = keys.getInt(1);
                }
                insertAnswers(c, id, app.getAnswers());
                c.commit();
                return id;
                }
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            boolean duplicate = e.getErrorCode() == 2627 || e.getErrorCode() == 2601;
            throw new DataAccessException("Không thể nộp hồ sơ ứng tuyển.", e, duplicate);
        }
    }

    @Override
    public boolean updateStatus(int id, List<String> expectedStatuses, String newStatus) {
        return transition(id, expectedStatuses, newStatus, null, null);
    }

    @Override
    public boolean withdraw(int id, List<String> expectedStatuses, String reason) {
        return transition(id, expectedStatuses, RecruitmentStatus.APP_WITHDRAWN, "withdraw_reason", reason);
    }

    @Override
    public boolean reject(int id, List<String> expectedStatuses, String reason) {
        return transition(id, expectedStatuses, RecruitmentStatus.APP_REJECTED, "decision_reason", reason);
    }

    /** Dùng cho mọi trạng thái trừ ACCEPTED (ACCEPTED phải đi qua accept để kiểm tra chỉ tiêu). */
    private boolean transition(int id, List<String> expected, String newStatus, String reasonColumn, String reason) {
        if (expected == null || expected.isEmpty() || RecruitmentStatus.APP_ACCEPTED.equals(newStatus)) {
            return false;
        }
        boolean decided = RecruitmentStatus.APP_REJECTED.equals(newStatus);
        String sql = "UPDATE oa SET status=?, accepted_position_id=NULL, "
                + (reasonColumn == null ? "" : reasonColumn + "=?, ")
                + "decided_at=" + (decided ? "SYSDATETIME()" : "NULL") + ", updated_at=SYSDATETIME() "
                + "FROM dbo.organizer_application oa JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id "
                + "JOIN dbo.extracurricular_activity a ON a.id=r.activity_id "
                + "WHERE oa.id=? AND oa.status IN (" + placeholders(expected.size()) + ") "
                + (decided ? "AND a.start_time>SYSUTCDATETIME() " : "")
                + (expected.contains(RecruitmentStatus.APP_ACCEPTED)
                    ? "AND a.start_time>DATEADD(HOUR,48,SYSUTCDATETIME()) " : "");
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql)) {
            int i = 1;
            p.setString(i++, newStatus);
            if (reasonColumn != null) {
                p.setString(i++, reason);
            }
            p.setInt(i++, id);
            for (String status : expected) {
                p.setString(i++, status);
            }
            if (p.executeUpdate() == 0) {
                c.rollback();
                return false;
            }
            if (expected.contains(RecruitmentStatus.APP_ACCEPTED)) {
                removeOrganizerParticipant(c, id);
            }
            c.commit();
            return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể cập nhật trạng thái hồ sơ.", e);
        }
    }

    @Override
    public boolean accept(int id, List<String> expectedStatuses, int positionId) {
        if (expectedStatuses == null || expectedStatuses.isEmpty()) {
            return false;
        }
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int activityId;
                int studentId;
                String lookup = "SELECT r.activity_id, oa.student_id FROM dbo.organizer_application oa "
                        + "JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id "
                        + "JOIN dbo.organizer_recruitment_position p ON p.round_id=r.id AND p.id=? WHERE oa.id=?";
                try (PreparedStatement p = c.prepareStatement(lookup)) {
                    p.setInt(1, positionId);
                    p.setInt(2, id);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                        activityId = rs.getInt(1);
                        studentId = rs.getInt(2);
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT id FROM dbo.extracurricular_activity WITH (UPDLOCK,HOLDLOCK) "
                        + "WHERE id=? AND start_time>SYSUTCDATETIME()")) {
                    p.setInt(1, activityId);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                    }
                }
                // Khóa dòng vị trí để hai staff không cùng nhận vượt chỉ tiêu.
                int quota;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT quota FROM dbo.organizer_recruitment_position WITH (UPDLOCK,HOLDLOCK) WHERE id=?")) {
                    p.setInt(1, positionId);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                        quota = rs.getInt(1);
                    }
                }
                int accepted;
                try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM dbo.organizer_application "
                        + "WHERE accepted_position_id=? AND status='ACCEPTED'")) {
                    p.setInt(1, positionId);
                    try (ResultSet rs = p.executeQuery()) {
                        accepted = rs.next() ? rs.getInt(1) : 0;
                    }
                }
                if (accepted >= quota) {
                    c.rollback();
                    return false;
                }
                String update = "UPDATE dbo.organizer_application SET status='ACCEPTED', accepted_position_id=?, "
                        + "decision_reason=NULL, decided_at=SYSDATETIME(), updated_at=SYSDATETIME() "
                        + "WHERE id=? AND status IN (" + placeholders(expectedStatuses.size()) + ") "
                        + "AND EXISTS (SELECT 1 FROM dbo.organizer_recruitment_round r "
                        + "JOIN dbo.extracurricular_activity a ON a.id=r.activity_id "
                        + "WHERE r.id=organizer_application.round_id AND a.start_time>SYSUTCDATETIME())";
                try (PreparedStatement p = c.prepareStatement(update)) {
                    int i = 1;
                    p.setInt(i++, positionId);
                    p.setInt(i++, id);
                    for (String status : expectedStatuses) {
                        p.setString(i++, status);
                    }
                    if (p.executeUpdate() == 0) {
                        c.rollback();
                        return false;
                    }
                }
                cancelParticipation(c, activityId, studentId);
                addOrganizerParticipant(c, activityId, studentId);
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể ghi nhận trúng tuyển.", e);
        }
    }

    /** Mỗi sinh viên giữ một vai trò: trúng tuyển ban tổ chức thì hủy đăng ký tham gia cùng hoạt động. */
    private static void cancelParticipation(Connection c, int activityId, int studentId) throws SQLException {
        String cancel = "UPDATE r SET status='CANCELLED', cancelled_for_organizer=1 FROM dbo.activity_registration r "
                + "JOIN dbo.activity_registration_form f ON f.id=r.form_id "
                + "WHERE f.activity_id=? AND r.student_id=? AND r.status IN ('SUBMITTED','APPROVED')";
        try (PreparedStatement p = c.prepareStatement(cancel)) {
            p.setInt(1, activityId);
            p.setInt(2, studentId);
            p.executeUpdate();
        }
        String removeParticipant = "DELETE ap FROM dbo.activity_participant ap "
                + "WHERE ap.activity_id=? AND ap.student_id=? AND ap.attendance_status='NOT_YET' "
                + "AND NOT EXISTS (SELECT 1 FROM dbo.activity_attendance_history h WHERE h.activity_participant_id=ap.id)";
        try (PreparedStatement p = c.prepareStatement(removeParticipant)) {
            p.setInt(1, activityId);
            p.setInt(2, studentId);
            p.executeUpdate();
        }
    }

    /** Ban tổ chức xuất hiện trong danh sách điểm danh với nhãn vai trò ORGANIZER. */
    private static void addOrganizerParticipant(Connection c, int activityId, int studentId) throws SQLException {
        String sql = "INSERT INTO dbo.activity_participant "
                + "(activity_id, student_id, registration_status, registration_by, participation_role) "
                + "SELECT ?, ?, 'STAFF_ENROLLED', ?, 'ORGANIZER' "
                + "WHERE NOT EXISTS (SELECT 1 FROM dbo.activity_participant ap WHERE ap.activity_id=? AND ap.student_id=?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            p.setInt(2, studentId);
            p.setInt(3, studentId);
            p.setInt(4, activityId);
            p.setInt(5, studentId);
            p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement("UPDATE dbo.activity_participant "
                + "SET participation_role='ORGANIZER', registration_status='STAFF_ENROLLED' "
                + "WHERE activity_id=? AND student_id=?")) {
            p.setInt(1, activityId);
            p.setInt(2, studentId);
            p.executeUpdate();
        }
        String updateResult = "UPDATE sar SET participation_role='ORGANIZER', early_reg_coefficient=1.00, organizer_coefficient=1.50, "
                + "final_point=CASE "
                + "WHEN ap.attendance_status IN ('ATTENDED','IN_LATE','OUT_EARLY') THEN ROUND("
                + "COALESCE(sar.average_score,0)*COALESCE(sar.org_level_coefficient,1)"
                + "*1.50,2) "
                + "WHEN ap.attendance_status='ABSENT' THEN COALESCE(sar.attendance_score,0) "
                + "ELSE 0 END "
                + "FROM dbo.student_activity_result sar "
                + "JOIN dbo.activity_participant ap ON ap.activity_id=sar.activity_id "
                + "AND ap.student_id=sar.student_id WHERE sar.activity_id=? AND sar.student_id=?";
        try (PreparedStatement p = c.prepareStatement(updateResult)) {
            p.setInt(1, activityId);
            p.setInt(2, studentId);
            p.executeUpdate();
        }
    }

    /** Gỡ dòng ban tổ chức chưa điểm danh khi hồ sơ không còn trúng tuyển. */
    private static void removeOrganizerParticipant(Connection c, int applicationId) throws SQLException {
        String sql = "DELETE ap FROM dbo.activity_participant ap "
                + "JOIN dbo.organizer_application oa ON oa.id=? AND oa.student_id=ap.student_id "
                + "JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id AND r.activity_id=ap.activity_id "
                + "WHERE ap.participation_role='ORGANIZER' AND ap.attendance_status='NOT_YET' "
                + "AND NOT EXISTS (SELECT 1 FROM dbo.activity_attendance_history h WHERE h.activity_participant_id=ap.id)";
        int deleted;
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, applicationId);
            deleted = p.executeUpdate();
        }
        String remaining = "SELECT 1 FROM dbo.activity_participant ap "
                + "JOIN dbo.organizer_application oa ON oa.id=? AND oa.student_id=ap.student_id "
                + "JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id AND r.activity_id=ap.activity_id "
                + "WHERE ap.participation_role='ORGANIZER'";
        try (PreparedStatement p = c.prepareStatement(remaining)) {
            p.setInt(1, applicationId);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) throw new SQLException("Không thể gỡ thành viên đã điểm danh.");
            }
        }
        if (deleted > 0) {
            String deleteResult = "DELETE sar FROM dbo.student_activity_result sar "
                    + "JOIN dbo.organizer_application oa ON oa.id=? AND oa.student_id=sar.student_id "
                    + "JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id AND r.activity_id=sar.activity_id "
                    + "WHERE sar.participation_role='ORGANIZER'";
            try (PreparedStatement p = c.prepareStatement(deleteResult)) {
                p.setInt(1, applicationId);
                p.executeUpdate();
            }
        }
    }

    @Override
    public boolean revertDecision(int id, String revertStatus) {
        String sql = "UPDATE oa SET status=?, accepted_position_id=NULL, decision_reason=NULL, decided_at=NULL, "
                + "updated_at=SYSDATETIME() FROM dbo.organizer_application oa "
                + "JOIN dbo.organizer_recruitment_round r ON r.id=oa.round_id "
                + "JOIN dbo.extracurricular_activity a ON a.id=r.activity_id "
                + "WHERE oa.id=? AND oa.status IN ('ACCEPTED','REJECTED') "
                + "AND a.start_time IS NOT NULL AND a.start_time>SYSUTCDATETIME()";
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, revertStatus);
                p.setInt(2, id);
                if (p.executeUpdate() == 0) {
                    c.rollback();
                    return false;
                }
            }
            removeOrganizerParticipant(c, id);
            c.commit();
            return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể thu hồi quyết định.", e);
        }
    }

    @Override
    public boolean updateInternalNote(int id, String note) {
        String sql = "UPDATE dbo.organizer_application SET internal_note=?, updated_at=SYSDATETIME() WHERE id=?";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, note);
            p.setInt(2, id);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể lưu ghi chú nội bộ.", e);
        }
    }

    @Override
    public RecruitmentInterview findInterview(int applicationId) {
        try (Connection c = DBContext.getConnection()) {
            return loadInterview(c, applicationId);
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải lịch phỏng vấn.", e);
        }
    }

    @Override
    public boolean saveInterview(RecruitmentInterview iv) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                String app = "UPDATE dbo.organizer_application SET status='INTERVIEW_INVITED', updated_at=SYSDATETIME() "
                        + "WHERE id=? AND status IN ('PENDING','INTERVIEW_INVITED')";
                try (PreparedStatement p = c.prepareStatement(app)) {
                    p.setInt(1, iv.getApplicationId());
                    if (p.executeUpdate() == 0) {
                        c.rollback();
                        return false;
                    }
                }
                String update = "UPDATE dbo.organizer_interview SET scheduled_at=?, format=?, location=?, note=?, "
                        + "confirm_deadline=?, confirmed_at=NULL, reminder_sent_at=NULL, status='SCHEDULED', "
                        + "evaluation=NULL, created_by=?, updated_at=SYSDATETIME() WHERE application_id=?";
                int changed;
                try (PreparedStatement p = c.prepareStatement(update)) {
                    bindInterview(p, iv, 1);
                    bindCreator(p, iv);
                    p.setInt(7, iv.getApplicationId());
                    changed = p.executeUpdate();
                }
                if (changed == 0) {
                    String insert = "INSERT INTO dbo.organizer_interview"
                            + "(scheduled_at,format,location,note,confirm_deadline,created_by,application_id) VALUES (?,?,?,?,?,?,?)";
                    try (PreparedStatement p = c.prepareStatement(insert)) {
                        bindInterview(p, iv, 1);
                        bindCreator(p, iv);
                        p.setInt(7, iv.getApplicationId());
                        p.executeUpdate();
                    }
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể lưu lịch phỏng vấn.", e);
        }
    }

    private static void bindInterview(PreparedStatement p, RecruitmentInterview iv, int start) throws SQLException {
        p.setTimestamp(start, Timestamp.valueOf(iv.getScheduledAt()));
        p.setString(start + 1, iv.getFormat());
        p.setString(start + 2, iv.getLocation());
        p.setString(start + 3, iv.getNote());
        p.setTimestamp(start + 4, Timestamp.valueOf(iv.getConfirmDeadline()));
    }

    private static void bindCreator(PreparedStatement p, RecruitmentInterview iv) throws SQLException {
        if (iv.getCreatedBy() == null) {
            p.setNull(6, Types.INTEGER);
        } else {
            p.setInt(6, iv.getCreatedBy());
        }
    }

    @Override
    public boolean confirmInterview(int applicationId) {
        String sql = "UPDATE dbo.organizer_interview SET status='CONFIRMED', confirmed_at=SYSDATETIME(), "
                + "updated_at=SYSDATETIME() WHERE application_id=? AND status='SCHEDULED' "
                + "AND confirm_deadline>=SYSDATETIME() AND EXISTS "
                + "(SELECT 1 FROM dbo.organizer_application oa WHERE oa.id=application_id AND oa.status='INTERVIEW_INVITED')";
        return executeById(sql, applicationId, "Không thể xác nhận lịch phỏng vấn.");
    }

    @Override
    public boolean cancelInterview(int applicationId, String revertApplicationStatus) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int cancelled;
                try (PreparedStatement p = c.prepareStatement("UPDATE dbo.organizer_interview SET status='CANCELLED', "
                        + "updated_at=SYSDATETIME() WHERE application_id=? AND status IN ('SCHEDULED','CONFIRMED')")) {
                    p.setInt(1, applicationId);
                    cancelled = p.executeUpdate();
                }
                if (cancelled == 0) {
                    c.rollback();
                    return false;
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE dbo.organizer_application SET status=?, "
                        + "updated_at=SYSDATETIME() WHERE id=? AND status='INTERVIEW_INVITED'")) {
                    p.setString(1, revertApplicationStatus);
                    p.setInt(2, applicationId);
                    p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể hủy lịch phỏng vấn.", e);
        }
    }

    @Override
    public boolean completeInterview(int applicationId, String evaluation) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int done;
                try (PreparedStatement p = c.prepareStatement("UPDATE dbo.organizer_interview SET status='DONE', "
                        + "evaluation=?, updated_at=SYSDATETIME() WHERE application_id=? "
                        + "AND status IN ('SCHEDULED','CONFIRMED') AND scheduled_at<=SYSDATETIME()")) {
                    p.setString(1, evaluation);
                    p.setInt(2, applicationId);
                    done = p.executeUpdate();
                }
                if (done == 0) {
                    c.rollback();
                    return false;
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE dbo.organizer_application SET status='INTERVIEWED', "
                        + "updated_at=SYSDATETIME() WHERE id=? AND status='INTERVIEW_INVITED'")) {
                    p.setInt(1, applicationId);
                    if (p.executeUpdate() == 0) {
                        c.rollback();
                        return false;
                    }
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể ghi nhận kết quả phỏng vấn.", e);
        }
    }

    @Override
    public List<RecruitmentInterview> findInterviewsNeedingReminder(int hoursAhead) {
        String sql = SELECT_INTERVIEW + "WHERE status='SCHEDULED' AND reminder_sent_at IS NULL "
                + "AND EXISTS (SELECT 1 FROM dbo.organizer_application oa "
                + "WHERE oa.id=organizer_interview.application_id AND oa.status='INTERVIEW_INVITED') "
                + "AND confirm_deadline>SYSDATETIME() AND confirm_deadline<=DATEADD(HOUR,?,SYSDATETIME())";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, hoursAhead);
            List<RecruitmentInterview> list = new ArrayList<>();
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInterview(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải lịch cần nhắc.", e);
        }
    }

    @Override
    public boolean sendInterviewReminderIfDue(RecruitmentInterview interview, int hoursAhead,
                                              String title, String content, String targetUrl) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                String claim = "UPDATE iv SET reminder_sent_at=SYSDATETIME() "
                        + "FROM dbo.organizer_interview iv JOIN dbo.organizer_application oa ON oa.id=iv.application_id "
                        + "WHERE iv.id=? AND iv.status='SCHEDULED' AND oa.status='INTERVIEW_INVITED' "
                        + "AND iv.reminder_sent_at IS NULL AND iv.confirm_deadline=? "
                        + "AND iv.confirm_deadline>SYSDATETIME() "
                        + "AND iv.confirm_deadline<=DATEADD(HOUR,?,SYSDATETIME())";
                try (PreparedStatement p = c.prepareStatement(claim)) {
                    p.setInt(1, interview.getId());
                    p.setTimestamp(2, Timestamp.valueOf(interview.getConfirmDeadline()));
                    p.setInt(3, hoursAhead);
                    if (p.executeUpdate() == 0) {
                        c.rollback();
                        return false;
                    }
                }
                int notificationId;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO dbo.notifications (title,content,type,event_type,target_url) "
                        + "VALUES (?,?,'IN_APP','RECRUITMENT_INTERVIEW_REMINDER',?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, title);
                    p.setString(2, content);
                    p.setString(3, targetUrl);
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không tạo được thông báo nhắc phỏng vấn.");
                        notificationId = keys.getInt(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO dbo.user_notifications (notification_id,user_id) "
                        + "SELECT ?,s.account_id FROM dbo.organizer_application oa "
                        + "JOIN dbo.student s ON s.id=oa.student_id "
                        + "WHERE oa.id=? AND s.account_id IS NOT NULL")) {
                    p.setInt(1, notificationId);
                    p.setInt(2, interview.getApplicationId());
                    if (p.executeUpdate() != 1) throw new SQLException("Không tìm thấy tài khoản sinh viên nhận thông báo.");
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể gửi thông báo nhắc phỏng vấn.", e);
        }
    }

    private static boolean executeById(String sql, int id, String error) {
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException(error, e);
        }
    }

    private static void insertAnswers(Connection c, int applicationId, List<RecruitmentAnswer> answers) throws SQLException {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO dbo.organizer_application_answer(application_id,question_id,answer_value) VALUES (?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (RecruitmentAnswer a : answers) {
                p.setInt(1, applicationId);
                p.setInt(2, a.getQuestionId());
                p.setString(3, a.getAnswerValue());
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private static List<RecruitmentAnswer> loadAnswers(Connection c, int applicationId) throws SQLException {
        String sql = "SELECT a.id,a.application_id,a.question_id,q.question_text,a.answer_value "
                + "FROM dbo.organizer_application_answer a "
                + "JOIN dbo.organizer_recruitment_question q ON q.id=a.question_id "
                + "WHERE a.application_id=? ORDER BY q.sort_order,q.id";
        List<RecruitmentAnswer> list = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, applicationId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    RecruitmentAnswer a = new RecruitmentAnswer();
                    a.setId(rs.getInt("id"));
                    a.setApplicationId(rs.getInt("application_id"));
                    a.setQuestionId(rs.getInt("question_id"));
                    a.setQuestionText(rs.getString("question_text"));
                    a.setAnswerValue(rs.getString("answer_value"));
                    list.add(a);
                }
            }
        }
        return list;
    }

    private static RecruitmentInterview loadInterview(Connection c, int applicationId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(SELECT_INTERVIEW + "WHERE application_id=?")) {
            p.setInt(1, applicationId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? mapInterview(rs) : null;
            }
        }
    }

    private static List<RecruitmentApplication> readList(PreparedStatement p) throws SQLException {
        List<RecruitmentApplication> list = new ArrayList<>();
        try (ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                list.add(mapApplication(rs));
            }
        }
        return list;
    }

    private static RecruitmentApplication mapApplication(ResultSet rs) throws SQLException {
        RecruitmentApplication a = new RecruitmentApplication();
        a.setId(rs.getInt("id"));
        a.setRoundId(rs.getInt("round_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setStudentCode(rs.getString("student_code"));
        a.setStudentName(rs.getString("student_name"));
        a.setClassName(rs.getString("class_code"));
        a.setRoundTitle(rs.getString("round_title"));
        a.setActivityId(rs.getInt("activity_id"));
        a.setActivityName(rs.getString("activity_name"));
        a.setActivityStart(toLocal(rs.getTimestamp("activity_start")));
        a.setChoice1PositionId(rs.getInt("choice1_position_id"));
        a.setChoice1Name(rs.getString("choice1_name"));
        a.setChoice2PositionId(nullableInt(rs, "choice2_position_id"));
        a.setChoice2Name(rs.getString("choice2_name"));
        a.setAcceptedPositionId(nullableInt(rs, "accepted_position_id"));
        a.setAcceptedPositionName(rs.getString("accepted_name"));
        a.setStatus(rs.getString("status"));
        a.setNote(rs.getString("note"));
        a.setInternalNote(rs.getString("internal_note"));
        a.setDecisionReason(rs.getString("decision_reason"));
        a.setWithdrawReason(rs.getString("withdraw_reason"));
        a.setSubmittedAt(toLocal(rs.getTimestamp("submitted_at")));
        a.setDecidedAt(toLocal(rs.getTimestamp("decided_at")));
        a.setUpdatedAt(toLocal(rs.getTimestamp("updated_at")));
        return a;
    }

    private static RecruitmentInterview mapInterview(ResultSet rs) throws SQLException {
        RecruitmentInterview i = new RecruitmentInterview();
        i.setId(rs.getInt("id"));
        i.setApplicationId(rs.getInt("application_id"));
        i.setScheduledAt(toLocal(rs.getTimestamp("scheduled_at")));
        i.setFormat(rs.getString("format"));
        i.setLocation(rs.getString("location"));
        i.setNote(rs.getString("note"));
        i.setConfirmDeadline(toLocal(rs.getTimestamp("confirm_deadline")));
        i.setConfirmedAt(toLocal(rs.getTimestamp("confirmed_at")));
        i.setReminderSentAt(toLocal(rs.getTimestamp("reminder_sent_at")));
        i.setStatus(rs.getString("status"));
        i.setEvaluation(rs.getString("evaluation"));
        i.setCreatedBy(nullableInt(rs, "created_by"));
        return i;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }
}
