package com.swp391.g1.dao;

import com.swp391.g1.dto.response.NotificationDTO;
import java.time.Instant;
import java.util.List;

public interface INotificationDAO {

    /** Tạo thông báo yêu cầu duyệt (IN_APP) và gửi tới mọi tài khoản type = ADMIN trong một transaction. Trả về id thông báo. */
    int createApprovalNotification(String title, String content, String targetUrl);

    int createLoginNotification(int accountId, Instant loginAt);

    /** Tạo thông báo trạng thái hoạt động và gửi tới nhân sự phụ trách cùng nhân sự đồng phụ trách. */
    int createActivityStatusNotification(
            int activityId, String eventType, String title, String content, String targetUrl);

    /** Notifies the invited partner staff and the company's leader/secretary accounts. */
    int createPartnerInvitationNotification(
            int invitationId, String eventType, String title, String content, String targetUrl);

    /** Notifies the activity's primary responsible staff about a partner invitation update. */
    int createPartnerInvitationOwnerNotification(
            int invitationId, String eventType, String title, String content, String targetUrl);

    int createActivityFeedbackReminder(int activityId);

    /** Gửi thông báo hoạt động tới sinh viên đang hoạt động thuộc đối tượng tham gia (kèm sinh viên đã ghi danh nếu includeParticipants = true). */
    int createEligibleStudentsActivityNotification(
            int activityId, boolean includeParticipants, String eventType, String title, String content, String targetUrl);

    /** Gửi thông báo chuyển trạng thái đợt đăng ký tới sinh viên thuộc đối tượng tham gia (kèm sinh viên đã nộp đơn nếu includeApplicants = true). */
    int createRegistrationFormStatusNotification(
            int activityId, int formId, boolean includeApplicants, String eventType, String title, String content, String targetUrl);

    /** Gửi thông báo tới danh sách tài khoản sinh viên cụ thể (ví dụ sau khi cập nhật điểm danh). */
    int createStudentAccountsNotification(
            List<Integer> accountIds, String eventType, String title, String content, String targetUrl);

    /** filter: "all" | "unread" | "read". */
    List<NotificationDTO> findByAccount(int accountId, String filter, int offset, int limit);

    int countByAccount(int accountId, String filter);

    int countUnread(int accountId);

    boolean markRead(int accountId, int notificationId);

    int markAllRead(int accountId);
}