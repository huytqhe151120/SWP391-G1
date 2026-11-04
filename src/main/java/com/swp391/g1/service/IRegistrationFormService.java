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

    String reviewApplication(int registrationId);

    String rejectApplication(int registrationId, String reason);

    String rejectApplications(int formId, List<Integer> registrationIds, String reason);

    String approveApplications(int formId, List<Integer> registrationIds);

    String undoApplicationRejection(int registrationId);

    String undoApplicationApproval(int registrationId);

    String save(RegistrationForm form);

    void synchronizeFormStatuses();

    String delete(int id);

    String registerQuick(int activityId, int studentId, String note);

    String registerWithAnswers(int formId, int studentId, String note, List<RegistrationFormField> answers,
                               Map<String, String> personalInfo, boolean attendanceCommitment);

    String cancelRegistration(int registrationId);

    Map<String, String> getSavedProfile(int studentId);

    String saveQuickForm(RegistrationForm f);
}