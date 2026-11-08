package com.swp391.g1.dao;

import com.swp391.g1.model.AssessmentOption;

import java.util.List;

public interface IAssessmentOptionDAO extends IGenericDAO<AssessmentOption, Integer> {
    List<AssessmentOption> findByQuestionId(int questionId);
    AssessmentOption findById(Integer id);
    boolean deleteByQuestionId(int questionId);

    /** Find option by ID using caller-provided Connection. */
    AssessmentOption findById(Connection conn, int id);
}
