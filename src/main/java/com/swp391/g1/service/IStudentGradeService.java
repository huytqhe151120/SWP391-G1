package com.swp391.g1.service;

import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;

import java.util.List;

public interface IStudentGradeService {
    List<StudentGradeSummaryDTO> getAllStudentGradeSummaries();

    StudentGradeSummaryDTO getStudentGradeSummaryById(int studentId);

    List<StudentActivityGradeDTO> getActivityGradesByStudentId(int studentId);

    StudentActivityGradeDTO getActivityGradeDetail(int studentId, int activityId);
}
