package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Answer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnswerDAO extends DBContext {

    private Answer mapRow(ResultSet rs) throws SQLException {
        Answer a = new Answer();
        a.setId(rs.getInt("id"));
        a.setQuestionId(rs.getInt("question_id"));
        a.setStaffId(rs.getInt("staff_id"));
        a.setContent(rs.getString("content"));
        a.setApprovalStatus(rs.getString("approval_status"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());
        Timestamp published = rs.getTimestamp("published_at");
        if (published != null) a.setPublishedAt(published.toLocalDateTime());
        try { a.setStaffName(rs.getString("staff_name")); } catch (SQLException ignored) {}
        try { a.setStaffCode(rs.getString("staff_code")); } catch (SQLException ignored) {}
        return a;
    }

    /** Get all answers for a specific question */
    public List<Answer> getByQuestionId(int questionId) {
        List<Answer> list = new ArrayList<>();
        String sql = "SELECT a.*, st.name AS staff_name, st.code AS staff_code " +
                     "FROM [dbo].[Answer] a " +
                     "LEFT JOIN [dbo].[staff] st ON a.staff_id = st.id " +
                     "WHERE a.question_id = ? " +
                     "ORDER BY a.created_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get only PUBLISHED answers for a question (public Q&A view) */
    public List<Answer> getPublishedByQuestionId(int questionId) {
        List<Answer> list = new ArrayList<>();
        String sql = "SELECT a.*, st.name AS staff_name, st.code AS staff_code " +
                     "FROM [dbo].[Answer] a " +
                     "LEFT JOIN [dbo].[staff] st ON a.staff_id = st.id " +
                     "WHERE a.question_id = ? AND a.approval_status = 'PUBLISHED' " +
                     "ORDER BY a.created_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Insert a new answer (default DRAFT status) */
    public boolean insert(Answer answer) {
        String sql = "INSERT INTO [dbo].[Answer] (question_id, staff_id, content, approval_status, created_at) " +
                     "VALUES (?, ?, ?, 'DRAFT', GETDATE())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, answer.getQuestionId());
            ps.setInt(2, answer.getStaffId());
            ps.setString(3, answer.getContent());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Approve/publish an answer */
    public boolean publish(int answerId) {
        String sql = "UPDATE [dbo].[Answer] SET approval_status = 'PUBLISHED', published_at = GETDATE() WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, answerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Delete an answer */
    public boolean delete(int answerId) {
        String sql = "DELETE FROM [dbo].[Answer] WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, answerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Get answer by id */
    public Answer getById(int id) {
        String sql = "SELECT a.*, st.name AS staff_name, st.code AS staff_code " +
                     "FROM [dbo].[Answer] a " +
                     "LEFT JOIN [dbo].[staff] st ON a.staff_id = st.id " +
                     "WHERE a.id = ?";
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
}
