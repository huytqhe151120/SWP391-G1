package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IAssessmentAnswerDAO;
import com.swp391.g1.model.AssessmentAnswer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AssessmentAnswerDAOImpl implements IAssessmentAnswerDAO {

    private AssessmentAnswer mapRow(ResultSet rs) throws java.sql.SQLException {
        AssessmentAnswer a = new AssessmentAnswer();
        a.setId(rs.getInt("id"));
        a.setAttemptId(rs.getInt("attempt_id"));
        a.setQuestionId(rs.getInt("question_id"));
        Object optId = rs.getObject("selected_option_id");
        a.setSelectedOptionId(optId == null ? null : ((Number) optId).intValue());
        a.setAnswerText(rs.getString("answer_text"));
        BigDecimal score = rs.getBigDecimal("awarded_score");
        a.setAwardedScore(score == null ? null : score.doubleValue());
        a.setFeedback(rs.getString("feedback"));
        String gs = rs.getString("grading_status");
        a.setGradingStatus(gs == null ? null : AssessmentAnswer.GradingStatus.valueOf(gs));
        Timestamp createdAt = rs.getTimestamp("created_at");
        a.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        a.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return a;
    }

    @Override
    public List<AssessmentAnswer> findAll() {
        List<AssessmentAnswer> list = new ArrayList<>();
        String sql = "SELECT * FROM assessment_answer ORDER BY attempt_id, question_id";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find all assessment answers.", e);
        }
        return list;
    }

    @Override
    public AssessmentAnswer findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT * FROM assessment_answer WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find assessment answer by ID.", e);
        }
    }

    @Override
    public List<AssessmentAnswer> findByAttemptId(int attemptId) {
        if (attemptId <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM assessment_answer WHERE attempt_id = ? ORDER BY question_id";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AssessmentAnswer> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find answers by attempt ID.", e);
        }
    }

    @Override
    public Integer insert(AssessmentAnswer answer) {
        String sql = "INSERT INTO assessment_answer (attempt_id, question_id, selected_option_id, answer_text, awarded_score, feedback, grading_status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2), CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2))";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, answer.getAttemptId());
            ps.setInt(2, answer.getQuestionId());
            if (answer.getSelectedOptionId() != null) {
                ps.setInt(3, answer.getSelectedOptionId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            if (answer.getAnswerText() != null) {
                ps.setString(4, answer.getAnswerText());
            } else {
                ps.setNull(4, java.sql.Types.NVARCHAR);
            }
            if (answer.getAwardedScore() != null) {
                ps.setBigDecimal(5, BigDecimal.valueOf(answer.getAwardedScore()));
            } else {
                ps.setNull(5, java.sql.Types.DECIMAL);
            }
            if (answer.getFeedback() != null) {
                ps.setString(6, answer.getFeedback());
            } else {
                ps.setNull(6, java.sql.Types.NVARCHAR);
            }
            ps.setString(7, answer.getGradingStatus() == null ? "PENDING_GRADING" : answer.getGradingStatus().name());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to insert assessment answer.", e);
        }
    }

    @Override
    public boolean update(AssessmentAnswer answer) {
        String sql = "UPDATE assessment_answer SET selected_option_id = ?, answer_text = ?, awarded_score = ?, feedback = ?, grading_status = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (answer.getSelectedOptionId() != null) {
                ps.setInt(1, answer.getSelectedOptionId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            if (answer.getAnswerText() != null) {
                ps.setString(2, answer.getAnswerText());
            } else {
                ps.setNull(2, java.sql.Types.NVARCHAR);
            }
            if (answer.getAwardedScore() != null) {
                ps.setBigDecimal(3, BigDecimal.valueOf(answer.getAwardedScore()));
            } else {
                ps.setNull(3, java.sql.Types.DECIMAL);
            }
            if (answer.getFeedback() != null) {
                ps.setString(4, answer.getFeedback());
            } else {
                ps.setNull(4, java.sql.Types.NVARCHAR);
            }
            ps.setString(5, answer.getGradingStatus() == null ? "PENDING_GRADING" : answer.getGradingStatus().name());
            ps.setInt(6, answer.getId());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update assessment answer.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_answer WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete assessment answer.", e);
        }
    }

    @Override
    public boolean updateGrade(int id, BigDecimal awardedScore, String feedback, AssessmentAnswer.GradingStatus gradingStatus) {
        String sql = "UPDATE assessment_answer SET awarded_score = ?, feedback = ?, grading_status = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ? AND grading_status = 'PENDING_GRADING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (awardedScore != null) {
                ps.setBigDecimal(1, awardedScore);
            } else {
                ps.setNull(1, java.sql.Types.DECIMAL);
            }
            if (feedback != null) {
                ps.setString(2, feedback);
            } else {
                ps.setNull(2, java.sql.Types.NVARCHAR);
            }
            ps.setString(3, gradingStatus == null ? "PENDING_GRADING" : gradingStatus.name());
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update answer grade.", e);
        }
    }

    @Override
    public boolean deleteByAttemptId(int attemptId) {
        if (attemptId <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_answer WHERE attempt_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete answers by attempt ID.", e);
        }
    }

    @Override
    public boolean deleteByAttemptId(Connection conn, int attemptId) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (attemptId <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_answer WHERE attempt_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete answers by attempt ID.", e);
        }
    }

    @Override
    public boolean insert(Connection conn, AssessmentAnswer answer) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (answer == null) {
            return false;
        }
        String sql = "INSERT INTO assessment_answer (attempt_id, question_id, selected_option_id, answer_text, awarded_score, feedback, grading_status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2), CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, answer.getAttemptId());
            ps.setInt(2, answer.getQuestionId());
            if (answer.getSelectedOptionId() != null) {
                ps.setInt(3, answer.getSelectedOptionId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            if (answer.getAnswerText() != null) {
                ps.setString(4, answer.getAnswerText());
            } else {
                ps.setNull(4, java.sql.Types.NVARCHAR);
            }
            if (answer.getAwardedScore() != null) {
                ps.setBigDecimal(5, BigDecimal.valueOf(answer.getAwardedScore()));
            } else {
                ps.setNull(5, java.sql.Types.DECIMAL);
            }
            if (answer.getFeedback() != null) {
                ps.setString(6, answer.getFeedback());
            } else {
                ps.setNull(6, java.sql.Types.NVARCHAR);
            }
            ps.setString(7, answer.getGradingStatus() == null ? "PENDING_GRADING" : answer.getGradingStatus().name());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to insert assessment answer.", e);
        }
    }

    @Override
    public boolean updateGrade(Connection conn, int id, BigDecimal awardedScore, String feedback, AssessmentAnswer.GradingStatus gradingStatus) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (id <= 0) {
            return false;
        }
        String sql = "UPDATE assessment_answer SET awarded_score = ?, feedback = ?, grading_status = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ? AND grading_status = 'PENDING_GRADING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (awardedScore != null) {
                ps.setBigDecimal(1, awardedScore);
            } else {
                ps.setNull(1, java.sql.Types.DECIMAL);
            }
            if (feedback != null) {
                ps.setString(2, feedback);
            } else {
                ps.setNull(2, java.sql.Types.NVARCHAR);
            }
            ps.setString(3, gradingStatus == null ? "PENDING_GRADING" : gradingStatus.name());
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update answer grade.", e);
        }
    }

    @Override
    public List<AssessmentAnswer> findByAttemptId(Connection conn, int attemptId) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (attemptId <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM assessment_answer WHERE attempt_id = ? ORDER BY question_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AssessmentAnswer> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find answers by attempt ID.", e);
        }
    }
}