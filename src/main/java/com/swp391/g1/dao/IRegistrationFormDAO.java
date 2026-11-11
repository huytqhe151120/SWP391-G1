package com.swp391.g1.dao;

import com.swp391.g1.model.RegistrationForm;
import com.swp391.g1.model.RegistrationFormField;
import com.swp391.g1.model.RegistrationApplication;

import java.util.List;
import java.util.Map;

public interface IRegistrationFormDAO {
    List<RegistrationForm> findAll();

    RegistrationForm findById(int id);

    RegistrationForm findByActivityId(int activityId);

    RegistrationForm findOpenFormById(int id, int studentId);

    List<RegistrationApplication> findApplications(int formId);

    RegistrationApplication findApplicationById(int registrationId);

    boolean isActivityApproved(int activityId);

    boolean isActivityUpcoming(int activityId);

    boolean isActivityFormRequired(int activityId);

    boolean isQuickRegistrationForm(int formId);

    java.time.LocalDateTime findActivityStartTime(int activityId);

    List<RegistrationApplication> findApplicationsForStudent(int studentId);

    boolean isAtCapacity(int formId);

    boolean isOpenFormAtCapacity(int formId);

    boolean isActivityEligibleForApplicationReview(int formId);

    boolean hasEarlierPendingApplication(int registrationId);

    boolean hasFormForActivity(int activityId, int exceptFormId);

    void synchronizeFormStatuses();

    boolean insert(RegistrationForm form);

    boolean update(RegistrationForm form);

    boolean delete(int id);

    String registerStudentQuick(int activityId, int studentId, String note);

    String getRegistrationStatus(int formId, int studentId);

    boolean registerStudentWithAnswers(int formId, int studentId, String note,
                                       List<RegistrationFormField> answers);

    boolean reviewApplication(int registrationId, String expectedStatus, String newStatus,
                              String rejectionReason);

    boolean rejectApplications(int formId, List<Integer> registrationIds, String reason);

    boolean approveApplications(int formId, List<Integer> registrationIds);

    boolean cancelRegistration(int registrationId);

    String getCancelRestrictionReason(int registrationId);

    Map<String, String> findSavedProfile(int studentId);

    void ensureQuickForm(int activityId);

    boolean updateQuickForm(int formId, String description, java.time.LocalDateTime openAt,
                            java.time.LocalDateTime closeAt, String status);

    boolean extendCloseTime(int formId, java.time.LocalDateTime closeAt);
}