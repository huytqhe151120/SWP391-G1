package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IAssessmentOptionDAO;
import com.swp391.g1.model.AssessmentOption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AssessmentOptionDAOImpl implements IAssessmentOptionDAO {

    private AssessmentOption mapRow(ResultSet rs) throws java.sql.SQLException {
        AssessmentOption o = new AssessmentOption();
        o.setId(rs.getInt("id"));
        o.setQuestionId(rs.getInt("question_id"));
        o.setOptionText(rs.getString("option_text"));
        o.setCorrect(rs.getBoolean("is_correct"));
        o.setDisplayOrder(rs.getInt("display_order"));
        return o;
    }

    @Override
    public List<AssessmentOption> findAll() {
        List<AssessmentOption> list = new ArrayList<>();
        String sql = "SELECT * FROM assessment_option ORDER BY question_id, display_order, id";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find all assessment options.", e);
        }
        return list;
    }

    @Override
    public AssessmentOption findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT * FROM assessment_option WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find assessment option by ID.", e);
        }
    }

    @Override
    public List<AssessmentOption> findByQuestionId(int questionId) {
        if (questionId <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM assessment_option WHERE question_id = ? ORDER BY display_order, id";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AssessmentOption> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find options by question ID.", e);
        }
    }

    @Override
    public Integer insert(AssessmentOption option) {
        String sql = "INSERT INTO assessment_option (question_id, option_text, is_correct, display_order) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, option.getQuestionId());
            ps.setString(2, option.getOptionText());
            ps.setBoolean(3, option.isCorrect());
            ps.setInt(4, option.getDisplayOrder());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to insert assessment option.", e);
        }
    }

    @Override
    public boolean update(AssessmentOption option) {
        String sql = "UPDATE assessment_option SET option_text = ?, is_correct = ?, display_order = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, option.getOptionText());
            ps.setBoolean(2, option.isCorrect());
            ps.setInt(3, option.getDisplayOrder());
            ps.setInt(4, option.getId());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update assessment option.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_option WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete assessment option.", e);
        }
    }

    @Override
    public boolean deleteByQuestionId(int questionId) {
        if (questionId <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_option WHERE question_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete options by question ID.", e);
        }
    }
}
