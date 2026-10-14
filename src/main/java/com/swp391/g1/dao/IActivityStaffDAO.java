package com.swp391.g1.dao;

import com.swp391.g1.dto.response.ActivityStaffResponseDTO;

import java.util.List;

public interface IActivityStaffDAO {
    List<ActivityStaffResponseDTO> findStaffsByActivityId(int activityId);

    boolean insert(int activityId, int staffId);

    boolean delete(int activityId, int staffId);

    boolean exists(int activityId, int staffId);

    boolean isStaffInDepartment(int staffId, int departmentId);
}
