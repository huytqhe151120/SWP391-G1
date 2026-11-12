package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityFeedbackDAO;
import com.swp391.g1.dao.INotificationDAO;
import com.swp391.g1.dao.impl.ActivityFeedbackDAOImpl;
import com.swp391.g1.dao.impl.NotificationDAOImpl;
import com.swp391.g1.dto.response.ActivityFeedbackAnswerDTO;
import com.swp391.g1.dto.response.ActivityFeedbackReportDTO;
import com.swp391.g1.dto.response.ActivityFeedbackSummaryDTO;
import com.swp391.g1.dto.response.FeedbackActivityDTO;
import com.swp391.g1.service.IActivityFeedbackService;
import com.swp391.g1.service.ActivityFeedbackUnavailableException;

import java.util.List;

public class ActivityFeedbackServiceImpl implements IActivityFeedbackService {
    private static final int MIN_ANSWER_LENGTH = 5;
    private static final int MAX_ANSWER_LENGTH = 500;
    private final IActivityFeedbackDAO feedbackDAO;
    private final INotificationDAO notificationDAO;

    public ActivityFeedbackServiceImpl() {
        this(new ActivityFeedbackDAOImpl(), new NotificationDAOImpl());
    }

    public ActivityFeedbackServiceImpl(IActivityFeedbackDAO feedbackDAO) {
        this(feedbackDAO, new NotificationDAOImpl());
    }

    public ActivityFeedbackServiceImpl(IActivityFeedbackDAO feedbackDAO, INotificationDAO notificationDAO) {
        this.feedbackDAO = feedbackDAO;
        this.notificationDAO = notificationDAO;
    }

    @Override
    public List<FeedbackActivityDTO> getStudentActivities(int studentId) {
        requirePositiveId(studentId, "Sinh viên");
        return feedbackDAO.findStudentActivities(studentId);
    }

    @Override
    public int countPending(int studentId) {
        requirePositiveId(studentId, "Sinh viên");
        return feedbackDAO.countPending(studentId);
    }

    @Override
    public ActivityFeedbackAnswerDTO getStudentFeedback(int studentId, int activityId) {
        requirePositiveId(studentId, "Sinh viên");
        requirePositiveId(activityId, "Hoạt động");
        return feedbackDAO.findStudentFeedback(studentId, activityId);
    }

    @Override
    public void submit(int studentId, int activityId, ActivityFeedbackAnswerDTO answer) {
        requirePositiveId(studentId, "Sinh viên");
        requirePositiveId(activityId, "Hoạt động");
        if (answer == null) {
            throw new IllegalArgumentException("Nội dung feedback không hợp lệ.");
        }
        validateRating(answer.getGeneralRating(), "Đánh giá chung");
        validateRating(answer.getContentRating(), "Nội dung hoạt động");
        validateRating(answer.getOrganizationRating(), "Công tác tổ chức");
        validateRating(answer.getVenueRating(), "Địa điểm và thời gian");
        answer.setBestPart(validateRequiredText(answer.getBestPart(), "Điều bạn hài lòng nhất"));
        answer.setImprovement(validateRequiredText(answer.getImprovement(), "Điều cần cải thiện"));
        answer.setFavorite(validateRequiredText(answer.getFavorite(), "Điều bạn yêu thích"));
        answer.setSuggestions(validateRequiredText(answer.getSuggestions(), "Đề xuất cho hoạt động sau"));
        answer.setNote(validateOptionalText(answer.getNote(), "Ghi chú"));
        if (!feedbackDAO.submit(studentId, activityId, answer)) {
            throw new ActivityFeedbackUnavailableException(
                    "Bạn chưa đủ điều kiện gửi feedback hoặc feedback đã được gửi trước đó.");
        }
    }

    @Override
    public List<ActivityFeedbackSummaryDTO> getActivitySummaries(Integer staffAccountId, String staffRole) {
        return feedbackDAO.findActivitySummaries(staffAccountId, staffRole);
    }

    @Override
    public ActivityFeedbackReportDTO getActivityReport(int activityId, Integer staffAccountId, String staffRole) {
        requirePositiveId(activityId, "Hoạt động");
        return feedbackDAO.findActivityReport(activityId, staffAccountId, staffRole);
    }

    @Override
    public int remindPendingStudents(int activityId, Integer staffAccountId, String staffRole) {
        requirePositiveId(activityId, "Hoạt động");
        ActivityFeedbackReportDTO report = feedbackDAO.findActivityReport(activityId, staffAccountId, staffRole);
        if (report == null) {
            throw new ActivityFeedbackUnavailableException("Không tìm thấy hoạt động trong phạm vi quản lý của bạn.");
        }
        if (report.getSummary().getPendingCount() == 0) {
            throw new ActivityFeedbackUnavailableException("Không còn sinh viên nào cần nhắc feedback.");
        }
        int notifiedCount = notificationDAO.createActivityFeedbackReminder(activityId);
        if (notifiedCount == 0) {
            throw new ActivityFeedbackUnavailableException("Không còn sinh viên nào cần nhắc feedback.");
        }
        return notifiedCount;
    }

    private static String validateRequiredText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " không được để trống.");
        }
        String trimmed = value.strip();
        if (trimmed.length() < MIN_ANSWER_LENGTH) {
            throw new IllegalArgumentException(label + " phải có ít nhất 5 ký tự.");
        }
        if (trimmed.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException(label + " không được vượt quá 500 ký tự.");
        }
        return trimmed;
    }

    private static String validateOptionalText(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException(label + " không được vượt quá 500 ký tự.");
        }
        return trimmed;
    }

    private static void validateRating(int rating, String label) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(label + " phải từ 1 đến 5 sao.");
        }
    }

    private static void requirePositiveId(int id, String label) {
        if (id <= 0) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }
}
