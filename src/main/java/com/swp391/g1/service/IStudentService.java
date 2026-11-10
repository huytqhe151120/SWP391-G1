package com.swp391.g1.service;

import com.swp391.g1.model.Student;

import java.util.List;

public interface IStudentService {
    List<Student> getAllStudents();

    List<Student> searchStudents(String keyword, String id, String code, String name, String classVal,
                                 String department, String major, String dob, String email,
                                 String phoneNumber, String gender, String status);

    Student getStudentById(int id);

    Student getStudentByAccountId(int accountId);

    boolean updateStudent(Student student);
}