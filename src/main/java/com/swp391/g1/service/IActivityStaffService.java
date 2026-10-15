package com.swp391.g1.service;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityStaffResponseDTO;
import com.swp391.g1.model.Staff;

import java.util.List;

public interface IActivityStaffService {
    List<ActivityStaffResponseDTO> getStaffsByActivityId(int activityId);

    List<Staff> getAvailableStaffsForActivity(int activityId);

    boolean canAssignStaff(int activityId, CurrentUser user);

    boolean assignStaff(int activityId, int staffId, CurrentUser user) throws Exception;

    boolean removeStaff(int activityId, int staffId, CurrentUser user);
}
