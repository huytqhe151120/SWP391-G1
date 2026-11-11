package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IStudentDAO;
import com.swp391.g1.model.Student;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StudentDAOImpl implements IStudentDAO {

    private static final String SELECT_BASE = """
            SELECT s.id, s.main_class_id, s.account_id, mc.code AS mainClassCode,
                   d.name AS departmentName, m.name AS majorName,
                   s.code, s.name, s.dob, s.gender, s.email,
                   s.phone_number, s.status
            FROM student s
            LEFT JOIN main_class mc ON mc.id = s.main_class_id
            LEFT JOIN academic_program p ON p.id = mc.responsible_academic_program_id
            LEFT JOIN major m ON m.id = p.responsible_major_id
            LEFT JOIN department d ON d.id = m.responsible_department_id
            """;

    private Student mapStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId(rs.getInt("id"));
        int mainClassId = rs.getInt("main_class_id");
        student.setMainClassId(rs.wasNull() ? 0 : mainClassId);
        student.setAccountId(rs.getInt("account_id"));
        student.setMainClassCode(rs.getString("mainClassCode"));
        student.setDepartmentName(rs.getString("departmentName"));
        student.setMajorName(rs.getString("majorName"));
        student.setCode(rs.getString("code"));
        student.setName(rs.getString("name"));

        Date dob = rs.getDate("dob");
        student.setDob(dob == null ? null : dob.toString());

        boolean gender = rs.getBoolean("gender");
        student.setGender(rs.wasNull() ? null : Boolean.toString(gender));

        student.setEmail(rs.getString("email"));
        student.setPhoneNumber(rs.getString("phone_number"));
        student.setStatus(rs.getString("status"));
        return student;
    }

    @Override
    public List<Student> findAll() {
        List<Student> list = new ArrayList<>();
        String sql = SELECT_BASE + " ORDER BY s.code";

        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapStudent(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find all students.", e);
        }
        return list;
    }

    public List<Student> searchStudents(String keyword, String id, String code, String name, String classVal,
                                        String department, String major, String dob, String email,
                                        String phoneNumber, String gender, String status) {
        List<Student> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_BASE);
        List<String> conditions = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            conditions.add("(s.code LIKE ? OR s.name LIKE ? OR s.email LIKE ?)");
        }
        if (id != null && !id.trim().isEmpty()) {
            conditions.add("CAST(s.id AS VARCHAR(20)) LIKE ?");
        }
        if (code != null && !code.trim().isEmpty()) {
            conditions.add("s.code LIKE ?");
        }
        if (name != null && !name.trim().isEmpty()) {
            conditions.add("s.name LIKE ?");
        }
        if (classVal != null && !classVal.trim().isEmpty()) {
            conditions.add("mc.code = ?");
        }
        if (department != null && !department.trim().isEmpty()) {
            conditions.add("d.name LIKE ?");
        }
        if (major != null && !major.trim().isEmpty()) {
            conditions.add("m.name LIKE ?");
        }
        if (dob != null && !dob.trim().isEmpty()) {
            conditions.add("CONVERT(VARCHAR(10), s.dob, 23) LIKE ?");
        }
        if (email != null && !email.trim().isEmpty()) {
            conditions.add("s.email LIKE ?");
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            conditions.add("s.phone_number LIKE ?");
        }
        if (gender != null && !gender.trim().isEmpty()) {
            conditions.add("s.gender = ?");
        }
        if (status != null && !status.trim().isEmpty()) {
            conditions.add("s.status = ?");
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY s.code");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.trim() + "%";
                ps.setString(paramIndex++, searchPattern);
                ps.setString(paramIndex++, searchPattern);
                ps.setString(paramIndex++, searchPattern);
            }
            if (id != null && !id.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + id.trim() + "%");
            }
            if (code != null && !code.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + code.trim() + "%");
            }
            if (name != null && !name.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + name.trim() + "%");
            }
            if (classVal != null && !classVal.trim().isEmpty()) {
                ps.setString(paramIndex++, classVal.trim());
            }
            if (department != null && !department.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + department.trim() + "%");
            }
            if (major != null && !major.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + major.trim() + "%");
            }
            if (dob != null && !dob.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + dob.trim() + "%");
            }
            if (email != null && !email.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + email.trim() + "%");
            }
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + phoneNumber.trim() + "%");
            }
            if (gender != null && !gender.trim().isEmpty()) {
                ps.setString(paramIndex++, gender.trim());
            }
            if (status != null && !status.trim().isEmpty()) {
                ps.setString(paramIndex++, status.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStudent(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to search students.", e);
        }
        return list;
    }

    @Override
    public Student findById(Integer id) {
        String sql = SELECT_BASE + " WHERE s.id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapStudent(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find student by ID.", e);
        }
    }

    @Override
    public Student findByAccountId(int accountId) {
        if (accountId <= 0) {
            return null;
        }
        String sql = SELECT_BASE + " WHERE s.account_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapStudent(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find student by account ID.", e);
        }
    }

    @Override
    public Integer insert(Student student) {
        if (student == null) {
            return null;
        }
        String sql = "INSERT INTO student "
                + "(main_class_id, account_id, code, name, dob, gender, email, phone_number, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInteger(ps, 1, student.getMainClassId());
            setNullableInteger(ps, 2, student.getAccountId());
            ps.setString(3, student.getCode());
            ps.setString(4, student.getName());
            setDate(ps, 5, student.getDob());
            setGender(ps, 6, student.getGender());
            ps.setString(7, student.getEmail());
            ps.setString(8, student.getPhoneNumber());
            ps.setString(9, student.getStatus());

            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert student.", e);
        }
    }

    @Override
    public boolean update(Student student) {
        if (student == null || student.getId() <= 0) {
            return false;
        }
        String sql = "UPDATE student SET code = ?, name = ?, dob = ?, gender = ?, email = ?, "
                + "phone_number = ?, status = ? WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getCode());
            ps.setString(2, student.getName());
            setDate(ps, 3, student.getDob());
            setGender(ps, 4, student.getGender());
            ps.setString(5, student.getEmail());
            ps.setString(6, student.getPhoneNumber());
            ps.setString(7, student.getStatus());
            ps.setInt(8, student.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update student.", e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }

        String sql = "DELETE FROM student WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete student.", e);
        }
    }

    private static void setNullableInteger(PreparedStatement statement, int parameterIndex, int value)
            throws SQLException {
        if (value <= 0) {
            statement.setNull(parameterIndex, java.sql.Types.INTEGER);
        } else {
            statement.setInt(parameterIndex, value);
        }
    }

    private static void setDate(PreparedStatement statement, int parameterIndex, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(parameterIndex, java.sql.Types.DATE);
        } else {
            statement.setDate(parameterIndex, Date.valueOf(value));
        }
    }

    private static void setGender(PreparedStatement statement, int parameterIndex, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(parameterIndex, java.sql.Types.BIT);
        } else if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            statement.setBoolean(parameterIndex, true);
        } else if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            statement.setBoolean(parameterIndex, false);
        } else {
            throw new IllegalArgumentException("Student gender must be true, false, 1, or 0.");
        }
    }
}