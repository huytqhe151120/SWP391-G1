package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IRegistrationFormDAO;
import com.swp391.g1.dao.impl.RegistrationFormDAOImpl;
import com.swp391.g1.model.Enum.RegistrationFormStatus;
import com.swp391.g1.model.RegistrationApplication;
import com.swp391.g1.model.RegistrationForm;
import com.swp391.g1.model.RegistrationFormField;
import com.swp391.g1.service.IRegistrationFormService;
import com.swp391.g1.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RegistrationFormServiceImpl implements IRegistrationFormService {
    private final IRegistrationFormDAO dao = new RegistrationFormDAOImpl();

    public List<RegistrationForm> getAll() {
        return dao.findAll();
    }

    public RegistrationForm getById(int id) {
        return id > 0 ? dao.findById(id) : null;
    }

    @Override
    public RegistrationForm getByActivityId(int activityId) {
        return activityId > 0 ? dao.findByActivityId(activityId) : null;
    }

    @Override
    public RegistrationForm getOpenFormById(int id, int studentId) {
        return id > 0 && studentId > 0 ? dao.findOpenFormById(id, studentId) : null;
    }

    @Override
    public String getRegistrationStatus(int formId, int studentId) {
        return formId > 0 && studentId > 0 ? dao.getRegistrationStatus(formId, studentId) : null;
    }

    @Override
    public boolean isAtCapacity(int formId) {
        return formId > 0 && dao.isAtCapacity(formId);
    }

    @Override
    public boolean isOpenFormAtCapacity(int formId) {
        return formId > 0 && dao.isOpenFormAtCapacity(formId);
    }

    @Override
    public boolean isActivityEligibleForApplicationReview(int formId) {
        return formId > 0 && dao.isActivityEligibleForApplicationReview(formId);
    }

    @Override
    public boolean hasEarlierPendingApplication(int registrationId) {
        return registrationId > 0 && dao.hasEarlierPendingApplication(registrationId);
    }

    @Override
    public List<RegistrationApplication> getApplications(int formId) {
        return formId > 0 ? dao.findApplications(formId) : List.of();
    }

    @Override
    public List<RegistrationApplication> getApplicationsForStudent(int studentId) {
        return studentId > 0 ? dao.findApplicationsForStudent(studentId) : List.of();
    }

    @Override
    public RegistrationApplication getApplicationById(int registrationId) {
        return registrationId > 0 ? dao.findApplicationById(registrationId) : null;
    }

    @Override
    public String reviewApplication(int registrationId) {
        if (registrationId <= 0) return "Đơn đăng ký không hợp lệ.";
        RegistrationApplication application = dao.findApplicationById(registrationId);
        if (application != null && dao.isQuickRegistrationForm(application.getFormId())) {
            return "Không thể duyệt đơn đăng ký nhanh (không cần duyệt).";
        }
        if (dao.reviewApplication(registrationId, "SUBMITTED", "APPROVED", null)) return null;
        if (dao.hasEarlierPendingApplication(registrationId)) {
            return "Không thể duyệt đơn này vì còn đơn chờ xử lý được gửi trước.";
        }
        if (application != null && "SUBMITTED".equals(application.getStatus())
                && !dao.isActivityEligibleForApplicationReview(application.getFormId())) {
            return "Không thể duyệt đơn vì hoạt động đã bắt đầu, bị hủy hoặc không còn đủ điều kiện.";
        }
        return "Chỉ có thể duyệt đơn đang chờ xử lý.";
    }

    @Override
    public String rejectApplication(int registrationId, String reason) {
        if (registrationId <= 0) return "Đơn đăng ký không hợp lệ.";
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) return "Vui lòng nhập lý do từ chối.";
        if (normalizedReason.length() > 1000) return "Lý do từ chối không được vượt quá 1000 ký tự.";
        RegistrationApplication application = dao.findApplicationById(registrationId);
        if (application != null && dao.isQuickRegistrationForm(application.getFormId())) {
            return "Không thể từ chối đơn đăng ký nhanh (không cần duyệt).";
        }
        if (dao.reviewApplication(registrationId, "SUBMITTED", "REJECTED", normalizedReason)) return null;
        if (dao.hasEarlierPendingApplication(registrationId)) {
            return "Không thể từ chối đơn này vì còn đơn chờ xử lý được gửi trước.";
        }
        return "Chỉ có thể từ chối đơn đang chờ xử lý.";
    }

    @Override
    public String rejectApplications(int formId, List<Integer> registrationIds, String reason) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (formId <= 0 || registrationIds == null || registrationIds.isEmpty()) {
            return "Hãy chọn ít nhất một đơn đang chờ xử lý.";
        }
        if (normalizedReason.isEmpty()) return "Vui lòng nhập lý do từ chối.";
        if (normalizedReason.length() > 1000) return "Lý do từ chối không được vượt quá 1000 ký tự.";
        if (registrationIds.size() > 100) return "Chỉ có thể từ chối tối đa 100 đơn trong một lần.";
        if (registrationIds.stream().anyMatch(id -> id == null || id <= 0)
                || registrationIds.stream().distinct().count() != registrationIds.size()) {
            return "Danh sách đơn đăng ký không hợp lệ.";
        }
        if (dao.isQuickRegistrationForm(formId)) {
            return "Không thể từ chối đơn đăng ký nhanh (không cần duyệt).";
        }
        return dao.rejectApplications(formId, registrationIds, normalizedReason)
                ? null
                : "Không thể từ chối các đơn đã chọn. Hãy chọn các đơn đang chờ theo thứ tự gửi và thử lại.";
    }

    @Override
    public String approveApplications(int formId, List<Integer> registrationIds) {
        String selectionError = validateBulkApplicationSelection(formId, registrationIds);
        if (selectionError != null) return selectionError;
        if (dao.isQuickRegistrationForm(formId)) {
            return "Không thể duyệt đơn đăng ký nhanh (không cần duyệt).";
        }
        return dao.approveApplications(formId, registrationIds)
                ? null
                : "Không thể duyệt các đơn đã chọn. Hãy chọn các đơn đang chờ theo thứ tự gửi và thử lại.";
    }

    private static String validateBulkApplicationSelection(int formId, List<Integer> registrationIds) {
        if (formId <= 0 || registrationIds == null || registrationIds.isEmpty()) {
            return "Hãy chọn ít nhất một đơn đang chờ xử lý.";
        }
        if (registrationIds.size() > 100) return "Chỉ có thể xử lý tối đa 100 đơn trong một lần.";
        if (registrationIds.stream().anyMatch(id -> id == null || id <= 0)
                || registrationIds.stream().distinct().count() != registrationIds.size()) {
            return "Danh sách đơn đăng ký không hợp lệ.";
        }
        return null;
    }

    @Override
    public String undoApplicationRejection(int registrationId) {
        if (registrationId <= 0) return "Đơn đăng ký không hợp lệ.";
        RegistrationApplication application = dao.findApplicationById(registrationId);
        if (application != null && dao.isQuickRegistrationForm(application.getFormId())) {
            return "Không thể thao tác trên đơn đăng ký nhanh.";
        }
        if (dao.reviewApplication(registrationId, "REJECTED", "SUBMITTED", null)) return null;
        if (application != null && "REJECTED".equals(application.getStatus())
                && dao.isAtCapacity(application.getFormId())) {
            return "Không thể khôi phục đơn vì hoạt động đã đủ số lượng tối đa.";
        }
        if (application != null && "REJECTED".equals(application.getStatus())
                && !dao.isActivityEligibleForApplicationReview(application.getFormId())) {
            return "Không thể đưa đơn trở lại hàng chờ vì hoạt động đã bắt đầu, bị hủy hoặc không còn đủ điều kiện.";
        }
        return "Chỉ có thể hủy từ chối đơn đã bị từ chối.";
    }

    @Override
    public String undoApplicationApproval(int registrationId) {
        if (registrationId <= 0) return "Đơn đăng ký không hợp lệ.";
        RegistrationApplication application = dao.findApplicationById(registrationId);
        if (application != null && dao.isQuickRegistrationForm(application.getFormId())) {
            return "Không thể thao tác trên đơn đăng ký nhanh.";
        }
        if (dao.reviewApplication(registrationId, "APPROVED", "SUBMITTED", null)) return null;
        if (application != null && "APPROVED".equals(application.getStatus())
                && !dao.isActivityEligibleForApplicationReview(application.getFormId())) {
            return "Không thể hủy duyệt vì hoạt động đã bắt đầu, bị hủy hoặc không còn đủ điều kiện.";
        }
        return "Chỉ có thể hủy duyệt đơn đã được duyệt.";
    }

    public String save(RegistrationForm f) {
        if (f.getActivityId() <= 0) return "Vui lòng chọn hoạt động.";
        if (!dao.isActivityApproved(f.getActivityId())) {
            return "Chỉ được tạo hoặc chỉnh sửa form cho hoạt động đã được duyệt.";
        }
        if (!dao.isActivityUpcoming(f.getActivityId())) {
            return "Chỉ được tạo hoặc chỉnh sửa form đăng ký cho hoạt động ở trạng thái Chưa diễn ra.";
        }
        // Reject if activity doesn't require registration form
        if (!dao.isActivityFormRequired(f.getActivityId())) {
            return "Hoạt động này không yêu cầu form đăng ký (đăng ký nhanh).";
        }
        if (dao.hasFormForActivity(f.getActivityId(), f.getId())) {
            return "Hoạt động này đã có form đăng ký. Mỗi hoạt động chỉ được tạo một form.";
        }
        if (f.getTitle() == null || f.getTitle().isBlank()) return "Vui lòng nhập tiêu đề form.";
        if (f.getTitle().trim().length() > 200) return "Tiêu đề không được vượt quá 200 ký tự.";
        if (f.getDescription() != null && f.getDescription().length() > 4000)
            return "Mô tả không được vượt quá 4.000 ký tự.";
        if (f.getOpenAt() == null) return "Vui lòng chọn thời gian mở đăng ký.";
        if (f.getCloseAt() == null) return "Vui lòng chọn thời gian đóng đăng ký.";
        if (!f.getCloseAt().isAfter(f.getOpenAt()))
            return "Thời gian đóng đăng ký phải lớn hơn thời gian mở đăng ký.";
        LocalDateTime activityStartTime = dao.findActivityStartTime(f.getActivityId());
        if (activityStartTime == null) return "Hoạt động chưa có thời gian bắt đầu hợp lệ.";
        if (!f.getOpenAt().isBefore(activityStartTime))
            return "Thời gian mở đăng ký phải trước thời gian bắt đầu hoạt động.";
        if (f.getCloseAt().isAfter(activityStartTime.minusHours(24)))
            return "Thời gian đóng đăng ký phải trước thời gian bắt đầu hoạt động ít nhất 24 giờ.";
        LocalDateTime now = DateTimeUtil.now();
        RegistrationForm existingForm = f.getId() > 0 ? getById(f.getId()) : null;
        boolean openingTimeUnchanged = existingForm != null
                && Objects.equals(f.getOpenAt(), existingForm.getOpenAt());
        if (f.getOpenAt().isBefore(now) && !openingTimeUnchanged)
            return "Thời gian mở đăng ký không được ở quá khứ.";
        if (!f.getCloseAt().isAfter(now))
            return "Thời gian đóng đăng ký phải ở tương lai.";
        f.setTitle(f.getTitle().trim());
        f.setDescription(f.getDescription() == null ? "" : f.getDescription().trim());
        
        if (f.getOpenAt().isAfter(now)) {
            f.setStatus(RegistrationFormStatus.DRAFT.name());
        } else {
            f.setStatus(RegistrationFormStatus.OPENING.name());
        }
        String fieldError = validateFields(f.getFields());
        if (fieldError != null) return fieldError;
        boolean saved = f.getId() > 0 ? dao.update(f) : dao.insert(f);
        if (saved) {
            new com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl().refreshPublic(f.getActivityId());
            return null;
        }
        if (!dao.isActivityApproved(f.getActivityId())) {
            return "Không thể lưu form vì hoạt động chưa được duyệt.";
        }
        if (!dao.isActivityUpcoming(f.getActivityId())) {
            return "Không thể lưu form vì hoạt động không còn ở trạng thái Chưa diễn ra.";
        }
        if (dao.hasFormForActivity(f.getActivityId(), f.getId())) {
            return "Hoạt động này đã có form đăng ký. Mỗi hoạt động chỉ được tạo một form.";
        }
        now = DateTimeUtil.now();
        if (f.getOpenAt() != null && f.getOpenAt().isBefore(now) && !openingTimeUnchanged) {
            return "Thời gian mở đăng ký không được ở quá khứ.";
        }
        if (f.getCloseAt() != null && !f.getCloseAt().isAfter(now)) {
            return "Thời gian đóng đăng ký phải ở tương lai.";
        }
        return f.getId() > 0
                ? "Không thể sửa form vì đã có sinh viên đăng ký hoặc form không còn tồn tại."
                : "Không thể tạo đợt đăng ký. Vui lòng kiểm tra dữ liệu và thử lại.";
    }

    public String saveQuickForm(RegistrationForm f) {
        RegistrationForm existing = f.getId() > 0 ? getById(f.getId()) : null;
        if (existing == null || !existing.isQuick()) return "Không tìm thấy form đăng ký nhanh.";
        if (f.getDescription() != null && f.getDescription().length() > 4000)
            return "Mô tả không được vượt quá 4.000 ký tự.";
        if (f.getOpenAt() == null) return "Vui lòng chọn thời gian mở đăng ký.";
        if (f.getCloseAt() == null) return "Vui lòng chọn thời gian đóng đăng ký.";
        if (!f.getCloseAt().isAfter(f.getOpenAt()))
            return "Thời gian đóng đăng ký phải lớn hơn thời gian mở đăng ký.";
        LocalDateTime activityStartTime = dao.findActivityStartTime(existing.getActivityId());
        if (activityStartTime == null) return "Hoạt động chưa có thời gian bắt đầu hợp lệ.";
        if (!f.getOpenAt().isBefore(activityStartTime))
            return "Thời gian mở đăng ký phải trước thời gian bắt đầu hoạt động.";
        if (f.getCloseAt().isAfter(activityStartTime.minusHours(24)))
            return "Thời gian đóng đăng ký phải trước thời gian bắt đầu hoạt động ít nhất 24 giờ.";
        if (!f.getCloseAt().isAfter(DateTimeUtil.now()))
            return "Thời gian đóng đăng ký phải ở tương lai.";
        LocalDateTime now = DateTimeUtil.now();
        String status;
        if (f.getOpenAt().isAfter(now)) {
            status = RegistrationFormStatus.DRAFT.name();
        } else {
            status = RegistrationFormStatus.OPENING.name();
        }
        String description = f.getDescription() == null ? "" : f.getDescription().trim();
        if (!dao.updateQuickForm(existing.getId(), description, f.getOpenAt(), f.getCloseAt(), status)) {
            return "Không thể lưu form đăng ký nhanh. Vui lòng tải lại trang và thử lại.";
        }
        return null;
    }

    @Override
    public String extendCloseTime(int formId, LocalDateTime closeAt) {
        RegistrationForm form = getById(formId);
        if (form == null) return "Không tìm thấy form đăng ký.";
        if (form.isQuick()) return "Không thể gia hạn form đăng ký nhanh.";
        if (form.getRegistrationCount() == 0) return "Chỉ có thể gia hạn form đã có sinh viên đăng ký.";
        if (form.getCloseAt() == null || form.getCloseAt().isAfter(DateTimeUtil.now())) {
            return "Chỉ có thể gia hạn sau khi form đã đóng.";
        }
        if (closeAt == null) return "Vui lòng chọn thời gian đóng đăng ký.";
        if (!closeAt.isAfter(DateTimeUtil.now())) return "Thời gian đóng đăng ký phải ở tương lai.";
        if (form.getOpenAt() != null && !closeAt.isAfter(form.getOpenAt())) {
            return "Thời gian đóng mới phải sau thời gian mở đăng ký.";
        }
        LocalDateTime start = dao.findActivityStartTime(form.getActivityId());
        if (start == null || closeAt.isAfter(start.minusHours(24)))
            return "Thời gian đóng đăng ký phải trước thời gian bắt đầu hoạt động ít nhất 24 giờ.";
        return dao.extendCloseTime(formId, closeAt) ? null
                : "Không thể gia hạn: kiểm tra hoạt động còn ở trạng thái Chưa diễn ra.";
    }

    public String delete(int id) {
        RegistrationForm form = dao.findById(id);
        if (form == null) return "Không tìm thấy form đăng ký.";
        if (form.isQuick()) {
            return "Form của hoạt động đăng ký nhanh do hệ thống quản lý nên không thể xóa.";
        }
        if (!dao.delete(id)) {
            return "Không thể xóa form vì còn đơn đăng ký đang chờ, đã duyệt, đã từ chối hoặc đã hủy. "
                    + "Hãy chuyển form sang bản nháp thay vì xóa.";
        }
        new com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl().refreshPublic(form.getActivityId());
        return null;
    }

    @Override
    public void synchronizeFormStatuses() {
        dao.synchronizeFormStatuses();
    }

    @Override
    public String registerQuick(int activityId, int studentId, String note) {
        if (activityId <= 0 || studentId <= 0) return "Thông tin đăng ký không hợp lệ.";
        if (note != null && note.length() > 2000) return "Ghi chú không được vượt quá 2.000 ký tự.";
        String result = dao.registerStudentQuick(activityId, studentId, note == null ? "" : note.trim());
        return switch (result) {
            case "OK" -> null;
            case "ALREADY" -> "Bạn đã đăng ký hoạt động này.";
            case "FULL" -> "Không thể đăng ký: hoạt động đã đủ số lượng tối đa.";
            case "NOT_OPEN" -> "Không thể đăng ký: đợt đăng ký chưa đến thời gian mở hoặc đã đóng.";
            case "UNAVAILABLE" -> "Không thể đăng ký: hoạt động chưa được duyệt, đã bắt đầu hoặc không cho phép đăng ký nhanh.";
            case "CANCELLED_BEFORE" -> "Bạn đã hủy đăng ký hoạt động này sau khi được duyệt nên không thể đăng ký lại. Vui lòng liên hệ ban tổ chức nếu cần.";
            default -> "Không thể đăng ký: lỗi không xác định.";
        };
    }

    @Override
    public String registerWithAnswers(int formId, int studentId, String note, List<RegistrationFormField> answers,
                                      Map<String, String> personalInfo, boolean attendanceCommitment) {
        if (formId <= 0 || studentId <= 0) return "Thông tin đăng ký không hợp lệ.";
        if (note != null && note.length() > 2000) return "Ghi chú không được vượt quá 2.000 ký tự.";
        if (!attendanceCommitment) return "Vui lòng xác nhận cam kết tham dự hoạt động.";

        RegistrationForm form = getById(formId);
        if (form == null) return "Không tìm thấy form đăng ký.";
        Map<String, RegistrationFormField> submitted = new HashMap<>();
        List<RegistrationFormField> answerList = answers == null ? new ArrayList<>() : new ArrayList<>(answers);
        for (RegistrationFormField answer : answerList) {
            if (answer == null || answer.getFieldKey() == null
                    || submitted.put(answer.getFieldKey(), answer) != null) {
                return "Thông tin câu trả lời không hợp lệ.";
            }
        }
        if (submitted.size() != form.getFields().size()) return "Vui lòng tải lại form và gửi lại đăng ký.";

        for (RegistrationFormField field : form.getFields()) {
            RegistrationFormField answer = submitted.get(field.getFieldKey());
            if (answer == null) return "Vui lòng tải lại form và gửi lại đăng ký.";
            String value = answer.getValue() == null ? "" : answer.getValue().trim();
            if (field.isRequired() && value.isEmpty()) {
                return "Vui lòng nhập " + field.getLabel() + ".";
            }
            if (field.isRequired() && "CHECKBOX".equals(field.getFieldType()) && !"true".equals(value)) {
                return "Vui lòng xác nhận " + field.getLabel() + ".";
            }
            if (value.length() > 4000) return "Câu trả lời không được vượt quá 4.000 ký tự.";
            if (!isValidAnswer(field, value)) return "Giá trị của trường \"" + field.getLabel() + "\" không hợp lệ.";
            answer.setId(field.getId());
            answer.setFormId(formId);
            answer.setFieldKey(field.getFieldKey());
            answer.setFieldType(field.getFieldType());
            answer.setSaveToProfile(field.isSaveToProfile());
        }

        String personalInfoError = appendPersonalInfo(studentId, personalInfo, answerList);
        if (personalInfoError != null) return personalInfoError;
        answerList.add(personalAnswer("application_attendance_commitment",
                "Tôi cam kết tham dự đầy đủ và đúng giờ.", "true"));
        if (dao.registerStudentWithAnswers(formId, studentId, note == null ? "" : note.trim(), answerList)) return null;
        RegistrationForm currentForm = getById(formId);
        if (currentForm != null && !sameFields(form.getFields(), currentForm.getFields())) {
            return "Form vừa được cập nhật. Vui lòng tải lại trang và gửi lại đăng ký.";
        }
        if (dao.isOpenFormAtCapacity(formId)) return "Không thể đăng ký: hoạt động đã đủ số lượng tối đa.";
        return "Không thể đăng ký: form đã đóng, hoạt động chưa được duyệt hoặc bạn đã đăng ký trước đó.";
    }

    @Override
    public Map<String, String> getSavedProfile(int studentId) {
        return studentId > 0 ? dao.findSavedProfile(studentId) : Map.of();
    }

    @Override
    public String cancelRegistration(int registrationId) {
        if (registrationId <= 0) return "Đơn đăng ký không hợp lệ.";
        if (dao.cancelRegistration(registrationId)) return null;
        // If UPDATE affected 0 rows, check the reason
        String restrictionReason = dao.getCancelRestrictionReason(registrationId);
        return restrictionReason != null ? restrictionReason : "Không thể hủy đăng ký.";
    }



    private static String validateFields(List<RegistrationFormField> fields) {
        if (fields == null) return null;
        if (fields.size() > 10) return "Mỗi form chỉ được có tối đa 10 câu hỏi bổ sung.";
        Map<String, Boolean> keys = new HashMap<>();
        for (RegistrationFormField field : fields) {
            if (field == null || field.getFieldKey() == null
                    || !field.getFieldKey().matches("[a-z][a-z0-9_]{0,79}")) {
                return "Định danh trường đăng ký không hợp lệ.";
            }
            if (keys.put(field.getFieldKey(), Boolean.TRUE) != null) return "Định danh trường không được trùng nhau.";
            if (field.getLabel() == null || field.getLabel().isBlank() || field.getLabel().length() > 200) {
                return "Tên trường phải có nội dung và không vượt quá 200 ký tự.";
            }
            if (field.getFieldType() == null
                    || !List.of("TEXT", "TEXTAREA", "NUMBER", "DATE", "EMAIL", "PHONE",
                    "SELECT", "CHECKBOX", "MULTISELECT")
                    .contains(field.getFieldType())) {
                return "Loại trường đăng ký không hợp lệ.";
            }
            if ("SELECT".equals(field.getFieldType()) || "MULTISELECT".equals(field.getFieldType())) {
                String[] rawOptions = field.getOptions() == null
                        ? new String[0] : field.getOptions().split("\\R", -1);
                List<String> options = parseOptions(field);
                if (options.isEmpty()) return "Câu hỏi lựa chọn cần có ít nhất một đáp án.";
                if (rawOptions.length > 10) return "Mỗi câu hỏi chỉ được có tối đa 10 đáp án.";
                if (Arrays.stream(rawOptions).anyMatch(option -> option.isBlank())) {
                    return "Vui lòng nhập đầy đủ các đáp án lựa chọn.";
                }
                if (options.stream().anyMatch(option -> option.length() > 200)) {
                    return "Mỗi đáp án không được vượt quá 200 ký tự.";
                }
                if (options.size() != rawOptions.length) return "Đáp án lựa chọn không được trùng nhau.";
            }
        }
        return null;
    }

    private static String appendPersonalInfo(int studentId, Map<String, String> personalInfo,
                                             List<RegistrationFormField> answers) {
        if (personalInfo == null) return "Vui lòng nhập đầy đủ thông tin sinh viên.";
        String studentCode = value(personalInfo, "studentCode");
        String studentName = value(personalInfo, "studentName");
        String classCode = value(personalInfo, "classCode");
        String email = value(personalInfo, "email");
        String phoneNumber = value(personalInfo, "phoneNumber");
        String dob = value(personalInfo, "dob");
        if (studentCode.isBlank() || studentCode.length() > 50) {
            return "Mã sinh viên là bắt buộc và không được vượt quá 50 ký tự.";
        }
        if (studentName.isBlank() || studentName.length() > 255) {
            return "Họ và tên là bắt buộc và không được vượt quá 255 ký tự.";
        }
        if (classCode.isBlank() || classCode.length() > 50) {
            return "Lớp là bắt buộc và không được vượt quá 50 ký tự.";
        }
        if (email.isBlank() || email.length() > 255 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            return "Vui lòng nhập địa chỉ email hợp lệ (tối đa 255 ký tự).";
        }
        if (phoneNumber.isBlank() || phoneNumber.length() > 30) {
            return "Hồ sơ sinh viên chưa có số điện thoại hợp lệ. Vui lòng cập nhật hồ sơ trước khi đăng ký.";
        }
        if (dob.isBlank()) {
            return "Hồ sơ sinh viên chưa có ngày sinh. Vui lòng cập nhật hồ sơ trước khi đăng ký.";
        }
        try {
            LocalDate.parse(dob);
        } catch (DateTimeParseException e) {
            return "Ngày sinh trong hồ sơ không hợp lệ. Vui lòng cập nhật hồ sơ trước khi đăng ký.";
        }
        for (RegistrationFormField answer : answers) {
            if (List.of("application_student_id", "application_student_code", "application_student_name",
                    "application_class_code", "application_email", "application_phone_number",
                    "application_dob", "application_attendance_commitment").contains(answer.getFieldKey())) {
                return "Form có trường trùng với thông tin hồ sơ sinh viên.";
            }
        }
        answers.add(personalAnswer("application_student_id", "ID", Integer.toString(studentId)));
        answers.add(personalAnswer("application_student_code", "Mã sinh viên", studentCode));
        answers.add(personalAnswer("application_student_name", "Họ và tên", studentName));
        answers.add(personalAnswer("application_class_code", "Lớp", classCode));
        answers.add(personalAnswer("application_email", "Email", email));
        answers.add(personalAnswer("application_phone_number", "Số điện thoại", phoneNumber));
        answers.add(personalAnswer("application_dob", "Ngày sinh", dob));
        return null;
    }

    private static RegistrationFormField personalAnswer(String key, String label, String value) {
        RegistrationFormField answer = new RegistrationFormField();
        answer.setFieldKey(key);
        answer.setLabel(label);
        answer.setValue(value);
        return answer;
    }

    private static String value(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null ? "" : value.trim();
    }

    private static boolean sameFields(List<RegistrationFormField> first, List<RegistrationFormField> second) {
        if (first == null || second == null || first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            RegistrationFormField a = first.get(i);
            RegistrationFormField b = second.get(i);
            if (a.getId() != b.getId()
                    || !Objects.equals(a.getFieldKey(), b.getFieldKey())
                    || !Objects.equals(a.getFieldType(), b.getFieldType())
                    || !Objects.equals(a.getLabel(), b.getLabel())
                    || !Objects.equals(a.getOptions(), b.getOptions())
                    || a.isRequired() != b.isRequired()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidAnswer(RegistrationFormField field, String value) {
        if (value.isEmpty()) return true;
        switch (field.getFieldType()) {
            case "NUMBER":
                try {
                    new BigDecimal(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "DATE":
                try {
                    LocalDate.parse(value);
                    return true;
                } catch (DateTimeParseException e) {
                    return false;
                }
            case "EMAIL":
                return value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
            case "SELECT":
                return parseOptions(field).contains(value);
            case "MULTISELECT":
                return value.lines().map(String::trim).filter(option -> !option.isEmpty())
                        .allMatch(parseOptions(field)::contains);
            case "CHECKBOX":
                return "true".equals(value) || "false".equals(value);
            default:
                return true;
        }
    }

    private static List<String> parseOptions(RegistrationFormField field) {
        return field.getOptions() == null || field.getOptions().isBlank()
                ? List.of()
                : field.getOptions().lines().map(String::trim).filter(option -> !option.isEmpty()).distinct().toList();
    }
}