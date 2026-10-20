package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IRegistrationFormDAO;
import com.swp391.g1.model.RegistrationForm;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegistrationFormDAOImpl implements IRegistrationFormDAO {
    private static final String SELECT = "SELECT f.*, a.name AS activity_name, " +
            "(SELECT COUNT(*) FROM dbo.activity_registration r WHERE r.form_id=f.id) AS registration_count " +
            "FROM dbo.activity_registration_form f JOIN dbo.extracurricular_activity a ON a.id=f.activity_id ";

    private RegistrationForm map(ResultSet rs) throws SQLException {
        RegistrationForm f = new RegistrationForm();
        f.setId(rs.getInt("id"));
        f.setActivityId(rs.getInt("activity_id"));
        f.setActivityName(rs.getString("activity_name"));
        f.setTitle(rs.getString("title"));
        f.setDescription(rs.getString("description"));
        Timestamp open = rs.getTimestamp("open_at");
        if (open != null) f.setOpenAt(open.toLocalDateTime());
        Timestamp close = rs.getTimestamp("close_at");
        if (close != null) f.setCloseAt(close.toLocalDateTime());
        f.setStatus(rs.getString("status"));
        f.setRegistrationCount(rs.getInt("registration_count"));
        return f;
    }

    @Override
    public List<RegistrationForm> findAll() {
        return query(SELECT + " ORDER BY f.created_at DESC", false);
    }

    @Override
    public List<RegistrationForm> findOpenForms() {
        return query(SELECT + " WHERE f.status='OPEN' AND (f.open_at IS NULL OR f.open_at <= SYSDATETIME()) " +
                "AND (f.close_at IS NULL OR f.close_at >= SYSDATETIME()) AND a.approval_status='APPROVED' ORDER BY f.created_at DESC", false);
    }

    private List<RegistrationForm> query(String sql, boolean ignored) {
        List<RegistrationForm> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) list.add(map(r));
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách form đăng ký.", e);
        }
        return list;
    }

    @Override
    public RegistrationForm findById(int id) {
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(SELECT + " WHERE f.id=?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải form đăng ký.", e);
        }
    }

    @Override
    public boolean insert(RegistrationForm f) {
        String sql = "INSERT INTO dbo.activity_registration_form(activity_id,title,description,open_at,close_at,status,created_at) VALUES(?,?,?,?,?,?,SYSDATETIME())";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, f.getActivityId());
            p.setString(2, f.getTitle());
            p.setString(3, f.getDescription());
            p.setTimestamp(4, f.getOpenAt() == null ? null : Timestamp.valueOf(f.getOpenAt()));
            p.setTimestamp(5, f.getCloseAt() == null ? null : Timestamp.valueOf(f.getCloseAt()));
            p.setString(6, f.getStatus());
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tạo form đăng ký.", e);
        }
    }

    @Override
    public boolean update(RegistrationForm f) {
        String sql = "UPDATE dbo.activity_registration_form SET activity_id=?,title=?,description=?,open_at=?,close_at=?,status=?,updated_at=SYSDATETIME() WHERE id=? AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration r WHERE r.form_id=?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, f.getActivityId());
            p.setString(2, f.getTitle());
            p.setString(3, f.getDescription());
            p.setTimestamp(4, f.getOpenAt() == null ? null : Timestamp.valueOf(f.getOpenAt()));
            p.setTimestamp(5, f.getCloseAt() == null ? null : Timestamp.valueOf(f.getCloseAt()));
            p.setString(6, f.getStatus());
            p.setInt(7, f.getId());
            p.setInt(8, f.getId());
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật form đăng ký.", e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM dbo.activity_registration_form WHERE id=? AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration r WHERE r.form_id=?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.setInt(2, id);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể xóa form đăng ký.", e);
        }
    }

    @Override
    public boolean hasStudentRegistered(int formId, int studentId) {
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement("SELECT 1 FROM dbo.activity_registration WHERE form_id=? AND student_id=?")) {
            p.setInt(1, formId);
            p.setInt(2, studentId);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra đăng ký.", e);
        }
    }

    @Override
    public boolean registerStudent(int formId, int studentId, String note) {
        String sql = "INSERT INTO dbo.activity_registration(form_id,student_id,note,status,submitted_at) SELECT f.id,?,?, 'SUBMITTED',SYSDATETIME() FROM dbo.activity_registration_form f JOIN dbo.extracurricular_activity a ON a.id=f.activity_id WHERE f.id=? AND f.status='OPEN' AND a.approval_status='APPROVED' AND (f.open_at IS NULL OR f.open_at<=SYSDATETIME()) AND (f.close_at IS NULL OR f.close_at>=SYSDATETIME()) AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration x WHERE x.form_id=f.id AND x.student_id=?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            p.setString(2, note);
            p.setInt(3, formId);
            p.setInt(4, studentId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể gửi đăng ký. Vui lòng kiểm tra form còn mở và bạn chưa đăng ký.", e);
        }
    }
}
