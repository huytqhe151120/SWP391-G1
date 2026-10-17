package com.swp391.g1.dao;

import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;

import java.util.List;

public interface IActivityCollaborationInvitationDAO {
    String PENDING = "PENDING";
    String APPROVED = "APPROVED";
    String REJECTED = "REJECTED";
    String CANCELLED = "CANCELLED";

    List<ActivityCollaborationInvitationDTO> findByActivityId(int activityId);

    ActivityCollaborationInvitationDTO findById(int id);

    /** True when the company already has a pending or approved invitation for the activity. */
    boolean existsOpenInvitation(int activityId, int partnerCompanyId);

    boolean insert(int activityId, int partnerCompanyId, int partnerStaffId, String createdBy);

    /** Changes the status only if the invitation is still in {@code expectedStatus}. */
    boolean updateStatus(int id, String expectedStatus, String newStatus);
}
