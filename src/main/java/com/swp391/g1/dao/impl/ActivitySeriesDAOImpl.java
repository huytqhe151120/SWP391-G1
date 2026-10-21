package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivitySeriesDAO;
import com.swp391.g1.model.ActivitySeries;
import com.swp391.g1.model.Enum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivitySeriesDAOImpl implements IActivitySeriesDAO {

    private ActivitySeries map(ResultSet rs) throws SQLException {
        ActivitySeries series = new ActivitySeries();
        series.setId(rs.getInt("id"));
        series.setSeriesCode(rs.getString("series_code"));
        series.setName(rs.getString("name"));
        series.setActivityTypeId(rs.getInt("activity_type_id"));
        series.setResponsibleDepartmentId(rs.getInt("responsible_department_id"));
        series.setResponsibleStaffId(rs.getInt("responsible_staff_id"));
        series.setDescription(rs.getString("description"));
        series.setRegistrationFormRequired(rs.getBoolean("registration_form_required"));
        String mode = rs.getString("recurrence_mode");
        series.setRecurrenceMode(mode == null ? null : Enum.RecurrenceMode.valueOf(mode));
        series.setRecurrenceInterval((Integer) rs.getObject("recurrence_interval"));
        Timestamp startDate = rs.getTimestamp("series_start_date");
        series.setSeriesStartDate(startDate == null ? null : startDate.toLocalDateTime().toLocalDate());
        Timestamp endDate = rs.getTimestamp("series_end_date");
        series.setSeriesEndDate(endDate == null ? null : endDate.toLocalDateTime().toLocalDate());
        series.setMaxOccurrences((Integer) rs.getObject("max_occurrences"));
        series.setOccurrenceDurationMinutes((Integer) rs.getObject("occurrence_duration_minutes"));
        Timestamp timeOfDay = rs.getTimestamp("time_of_day");
        series.setTimeOfDay(timeOfDay == null ? null : timeOfDay.toLocalDateTime().toLocalTime());
        series.setSemesterId((Integer) rs.getObject("semester_id"));
        String approvalStatus = rs.getString("approval_status");
        series.setApprovalStatus(approvalStatus == null ? null : Enum.ApprovalStatus.valueOf(approvalStatus));
        Timestamp createdAt = rs.getTimestamp("created_at");
        series.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        series.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return series;
    }

    @Override
    public List<ActivitySeries> findAll() {
        List<ActivitySeries> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_series";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all activity series.", e);
        }
        return list;
    }

    @Override
    public ActivitySeries findById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT * FROM activity_series WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find activity series by id.", e);
        }
    }

    @Override
    public Integer insert(ActivitySeries entity) {
        String sql = """
            INSERT INTO activity_series
                (series_code, name, activity_type_id, responsible_department_id, responsible_staff_id,
                 description, registration_form_required, recurrence_mode, recurrence_interval,
                 series_start_date, series_end_date, max_occurrences, occurrence_duration_minutes,
                 time_of_day, semester_id, approval_status, created_at, updated_at)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME(), SYSDATETIME())
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getSeriesCode());
            ps.setString(2, entity.getName());
            ps.setInt(3, entity.getActivityTypeId());
            ps.setInt(4, entity.getResponsibleDepartmentId());
            ps.setInt(5, entity.getResponsibleStaffId());
            ps.setString(6, entity.getDescription());
            ps.setBoolean(7, entity.isRegistrationFormRequired());
            ps.setString(8, entity.getRecurrenceMode() == null ? null : entity.getRecurrenceMode().name());
            if (entity.getRecurrenceInterval() == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, entity.getRecurrenceInterval());
            }
            ps.setTimestamp(10, Timestamp.valueOf(entity.getSeriesStartDate().atStartOfDay()));
            if (entity.getSeriesEndDate() == null) {
                ps.setNull(11, java.sql.Types.DATE);
            } else {
                ps.setTimestamp(11, Timestamp.valueOf(entity.getSeriesEndDate().atStartOfDay()));
            }
            if (entity.getMaxOccurrences() == null) {
                ps.setNull(12, java.sql.Types.INTEGER);
            } else {
                ps.setInt(12, entity.getMaxOccurrences());
            }
            if (entity.getOccurrenceDurationMinutes() == null) {
                ps.setNull(13, java.sql.Types.INTEGER);
            } else {
                ps.setInt(13, entity.getOccurrenceDurationMinutes());
            }
            ps.setTimestamp(14, Timestamp.valueOf(entity.getTimeOfDay().atDate(java.time.LocalDate.of(1900, 1, 1))));
            if (entity.getSemesterId() == null) {
                ps.setNull(15, java.sql.Types.INTEGER);
            } else {
                ps.setInt(15, entity.getSemesterId());
            }
            ps.setString(16, entity.getApprovalStatus() == null ? null : entity.getApprovalStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert activity series.", e);
        }
    }


    @Override
    public boolean update(ActivitySeries entity) {
        String sql = """
            UPDATE activity_series
            SET series_code = ?, name = ?, activity_type_id = ?, responsible_department_id = ?,
                responsible_staff_id = ?, description = ?, registration_form_required = ?,
                recurrence_mode = ?, recurrence_interval = ?, series_start_date = ?,
                series_end_date = ?, max_occurrences = ?, occurrence_duration_minutes = ?,
                time_of_day = ?, semester_id = ?, approval_status = ?, updated_at = SYSDATETIME()
            WHERE id = ?
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getSeriesCode());
            ps.setString(2, entity.getName());
            ps.setInt(3, entity.getActivityTypeId());
            ps.setInt(4, entity.getResponsibleDepartmentId());
            ps.setInt(5, entity.getResponsibleStaffId());
            ps.setString(6, entity.getDescription());
            ps.setBoolean(7, entity.isRegistrationFormRequired());
            ps.setString(8, entity.getRecurrenceMode() == null ? null : entity.getRecurrenceMode().name());
            if (entity.getRecurrenceInterval() == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, entity.getRecurrenceInterval());
            }
            ps.setTimestamp(10, Timestamp.valueOf(entity.getSeriesStartDate().atStartOfDay()));
            if (entity.getSeriesEndDate() == null) {
                ps.setNull(11, java.sql.Types.DATE);
            } else {
                ps.setTimestamp(11, Timestamp.valueOf(entity.getSeriesEndDate().atStartOfDay()));
            }
            if (entity.getMaxOccurrences() == null) {
                ps.setNull(12, java.sql.Types.INTEGER);
            } else {
                ps.setInt(12, entity.getMaxOccurrences());
            }
            if (entity.getOccurrenceDurationMinutes() == null) {
                ps.setNull(13, java.sql.Types.INTEGER);
            } else {
                ps.setInt(13, entity.getOccurrenceDurationMinutes());
            }
            ps.setTimestamp(14, Timestamp.valueOf(entity.getTimeOfDay().atDate(java.time.LocalDate.of(1900, 1, 1))));
            if (entity.getSemesterId() == null) {
                ps.setNull(15, java.sql.Types.INTEGER);
            } else {
                ps.setInt(15, entity.getSemesterId());
            }
            ps.setString(16, entity.getApprovalStatus() == null ? null : entity.getApprovalStatus().name());
            ps.setInt(17, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update activity series.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM activity_series WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete activity series.", e);
        }
    }


    @Override
    public ActivitySeries findBySeriesCode(String seriesCode) {
        if (seriesCode == null || seriesCode.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT * FROM activity_series WHERE series_code = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seriesCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find activity series by series code.", e);
        }
    }

    @Override
    public List<ActivitySeries> findByApprovalStatus(String approvalStatus) {
        List<ActivitySeries> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_series WHERE approval_status = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, approvalStatus);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find activity series by approval status.", e);
        }
        return list;
    }

    @Override
    public boolean existsBySeriesCode(String seriesCode) {
        if (seriesCode == null || seriesCode.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM activity_series WHERE series_code = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seriesCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check existence of activity series code.", e);
        }
    }


    public List<Integer> findOccurrenceIdsBySeries(Connection conn, int seriesId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM extracurricular_activity WHERE series_id = ? ORDER BY occurrence_no";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seriesId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        }
        return ids;
    }

    public Integer insert(Connection conn, ActivitySeries entity) throws SQLException {
        String sql = """
            INSERT INTO activity_series
                (series_code, name, activity_type_id, responsible_department_id, responsible_staff_id,
                 description, registration_form_required, recurrence_mode, recurrence_interval,
                 series_start_date, series_end_date, max_occurrences, occurrence_duration_minutes,
                 time_of_day, semester_id, approval_status, created_at, updated_at)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME(), SYSDATETIME())
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getSeriesCode());
            ps.setString(2, entity.getName());
            ps.setInt(3, entity.getActivityTypeId());
            ps.setInt(4, entity.getResponsibleDepartmentId());
            ps.setInt(5, entity.getResponsibleStaffId());
            ps.setString(6, entity.getDescription());
            ps.setBoolean(7, entity.isRegistrationFormRequired());
            ps.setString(8, entity.getRecurrenceMode() == null ? null : entity.getRecurrenceMode().name());
            if (entity.getRecurrenceInterval() == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, entity.getRecurrenceInterval());
            }
            ps.setTimestamp(10, Timestamp.valueOf(entity.getSeriesStartDate().atStartOfDay()));
            if (entity.getSeriesEndDate() == null) {
                ps.setNull(11, java.sql.Types.DATE);
            } else {
                ps.setTimestamp(11, Timestamp.valueOf(entity.getSeriesEndDate().atStartOfDay()));
            }
            if (entity.getMaxOccurrences() == null) {
                ps.setNull(12, java.sql.Types.INTEGER);
            } else {
                ps.setInt(12, entity.getMaxOccurrences());
            }
            if (entity.getOccurrenceDurationMinutes() == null) {
                ps.setNull(13, java.sql.Types.INTEGER);
            } else {
                ps.setInt(13, entity.getOccurrenceDurationMinutes());
            }
            ps.setTimestamp(14, Timestamp.valueOf(entity.getTimeOfDay().atDate(java.time.LocalDate.of(1900, 1, 1))));
            if (entity.getSemesterId() == null) {
                ps.setNull(15, java.sql.Types.INTEGER);
            } else {
                ps.setInt(15, entity.getSemesterId());
            }
            ps.setString(16, entity.getApprovalStatus() == null ? null : entity.getApprovalStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return null;
        }
    }

    @Override
    public int countOccurrencesBySeriesId(int seriesId) {
        String sql = "SELECT COUNT(*) FROM extracurricular_activity WHERE series_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seriesId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count occurrences by series id.", e);
        }
        return 0;
    }

    public Integer insertOccurrence(Connection conn,
                                    String code,
                                    String name,
                                    int activityTypeId,
                                    int responsibleDepartmentId,
                                    int responsibleStaffId,
                                    String description,
                                    boolean registrationFormRequired,
                                    Integer semesterId,
                                    java.time.LocalDateTime startTimeUtc,
                                    java.time.LocalDateTime endTimeUtc,
                                    int seriesId,
                                    int occurrenceNo,
                                    java.time.LocalDate occurrenceStartDate) throws SQLException {
        String sql = """
            INSERT INTO extracurricular_activity
                (code, name, activity_type_id, responsible_department_id, responsible_staff_id,
                 description, registration_form_required, activity_status, approval_status,
                 activity_type_id, start_time, end_time, series_id, occurrence_no,
                 occurrence_start_date, semester_id, approved_via_series, created_at)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?, 'UPCOMING', 'DRAFT', ?, ?, ?, ?, ?, ?, ?, 0, SYSDATETIME())
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, activityTypeId);
            ps.setInt(4, responsibleDepartmentId);
            ps.setInt(5, responsibleStaffId);
            ps.setString(6, description);
            ps.setBoolean(7, registrationFormRequired);
            ps.setInt(8, activityTypeId);
            ps.setTimestamp(9, Timestamp.valueOf(startTimeUtc));
            ps.setTimestamp(10, Timestamp.valueOf(endTimeUtc));
            ps.setInt(11, seriesId);
            ps.setInt(12, occurrenceNo);
            ps.setTimestamp(13, Timestamp.valueOf(occurrenceStartDate.atStartOfDay()));
            if (semesterId == null) {
                ps.setNull(14, java.sql.Types.INTEGER);
            } else {
                ps.setInt(14, semesterId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return null;
        }
    }

}
