package com.swp391.g1.dao;

import com.swp391.g1.model.RecruitmentApplication;
import com.swp391.g1.model.RecruitmentInterview;

import java.util.List;
import java.time.LocalDateTime;

public interface IRecruitmentApplicationDAO {

    RecruitmentApplication findById(int id);

    /** Hồ sơ của sinh viên trong một đợt (có câu trả lời và lịch phỏng vấn); null nếu chưa nộp. */
    RecruitmentApplication findByRoundAndStudent(int roundId, int studentId);

    /** statusFilter và positionId có thể null để không lọc; positionId lọc theo nguyện vọng 1, 2 hoặc ban được nhận. */
    List<RecruitmentApplication> findByRound(int roundId, String statusFilter, Integer positionId);

    List<RecruitmentApplication> findByStudent(int studentId);

    /** Sinh viên đã trúng tuyển ban tổ chức ở hoạt động này. */
    boolean hasAcceptedApplication(int activityId, int studentId);

    /** Sinh viên đang có đăng ký tham gia còn hiệu lực (chờ duyệt hoặc đã duyệt) ở hoạt động. */
    boolean hasActiveParticipation(int activityId, int studentId);

    /** Nộp hồ sơ kèm câu trả lời; trùng (round, student) thì ném DataAccessException duplicateKey. */
    /** Đăng ký tham gia lại: xóa hồ sơ đã rút để sinh viên nộp lại cùng đợt. */
    boolean deleteWithdrawn(int id);

    int insert(RecruitmentApplication application, LocalDateTime roundUpdatedAt);

    /** Chuyển trạng thái nếu hồ sơ đang ở một trong expectedStatuses; false nếu trạng thái đã đổi. */
    boolean updateStatus(int id, List<String> expectedStatuses, String newStatus);

    boolean withdraw(int id, List<String> expectedStatuses, String reason);

    boolean reject(int id, List<String> expectedStatuses, String reason);

    /**
     * Trúng tuyển vào positionId trong một transaction: khóa vị trí, kiểm tra còn chỉ tiêu,
     * hủy đăng ký tham gia cùng hoạt động. Trả về false nếu hết chỉ tiêu hoặc trạng thái đã đổi.
     */
    boolean accept(int id, List<String> expectedStatuses, int positionId);

    /** Thu hồi quyết định trúng tuyển/không đạt về revertStatus, trả lại chỉ tiêu. */
    boolean revertDecision(int id, String revertStatus);

    boolean updateInternalNote(int id, String note);

    RecruitmentInterview findInterview(int applicationId);

    /** Đặt hoặc đổi lịch (ghi đè lịch cũ, xóa xác nhận cũ) và chuyển hồ sơ sang INTERVIEW_INVITED. */
    boolean saveInterview(RecruitmentInterview interview);

    boolean confirmInterview(int applicationId);

    boolean cancelInterview(int applicationId, String revertApplicationStatus);

    /** Đánh dấu đã phỏng vấn, lưu nhận xét và chuyển hồ sơ sang INTERVIEWED. */
    boolean completeInterview(int applicationId, String evaluation);

    /** Lịch chưa xác nhận có hạn xác nhận trong vòng hoursAhead giờ tới và chưa nhắc. */
    List<RecruitmentInterview> findInterviewsNeedingReminder(int hoursAhead);

    boolean sendInterviewReminderIfDue(RecruitmentInterview interview, int hoursAhead,
                                       String title, String content, String targetUrl);
}
