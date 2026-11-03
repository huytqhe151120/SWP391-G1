package com.swp391.g1.dao;

import com.swp391.g1.model.AssessmentAnswer;

import java.util.List;

public interface IAssessmentAnswerDAO extends IGenericDAO<AssessmentAnswer, Integer> {
    List<AssessmentAnswer> findByAttemptId(int attemptId);
    AssessmentAnswer findById(Integer id);
    boolean updateGrade(int id, java.math.BigDecimal awardedScore, String feedback, AssessmentAnswer.GradingStatus gradingStatus);
    boolean deleteByAttemptId(int attemptId);
}
