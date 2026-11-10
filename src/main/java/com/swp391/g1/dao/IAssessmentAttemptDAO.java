package com.swp391.g1.dao;

import com.swp391.g1.model.AssessmentAttempt;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public interface IAssessmentAttemptDAO extends IGenericDAO<AssessmentAttempt, Integer> {
    AssessmentAttempt findById(Integer id);
    AssessmentAttempt findByAssessmentAndStudent(int assessmentId, int studentId);
    List<AssessmentAttempt> findByAssessmentId(int assessmentId);
    boolean updateStatusAndScore(int id, AssessmentAttempt.Status status, java.math.BigDecimal finalScore, LocalDateTime submittedAt);

    /** Conditional status transition using caller-provided Connection. Only updates if current status matches expectedStatus. Does not commit or close the Connection. */
    boolean updateStatusAndScore(Connection conn, int id, AssessmentAttempt.Status expectedStatus, AssessmentAttempt.Status newStatus, java.math.BigDecimal finalScore, LocalDateTime submittedAt);
}
