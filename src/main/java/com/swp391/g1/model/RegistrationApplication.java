package com.swp391.g1.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RegistrationApplication {
    private int id;
    private int formId;
    private int studentId;
    private String studentCode;
    private String studentName;
    private String classCode;
    private String departmentName;
    private String majorName;
    private String academicProgramName;
    private String email;
    private String phoneNumber;
    private String note;
    private String status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime pendingSince;
    private List<RegistrationFormField> answers = new ArrayList<>();
    private String formTitle;
    private String activityName;
    private String activityStatus;
    private int activityId;
    private boolean canCancel;
    private boolean quick;


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFormId() { return formId; }
    public void setFormId(int formId) { this.formId = formId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getAcademicProgramName() { return academicProgramName; }
    public void setAcademicProgramName(String academicProgramName) { this.academicProgramName = academicProgramName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getPendingSince() { return pendingSince; }
    public void setPendingSince(LocalDateTime pendingSince) { this.pendingSince = pendingSince; }

    public List<RegistrationFormField> getAnswers() { return answers; }
    public void setAnswers(List<RegistrationFormField> answers) {
        this.answers = answers == null ? new ArrayList<>() : answers;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getFormTitle() {
        return formTitle;
    }

    public void setFormTitle(String formTitle) {
        this.formTitle = formTitle;
    }

    public String getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }

    public boolean isQuick() {
        return quick;
    }

    public void setQuick(boolean quick) {
        this.quick = quick;
    }
}
