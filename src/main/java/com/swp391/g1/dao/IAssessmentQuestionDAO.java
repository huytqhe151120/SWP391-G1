package com.swp391.g1.dao;

import com.swp391.g1.model.AssessmentQuestion;

import java.sql.Connection;
import java.util.List;

public interface IAssessmentQuestionDAO extends IGenericDAO<AssessmentQuestion, Integer> {
    List<AssessmentQuestion> findByAssessmentId(int assessmentId);
    AssessmentQuestion findById(Integer id);
    boolean deleteByAssessmentId(int assessmentId);

    /** Find questions by assessment using caller-provided Connection. */
    List<AssessmentQuestion> findByAssessmentId(Connection conn, int assessmentId);
}
