package com.swp391.g1.dao;

import com.swp391.g1.model.Assessment;

public interface IAssessmentDAO extends IGenericDAO<Assessment, Integer> {
    Assessment findByActivityId(int activityId);
    boolean publish(int id, boolean published);
}
