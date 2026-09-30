package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IStudentDAO;
import com.swp391.g1.dao.impl.StudentDAOImpl;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.IStudentService;

import java.util.List;

public class StudentServiceImpl implements IStudentService {

    private final IStudentDAO studentDAO;

    public StudentServiceImpl() {
        this.studentDAO = new StudentDAOImpl();
    }

    @Override
    public List<Student> getAllStudents() {
        return studentDAO.findAll();
    }
}