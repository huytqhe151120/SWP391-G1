package com.swp391.g1.service;

import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;

import java.util.List;

public interface IStudentGradeService {
    List<StudentGradeSummaryDTO> getAllStudentGradeSummaries();

    List<StudentGradeSummaryDTO> getAllStudentGradeSummaries(int semesterId);

    StudentGradeSummaryDTO getStudentGradeSummaryById(int studentId);

    StudentGradeSummaryDTO getStudentGradeSummaryById(int studentId, int semesterId);

    List<StudentActivityGradeDTO> getActivityGradesByStudentId(int studentId);

    List<StudentActivityGradeDTO> getActivityGradesByStudentId(int studentId, int semesterId);

    StudentActivityGradeDTO getActivityGradeDetail(int studentId, int activityId);
}
