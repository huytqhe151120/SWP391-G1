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

    @Override
    public List<Student> searchStudents(String keyword, String id, String code, String name, String classVal,
                                        String department, String major, String dob, String email,
                                        String phoneNumber, String gender, String status) {
        return studentDAO.searchStudents(keyword, id, code, name, classVal, department, major,
                dob, email, phoneNumber, gender, status);
    }

    @Override
    public Student getStudentById(int id) {
        return id <= 0 ? null : studentDAO.findById(id);
    }

    @Override
    public Student getStudentByAccountId(int accountId) {
        return accountId <= 0 ? null : studentDAO.findByAccountId(accountId);
    }

    @Override
    public boolean updateStudent(Student student) {
        if (student == null || student.getId() <= 0
                || student.getCode() == null || student.getCode().isBlank()
                || student.getName() == null || student.getName().isBlank()) {
            throw new IllegalArgumentException("Mã sinh viên và họ tên không được để trống.");
        }
        if (student.getCode().trim().length() > 50) {
            throw new IllegalArgumentException("Mã sinh viên không được vượt quá 50 ký tự.");
        }
        if (student.getName().trim().length() > 255) {
            throw new IllegalArgumentException("Họ tên không được vượt quá 255 ký tự.");
        }
        if (student.getDob() != null && !student.getDob().isBlank()) {
            try {
                java.time.LocalDate.parse(student.getDob());
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("Ngày sinh phải theo định dạng yyyy-MM-dd.", e);
            }
        }
        if (student.getGender() != null && !student.getGender().isBlank()
                && !"true".equalsIgnoreCase(student.getGender())
                && !"false".equalsIgnoreCase(student.getGender())) {
            throw new IllegalArgumentException("Giới tính không hợp lệ.");
        }
        if (student.getEmail() != null && student.getEmail().length() > 255) {
            throw new IllegalArgumentException("Email không được vượt quá 255 ký tự.");
        }
        if (student.getEmail() != null && !student.getEmail().isBlank()
                && !student.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }
        if (student.getPhoneNumber() != null && student.getPhoneNumber().length() > 30) {
            throw new IllegalArgumentException("Số điện thoại không được vượt quá 30 ký tự.");
        }
        if (!"ACTIVE".equals(student.getStatus()) && !"INACTIVE".equals(student.getStatus())) {
            throw new IllegalArgumentException("Trạng thái sinh viên không hợp lệ.");
        }
        return studentDAO.update(student);
    }
}