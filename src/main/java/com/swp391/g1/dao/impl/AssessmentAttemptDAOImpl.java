package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IAssessmentAttemptDAO;
import com.swp391.g1.model.AssessmentAttempt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AssessmentAttemptDAOImpl implements IAssessmentAttemptDAO {

    private AssessmentAttempt mapRow(ResultSet rs) throws java.sql.SQLException {
        AssessmentAttempt a = new AssessmentAttempt();
        a.setId(rs.getInt("id"));
        a.setAssessmentId(rs.getInt("assessment_id"));
        a.setStudentId(rs.getInt("student_id"));
        String status = rs.getString("status");
        a.setStatus(status == null ? null : AssessmentAttempt.Status.valueOf(status));
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        a.setSubmittedAt(submittedAt == null ? null : submittedAt.toLocalDateTime());
        BigDecimal finalScore = rs.getBigDecimal("final_score");
        a.setFinalScore(finalScore == null ? null : finalScore.doubleValue());
        Timestamp createdAt = rs.getTimestamp("created_at");
        a.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        a.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return a;
    }

    @Override
    public List<AssessmentAttempt> findAll() {
        List<AssessmentAttempt> list = new ArrayList<>();
        String sql = "SELECT * FROM assessment_attempt ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find all assessment attempts.", e);
        }
        return list;
    }

    @Override
    public AssessmentAttempt findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT * FROM assessment_attempt WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find assessment attempt by ID.", e);
        }
    }

    @Override
    public AssessmentAttempt findByAssessmentAndStudent(int assessmentId, int studentId) {
        if (assessmentId <= 0 || studentId <= 0) {
            return null;
        }
        String sql = "SELECT * FROM assessment_attempt WHERE assessment_id = ? AND student_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assessmentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find attempt by assessment and student.", e);
        }
    }

    @Override
    public List<AssessmentAttempt> findByAssessmentId(int assessmentId) {
        if (assessmentId <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM assessment_attempt WHERE assessment_id = ? ORDER BY submitted_at DESC, created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AssessmentAttempt> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find attempts by assessment ID.", e);
        }
    }

    @Override
    public Integer insert(AssessmentAttempt attempt) {
        String sql = "INSERT INTO assessment_attempt (assessment_id, student_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'IN_PROGRESS', CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2), CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2))";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, attempt.getAssessmentId());
            ps.setInt(2, attempt.getStudentId());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to insert assessment attempt.", e);
        }
    }

    @Override
    public boolean update(AssessmentAttempt attempt) {
        String sql = "UPDATE assessment_attempt SET status = ?, submitted_at = ?, final_score = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, attempt.getStatus() == null ? "IN_PROGRESS" : attempt.getStatus().name());
            if (attempt.getSubmittedAt() != null) {
                ps.setTimestamp(2, Timestamp.valueOf(attempt.getSubmittedAt()));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }
            if (attempt.getFinalScore() != null) {
                ps.setBigDecimal(3, BigDecimal.valueOf(attempt.getFinalScore()));
            } else {
                ps.setNull(3, java.sql.Types.DECIMAL);
            }
            ps.setInt(4, attempt.getId());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update assessment attempt.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_attempt WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete assessment attempt.", e);
        }
    }

    @Override
    public boolean updateStatusAndScore(int id, AssessmentAttempt.Status status, BigDecimal finalScore, LocalDateTime submittedAt) {
        String sql = "UPDATE assessment_attempt SET status = ?, final_score = ?, submitted_at = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status == null ? "IN_PROGRESS" : status.name());
            if (finalScore != null) {
                ps.setBigDecimal(2, finalScore);
            } else {
                ps.setNull(2, java.sql.Types.DECIMAL);
            }
            if (submittedAt != null) {
                ps.setTimestamp(3, Timestamp.valueOf(submittedAt));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update attempt status and score.", e);
        }
    }

    @Override
    public boolean updateStatusAndScore(Connection conn, int id, AssessmentAttempt.Status expectedStatus, AssessmentAttempt.Status newStatus, BigDecimal finalScore, LocalDateTime submittedAt) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (id <= 0) {
            return false;
        }
        if (expectedStatus == null || newStatus == null) {
            throw new IllegalArgumentException("Expected status and new status must not be null.");
        }
        String sql = "UPDATE assessment_attempt SET status = ?, final_score = ?, submitted_at = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus == null ? "IN_PROGRESS" : newStatus.name());
            if (finalScore != null) {
                ps.setBigDecimal(2, finalScore);
            } else {
                ps.setNull(2, java.sql.Types.DECIMAL);
            }
            if (submittedAt != null) {
                ps.setTimestamp(3, Timestamp.valueOf(submittedAt));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }
            ps.setInt(4, id);
            ps.setString(5, expectedStatus.name());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update attempt status and score.", e);
        }
    }
}