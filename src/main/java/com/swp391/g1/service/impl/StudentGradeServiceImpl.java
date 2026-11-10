package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IStudentGradeDAO;
import com.swp391.g1.dao.impl.StudentGradeDAOImpl;
import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;
import com.swp391.g1.service.IStudentGradeService;

import java.util.List;

public class StudentGradeServiceImpl implements IStudentGradeService {

    private final IStudentGradeDAO studentGradeDAO;

    public StudentGradeServiceImpl() {
        this.studentGradeDAO = new StudentGradeDAOImpl();
    }

    @Override
    public List<StudentGradeSummaryDTO> getAllStudentGradeSummaries() {
        return studentGradeDAO.findAllStudentGradeSummaries();
    }

    @Override
    public StudentGradeSummaryDTO getStudentGradeSummaryById(int studentId) {
        return studentGradeDAO.findStudentGradeSummaryById(studentId);
    }

    @Override
    public List<StudentActivityGradeDTO> getActivityGradesByStudentId(int studentId) {
        return studentGradeDAO.findActivityGradesByStudentId(studentId);
    }

    @Override
    public StudentActivityGradeDTO getActivityGradeDetail(int studentId, int activityId) {
        return studentGradeDAO.findActivityGradeDetail(studentId, activityId);
    }
}
