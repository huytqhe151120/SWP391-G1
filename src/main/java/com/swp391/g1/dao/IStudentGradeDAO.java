package com.swp391.g1.dao;

import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;

import java.util.List;

public interface IStudentGradeDAO {
    List<StudentGradeSummaryDTO> findAllStudentGradeSummaries();

    StudentGradeSummaryDTO findStudentGradeSummaryById(int studentId);

    List<StudentActivityGradeDTO> findActivityGradesByStudentId(int studentId);

    StudentActivityGradeDTO findActivityGradeDetail(int studentId, int activityId);
}
