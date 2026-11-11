package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IAssessmentDAO;
import com.swp391.g1.model.Assessment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AssessmentDAOImpl implements IAssessmentDAO {

    private Assessment mapRow(ResultSet rs) throws java.sql.SQLException {
        Assessment a = new Assessment();
        a.setId(rs.getInt("id"));
        a.setActivityId(rs.getInt("activity_id"));
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        Timestamp ot = rs.getTimestamp("open_time");
        a.setOpenTime(ot == null ? null : ot.toLocalDateTime());
        Timestamp ct = rs.getTimestamp("close_time");
        a.setCloseTime(ct == null ? null : ct.toLocalDateTime());
        a.setPublished(rs.getBoolean("is_published"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        a.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        a.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return a;
    }

    @Override
    public List<Assessment> findAll() {
        List<Assessment> list = new ArrayList<>();
        String sql = "SELECT * FROM assessment ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find all assessments.", e);
        }
        return list;
    }

    @Override
    public Assessment findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT * FROM assessment WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find assessment by ID.", e);
        }
    }

    @Override
    public Assessment findByActivityId(int activityId) {
        if (activityId <= 0) {
            return null;
        }
        String sql = "SELECT * FROM assessment WHERE activity_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to find assessment by activity ID.", e);
        }
    }
    @Override
    public Integer insert(Assessment assessment) {
        String sql = "INSERT INTO assessment (activity_id, title, description, open_time, close_time, is_published, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2), CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2))";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, assessment.getActivityId());
            ps.setString(2, assessment.getTitle());
            ps.setString(3, assessment.getDescription());
            if (assessment.getOpenTime() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(assessment.getOpenTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            if (assessment.getCloseTime() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(assessment.getCloseTime()));
            } else {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            }
            ps.setBoolean(6, assessment.isPublished());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to insert assessment.", e);
        }
    }

    @Override
    public boolean update(Assessment assessment) {
        String sql = "UPDATE assessment SET title = ?, description = ?, open_time = ?, close_time = ?, is_published = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assessment.getTitle());
            ps.setString(2, assessment.getDescription());
            if (assessment.getOpenTime() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(assessment.getOpenTime()));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }
            if (assessment.getCloseTime() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(assessment.getCloseTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            ps.setBoolean(5, assessment.isPublished());
            ps.setInt(6, assessment.getId());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to update assessment.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to delete assessment.", e);
        }
    }

    @Override
    public boolean publish(int id, boolean published) {
        String sql = "UPDATE assessment SET is_published = ?, updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, published);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to publish assessment.", e);
        }
    }
}