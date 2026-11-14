package com.swp391.g1.dao;

import com.swp391.g1.dto.AssessmentListItem;
import com.swp391.g1.model.Assessment;

import java.util.List;

public interface IAssessmentDAO extends IGenericDAO<Assessment, Integer> {
    Assessment findByActivityId(int activityId);
    boolean publish(int id, boolean published);

    /** All assessments with owning activity name + attempt counters, for the staff management list. */
    List<AssessmentListItem> listForStaff();

    /** Published assessments of activities the student participates in, with the student's attempt status. */
    List<AssessmentListItem> listForStudent(int studentId);
}
