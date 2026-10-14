package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityStaffDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IStaffDAO;
import com.swp391.g1.dao.impl.ActivityStaffDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.StaffDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityStaffResponseDTO;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.Staff;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityStaffService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ActivityStaffServiceImpl implements IActivityStaffService {

    private final IActivityStaffDAO activityStaffDAO;
    private final IExtracurricularActivityDAO activityDAO;
    private final IStaffDAO staffDAO;

    public ActivityStaffServiceImpl() {
        this.activityStaffDAO = new ActivityStaffDAOImpl();
        this.activityDAO = new ExtracurricularActivityDAOImpl();
        this.staffDAO = new StaffDAOImpl();
    }

    @Override
    public List<ActivityStaffResponseDTO> getStaffsByActivityId(int activityId) {
        if (activityId <= 0) {
            return List.of();
        }
        return activityStaffDAO.findStaffsByActivityId(activityId);
    }

    @Override
    public List<Staff> getAvailableStaffsForActivity(int activityId) {
        if (activityId <= 0) {
            return List.of();
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        if (activity == null) {
            return List.of();
        }
        int deptId = activity.getResponsibleDepartmentId();
        List<Staff> deptStaffs = staffDAO.findByDepartmentId(deptId);
        List<ActivityStaffResponseDTO> assigned = activityStaffDAO.findStaffsByActivityId(activityId);
        Set<Integer> assignedStaffIds = assigned.stream()
                .map(ActivityStaffResponseDTO::getStaffId)
                .collect(Collectors.toSet());

        return deptStaffs.stream()
                .filter(s -> s.getId() != activity.getResponsibleStaffId())
                .filter(s -> !assignedStaffIds.contains(s.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean canAssignStaff(int activityId, CurrentUser user) {
        if (activityId <= 0 || user == null) {
            return false;
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        if (activity == null) {
            return false;
        }
        if (AccessPolicy.canAssignStaffToActivity(user)
                || user.getId() == activity.getResponsibleStaffId()) {
            return true;
        }
        Staff currentStaff = staffDAO.findByAccountId(user.getId());
        return currentStaff != null
                && currentStaff.getId() == activity.getResponsibleStaffId();
    }

    @Override
    public boolean assignStaff(int activityId, int staffId, CurrentUser user) throws Exception {
        if (activityId <= 0 || staffId <= 0) {
            throw new IllegalArgumentException("Thông tin phân công không hợp lệ.");
        }
        if (!canAssignStaff(activityId, user)) {
            throw new SecurityException("Bạn không có quyền phân công nhân sự vào hoạt động này.");
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        Staff staff = staffDAO.findById(staffId);
        if (staff == null) {
            throw new IllegalArgumentException("Không tìm thấy nhân sự được chọn.");
        }
        if (staffId == activity.getResponsibleStaffId()) {
            throw new IllegalArgumentException("Nhân sự phụ trách chính không được thêm vào danh sách nhân sự của hoạt động.");
        }
        int deptId = activity.getResponsibleDepartmentId();
        if (!activityStaffDAO.isStaffInDepartment(staffId, deptId)) {
            throw new IllegalArgumentException("Nhân sự được phân công phải thuộc phòng ban phụ trách hoạt động.");
        }
        if (activityStaffDAO.exists(activityId, staffId)) {
            throw new IllegalArgumentException("Nhân sự này đã được phân công vào hoạt động.");
        }
        return activityStaffDAO.insert(activityId, staffId);
    }

    @Override
    public boolean removeStaff(int activityId, int staffId, CurrentUser user) {
        if (activityId <= 0 || staffId <= 0) {
            return false;
        }
        if (!canAssignStaff(activityId, user)) {
            return false;
        }
        return activityStaffDAO.delete(activityId, staffId);
    }
}
