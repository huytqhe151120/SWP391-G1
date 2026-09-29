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
        // Map từ RequestDTO sang Entity
        ExtracurricularActivity entity = mapToEntity(dto);
        // Gán trạng thái mặc định khi tạo mới
        entity.setApprovalStatus(ApprovalStatus.DRAFT);
        entity.setActivityStatus(ActivityStatus.UPCOMING);

        // 3. Gọi DAO lưu xuống DB
        Integer generatedId = activityDAO.insert(entity);
        return generatedId != null && generatedId > 0;
    }

    @Override
    public boolean updateActivity(ExtracurricularActivityRequestDTO dto) throws Exception {
        // 1. Validate dữ liệu đầu vào (cần check ID)
        validateRequestDTO(dto, true);

        // 2. Map từ RequestDTO sang Entity
        ExtracurricularActivity entity = mapToEntity(dto);
        entity.setId(Integer.parseInt(dto.getId()));

        // 3. Gọi DAO cập nhật
        return activityDAO.update(entity);
    }

    @Override
    public boolean deleteActivity(int id) {
        if (id <= 0) {
            return false;
        }
        return activityDAO.delete(id);
    }

    // =========================================================================
    // HELPER METHODS: VALIDATION & MAPPING
    // =========================================================================

    private void validateRequestDTO(ExtracurricularActivityRequestDTO dto, boolean isUpdate) throws Exception {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ.");
        }

        if (isUpdate && (dto.getId() == null || dto.getId().trim().isEmpty())) {
            throw new IllegalArgumentException("ID hoạt động không hợp lệ khi cập nhật.");
        }

        // Validate các trường bắt buộc (Not Null)
        if (isEmpty(dto.getCode())) {
            throw new IllegalArgumentException("Mã hoạt động không được để trống.");
        }
        if (isEmpty(dto.getName())) {
            throw new IllegalArgumentException("Tên hoạt động không được để trống.");
        }
        if (isEmpty(dto.getSemesterId())) {
            throw new IllegalArgumentException("Vui lòng chọn học kỳ.");
        }
        if (isEmpty(dto.getActivityTypeId())) {
            throw new IllegalArgumentException("Vui lòng chọn loại hoạt động.");
        }
        if (isEmpty(dto.getResponsibleDepartmentId())) {
            throw new IllegalArgumentException("Vui lòng chọn phòng ban phụ trách.");
        }
        if (isEmpty(dto.getResponsibleStaffId())) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên phụ trách.");
        }

        // Validate ràng buộc đối tác: Chọn nhân viên đối tác thì BẮT BỘC phải chọn công ty đối tác
        if (!isEmpty(dto.getPartnerStaffId()) && isEmpty(dto.getPartnerCompanyId())) {
            throw new IllegalArgumentException("Phải chọn công ty đối tác trước khi chọn nhân viên đối tác.");
        }

        // Validate điểm không được âm
        if (!isEmpty(dto.getBonusPoint())) {
            try {
                BigDecimal bonus = new BigDecimal(dto.getBonusPoint());
                if (bonus.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Điểm thưởng không được là số âm.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Điểm thưởng không đúng định dạng số.");
            }
        }

        if (!isEmpty(dto.getPenaltyPoint())) {
            try {
                BigDecimal penalty = new BigDecimal(dto.getPenaltyPoint());
                if (penalty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Điểm phạt không được là số âm.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Điểm phạt không đúng định dạng số.");
            }
        }

        // Validate khoảng thời gian: StartTime phải trước EndTime
        if (!isEmpty(dto.getStartTime()) && !isEmpty(dto.getEndTime())) {
            try {
                LocalDateTime start = LocalDateTime.parse(dto.getStartTime());
                LocalDateTime end = LocalDateTime.parse(dto.getEndTime());

                if (!start.isBefore(end)) {
                    throw new IllegalArgumentException("Thời gian bắt đầu phải diễn ra trước thời gian kết thúc.");
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Định dạng ngày giờ không hợp lệ.");
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
