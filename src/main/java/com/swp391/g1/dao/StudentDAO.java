package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Student;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO extends DBContext {

    public List<Student> getAll() throws SQLException {
        String sql = """
                SELECT s.id, s.main_class_id, mc.code AS mainClassCode,
                       s.code, s.name, s.dob, s.gender, s.email,
                       s.phone_number, s.status
                FROM student s
                LEFT JOIN main_class mc ON mc.id = s.main_class_id
                ORDER BY s.code
                """;

        List<Student> students = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                students.add(mapRow(resultSet));
            }
        }

        return students;
    }

    private Student mapRow(ResultSet resultSet) throws SQLException {
        Student student = new Student();

        student.setId(resultSet.getInt("id"));

        int mainClassId = resultSet.getInt("main_class_id");
        student.setMainClassId(resultSet.wasNull() ? null : mainClassId);

        student.setMainClassCode(resultSet.getString("mainClassCode"));
        student.setCode(resultSet.getString("code"));
        student.setName(resultSet.getString("name"));

        Date dob = resultSet.getDate("dob");
        student.setDob(dob == null ? null : dob.toLocalDate());

        boolean gender = resultSet.getBoolean("gender");
        student.setGender(resultSet.wasNull() ? null : gender);

        student.setEmail(resultSet.getString("email"));
        student.setPhoneNumber(resultSet.getString("phone_number"));
        student.setStatus(resultSet.getString("status"));

        return student;
    }
}
