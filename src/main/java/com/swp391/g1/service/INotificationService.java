package com.swp391.g1.service;

import com.swp391.g1.dto.response.NotificationDTO;
import java.util.List;

public interface INotificationService {

    /** Gửi thông báo yêu cầu duyệt hoạt động tới toàn bộ ADMIN. Không ném lỗi ra ngoài. */
    void notifyApprovalRequest(int activityId, String activityName);

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

    /** Gửi thông báo câu hỏi mới tới các tài khoản có quyền trả lời. */
    void notifyQuestionCreated(int questionId, int activityId, String title, String activityName);

    List<NotificationDTO> getNotifications(int accountId, String filter, String category, int page, int size);

    int countNotifications(int accountId, String filter, String category);

    int countUnread(int accountId);

    boolean markRead(int accountId, int notificationId);

    int markAllRead(int accountId);
}