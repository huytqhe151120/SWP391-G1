package com.swp391.g1.service.impl;

import com.swp391.g1.dao.INotificationDAO;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.NotificationDAOImpl;
import com.swp391.g1.dto.response.NotificationDTO;
import com.swp391.g1.model.Enum.ActivityNature;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.util.DateTimeUtil;
import com.swp391.g1.util.StatusUtil;
import java.time.Instant;
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
    public void notifyLoginSuccessful(int accountId, Instant loginAt) {
        try {
            notificationDAO.createLoginNotification(accountId, loginAt);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not save successful login notification for account "
                    + accountId + " at " + DateTimeUtil.format(loginAt) + ".", e);
        }
    }

    @Override
    public void notifyActivityApproved(int activityId, String activityName) {
        notifyActivityStatus(activityId, "ACTIVITY_APPROVED", activityName,
                "Hoạt động đã được duyệt", "đã được duyệt.");
    }

    @Override
    public void notifyStudentsActivityApproved(
            int activityId, String activityName, ActivityNature nature, boolean isPublic) {
        if (activityId <= 0 || nature == null) {
            return;
        }
        try {
            String targetUrl = isPublic ? ("/activities?action=detail&id=" + activityId) : "/activities";
            if (nature == ActivityNature.REQUIRE_REGISTRATION || nature == ActivityNature.VOLUNTARY) {
                notificationDAO.createEligibleStudentsActivityNotification(
                        activityId,
                        false,
                        "ACTIVITY_NEW",
                        "Hoạt động ngoại khóa mới",
                        "Có hoạt động mới \"" + activityName + "\".",
                        targetUrl);
            } else if (nature == ActivityNature.MANDATORY) {
                notificationDAO.createEligibleStudentsActivityNotification(
                        activityId,
                        true,
                        "ACTIVITY_MANDATORY_ASSIGNED",
                        "Chỉ định tham gia hoạt động mới",
                        "Bạn được chỉ định tham gia hoạt động mới \"" + activityName + "\".",
                        targetUrl);
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send student activity approval notification.", e);
        }
    }

    @Override
    public void notifyRegistrationFormStatusChanged(int activityId, int formId, String newStatus) {
        if (activityId <= 0 || (!"OPENING".equals(newStatus) && !"CLOSED".equals(newStatus))) {
            return;
        }
        try {
            ExtracurricularActivity activity = new ExtracurricularActivityDAOImpl().findById(activityId);
            if (activity == null) {
                return;
            }
            boolean canOpenDetail = activity.getApprovalStatus() == ApprovalStatus.APPROVED && activity.isPublic();
            notifyRegistrationFormStatusChanged(
                    activityId, formId, activity.getName(), newStatus, canOpenDetail);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not resolve activity for registration form status notification.", e);
        }
    }

    @Override
    public void notifyRegistrationFormStatusChanged(
            int activityId, int formId, String activityName, String newStatus, boolean canOpenDetail) {
        if (activityId <= 0 || (!"OPENING".equals(newStatus) && !"CLOSED".equals(newStatus))) {
            return;
        }
        try {
            String targetUrl = canOpenDetail ? ("/activities?action=detail&id=" + activityId) : "/activities";
            if ("OPENING".equals(newStatus)) {
                notificationDAO.createRegistrationFormStatusNotification(
                        activityId,
                        formId,
                        false,
                        "REGISTRATION_FORM_OPENING",
                        "Đợt đăng ký hoạt động đã mở",
                        "Đợt đăng ký cho hoạt động \"" + activityName + "\" đã mở.",
                        targetUrl);
            } else {
                notificationDAO.createRegistrationFormStatusNotification(
                        activityId,
                        formId,
                        true,
                        "REGISTRATION_FORM_CLOSED",
                        "Đợt đăng ký hoạt động đã đóng",
                        "Đợt đăng ký cho hoạt động \"" + activityName + "\" đã đóng.",
                        targetUrl);
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send registration form status notification.", e);
        }
    }

    @Override
    public void notifyAttendanceStatusChanged(
            int activityId, String activityName, int sessionNumber, int totalSessions,
            String oldStatus, String newStatus, List<Integer> studentAccountIds) {
        if (activityId <= 0 || studentAccountIds == null || studentAccountIds.isEmpty()) {
            return;
        }
        try {
            int safeSession = Math.max(1, sessionNumber);
            String sessionLabel = totalSessions > 1 ? (" buổi " + safeSession) : "";
            String content = "Trạng thái điểm danh" + sessionLabel + " của bạn trong hoạt động \""
                    + activityName + "\" đã thay đổi từ \"" + StatusUtil.label(oldStatus)
                    + "\" sang \"" + StatusUtil.label(newStatus) + "\".";
            String targetUrl = "/attendance?action=detail&activityId=" + activityId + "&sessionNumber=" + safeSession;
            notificationDAO.createStudentAccountsNotification(
                    studentAccountIds,
                    "ATTENDANCE_STATUS_CHANGED",
                    "Cập nhật trạng thái điểm danh",
                    content,
                    targetUrl);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not send attendance status change notification.", e);
        }
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
    public List<NotificationDTO> getNotifications(int accountId, String filter, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = (Math.max(page, 1) - 1) * safeSize;
        return notificationDAO.findByAccount(accountId, filter, offset, safeSize);
    }

    @Override
    public int countNotifications(int accountId, String filter) {
        return notificationDAO.countByAccount(accountId, filter);
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