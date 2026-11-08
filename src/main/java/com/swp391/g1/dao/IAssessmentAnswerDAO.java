package com.swp391.g1.dao;

import com.swp391.g1.model.AssessmentAnswer;

import java.sql.Connection;
import java.util.List;

public interface IAssessmentAnswerDAO extends IGenericDAO<AssessmentAnswer, Integer> {
    List<AssessmentAnswer> findByAttemptId(int attemptId);
    AssessmentAnswer findById(Integer id);
    boolean updateGrade(int id, java.math.BigDecimal awardedScore, String feedback, AssessmentAnswer.GradingStatus gradingStatus);
    boolean deleteByAttemptId(int attemptId);

    /** Update essay grade using caller-provided Connection. Does not commit or close the Connection. */
    boolean updateGrade(Connection conn, int id, java.math.BigDecimal awardedScore, String feedback, AssessmentAnswer.GradingStatus gradingStatus);

    /** Find all answers for an attempt using caller-provided Connection. */
    List<AssessmentAnswer> findByAttemptId(Connection conn, int attemptId);

    /** Delete all answers for an attempt using the caller-provided Connection. Does not commit or close the Connection. */
    boolean deleteByAttemptId(Connection conn, int attemptId);

    /** Insert an answer using the caller-provided Connection. Does not commit or close the Connection. */
    boolean insert(Connection conn, AssessmentAnswer answer);
}
