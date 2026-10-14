package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityPartnerStaffDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IPartnerStaffDAO;
import com.swp391.g1.dao.impl.ActivityPartnerStaffDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.PartnerStaffDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityPartnerStaffResponseDTO;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.PartnerStaff;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityPartnerStaffService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ActivityPartnerStaffServiceImpl implements IActivityPartnerStaffService {

    private final IActivityPartnerStaffDAO activityPartnerStaffDAO;
    private final IExtracurricularActivityDAO activityDAO;
    private final IPartnerStaffDAO partnerStaffDAO;

    public ActivityPartnerStaffServiceImpl() {
        this.activityPartnerStaffDAO = new ActivityPartnerStaffDAOImpl();
        this.activityDAO = new ExtracurricularActivityDAOImpl();
        this.partnerStaffDAO = new PartnerStaffDAOImpl();
    }

    @Override
    public List<ActivityPartnerStaffResponseDTO> getPartnerStaffsByActivityId(int activityId) {
        return activityId <= 0 ? List.of() : activityPartnerStaffDAO.findByActivityId(activityId);
    }

    @Override
    public List<PartnerStaff> getAvailablePartnerStaffsForActivity(int activityId) {
        ExtracurricularActivity activity = activityId <= 0 ? null : activityDAO.findById(activityId);
        if (activity == null || activity.getPartnerCompanyId() == null) {
            return List.of();
        }
        Set<Integer> assignedIds = activityPartnerStaffDAO.findByActivityId(activityId).stream()
                .map(ActivityPartnerStaffResponseDTO::getPartnerStaffId)
                .collect(Collectors.toSet());
        Integer mainId = activity.getPartnerStaffId();

        return partnerStaffDAO.findByCompany(activity.getPartnerCompanyId()).stream()
                .filter(s -> s.getStatus() == Enum.CommonStatus.ACTIVE)
                .filter(s -> mainId == null || s.getId() != mainId)
                .filter(s -> !assignedIds.contains(s.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean canAssignPartnerStaff(int activityId, CurrentUser user) {
        if (activityId <= 0 || user == null) {
            return false;
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        if (activity == null) {
            return false;
        }
        if (!"PARTNER_STAFF".equalsIgnoreCase(user.getType())) {
            return false;
        }
        Integer mainId = activity.getPartnerStaffId();
        if (mainId != null) {
            PartnerStaff main = partnerStaffDAO.findById(mainId);
            if (main != null && main.getAccountId() != null && main.getAccountId() == user.getId()) {
                return true;
            }
        }
        // Leader/secretary must belong to the partner company of the activity.
        if (!AccessPolicy.canAssignPartnerStaffToActivity(user) || activity.getPartnerCompanyId() == null) {
            return false;
        }
        return partnerStaffDAO.findByCompany(activity.getPartnerCompanyId()).stream()
                .anyMatch(s -> s.getAccountId() != null && s.getAccountId() == user.getId());
    }

    @Override
    public boolean assignPartnerStaff(int activityId, int partnerStaffId, CurrentUser user) {
        if (activityId <= 0 || partnerStaffId <= 0) {
            throw new IllegalArgumentException("Thông tin phân công không hợp lệ.");
        }
        if (!canAssignPartnerStaff(activityId, user)) {
            throw new SecurityException("Bạn không có quyền phân công nhân sự đối tác vào hoạt động này.");
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        PartnerStaff staff = partnerStaffDAO.findById(partnerStaffId);
        if (staff == null) {
            throw new IllegalArgumentException("Không tìm thấy nhân sự đối tác được chọn.");
        }
        if (activity.getPartnerStaffId() != null && partnerStaffId == activity.getPartnerStaffId()) {
            throw new IllegalArgumentException(
                    "Nhân sự đối tác phụ trách chính không được thêm vào danh sách nhân sự đối tác của hoạt động.");
        }
        if (activity.getPartnerCompanyId() == null || staff.getCompanyId() != activity.getPartnerCompanyId()) {
            throw new IllegalArgumentException("Nhân sự đối tác phải thuộc công ty đối tác của hoạt động.");
        }
        if (activityPartnerStaffDAO.exists(activityId, partnerStaffId)) {
            throw new IllegalArgumentException("Nhân sự đối tác này đã được phân công vào hoạt động.");
        }
        return activityPartnerStaffDAO.insert(activityId, partnerStaffId);
    }

    @Override
    public boolean removePartnerStaff(int activityId, int partnerStaffId, CurrentUser user) {
        if (activityId <= 0 || partnerStaffId <= 0 || !canAssignPartnerStaff(activityId, user)) {
            return false;
        }
        return activityPartnerStaffDAO.delete(activityId, partnerStaffId);
    }
}
