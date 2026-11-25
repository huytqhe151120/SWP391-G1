package com.swp391.g1.service;

import com.swp391.g1.model.RegistrationForm;
import com.swp391.g1.model.RegistrationFormField;
import com.swp391.g1.model.RegistrationApplication;

import java.util.List;
import java.util.Map;

public interface IRegistrationFormService {
    List<RegistrationForm> getAll();

    RegistrationForm getById(int id);

    RegistrationForm getByActivityId(int activityId);

    RegistrationForm getOpenFormById(int id, int studentId);

    String getRegistrationStatus(int formId, int studentId);

    boolean isAtCapacity(int formId);

    boolean isOpenFormAtCapacity(int formId);

    boolean isActivityEligibleForApplicationReview(int formId);

    boolean hasEarlierPendingApplication(int registrationId);

    List<RegistrationApplication> getApplications(int formId);

    List<RegistrationApplication> getApplicationsForStudent(int studentId);

    RegistrationApplication getApplicationById(int registrationId);

    // Duyệt đơn
    String reviewApplication(int registrationId);

    // Từ chối đơn
    String rejectApplication(int registrationId, String reason);

    // Từ chối nhiều đơn
    String rejectApplications(int formId, List<Integer> registrationIds, String reason);

    // Duyệt nhiều đơn
    String approveApplications(int formId, List<Integer> registrationIds);

    // Thu hồi kết quả Từ chối
    String undoApplicationRejection(int registrationId);

    // Thu hồi kết quả Duyệt
    String undoApplicationApproval(int registrationId);

    // Điều kiện staff tạo form đăng ký phải điền đầy đủ các trường bắt buộc, nếu không sẽ báo lỗi.
    String save(RegistrationForm form);

    void synchronizeFormStatuses();

    String delete(int id);

    // Đăng ký nhanh
    String registerQuick(int activityId, int studentId, String note);

    // Kiểm tra câu trả lời, cam kết và thông tin sinh viên
    String registerWithAnswers(int formId, int studentId, String note, List<RegistrationFormField> answers,
                               Map<String, String> personalInfo, boolean attendanceCommitment);

    // Sinh viên hủy đăng ký
    String cancelRegistration(int registrationId);

    Map<String, String> getSavedProfile(int studentId);

    // Staff tạo form đăng ký nhanh
    String saveQuickForm(RegistrationForm f);

    String extendCloseTime(int formId, java.time.LocalDateTime closeAt);
}