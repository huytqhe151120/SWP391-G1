package com.swp391.g1.service;

import com.swp391.g1.dto.response.NotificationDTO;
import java.time.Instant;
import java.util.List;

public interface INotificationService {

    /** Gửi thông báo yêu cầu duyệt hoạt động tới toàn bộ ADMIN. Không ném lỗi ra ngoài. */
    void notifyApprovalRequest(int activityId, String activityName);

    void notifyLoginSuccessful(int accountId, Instant loginAt);

    /** Gửi thông báo hoạt động đã được duyệt tới nhân sự phụ trách. Không ném lỗi ra ngoài. */
    void notifyActivityApproved(int activityId, String activityName);

    /** Gửi thông báo hoạt động bị từ chối tới nhân sự phụ trách. Không ném lỗi ra ngoài. */
    void notifyActivityRejected(int activityId, String activityName);

    /** Gửi thông báo quyết định duyệt bị hủy tới nhân sự phụ trách. Không ném lỗi ra ngoài. */
    void notifyApprovalRevoked(int activityId, String activityName);

    /** Gửi thông báo quyết định từ chối bị hủy tới nhân sự phụ trách. Không ném lỗi ra ngoài. */
    void notifyRejectionRevoked(int activityId, String activityName);

    /** Gửi thông báo lời mời hợp tác tới nhân sự được mời và lãnh đạo công ty. */
    void notifyPartnerInvitationSent(int invitationId, String activityName, String companyName);

    /** Gửi thông báo yêu cầu ngừng hợp tác tới nhân sự đối tác và lãnh đạo công ty. */
    void notifyPartnerTerminationRequested(int invitationId, String activityName, String companyName);

    /** Gửi thông báo phản hồi lời mời của đối tác tới nhân sự phụ trách chính. */
    void notifyPartnerInvitationOwner(int invitationId, String eventType, String title, String content);

    List<NotificationDTO> getNotifications(int accountId, String filter, int page, int size);

    /** Thông báo tuyển ban tổ chức tới sinh viên. Không ném lỗi ra ngoài. */
    void notifyStudent(int studentId, String eventType, String title, String content, String targetUrl);

    /** Thông báo tuyển ban tổ chức tới nhân sự phụ trách hoạt động. Không ném lỗi ra ngoài. */
    void notifyActivityStaff(int activityId, String eventType, String title, String content, String targetUrl);

    int countNotifications(int accountId, String filter);

    int countUnread(int accountId);

    boolean markRead(int accountId, int notificationId);

    int markAllRead(int accountId);
}