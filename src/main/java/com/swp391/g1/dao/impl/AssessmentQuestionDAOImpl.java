package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IAssessmentQuestionDAO;
import com.swp391.g1.model.AssessmentQuestion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AssessmentQuestionDAOImpl implements IAssessmentQuestionDAO {

    private AssessmentQuestion mapRow(ResultSet rs) throws java.sql.SQLException {
        AssessmentQuestion q = new AssessmentQuestion();
        q.setId(rs.getInt("id"));
        q.setAssessmentId(rs.getInt("assessment_id"));
        String type = rs.getString("question_type");
        q.setQuestionType(type == null ? null : AssessmentQuestion.QuestionType.valueOf(type));
        q.setContent(rs.getString("content"));
        BigDecimal maxScore = rs.getBigDecimal("max_score");
        q.setMaxScore(maxScore == null ? 0 : maxScore.doubleValue());
        q.setDisplayOrder(rs.getInt("display_order"));
        return q;
    }

    @Override
    public List<AssessmentQuestion> findAll() {
        List<AssessmentQuestion> list = new ArrayList<>();
        String sql = "SELECT * FROM assessment_question ORDER BY assessment_id, display_order, id";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find all assessment questions.", e);
        }
        return list;
    }

    @Override
    public AssessmentQuestion findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT * FROM assessment_question WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find assessment question by ID.", e);
        }
    }

    @Override
    public List<AssessmentQuestion> findByAssessmentId(int assessmentId) {
        if (assessmentId <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM assessment_question WHERE assessment_id = ? ORDER BY display_order, id";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AssessmentQuestion> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find questions by assessment ID.", e);
        }
    }

    @Override
    public Integer insert(AssessmentQuestion question) {
        String sql = "INSERT INTO assessment_question (assessment_id, question_type, content, max_score, display_order) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, question.getAssessmentId());
            ps.setString(2, question.getQuestionType() == null ? "MCQ" : question.getQuestionType().name());
            ps.setString(3, question.getContent());
            ps.setBigDecimal(4, BigDecimal.valueOf(question.getMaxScore()));
            ps.setInt(5, question.getDisplayOrder());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to insert assessment question.", e);
        }
    }

    @Override
    public boolean update(AssessmentQuestion question) {
        String sql = "UPDATE assessment_question SET question_type = ?, content = ?, max_score = ?, display_order = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, question.getQuestionType() == null ? "MCQ" : question.getQuestionType().name());
            ps.setString(2, question.getContent());
            ps.setBigDecimal(3, BigDecimal.valueOf(question.getMaxScore()));
            ps.setInt(4, question.getDisplayOrder());
            ps.setInt(5, question.getId());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update assessment question.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_question WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete assessment question.", e);
        }
    }

    @Override
    public boolean deleteByAssessmentId(int assessmentId) {
        if (assessmentId <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_question WHERE assessment_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assessmentId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete questions by assessment ID.", e);
        }
    }
}