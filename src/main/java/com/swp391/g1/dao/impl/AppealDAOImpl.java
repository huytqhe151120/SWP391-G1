package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IAppealDAO;
import com.swp391.g1.model.Appeal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of IAppealDAO using SQL Server (dbo.appeal).
 */
public class AppealDAOImpl implements IAppealDAO {

    // ── Common JOIN query ────────────────────────────────────────────────────
    private static final String BASE_SELECT =
            "SELECT a.*, " +
            "       s.name  AS student_name,  s.code  AS student_code, " +
            "       ea.name AS activity_name, ea.code AS activity_code, " +
            "       st.name AS reviewer_name " +
            "FROM   [dbo].[appeal] a " +
            "       LEFT JOIN [dbo].[student]                  s  ON a.student_id  = s.id  " +
            "       LEFT JOIN [dbo].[extracurricular_activity] ea ON a.activity_id = ea.id " +
            "       LEFT JOIN [dbo].[staff]                    st ON a.reviewed_by = st.id ";

    // ── Row mapper ───────────────────────────────────────────────────────────
    private Appeal mapRow(ResultSet rs) throws SQLException {
        Appeal ap = new Appeal();
        ap.setId(rs.getInt("id"));
        ap.setStudentId(rs.getInt("student_id"));
        ap.setActivityId(rs.getInt("activity_id"));
        ap.setReason(rs.getString("reason"));
        ap.setStatus(rs.getString("status"));

        int reviewedBy = rs.getInt("reviewed_by");
        ap.setReviewedBy(rs.wasNull() ? null : reviewedBy);

        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) ap.setReviewedAt(reviewedAt.toLocalDateTime());

        ap.setOldPoint(rs.getBigDecimal("old_point"));
        ap.setNewPoint(rs.getBigDecimal("new_point"));
        ap.setReviewNote(rs.getString("review_note"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) ap.setCreatedAt(createdAt.toLocalDateTime());

        // JOIN fields — swallow if column absent
        try { ap.setStudentName(rs.getString("student_name")); }  catch (SQLException ignored) {}
        try { ap.setStudentCode(rs.getString("student_code")); }  catch (SQLException ignored) {}
        try { ap.setActivityName(rs.getString("activity_name")); } catch (SQLException ignored) {}
        try { ap.setActivityCode(rs.getString("activity_code")); } catch (SQLException ignored) {}
        try { ap.setReviewerName(rs.getString("reviewer_name")); } catch (SQLException ignored) {}
        return ap;
    }

    // ── IAppealDAO ────────────────────────────────────────────────────────────

    @Override
    public boolean insert(Appeal appeal) {
        String sql = "INSERT INTO [dbo].[appeal] " +
                     "(student_id, activity_id, reason, status, old_point, created_at) " +
                     "SELECT ?, ?, ?, 'PENDING', r.final_point, " +
                     "CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) " +
                     "FROM [dbo].[student_activity_result] r " +
                     "JOIN [dbo].[extracurricular_activity] ea ON ea.id = r.activity_id " +
                     "WHERE r.student_id = ? AND r.activity_id = ? " +
                     "AND r.final_point IS NOT NULL AND ea.activity_status = 'COMPLETED' " +
                     "AND NOT EXISTS (SELECT 1 FROM [dbo].[appeal] existing " +
                     "WHERE existing.student_id = ? AND existing.activity_id = ? " +
                     "AND existing.status = 'PENDING')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appeal.getStudentId());
            ps.setInt(2, appeal.getActivityId());
            ps.setString(3, appeal.getReason());
            ps.setInt(4, appeal.getStudentId());
            ps.setInt(5, appeal.getActivityId());
            ps.setInt(6, appeal.getStudentId());
            ps.setInt(7, appeal.getActivityId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                return false;
            }
            throw new IllegalStateException("Failed to insert appeal.", e);
        }
    }

    @Override
    public List<Appeal> getAll() {
        return query(BASE_SELECT + "ORDER BY a.created_at DESC");
    }

    @Override
    public List<Appeal> getPending() {
        return query(BASE_SELECT + "WHERE a.status = 'PENDING' ORDER BY a.created_at ASC");
    }

    @Override
    public List<Appeal> getByStudentId(int studentId) {
        String sql = BASE_SELECT + "WHERE a.student_id = ? ORDER BY a.created_at DESC";
        return queryWithInt(sql, studentId);
    }

    @Override
    public List<Appeal> getByActivityId(int activityId) {
        String sql = BASE_SELECT + "WHERE a.activity_id = ? ORDER BY a.created_at DESC";
        return queryWithInt(sql, activityId);
    }

    @Override
    public List<Appeal> getEligibleActivities(int studentId) {
        String sql = "SELECT 0 AS id, r.student_id, r.activity_id, '' AS reason, " +
                "'ELIGIBLE' AS status, NULL AS reviewed_by, NULL AS reviewed_at, " +
                "r.final_point AS old_point, NULL AS new_point, NULL AS review_note, " +
                "NULL AS created_at, s.name AS student_name, s.code AS student_code, " +
                "ea.name AS activity_name, ea.code AS activity_code, NULL AS reviewer_name " +
                "FROM [dbo].[student_activity_result] r " +
                "JOIN [dbo].[student] s ON s.id = r.student_id " +
                "JOIN [dbo].[extracurricular_activity] ea ON ea.id = r.activity_id " +
                "WHERE r.student_id = ? AND r.final_point IS NOT NULL " +
                "AND ea.activity_status = 'COMPLETED' " +
                "ORDER BY ea.end_time DESC";
        return queryWithInt(sql, studentId);
    }

    @Override
    public Appeal getById(int id) {
        String sql = BASE_SELECT + "WHERE a.id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find appeal by ID.", e);
        }
        return null;
    }

    @Override
    public boolean approve(int appealId, int staffId, BigDecimal newPoint, String reviewNote) {
        String lockSql = "SELECT student_id, activity_id FROM [dbo].[appeal] WITH (UPDLOCK, ROWLOCK) " +
                "WHERE id = ? AND status = 'PENDING'";
        String updatePointSql = "UPDATE [dbo].[student_activity_result] SET final_point = ?, " +
                "updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) " +
                "WHERE student_id = ? AND activity_id = ?";
        String updateAppealSql = "UPDATE [dbo].[appeal] " +
                     "SET status = 'APPROVED', " +
                     "    reviewed_by  = ?, " +
                     "    reviewed_at  = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2), " +
                     "    new_point    = ?, " +
                     "    review_note  = ? " +
                     "WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int studentId;
                int activityId;
                try (PreparedStatement lock = conn.prepareStatement(lockSql)) {
                    lock.setInt(1, appealId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        studentId = rs.getInt("student_id");
                        activityId = rs.getInt("activity_id");
                    }
                }
                try (PreparedStatement point = conn.prepareStatement(updatePointSql)) {
                    point.setBigDecimal(1, newPoint);
                    point.setInt(2, studentId);
                    point.setInt(3, activityId);
                    if (point.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                try (PreparedStatement appeal = conn.prepareStatement(updateAppealSql)) {
                    appeal.setInt(1, staffId);
                    appeal.setBigDecimal(2, newPoint);
                    appeal.setString(3, reviewNote);
                    appeal.setInt(4, appealId);
                    if (appeal.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to approve appeal.", e);
        }
    }

    @Override
    public boolean reject(int appealId, int staffId, String reviewNote) {
        String sql = "UPDATE [dbo].[appeal] " +
                     "SET status = 'REJECTED', " +
                     "    reviewed_by  = ?, " +
                     "    reviewed_at  = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2), " +
                     "    review_note  = ? " +
                     "WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setString(2, reviewNote);
            ps.setInt(3, appealId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reject appeal.", e);
        }
    }

    @Override
    public boolean hasPendingAppeal(int studentId, int activityId) {
        String sql = "SELECT COUNT(*) FROM [dbo].[appeal] " +
                     "WHERE student_id = ? AND activity_id = ? AND status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check pending appeal.", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<Appeal> query(String sql) {
        List<Appeal> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query appeals.", e);
        }
        return list;
    }

    private List<Appeal> queryWithInt(String sql, int param) {
        List<Appeal> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query appeals with param.", e);
        }
        return list;
    }
}
