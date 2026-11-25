package com.swp391.g1.dao;

import com.swp391.g1.dto.response.ActivityFeedbackAnswerDTO;
import com.swp391.g1.dto.response.ActivityFeedbackReportDTO;
import com.swp391.g1.dto.response.ActivityFeedbackSummaryDTO;
import com.swp391.g1.dto.response.FeedbackActivityDTO;

import java.util.List;

public interface IActivityFeedbackDAO {

    // Lấy danh sách hoạt động liên quan đến việc feedback của một sinh viên
    List<FeedbackActivityDTO> findStudentActivities(int studentId);

    // Đếm số hoạt động mà sinh viên đủ điều kiện feedback nhưng chưa gửi feedback
    int countPending(int studentId);

    // Lấy nội dung feedback mà một sinh viên đã gửi cho một hoạt động cụ thể
    ActivityFeedbackAnswerDTO findStudentFeedback(int studentId, int activityId);

    // Lưu feedback của sinh viên
    boolean submit(int studentId, int activityId, ActivityFeedbackAnswerDTO answer);

    // Lấy danh sách hoạt động đã hoàn thành kèm số liệu thống kê feedback cho Staff
    List<ActivityFeedbackSummaryDTO> findActivitySummaries(Integer staffAccountId, String staffRole);

    // Lấy báo cáo feedback chi tiết của một hoạt động cụ thể
    ActivityFeedbackReportDTO findActivityReport(int activityId, Integer staffAccountId, String staffRole);

    /** participationRole: PARTICIPANT, ORGANIZER hoặc null/rỗng để xem tất cả. */
    default ActivityFeedbackReportDTO findActivityReport(int activityId, Integer staffAccountId, String staffRole,
                                                         String participationRole) {
        return findActivityReport(activityId, staffAccountId, staffRole);
    }
}
