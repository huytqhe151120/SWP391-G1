package com.swp391.g1.service.impl;

import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dto.request.ExtracurricularActivityRequestDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.service.IExtracurricularActivityService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ExtracurricularActivityServiceImpl implements IExtracurricularActivityService {

    private final IExtracurricularActivityDAO activityDAO;

    public ExtracurricularActivityServiceImpl() {
        this.activityDAO = new ExtracurricularActivityDAOImpl();
    }

    @Override
    public List<ExtracurricularActivityResponseDTO> getAllActivities() {
        return activityDAO.findAllWithDetails();
    }

    @Override
    public ExtracurricularActivityResponseDTO getActivityById(int id) {
        if (id <= 0) {
            return null;
        }
        // Find DTO by ID from the list of all activities with details
        return activityDAO.findAllWithDetails().stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean createActivity(ExtracurricularActivityRequestDTO dto) throws Exception {
        // Validate input data (no need to check ID for creation)
        validateRequestDTO(dto, false);
        // Map from RequestDTO to Entity
        ExtracurricularActivity entity = mapToEntity(dto);
        // Set default values for ApprovalStatus and ActivityStatus
        entity.setApprovalStatus(ApprovalStatus.DRAFT);
        entity.setActivityStatus(ActivityStatus.UPCOMING);
        // Call DAO to insert the new activity
        Integer generatedId = activityDAO.insert(entity);
        return generatedId != null && generatedId > 0;
    }

    @Override
    public boolean updateActivity(ExtracurricularActivityRequestDTO dto) throws Exception {
        // Validate input data (need to check ID)
        validateRequestDTO(dto, true);
        // Map from RequestDTO to Entity
        ExtracurricularActivity entity = mapToEntity(dto);
        entity.setId(Integer.parseInt(dto.getId()));
        // Call DAO to update the activity
        return activityDAO.update(entity);
    }

    @Override
    public boolean deleteActivity(int id) {
        if (id <= 0) {
            return false;
        }
        return activityDAO.delete(id);
    }

    private void validateRequestDTO(ExtracurricularActivityRequestDTO dto, boolean isUpdate) throws Exception {
        if (dto == null) {
            throw new IllegalArgumentException("Invalid request data: ExtracurricularActivityRequestDTO is null.");
        }

        if (isUpdate && (dto.getId() == null || dto.getId().trim().isEmpty())) {
            throw new IllegalArgumentException("Invalid activity ID when updating.");
        }

        // Validate các trường bắt buộc (Not Null)
        if (isEmpty(dto.getCode())) {
            throw new IllegalArgumentException("Activity code is required.");
        }
        if (isEmpty(dto.getName())) {
            throw new IllegalArgumentException("Activity name is required.");
        }
        if (isEmpty(dto.getSemesterId())) {
            throw new IllegalArgumentException("Please select a semester.");
        }
        if (isEmpty(dto.getActivityTypeId())) {
            throw new IllegalArgumentException("Please select an activity type.");
        }
        if (isEmpty(dto.getResponsibleDepartmentId())) {
            throw new IllegalArgumentException("Please select the responsible department.");
        }
        if (isEmpty(dto.getResponsibleStaffId())) {
            throw new IllegalArgumentException("Please select the responsible staff.");
        }

        // Validate ràng buộc đối tác: Chọn nhân viên đối tác thì BẮT BỘC phải chọn công ty đối tác
        if (!isEmpty(dto.getPartnerStaffId()) && isEmpty(dto.getPartnerCompanyId())) {
            throw new IllegalArgumentException("Please select the partner company before selecting a partner staff.");
        }

        // Validate điểm không được âm
        if (!isEmpty(dto.getBonusPoint())) {
            try {
                BigDecimal bonus = new BigDecimal(dto.getBonusPoint());
                if (bonus.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Bonus points cannot be negative.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bonus points are in an invalid format.");
            }
        }

        if (!isEmpty(dto.getPenaltyPoint())) {
            try {
                BigDecimal penalty = new BigDecimal(dto.getPenaltyPoint());
                if (penalty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Penalty points cannot be negative.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Penalty points are in an invalid format.");
            }
        }

        // Validate khoảng thời gian: StartTime phải trước EndTime
        if (!isEmpty(dto.getStartTime()) && !isEmpty(dto.getEndTime())) {
            try {
                LocalDateTime start = LocalDateTime.parse(dto.getStartTime());
                LocalDateTime end = LocalDateTime.parse(dto.getEndTime());

                if (!start.isBefore(end)) {
                    throw new IllegalArgumentException("The start time must be before the end time.");
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date/time format.");
            }
        }
    }

    private ExtracurricularActivity mapToEntity(ExtracurricularActivityRequestDTO dto) {
        ExtracurricularActivity entity = new ExtracurricularActivity();

        entity.setCode(dto.getCode().trim());
        entity.setName(dto.getName().trim());
        entity.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        entity.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        // Convert String -> int
        entity.setSemesterId(Integer.parseInt(dto.getSemesterId()));
        entity.setActivityTypeId(Integer.parseInt(dto.getActivityTypeId()));
        entity.setResponsibleDepartmentId(Integer.parseInt(dto.getResponsibleDepartmentId()));
        entity.setResponsibleStaffId(Integer.parseInt(dto.getResponsibleStaffId()));

        // Convert các khóa ngoại Nullable
        entity.setPartnerCompanyId(isEmpty(dto.getPartnerCompanyId()) ? null : Integer.parseInt(dto.getPartnerCompanyId()));
        entity.setPartnerStaffId(isEmpty(dto.getPartnerStaffId()) ? null : Integer.parseInt(dto.getPartnerStaffId()));

        // Convert Điểm
        entity.setBonusPoint(isEmpty(dto.getBonusPoint()) ? BigDecimal.ZERO : new BigDecimal(dto.getBonusPoint()));
        entity.setPenaltyPoint(isEmpty(dto.getPenaltyPoint()) ? BigDecimal.ZERO : new BigDecimal(dto.getPenaltyPoint()));

        // Convert Thời gian (<input type="datetime-local"> trả về định dạng yyyy-MM-ddTHH:mm)[cite: 1]
        if (!isEmpty(dto.getStartTime())) {
            entity.setStartTime(LocalDateTime.parse(dto.getStartTime()));
        }
        if (!isEmpty(dto.getEndTime())) {
            entity.setEndTime(LocalDateTime.parse(dto.getEndTime()));
        }

        return entity;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
