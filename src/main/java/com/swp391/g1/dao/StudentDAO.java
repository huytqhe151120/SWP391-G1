package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO extends DBContext {

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setMainClassId(rs.getInt("main_class_id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setGender(rs.getString("gender"));
        s.setEmail(rs.getString("email"));
        s.setStatus(rs.getString("status"));
        s.setAccountId(rs.getInt("account_id"));
        try {
            s.setPhoneNumber(rs.getString("phone_number"));
        } catch (SQLException ignored) {}
        Timestamp dob = rs.getTimestamp("dob");
        if (dob != null) s.setDob(dob.toString().substring(0, 10));
        return s;
    }

    public Student getById(int id) {
        String sql = "SELECT * FROM [dbo].[student] WHERE id = ?";
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

    public List<Student> getAll() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM [dbo].[student] WHERE status = 'ACTIVE' ORDER BY name";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get student by account_id */
    public Student getByAccountId(int accountId) {
        String sql = "SELECT * FROM [dbo].[student] WHERE account_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
