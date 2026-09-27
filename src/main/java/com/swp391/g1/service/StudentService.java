package com.swp391.g1.service;

import com.swp391.g1.dao.StudentDAO;
import com.swp391.g1.model.Student;

import java.sql.SQLException;
import java.util.List;

public class StudentService {

    private final StudentDAO studentDAO = new StudentDAO();

    public List<Student> getAll() throws SQLException {
        return studentDAO.getAll();
    }
}
