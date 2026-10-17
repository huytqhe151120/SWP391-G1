package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityCollaborationInvitationDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IPartnerCompanyDAO;
import com.swp391.g1.dao.IPartnerStaffDAO;
import com.swp391.g1.dao.impl.ActivityCollaborationInvitationDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.PartnerCompanyDAOImpl;
import com.swp391.g1.dao.impl.PartnerStaffDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.PartnerCompany;
import com.swp391.g1.model.PartnerStaff;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityCollaborationInvitationService;
import com.swp391.g1.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ActivityCollaborationInvitationServiceImpl implements IActivityCollaborationInvitationService {

    private final IActivityCollaborationInvitationDAO invitationDAO = new ActivityCollaborationInvitationDAOImpl();
    private final IExtracurricularActivityDAO activityDAO = new ExtracurricularActivityDAOImpl();
    private final IPartnerCompanyDAO partnerCompanyDAO = new PartnerCompanyDAOImpl();
    private final IPartnerStaffDAO partnerStaffDAO = new PartnerStaffDAOImpl();

    @Override
    public List<ActivityCollaborationInvitationDTO> getInvitations(int activityId, CurrentUser user) {
        if (activityId <= 0) {
            return List.of();
        }
        List<ActivityCollaborationInvitationDTO> invitations = invitationDAO.findByActivityId(activityId);
        PartnerStaff me = isPartnerStaffUser(user) ? partnerStaffDAO.findByAccountId(user.getId()) : null;
        for (ActivityCollaborationInvitationDTO invitation : invitations) {
            invitation.setCanRespond(canRespond(invitation, user, me));
        }
        return invitations;
    }

    @Override
    public List<ActivityCollaborationInvitationDTO> getApprovedPartners(int activityId) {
        if (activityId <= 0) {
            return List.of();
        }
        return invitationDAO.findByActivityId(activityId).stream()
                .filter(i -> IActivityCollaborationInvitationDAO.APPROVED.equalsIgnoreCase(i.getApprovalStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public void invite(int activityId, String partnerCompanyId, String partnerStaffId, CurrentUser user) {
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new SecurityException("Bạn không có quyền mời đối tác tham gia hoạt động.");
        }

        requireUpcomingActivity(activityId);

        int companyId = parseId(partnerCompanyId, "Vui lòng chọn công ty đối tác.");
        int staffId = parseId(partnerStaffId, "Vui lòng chọn nhân sự đối tác phụ trách chính.");

        PartnerCompany company = partnerCompanyDAO.findById(companyId);
        if (company == null || company.getStatus() != CommonStatus.ACTIVE) {
            throw new IllegalArgumentException("Công ty đối tác không tồn tại hoặc không còn hoạt động.");
        }
        PartnerStaff staff = partnerStaffDAO.findById(staffId);
        if (staff == null || staff.getStatus() != CommonStatus.ACTIVE || staff.getCompanyId() != companyId) {
            throw new IllegalArgumentException(
                    "Nhân sự đối tác phải đang hoạt động và thuộc công ty đối tác đã chọn.");
        }
        if (invitationDAO.existsOpenInvitation(activityId, companyId)) {
            throw new IllegalArgumentException(
                    "Công ty đối tác này đã được mời hoặc đã tham gia hoạt động.");
        }

        boolean inserted = invitationDAO.insert(
                activityId,
                companyId,
                staffId,
                String.valueOf(user.getId())
        );

        if (!inserted) {
            throw new IllegalArgumentException(
                    "Không thể gửi lời mời. Hoạt động có thể đã bắt đầu "
                            + "hoặc không còn ở trạng thái UPCOMING."
            );
        }
    }

    @Override
    public void respond(int invitationId, boolean accept, CurrentUser user) {
        ActivityCollaborationInvitationDTO invitation = requirePending(invitationId);

        // Kiểm tra hoạt động còn cho phép phản hồi lời mời không.
        requireUpcomingActivity(invitation.getActivityId());

        PartnerStaff me = isPartnerStaffUser(user)
                ? partnerStaffDAO.findByAccountId(user.getId())
                : null;

        if (!canRespond(invitation, user, me)) {
            throw new SecurityException(
                    "Bạn không có quyền phản hồi lời mời này."
            );
        }

        String newStatus = accept
                ? IActivityCollaborationInvitationDAO.APPROVED
                : IActivityCollaborationInvitationDAO.REJECTED;

        if (!invitationDAO.updateStatus(
                invitationId,
                IActivityCollaborationInvitationDAO.PENDING,
                newStatus)) {
            throw new IllegalArgumentException(
                    "Lời mời đã được xử lý trước đó."
            );
        }
    }

    @Override
    public void cancel(int invitationId, CurrentUser user) {
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new SecurityException("Bạn không có quyền hủy lời mời này.");
        }

        ActivityCollaborationInvitationDTO invitation =
                requirePending(invitationId);

        // Chỉ hủy khi hoạt động còn UPCOMING và chưa bắt đầu.
        requireUpcomingActivity(invitation.getActivityId());

        if (!invitationDAO.updateStatus(
                invitationId,
                IActivityCollaborationInvitationDAO.PENDING,
                IActivityCollaborationInvitationDAO.CANCELLED)) {
            throw new IllegalArgumentException(
                    "Lời mời đã được xử lý trước đó."
            );
        }
    }

    private ActivityCollaborationInvitationDTO requirePending(int invitationId) {
        ActivityCollaborationInvitationDTO invitation = invitationId <= 0 ? null : invitationDAO.findById(invitationId);
        if (invitation == null) {
            throw new IllegalArgumentException("Không tìm thấy lời mời.");
        }
        if (!IActivityCollaborationInvitationDAO.PENDING.equalsIgnoreCase(invitation.getApprovalStatus())) {
            throw new IllegalArgumentException("Lời mời đã được xử lý trước đó.");
        }
        return invitation;
    }

    private boolean isPartnerStaffUser(CurrentUser user) {
        return user != null && "PARTNER_STAFF".equalsIgnoreCase(user.getType());
    }

    /**
     * The invited main staff, or a leader/secretary of the invited company, may accept or reject a pending invitation.
     */
    private boolean canRespond(ActivityCollaborationInvitationDTO invitation, CurrentUser user, PartnerStaff me) {
        if (me == null || !IActivityCollaborationInvitationDAO.PENDING.equalsIgnoreCase(invitation.getApprovalStatus())) {
            return false;
        }
        if (invitation.getPartnerStaffId() != null && invitation.getPartnerStaffId() == me.getId()) {
            return true;
        }
        return AccessPolicy.canAssignPartnerStaffToActivity(user)
                && me.getCompanyId() == invitation.getPartnerCompanyId();
    }

    private int parseId(String value, String errorMessage) {
        try {
            int id = Integer.parseInt(value == null ? "" : value.trim());
            if (id <= 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    /**
     * Chỉ cho phép thao tác lời mời khi hoạt động còn UPCOMING
     * và chưa đến thời điểm bắt đầu.
     */
    private void requireUpcomingActivity(int activityId) {
        if (activityId <= 0) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }

        ExtracurricularActivity activity = activityDAO.findById(activityId);

        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }

        if (activity.getActivityStatus() == null
                || !"UPCOMING".equalsIgnoreCase(
                activity.getActivityStatus().toString())) {
            throw new IllegalArgumentException(
                    "Chỉ được thao tác lời mời khi hoạt động đang ở trạng thái UPCOMING."
            );
        }

        LocalDateTime startTime = activity.getStartTime();
        LocalDateTime now = DateTimeUtil.now();

        if (startTime == null || !now.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "Không thể thao tác lời mời sau khi hoạt động bắt đầu."
            );
        }
    }
}
