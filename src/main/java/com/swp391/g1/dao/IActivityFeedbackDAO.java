package com.swp391.g1.dao;

import com.swp391.g1.dto.response.ActivityFeedbackAnswerDTO;
import com.swp391.g1.dto.response.ActivityFeedbackReportDTO;
import com.swp391.g1.dto.response.ActivityFeedbackSummaryDTO;
import com.swp391.g1.dto.response.FeedbackActivityDTO;

import java.util.List;

public interface IActivityFeedbackDAO {
    List<FeedbackActivityDTO> findStudentActivities(int studentId);
    int countPending(int studentId);
    ActivityFeedbackAnswerDTO findStudentFeedback(int studentId, int activityId);
    boolean submit(int studentId, int activityId, ActivityFeedbackAnswerDTO answer);
    List<ActivityFeedbackSummaryDTO> findActivitySummaries(Integer staffAccountId, String staffRole);
    ActivityFeedbackReportDTO findActivityReport(int activityId, Integer staffAccountId, String staffRole);
}
