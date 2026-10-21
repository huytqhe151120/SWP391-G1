package com.swp391.g1.service.impl;

import com.swp391.g1.dao.INotificationDAO;
import com.swp391.g1.dao.impl.NotificationDAOImpl;
import com.swp391.g1.dto.response.NotificationDTO;
import com.swp391.g1.service.INotificationService;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationServiceImpl implements INotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationServiceImpl.class.getName());

    private final INotificationDAO notificationDAO = new NotificationDAOImpl();

    @Override
    public void notifyApprovalRequest(int activityId, String activityName) {
        try {
            notificationDAO.createApprovalNotification(
                    "Yêu cầu duyệt hoạt động mới",
                    "Hoạt động \"" + activityName + "\" đang chờ duyệt.",
                    "/activities?action=detail&id=" + activityId);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send admin notification.", e);
        }
    }

    @Override
    public void notifyActivityApproved(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_APPROVED", activityName,
                "Hoạt động đã được duyệt", "đã được duyệt.");
    }

    @Override
    public void notifyActivityRejected(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_REJECTED", activityName,
                "Hoạt động đã bị từ chối", "đã bị từ chối.");
    }

    @Override
    public void notifyApprovalRevoked(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_APPROVAL_REVOKED", activityName,
                "Đã hủy duyệt hoạt động", "đã bị hủy quyết định duyệt và chuyển về trạng thái Chờ duyệt.");
    }

    @Override
    public void notifyRejectionRevoked(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_REJECTION_REVOKED", activityName,
                "Đã hủy từ chối hoạt động", "đã được hủy quyết định từ chối và chuyển về trạng thái Chờ duyệt.");
    }

    @Override
    public void notifyPartnerInvitationSent(int invitationId, String activityName, String companyName) {
        notifyPartnerInvitation(invitationId, "PARTNER_COLLABORATION_INVITATION",
                "Lời mời hợp tác hoạt động mới",
                "Công ty \"" + companyName + "\" được mời hợp tác hoạt động \"" + activityName + "\".");
    }

    @Override
    public void notifyPartnerTerminationRequested(int invitationId, String activityName, String companyName) {
        notifyPartnerInvitation(invitationId, "PARTNER_TERMINATION_REQUEST",
                "Yêu cầu ngừng hợp tác",
                "Công ty \"" + companyName + "\" nhận được yêu cầu ngừng hợp tác trong hoạt động \""
                        + activityName + "\".");
    }

    @Override
    public void notifyPartnerInvitationOwner(int invitationId, String eventType, String title, String content) {
        try {
            notificationDAO.createPartnerInvitationOwnerNotification(
                    invitationId, eventType, title, content, "/activities");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send partner invitation update notification.", e);
        }
    }

    private void notifyPartnerInvitation(int invitationId, String eventType, String title, String content) {
        try {
            notificationDAO.createPartnerInvitationNotification(
                    invitationId, eventType, title, content, "/partner-invitations");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send partner invitation notification.", e);
        }
    }

    private void notifyActivityStatus(
            int activityId, String eventType, String activityName, String title, String statusMessage) {
        try {
            notificationDAO.createActivityStatusNotification(
                    activityId,
                    eventType,
                    title,
                    "Hoạt động \"" + activityName + "\" " + statusMessage,
                    "/activities?action=detail&id=" + activityId);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send activity status notification.", e);
        }
    }

    @Override
    public void notifyQuestionCreated(int questionId, int activityId, String title, String activityName) {
        try {
            notificationDAO.createQuestionNotification(activityId,
                    "Câu hỏi mới",
                    "Câu hỏi \"" + title + "\" về hoạt động \"" + activityName + "\" đang chờ trả lời.",
                    "/qa-management?action=view&id=" + questionId);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send new question notification.", e);
        }
    }

    @Override
    public List<NotificationDTO> getNotifications(int accountId, String filter, String category, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = (Math.max(page, 1) - 1) * safeSize;
        return notificationDAO.findByAccount(accountId, filter, category, offset, safeSize);
    }

    @Override
    public int countNotifications(int accountId, String filter, String category) {
        return notificationDAO.countByAccount(accountId, filter, category);
    }

    @Override
    public int countUnread(int accountId) {
        return notificationDAO.countUnread(accountId);
    }

    @Override
    public boolean markRead(int accountId, int notificationId) {
        return notificationDAO.markRead(accountId, notificationId);
    }

    @Override
    public int markAllRead(int accountId) {
        return notificationDAO.markAllRead(accountId);
    }
}