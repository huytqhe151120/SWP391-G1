package com.swp391.g1.service.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivitySeriesDAO;
import com.swp391.g1.dao.impl.ActivitySeriesDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.ActivitySeries;
import com.swp391.g1.model.Enum;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivitySeriesService;
import com.swp391.g1.util.DateTimeUtil;
import com.swp391.g1.util.RecurrenceGenerator;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class ActivitySeriesServiceImpl implements IActivitySeriesService {

    private final IActivitySeriesDAO seriesDAO;

    public ActivitySeriesServiceImpl() {
        this.seriesDAO = new ActivitySeriesDAOImpl();
    }

    @Override
    public List<ActivitySeries> getAllSeries() {
        return seriesDAO.findAll();
    }

    @Override
    public ActivitySeries getSeriesById(int id) {
        if (id <= 0) {
            return null;
        }
        return seriesDAO.findById(id);
    }

    @Override
    public ActivitySeries getSeriesBySeriesCode(String seriesCode) {
        if (seriesCode == null || seriesCode.trim().isEmpty()) {
            return null;
        }
        return seriesDAO.findBySeriesCode(seriesCode.trim());
    }

    @Override
    public List<ActivitySeries> findSeriesByApprovalStatus(String approvalStatus) {
        if (approvalStatus == null || approvalStatus.trim().isEmpty()) {
            return List.of();
        }
        return seriesDAO.findByApprovalStatus(approvalStatus.trim());
    }

    @Override
    public boolean createSeries(ActivitySeries series, CurrentUser user) throws Exception {
        if (series == null) {
            throw new IllegalArgumentException("Dữ liệu chuỗi hoạt động không được để trống.");
        }
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new IllegalArgumentException("Người dùng không có quyền quản lý chuỗi hoạt động.");
        }

        validateSeries(series, false);

        series.setApprovalStatus(Enum.ApprovalStatus.DRAFT);
        series.setCreatedAt(DateTimeUtil.nowUtc());
        series.setUpdatedAt(DateTimeUtil.nowUtc());

        Integer generatedId = seriesDAO.insert(series);
        return generatedId != null && generatedId > 0;
    }

    @Override
    public boolean updateSeries(ActivitySeries series, CurrentUser user) throws Exception {
        if (series == null || series.getId() <= 0) {
            throw new IllegalArgumentException("ID chuỗi hoạt động không hợp lệ.");
        }
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new IllegalArgumentException("Người dùng không có quyền quản lý chuỗi hoạt động.");
        }

        ActivitySeries existing = seriesDAO.findById(series.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy chuỗi hoạt động.");
        }

        // OQ-09: Nếu series đã APPROVED, chỉ cho phép thay đổi description.
        // Các trường ảnh hưởng đến recurrence không được thay đổi.
        if (existing.getApprovalStatus() == Enum.ApprovalStatus.APPROVED) {
            if (!Objects.equals(series.getDescription(), existing.getDescription())) {
                // description change is allowed; fall through
            } else if (series.isRegistrationFormRequired() != existing.isRegistrationFormRequired()) {
                throw new IllegalStateException("Chuỗi hoạt động đã được duyệt (APPROVED) không cho phép thay đổi registration_form_required.");
            } else {
                throw new IllegalStateException("Chuỗi hoạt động đã được duyệt (APPROVED) chỉ cho phép thay đổi mô tả (description).");
            }

            // Enforce: recurrence-affecting fields must match existing
            if (series.getRecurrenceMode() != existing.getRecurrenceMode()) {
                throw new IllegalStateException("Không thể thay đổi recurrence_mode trên chuỗi hoạt động đã duyệt.");
            }
            if (!Objects.equals(series.getRecurrenceInterval(), existing.getRecurrenceInterval())) {
                throw new IllegalStateException("Không thể thay đổi recurrence_interval trên chuỗi hoạt động đã duyệt.");
            }
            if (!Objects.equals(series.getSeriesStartDate(), existing.getSeriesStartDate())) {
                throw new IllegalStateException("Không thể thay đổi series_start_date trên chuỗi hoạt động đã duyệt.");
            }
            if (!Objects.equals(series.getSeriesEndDate(), existing.getSeriesEndDate())) {
                throw new IllegalStateException("Không thể thay đổi series_end_date trên chuỗi hoạt động đã duyệt.");
            }
            if (!Objects.equals(series.getMaxOccurrences(), existing.getMaxOccurrences())) {
                throw new IllegalStateException("Không thể thay đổi max_occurrences trên chuỗi hoạt động đã duyệt.");
            }
            if (!Objects.equals(series.getOccurrenceDurationMinutes(), existing.getOccurrenceDurationMinutes())) {
                throw new IllegalStateException("Không thể thay đổi occurrence_duration_minutes trên chuỗi hoạt động đã duyệt.");
            }
            if (!Objects.equals(series.getTimeOfDay(), existing.getTimeOfDay())) {
                throw new IllegalStateException("Không thể thay đổi time_of_day trên chuỗi hoạt động đã duyệt.");
            }
            if (series.getActivityTypeId() != existing.getActivityTypeId()) {
                throw new IllegalStateException("Không thể thay đổi activity_type_id trên chuỗi hoạt động đã duyệt.");
            }
            if (series.getResponsibleDepartmentId() != existing.getResponsibleDepartmentId()) {
                throw new IllegalStateException("Không thể thay đổi responsible_department_id trên chuỗi hoạt động đã duyệt.");
            }
            if (series.getResponsibleStaffId() != existing.getResponsibleStaffId()) {
                throw new IllegalStateException("Không thể thay đổi responsible_staff_id trên chuỗi hoạt động đã duyệt.");
            }

            // Override all non-description fields with existing values to be safe
            series.setActivityTypeId(existing.getActivityTypeId());
            series.setResponsibleDepartmentId(existing.getResponsibleDepartmentId());
            series.setResponsibleStaffId(existing.getResponsibleStaffId());
            series.setRecurrenceMode(existing.getRecurrenceMode());
            series.setRecurrenceInterval(existing.getRecurrenceInterval());
            series.setSeriesStartDate(existing.getSeriesStartDate());
            series.setSeriesEndDate(existing.getSeriesEndDate());
            series.setMaxOccurrences(existing.getMaxOccurrences());
            series.setOccurrenceDurationMinutes(existing.getOccurrenceDurationMinutes());
            series.setTimeOfDay(existing.getTimeOfDay());
            series.setRegistrationFormRequired(existing.isRegistrationFormRequired());
            series.setSemesterId(existing.getSemesterId());
            series.setApprovalStatus(existing.getApprovalStatus());
        } else {
            validateSeries(series, true);
        }

        series.setCreatedAt(existing.getCreatedAt());
        series.setUpdatedAt(DateTimeUtil.nowUtc());

        return seriesDAO.update(series);
    }

    @Override
    public boolean deleteSeries(int id, CurrentUser user) throws Exception {
        if (id <= 0) {
            return false;
        }
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new IllegalArgumentException("Người dùng không có quyền quản lý chuỗi hoạt động.");
        }

        ActivitySeries existing = seriesDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy chuỗi hoạt động.");
        }

        // OQ-17: Không bao giờ xóa series đã có occurrence được tạo.
        int occurrenceCount = seriesDAO.countOccurrencesBySeriesId(id);
        if (occurrenceCount > 0) {
            throw new IllegalStateException(
                "Không thể xóa chuỗi hoạt động id=" + id + " vì đã có " + occurrenceCount + " occurrence được tạo."
            );
        }

        // OQ-17: Nếu quy tắc trạng thái chưa rõ ràng, thất bại an toàn và báo ambiguous.
        // APPROVED series không có occurrence: quy tắc xóa chưa chốt → từ chối an toàn.
        if (existing.getApprovalStatus() == Enum.ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                "Không thể xóa chuỗi hoạt động id=" + id + " (APPROVED). Quy tắc trạng thái cho phép xóa chưa rõ ràng, từ chối an toàn."
            );
        }

        // DRAFT / PENDING / REJECTED mà không có occurrence: cho phép xóa theo quyền hiện có.
        return seriesDAO.delete(id);
    }

    @Override
    public boolean generateOccurrences(int seriesId, CurrentUser user) throws Exception {
        if (seriesId <= 0) {
            throw new IllegalArgumentException("ID chuỗi hoạt động không hợp lệ.");
        }
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new IllegalArgumentException("Người dùng không có quyền quản lý chuỗi hoạt động.");
        }

        ActivitySeries series = seriesDAO.findById(seriesId);
        if (series == null) {
            throw new IllegalArgumentException("Không tìm thấy chuỗi hoạt động.");
        }

        List<LocalDate> occurrenceDates;
        if (series.getRecurrenceMode() == Enum.RecurrenceMode.MONTHLY) {
            occurrenceDates = RecurrenceGenerator.generateMonthly(
                    series.getSeriesStartDate(),
                    series.getRecurrenceInterval() != null ? series.getRecurrenceInterval() : 1,
                    series.getSeriesEndDate(),
                    series.getMaxOccurrences()
            );
        } else if (series.getRecurrenceMode() == Enum.RecurrenceMode.YEARLY) {
            occurrenceDates = RecurrenceGenerator.generateYearly(
                    series.getSeriesStartDate(),
                    series.getRecurrenceInterval() != null ? series.getRecurrenceInterval() : 1,
                    series.getSeriesEndDate(),
                    series.getMaxOccurrences()
            );
        } else {
            throw new IllegalArgumentException("Chế độ lặp không được hỗ trợ.");
        }

        if (occurrenceDates.isEmpty()) {
            return true;
        }

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            int occurrenceNo = 1;
            for (LocalDate occurrenceDate : occurrenceDates) {
                LocalTime timeOfDay = series.getTimeOfDay();
                if (timeOfDay == null) {
                    throw new IllegalArgumentException("Thời gian trong ngày không được để trống.");
                }

                LocalDateTime startTimeLocal = RecurrenceGenerator.buildStartTime(occurrenceDate, timeOfDay);
                LocalDateTime startTimeUtc = DateTimeUtil.toUtc(startTimeLocal);

                Integer durationMinutes = series.getOccurrenceDurationMinutes();
                if (durationMinutes == null || durationMinutes <= 0) {
                    throw new IllegalArgumentException("Thời lượng occurrence phải lớn hơn 0.");
                }

                LocalDateTime endTimeLocal = RecurrenceGenerator.buildEndTime(startTimeLocal, durationMinutes);
                LocalDateTime endTimeUtc = DateTimeUtil.toUtc(endTimeLocal);

                String occurrenceCode = series.getSeriesCode() + "-" + String.format("%03d", occurrenceNo);

                Integer occId = seriesDAO.insertOccurrence(
                        conn,
                        occurrenceCode,
                        series.getName(),
                        series.getActivityTypeId(),
                        series.getResponsibleDepartmentId(),
                        series.getResponsibleStaffId(),
                        series.getDescription(),
                        series.isRegistrationFormRequired(),
                        series.getSemesterId(),
                        startTimeUtc,
                        endTimeUtc,
                        seriesId,
                        occurrenceNo,
                        occurrenceDate
                );

                if (occId == null) {
                    throw new IllegalStateException("Không thể chèn occurrence thứ " + occurrenceNo);
                }

                occurrenceNo++;
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void validateSeries(ActivitySeries series, boolean isUpdate) throws Exception {
        if (series.getSeriesCode() == null || series.getSeriesCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã chuỗi hoạt động không được để trống.");
        }
        if (series.getName() == null || series.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên chuỗi hoạt động không được để trống.");
        }
        if (series.getActivityTypeId() <= 0) {
            throw new IllegalArgumentException("Loại hoạt động không hợp lệ.");
        }
        if (series.getResponsibleDepartmentId() <= 0) {
            throw new IllegalArgumentException("Phòng ban phụ trách không hợp lệ.");
        }
        if (series.getResponsibleStaffId() <= 0) {
            throw new IllegalArgumentException("Nhân viên phụ trách không hợp lệ.");
        }
        if (series.getRecurrenceMode() == null) {
            throw new IllegalArgumentException("Chế độ lặp không được để trống (MONTHLY hoặc YEARLY).");
        }
        if (series.getRecurrenceInterval() == null || series.getRecurrenceInterval() < 1) {
            throw new IllegalArgumentException("Khoảng cách lặp phải lớn hơn hoặc bằng 1.");
        }
        if (series.getSeriesStartDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu chuỗi không được để trống.");
        }
        if (!series.getSeriesStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu chuỗi phải là ngày trong tương lai.");
        }
        if (series.getSeriesEndDate() != null && series.getSeriesEndDate().isBefore(series.getSeriesStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc chuỗi không được trước ngày bắt đầu.");
        }
        if (series.getMaxOccurrences() != null && series.getMaxOccurrences() < 1) {
            throw new IllegalArgumentException("Số lần tối đa phải lớn hơn hoặc bằng 1.");
        }
        if (series.getOccurrenceDurationMinutes() == null || series.getOccurrenceDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Thời lượng occurrence phải lớn hơn 0.");
        }
        if (series.getTimeOfDay() == null) {
            throw new IllegalArgumentException("Thời gian trong ngày không được để trống.");
        }
    }

}