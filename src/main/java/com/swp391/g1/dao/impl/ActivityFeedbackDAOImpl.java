package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityFeedbackDAO;
import com.swp391.g1.dto.response.ActivityFeedbackAnswerDTO;
import com.swp391.g1.dto.response.ActivityFeedbackReportDTO;
import com.swp391.g1.dto.response.ActivityFeedbackSummaryDTO;
import com.swp391.g1.dto.response.FeedbackActivityDTO;
import com.swp391.g1.dto.response.FeedbackStudentDTO;
import com.swp391.g1.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ActivityFeedbackDAOImpl implements IActivityFeedbackDAO {
    private static final String PRESENT_STATUSES = "'ATTENDED', 'IN_LATE', 'OUT_EARLY'";
    private static final String STUDENT_ACTIVITY_SQL = """
            SELECT ea.id, ea.code, ea.name, ea.address, ea.end_time,
                   CASE WHEN f.id IS NULL THEN 0 ELSE 1 END AS submitted
            FROM dbo.activity_participant ap
            JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
            LEFT JOIN dbo.activity_feedback f ON f.activity_participant_id = ap.id
            WHERE ap.student_id = ?
              AND ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
              AND (f.id IS NOT NULL OR (
                   ea.activity_status = 'COMPLETED'
                   AND ea.end_time <= SYSUTCDATETIME()))
            ORDER BY ea.end_time DESC, ea.id DESC
            """;

    @Override
    public List<FeedbackActivityDTO> findStudentActivities(int studentId) {
        List<FeedbackActivityDTO> activities = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(STUDENT_ACTIVITY_SQL)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    FeedbackActivityDTO activity = new FeedbackActivityDTO();
                    activity.setActivityId(rs.getInt("id"));
                    activity.setActivityCode(rs.getString("code"));
                    activity.setActivityName(rs.getString("name"));
                    activity.setAddress(rs.getString("address"));
                    activity.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    activity.setSubmitted(rs.getBoolean("submitted"));
                    activities.add(activity);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load student activity feedback.", e);
        }
        return activities;
    }

    @Override
    public int countPending(int studentId) {
        String sql = """
                SELECT COUNT(*)
                FROM dbo.activity_participant ap
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                WHERE ap.student_id = ?
                  AND ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                  AND ea.activity_status = 'COMPLETED'
                  AND ea.end_time <= SYSUTCDATETIME()
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_feedback f
                      WHERE f.activity_participant_id = ap.id)
                """;
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count pending student feedback.", e);
        }
    }

    @Override
    public ActivityFeedbackAnswerDTO findStudentFeedback(int studentId, int activityId) {
        String sql = """
                SELECT f.general_rating, f.content_rating, f.organization_rating, f.venue_rating,
                       f.best_part, f.improvement, f.favorite, f.suggestions, f.note, f.created_at
                FROM dbo.activity_feedback f
                JOIN dbo.activity_participant ap ON ap.id = f.activity_participant_id
                WHERE ap.student_id = ? AND ap.activity_id = ?
                  AND ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                """;
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, activityId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapAnswer(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load submitted feedback.", e);
        }
    }

    @Override
    public boolean submit(int studentId, int activityId, ActivityFeedbackAnswerDTO answer) {
        String sql = """
                INSERT INTO dbo.activity_feedback
                    (activity_participant_id, general_rating, content_rating, organization_rating,
                     venue_rating, best_part, improvement, favorite, suggestions, note)
                SELECT ap.id, ?, ?, ?, ?, ?, ?, ?, ?, ?
                FROM dbo.activity_participant ap WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                WHERE ap.student_id = ? AND ap.activity_id = ?
                  AND ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                  AND ea.activity_status = 'COMPLETED'
                  AND ea.end_time <= SYSUTCDATETIME()
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_feedback existing
                      WHERE existing.activity_participant_id = ap.id)
                """;
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, answer.getGeneralRating());
            statement.setInt(2, answer.getContentRating());
            statement.setInt(3, answer.getOrganizationRating());
            statement.setInt(4, answer.getVenueRating());
            statement.setString(5, answer.getBestPart().strip());
            statement.setString(6, answer.getImprovement().strip());
            statement.setString(7, answer.getFavorite().strip());
            statement.setString(8, answer.getSuggestions().strip());
            if (answer.getNote() == null || answer.getNote().isBlank()) {
                statement.setNull(9, Types.NVARCHAR);
            } else {
                statement.setString(9, answer.getNote().strip());
            }
            statement.setInt(10, studentId);
            statement.setInt(11, activityId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                return false;
            }
            throw new IllegalStateException("Failed to submit student activity feedback.", e);
        }
    }

    @Override
    public List<ActivityFeedbackSummaryDTO> findActivitySummaries(Integer staffAccountId, String staffRole) {
        String scope = staffScopePredicate(staffRole);
        String sql = """
                SELECT ea.id, ea.code, ea.name, ea.end_time,
                       at.name AS activity_type_name, d.name AS department_name, sem.name AS semester_name,
                       COUNT(ap.id) AS participant_count,
                       COUNT(f.id) AS submitted_count,
                       COALESCE(AVG(CONVERT(FLOAT, f.general_rating)), 0) AS average_rating
                FROM dbo.extracurricular_activity ea
                LEFT JOIN dbo.activity_type at ON at.id = ea.activity_type_id
                LEFT JOIN dbo.department d ON d.id = ea.responsible_department_id
                LEFT JOIN dbo.semester sem ON sem.id = ea.semester_id
                LEFT JOIN dbo.activity_participant ap
                    ON ap.activity_id = ea.id AND ap.attendance_status IN (%s)
                LEFT JOIN dbo.activity_feedback f ON f.activity_participant_id = ap.id
                WHERE ea.activity_status = 'COMPLETED'
                  AND ea.end_time <= SYSUTCDATETIME()
                """.formatted(PRESENT_STATUSES) + (scope == null ? "" : " AND " + scope) + """
                GROUP BY ea.id, ea.code, ea.name, ea.end_time, at.name, d.name, sem.name
                ORDER BY ea.end_time DESC, ea.id DESC
                """;
        List<ActivityFeedbackSummaryDTO> summaries = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindStaffScope(statement, staffAccountId, staffRole, 1);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ActivityFeedbackSummaryDTO summary = new ActivityFeedbackSummaryDTO();
                    summary.setActivityId(rs.getInt("id"));
                    summary.setActivityCode(rs.getString("code"));
                    summary.setActivityName(rs.getString("name"));
                    summary.setActivityTypeName(rs.getString("activity_type_name"));
                    summary.setDepartmentName(rs.getString("department_name"));
                    summary.setSemesterName(rs.getString("semester_name"));
                    summary.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    summary.setParticipantCount(rs.getInt("participant_count"));
                    summary.setSubmittedCount(rs.getInt("submitted_count"));
                    summary.setPendingCount(summary.getParticipantCount() - summary.getSubmittedCount());
                    summary.setAverageRating(rs.getDouble("average_rating"));
                    summaries.add(summary);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load activity feedback summaries.", e);
        }
        return summaries;
    }

    @Override
    public ActivityFeedbackReportDTO findActivityReport(int activityId, Integer staffAccountId, String staffRole) {
        return findActivityReport(activityId, staffAccountId, staffRole, null);
    }

    @Override
    public ActivityFeedbackReportDTO findActivityReport(int activityId, Integer staffAccountId, String staffRole,
                                                        String participationRole) {
        RecruitmentSchema.ensure();
        String roleFilter = "ORGANIZER".equals(participationRole) || "PARTICIPANT".equals(participationRole)
                ? " AND ap.participation_role = '" + participationRole + "'" : "";
        String scope = staffScopePredicate(staffRole);
        String scopeFilter = scope == null ? "" : " AND " + scope;
        String summarySql = """
                SELECT ea.id, ea.code, ea.name, ea.end_time,
                       at.name AS activity_type_name, d.name AS department_name, sem.name AS semester_name,
                       COUNT(ap.id) AS participant_count, COUNT(f.id) AS submitted_count,
                       COALESCE(AVG(CONVERT(FLOAT, f.general_rating)), 0) AS average_rating,
                       COALESCE(AVG(CONVERT(FLOAT, f.content_rating)), 0) AS average_content,
                       COALESCE(AVG(CONVERT(FLOAT, f.organization_rating)), 0) AS average_organization,
                       COALESCE(AVG(CONVERT(FLOAT, f.venue_rating)), 0) AS average_venue
                FROM dbo.extracurricular_activity ea
                LEFT JOIN dbo.activity_type at ON at.id = ea.activity_type_id
                LEFT JOIN dbo.department d ON d.id = ea.responsible_department_id
                LEFT JOIN dbo.semester sem ON sem.id = ea.semester_id
                LEFT JOIN dbo.activity_participant ap
                    ON ap.activity_id = ea.id AND ap.attendance_status IN (%s)%s
                LEFT JOIN dbo.activity_feedback f ON f.activity_participant_id = ap.id
                WHERE ea.id = ? AND ea.activity_status = 'COMPLETED'
                  AND ea.end_time <= SYSUTCDATETIME()
                """.formatted(PRESENT_STATUSES, roleFilter) + scopeFilter + """
                GROUP BY ea.id, ea.code, ea.name, ea.end_time, at.name, d.name, sem.name
                """;
        ActivityFeedbackReportDTO report = new ActivityFeedbackReportDTO();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(summarySql)) {
            statement.setInt(1, activityId);
            bindStaffScope(statement, staffAccountId, staffRole, 2);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                ActivityFeedbackSummaryDTO summary = new ActivityFeedbackSummaryDTO();
                summary.setActivityId(rs.getInt("id"));
                summary.setActivityCode(rs.getString("code"));
                summary.setActivityName(rs.getString("name"));
                summary.setActivityTypeName(rs.getString("activity_type_name"));
                summary.setDepartmentName(rs.getString("department_name"));
                summary.setSemesterName(rs.getString("semester_name"));
                summary.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                summary.setParticipantCount(rs.getInt("participant_count"));
                summary.setSubmittedCount(rs.getInt("submitted_count"));
                summary.setPendingCount(summary.getParticipantCount() - summary.getSubmittedCount());
                summary.setAverageRating(rs.getDouble("average_rating"));
                report.setSummary(summary);
                report.setAverageContentRating(rs.getDouble("average_content"));
                report.setAverageOrganizationRating(rs.getDouble("average_organization"));
                report.setAverageVenueRating(rs.getDouble("average_venue"));
            }

            loadAnswers(connection, report, activityId, roleFilter);
            loadPendingStudents(connection, report, activityId, roleFilter);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load activity feedback report.", e);
        }
        return report;
    }

    private void loadAnswers(Connection connection, ActivityFeedbackReportDTO report, int activityId,
                             String roleFilter) throws SQLException {
        String sql = """
                SELECT f.general_rating, f.content_rating, f.organization_rating, f.venue_rating,
                       f.best_part, f.improvement, f.favorite, f.suggestions, f.note, f.created_at
                FROM dbo.activity_feedback f
                JOIN dbo.activity_participant ap ON ap.id = f.activity_participant_id
                WHERE ap.activity_id = ?
                  AND ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                """ + roleFilter + """

                ORDER BY f.created_at DESC, f.id DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, activityId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ActivityFeedbackAnswerDTO answer = mapAnswer(rs);
                    report.getAnswers().add(answer);
                    report.getRatingDistribution()[answer.getGeneralRating() - 1]++;
                    if (answer.getNote() != null && !answer.getNote().isBlank()) {
                        report.setNoteCount(report.getNoteCount() + 1);
                    }
                }
            }
        }
    }

    private void loadPendingStudents(Connection connection, ActivityFeedbackReportDTO report, int activityId,
                                     String roleFilter) throws SQLException {
        String sql = """
                SELECT s.code, s.name
                FROM dbo.activity_participant ap
                JOIN dbo.student s ON s.id = ap.student_id
                WHERE ap.activity_id = ?
                  AND ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_feedback f
                      WHERE f.activity_participant_id = ap.id)
                """ + roleFilter + """

                ORDER BY s.name, s.code
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, activityId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    FeedbackStudentDTO student = new FeedbackStudentDTO();
                    student.setStudentCode(rs.getString("code"));
                    student.setStudentName(rs.getString("name"));
                    report.getPendingStudents().add(student);
                }
            }
        }
    }

    private static ActivityFeedbackAnswerDTO mapAnswer(ResultSet rs) throws SQLException {
        ActivityFeedbackAnswerDTO answer = new ActivityFeedbackAnswerDTO();
        answer.setGeneralRating(rs.getInt("general_rating"));
        answer.setContentRating(rs.getInt("content_rating"));
        answer.setOrganizationRating(rs.getInt("organization_rating"));
        answer.setVenueRating(rs.getInt("venue_rating"));
        answer.setBestPart(rs.getString("best_part"));
        answer.setImprovement(rs.getString("improvement"));
        answer.setFavorite(rs.getString("favorite"));
        answer.setSuggestions(rs.getString("suggestions"));
        answer.setNote(rs.getString("note"));
        answer.setSubmittedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return answer;
    }

    private static String staffScopePredicate(String role) {
        if ("STAFF".equals(role)) {
            return """
                    EXISTS (SELECT 1 FROM dbo.staff scoped_staff
                            WHERE scoped_staff.account_id = ?
                              AND scoped_staff.id = ea.responsible_staff_id)
                    """;
        }
        if ("UNIT_DEPUTY_HEAD".equals(role) || "UNIT_HEAD".equals(role)
                || "UNIT_SECRETARY".equals(role)) {
            return """
                    EXISTS (SELECT 1 FROM dbo.staff scoped_staff
                            JOIN dbo.staff_assignment sa ON sa.staff_id = scoped_staff.id
                            WHERE scoped_staff.account_id = ?
                              AND sa.department_id = ea.responsible_department_id)
                    """;
        }
        return null;
    }

    private static void bindStaffScope(PreparedStatement statement, Integer accountId, String role, int index)
            throws SQLException {
        if (staffScopePredicate(role) != null) {
            if (accountId == null || accountId <= 0) {
                throw new IllegalArgumentException("Tài khoản nhân viên không hợp lệ.");
            }
            statement.setInt(index, accountId);
        }
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : DateTimeUtil.fromUtc(timestamp.toLocalDateTime());
    }
}
