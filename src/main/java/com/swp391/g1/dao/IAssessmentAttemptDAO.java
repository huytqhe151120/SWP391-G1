package com.swp391.g1.dao;

import com.swp391.g1.model.AssessmentAttempt;

import java.time.LocalDateTime;
import java.util.List;

public interface IAssessmentAttemptDAO extends IGenericDAO<AssessmentAttempt, Integer> {
    AssessmentAttempt findById(Integer id);
    AssessmentAttempt findByAssessmentAndStudent(int assessmentId, int studentId);
    List<AssessmentAttempt> findByAssessmentId(int assessmentId);
    boolean updateStatusAndScore(int id, AssessmentAttempt.Status status, java.math.BigDecimal finalScore, LocalDateTime submittedAt);
}
