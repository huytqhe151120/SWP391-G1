package com.swp391.g1.service;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;

import java.util.List;

public interface IActivityCollaborationInvitationService {
    /** All invitations of the activity; {@code canRespond} is evaluated for the given user. */
    List<ActivityCollaborationInvitationDTO> getInvitations(int activityId, CurrentUser user);

    /** Accepted invitations, i.e. the partners that take part in the activity. */
    List<ActivityCollaborationInvitationDTO> getApprovedPartners(int activityId);

    void invite(int activityId, String partnerCompanyId, String partnerStaffId, CurrentUser user);

    void respond(int invitationId, boolean accept, CurrentUser user);

    void cancel(int invitationId, CurrentUser user);
}
