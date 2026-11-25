package com.swp391.g1.service;

import com.swp391.g1.model.RecruitmentApplication;
import com.swp391.g1.model.RecruitmentInterview;
import com.swp391.g1.model.RecruitmentRound;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IRecruitmentService {

    List<RecruitmentRound> getRounds();

    RecruitmentRound getRound(int id);

    RecruitmentRound getRoundByActivity(int activityId);

    List<RecruitmentRound> getSelectableActivities(Integer includeActivityId);

    LocalDateTime getParticipationOpenAt(int activityId);

    // Kiểm tra và lưu đợt tuyển
    String saveRound(RecruitmentRound round, int accountId);

    boolean isStructureLocked(int roundId);

    String deleteRound(int id);

    List<RecruitmentRound> getOpenRounds();

    List<RecruitmentApplication> getStudentApplications(int studentId);

    RecruitmentApplication getStudentApplication(int applicationId, int studentId);

    // Hồ sơ đang còn hiệu lực (chưa rút) của sinh viên trong đợt
    RecruitmentApplication getActiveApplication(int roundId, int studentId);

    boolean hasActiveParticipation(int activityId, int studentId);

    // Sinh viên nộp hồ sơ ứng tuyển
    String apply(int roundId, int studentId, Integer choice1, Integer choice2,
                 Map<Integer, List<String>> answers, String note);

    // Rút hồ sơ
    String withdraw(int applicationId, int studentId, String reason);

    // Xác nhận phỏng vấn
    String confirmInterview(int applicationId, int studentId);

    // Sinh viên đã trúng tuyển ban tổ chức ở hoạt động (không được đăng ký tham gia).
    boolean hasAcceptedApplication(int activityId, int studentId);

    // Xét hồ sơ (staff/admin)
    List<RecruitmentApplication> getApplications(int roundId, String statusFilter, Integer positionId);

    RecruitmentApplication getApplication(int applicationId);

    // Lưu ghi chú nội bộ
    String saveInternalNote(int applicationId, String note);

    // Mời phỏng vấn hoặc đổi lịch.
    String invite(int applicationId, int accountId, RecruitmentInterview interview);

    // Hủy phỏng vấn
    String cancelInterview(int applicationId);

    // Hoàn thành phỏng vấn
    String completeInterview(int applicationId, String evaluation);

    // Duyệt trúng tuyển btc
    String accept(int applicationId, int positionId);

    // Từ chối trúng tuyển btc
    String reject(int applicationId, String reason);

    // Thu hồi quyết định trúng tuyển hoặc không đạt, cho đến khi hoạt động bắt đầu.
    String revertDecision(int applicationId);

    // Nhắc sinh viên xác nhận lịch phỏng vấn sắp hết hạn (24 giờ). Trả về số thông báo đã gửi.
    int sendInterviewReminders();

    // Đồng bộ trạng thái Đợt tuyển
    void synchronizeRoundStatuses();

    // Staff gọi lại: nhắc sinh viên xác nhận lịch phỏng vấn đang chờ.
    String remindInterview(int applicationId);
}
