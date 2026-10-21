package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IQuestionDAO;
import com.swp391.g1.model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAOImpl implements IQuestionDAO {

    private Question mapRow(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setId(rs.getInt("id"));
        q.setPublicQuestion(rs.getBoolean("is_public"));
        q.setTitle(rs.getString("title"));
        q.setContent(rs.getString("content"));
        q.setAnonymous(rs.getBoolean("is_anonymous"));
        q.setStatus(rs.getString("status"));
        q.setStudentId(rs.getInt("student_id"));
        int activityId = rs.getInt("activity_id");
        q.setActivityId(rs.wasNull() ? null : activityId);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) q.setCreatedAt(ts.toLocalDateTime());
        try { q.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        try { q.setStudentCode(rs.getString("student_code")); } catch (SQLException ignored) {}
        try { q.setActivityName(rs.getString("activity_name")); } catch (SQLException ignored) {}
        try { q.setActivityCode(rs.getString("activity_code")); } catch (SQLException ignored) {}
        return q;
    }

    /** Get all questions (with student info) for staff/organizer - all statuses */
    @Override
    public List<Question> getAllQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code, ea.name AS activity_name, ea.code AS activity_code " +
                     "FROM [dbo].[question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "LEFT JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                     "ORDER BY q.created_at DESC";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all questions.", e);
        }
        return list;
    }

    /** Get only PENDING questions for staff to answer */
    @Override
    public List<Question> getPendingQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code, ea.name AS activity_name, ea.code AS activity_code " +
                     "FROM [dbo].[question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "LEFT JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                     "WHERE q.status = 'PENDING' " +
                     "ORDER BY q.created_at DESC";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find pending questions.", e);
        }
        return list;
    }

    /** Get published Q&A for public view (questions that have at least one PUBLISHED answer) */
    @Override
    public List<Question> getPublishedQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code, ea.name AS activity_name, ea.code AS activity_code " +
                     "FROM [dbo].[question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "LEFT JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                     "WHERE q.is_public = 1 " +
                     "ORDER BY q.created_at DESC";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find published questions.", e);
        }
        return list;
    }

    /** Get questions by a specific student */
    @Override
    public List<Question> getQuestionsByStudent(int studentId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code, ea.name AS activity_name, ea.code AS activity_code " +
                     "FROM [dbo].[question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "LEFT JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                     "WHERE q.student_id = ? " +
                     "ORDER BY q.created_at DESC";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find questions by student.", e);
        }
        return list;
    }

    /** Published questions from everyone, plus the student's own pending questions. */
    @Override
    public List<Question> getVisibleQuestionsForStudent(int studentId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code, ea.name AS activity_name, ea.code AS activity_code " +
                     "FROM [dbo].[question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "LEFT JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                     "WHERE q.is_public = 1 OR q.student_id = ? " +
                     "ORDER BY q.created_at DESC";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find visible questions for student.", e);
        }
        return list;
    }

    @Override
    public List<Question> getVisibleQuestionsForStudent(int studentId, int activityId) {
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code, "
                + "ea.name AS activity_name, ea.code AS activity_code FROM dbo.question q "
                + "LEFT JOIN dbo.student s ON q.student_id = s.id "
                + "JOIN dbo.extracurricular_activity ea ON q.activity_id = ea.id "
                + "WHERE q.activity_id = ? AND (q.is_public = 1 OR q.student_id = ?) "
                + "AND ea.approval_status = 'APPROVED' ORDER BY q.created_at DESC";
        List<Question> questions = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) questions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load activity questions.", e);
        }
        return questions;
    }

    @Override
    public List<Question> getQuestionsForPartnerStaff(int partnerStaffId, boolean pendingOnly) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT DISTINCT q.*, s.name AS student_name, s.code AS student_code, " +
                "ea.name AS activity_name, ea.code AS activity_code " +
                "FROM [dbo].[question] q " +
                "JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                "LEFT JOIN [dbo].[activity_partner_staff] aps ON aps.activity_id = ea.id " +
                "WHERE (ea.partner_staff_id = ? OR aps.partner_staff_id = ?) " +
                (pendingOnly ? "AND q.status = 'PENDING' " : "") +
                "ORDER BY q.created_at DESC";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, partnerStaffId);
            ps.setInt(2, partnerStaffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find questions assigned to partner staff.", e);
        }
        return list;
    }

    @Override
    public boolean isAssignedToPartnerStaff(int questionId, int partnerStaffId) {
        String sql = "SELECT 1 FROM [dbo].[question] q " +
                "JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                "LEFT JOIN [dbo].[activity_partner_staff] aps ON aps.activity_id = ea.id " +
                "WHERE q.id = ? AND (ea.partner_staff_id = ? OR aps.partner_staff_id = ?)";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.setInt(2, partnerStaffId);
            ps.setInt(3, partnerStaffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check partner staff question access.", e);
        }
    }

    /** Create a question owned by the authenticated student. */
    @Override
    public boolean insert(Question question) {
        String sql = "INSERT INTO [dbo].[question] " +
                     "(title, content, is_anonymous, status, student_id, activity_id, is_public, created_at) " +
                     "VALUES (?, ?, ?, 'PENDING', ?, ?, 0, CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2))";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, question.getTitle());
            ps.setString(2, question.getContent());
            ps.setBoolean(3, question.isAnonymous());
            ps.setInt(4, question.getStudentId());
            ps.setInt(5, question.getActivityId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert question.", e);
        }
    }

    /** Get single question by id */
    @Override
    public Question getById(int id) {
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code, ea.name AS activity_name, ea.code AS activity_code " +
                     "FROM [dbo].[question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "LEFT JOIN [dbo].[extracurricular_activity] ea ON q.activity_id = ea.id " +
                     "WHERE q.id = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find question by ID.", e);
        }
        return null;
    }

    @Override
    public boolean updateVisibility(int questionId, boolean publicQuestion) {
        String sql = "UPDATE [dbo].[question] SET is_public = ? WHERE id = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, publicQuestion);
            ps.setInt(2, questionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update question visibility.", e);
        }
    }

    /** Update status of a question (e.g., PENDING -> ANSWERED) */
    @Override
    public boolean updateStatus(int questionId, String status) {
        String sql = "UPDATE [dbo].[question] SET status = ? WHERE id = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, questionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update question status.", e);
        }
    }
}
