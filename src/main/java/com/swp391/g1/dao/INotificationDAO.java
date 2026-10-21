package com.swp391.g1.dao;

import com.swp391.g1.dto.response.NotificationDTO;
import java.util.List;

public interface INotificationDAO {

    /** Tạo thông báo yêu cầu duyệt (IN_APP) và gửi tới mọi tài khoản type = ADMIN trong một transaction. Trả về id thông báo. */
    int createApprovalNotification(String title, String content, String targetUrl);

    /** Tạo thông báo trạng thái hoạt động và gửi tới nhân sự phụ trách cùng nhân sự đồng phụ trách. */
    int createActivityStatusNotification(
            int activityId, String eventType, String title, String content, String targetUrl);

    /** Notifies the invited partner staff and the company's leader/secretary accounts. */
    int createPartnerInvitationNotification(
            int invitationId, String eventType, String title, String content, String targetUrl);

    /** Notifies the activity's primary responsible staff about a partner invitation update. */
    int createPartnerInvitationOwnerNotification(
            int invitationId, String eventType, String title, String content, String targetUrl);

    /** Notifies university responders and partner staff assigned to the activity. */
    int createQuestionNotification(int activityId, String title, String content, String targetUrl);

    /** filter: all|unread|read; category: all|activities|qa|appeals|results|messages. */
    List<NotificationDTO> findByAccount(int accountId, String filter, String category, int offset, int limit);

    int countByAccount(int accountId, String filter, String category);

    int countUnread(int accountId);

    boolean markRead(int accountId, int notificationId);

    int markAllRead(int accountId);
}