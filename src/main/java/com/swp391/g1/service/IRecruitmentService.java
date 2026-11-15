package com.swp391.g1.service;

import com.swp391.g1.model.RecruitmentApplication;
import com.swp391.g1.model.RecruitmentInterview;
import com.swp391.g1.model.RecruitmentRound;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Nghiệp vụ tuyển ban tổ chức. Các phương thức trả String: null là thành công, ngược lại là thông báo lỗi. */
public interface IRecruitmentService {

    List<RecruitmentRound> getRounds();

    RecruitmentRound getRound(int id);

    RecruitmentRound getRoundByActivity(int activityId);

    List<RecruitmentRound> getSelectableActivities(Integer includeActivityId);

    LocalDateTime getParticipationOpenAt(int activityId);

    /** Tạo (id = 0) hoặc sửa đợt; openAfterSave = true thì mở nhận hồ sơ ngay sau khi lưu. */
    String saveRound(RecruitmentRound round, int accountId, boolean openAfterSave);

    String openRound(int id);

    boolean isStructureLocked(int roundId);

    String closeRound(int id);

    String deleteRound(int id);

    List<RecruitmentRound> getOpenRounds();

    List<RecruitmentApplication> getStudentApplications(int studentId);

    RecruitmentApplication getStudentApplication(int applicationId, int studentId);

    /** Hồ sơ đang còn hiệu lực (chưa rút) của sinh viên trong đợt; null nếu chưa nộp. */
    RecruitmentApplication getActiveApplication(int roundId, int studentId);

    boolean hasActiveParticipation(int activityId, int studentId);

    /** answers: questionId → các giá trị đã chọn/nhập. */
    String apply(int roundId, int studentId, Integer choice1, Integer choice2,
                 Map<Integer, List<String>> answers, String note);

    String withdraw(int applicationId, int studentId, String reason);

    String confirmInterview(int applicationId, int studentId);

    /** Sinh viên đã trúng tuyển ban tổ chức ở hoạt động (không được đăng ký tham gia). */
    boolean hasAcceptedApplication(int activityId, int studentId);

    // ---- Xét hồ sơ (staff/admin). Các phương thức trả String: null là thành công, ngược lại là lỗi.

    List<RecruitmentApplication> getApplications(int roundId, String statusFilter, Integer positionId);

    RecruitmentApplication getApplication(int applicationId);

    String saveInternalNote(int applicationId, String note);

    /** Mời phỏng vấn hoặc đổi lịch. */
    String invite(int applicationId, int accountId, RecruitmentInterview interview);

    String cancelInterview(int applicationId);

    String completeInterview(int applicationId, String evaluation);

    String accept(int applicationId, int positionId);

    String reject(int applicationId, String reason);

    /** Thu hồi quyết định trúng tuyển hoặc không đạt, cho đến khi hoạt động bắt đầu. */
    String revertDecision(int applicationId);

    /** Nhắc sinh viên xác nhận lịch phỏng vấn sắp hết hạn (24 giờ). Trả về số thông báo đã gửi. */
    int sendInterviewReminders();

    void synchronizeRoundStatuses();

    /** Staff gọi lại: nhắc sinh viên xác nhận lịch phỏng vấn đang chờ. */
    String remindInterview(int applicationId);
}
