package com.swp391.g1.dao;

import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;

import java.util.List;

public interface IStudentGradeDAO {
    List<StudentGradeSummaryDTO> findAllStudentGradeSummaries();

    List<StudentGradeSummaryDTO> findAllStudentGradeSummaries(int semesterId);

    StudentGradeSummaryDTO findStudentGradeSummaryById(int studentId);

    StudentGradeSummaryDTO findStudentGradeSummaryById(int studentId, int semesterId);

    List<StudentActivityGradeDTO> findActivityGradesByStudentId(int studentId);

    List<StudentActivityGradeDTO> findActivityGradesByStudentId(int studentId, int semesterId);

    StudentActivityGradeDTO findActivityGradeDetail(int studentId, int activityId);
}
