package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Question;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO extends DBContext {

    private Question mapRow(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setId(rs.getInt("id"));
        q.setTitle(rs.getString("title"));
        q.setContent(rs.getString("content"));
        q.setAnonymous(rs.getBoolean("is_anonymous"));
        q.setStatus(rs.getString("status"));
        q.setStudentId(rs.getInt("student_id"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) q.setCreatedAt(ts.toLocalDateTime());
        try { q.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        try { q.setStudentCode(rs.getString("student_code")); } catch (SQLException ignored) {}
        return q;
    }

    /** Get all questions (with student info) for staff/organizer - all statuses */
    public List<Question> getAllQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code " +
                     "FROM [dbo].[Question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "ORDER BY q.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get only PENDING questions for staff to answer */
    public List<Question> getPendingQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code " +
                     "FROM [dbo].[Question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "WHERE q.status = 'PENDING' " +
                     "ORDER BY q.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get published Q&A for public view (questions that have at least one PUBLISHED answer) */
    public List<Question> getPublishedQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code " +
                     "FROM [dbo].[Question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "WHERE q.status = 'ANSWERED' " +
                     "ORDER BY q.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get questions by a specific student */
    public List<Question> getQuestionsByStudent(int studentId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code " +
                     "FROM [dbo].[Question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "WHERE q.student_id = ? " +
                     "ORDER BY q.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get single question by id */
    public Question getById(int id) {
        String sql = "SELECT q.*, s.name AS student_name, s.code AS student_code " +
                     "FROM [dbo].[Question] q " +
                     "LEFT JOIN [dbo].[student] s ON q.student_id = s.id " +
                     "WHERE q.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Update status of a question (e.g., PENDING -> ANSWERED) */
    public boolean updateStatus(int questionId, String status) {
        String sql = "UPDATE [dbo].[Question] SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, questionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
