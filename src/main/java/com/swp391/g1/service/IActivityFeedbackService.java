package com.swp391.g1.service;

import com.swp391.g1.dto.response.ActivityFeedbackAnswerDTO;
import com.swp391.g1.dto.response.ActivityFeedbackReportDTO;
import com.swp391.g1.dto.response.ActivityFeedbackSummaryDTO;
import com.swp391.g1.dto.response.FeedbackActivityDTO;

import java.util.List;

public interface IActivityFeedbackService {
    List<FeedbackActivityDTO> getStudentActivities(int studentId);
    int countPending(int studentId);
    ActivityFeedbackAnswerDTO getStudentFeedback(int studentId, int activityId);

    // Xử lý sinh viên gửi feedback hoạt động
    void submit(int studentId, int activityId, ActivityFeedbackAnswerDTO answer);
    List<ActivityFeedbackSummaryDTO> getActivitySummaries(Integer staffAccountId, String staffRole);
    ActivityFeedbackReportDTO getActivityReport(int activityId, Integer staffAccountId, String staffRole);

    ActivityFeedbackReportDTO getActivityReport(int activityId, Integer staffAccountId, String staffRole,
                                                String participationRole);
    int remindPendingStudents(int activityId, Integer staffAccountId, String staffRole);
}
