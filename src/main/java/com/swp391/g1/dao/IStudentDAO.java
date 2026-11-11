package com.swp391.g1.dao;

import com.swp391.g1.model.Student;

import java.util.List;

public interface IStudentDAO extends IGenericDAO<Student, Integer> {
    List<Student> searchStudents(String keyword, String id, String code, String name, String classVal,
                                 String department, String major, String dob, String email,
                                 String phoneNumber, String gender, String status);

    Student findByAccountId(int accountId);

    boolean update(Student student);
}