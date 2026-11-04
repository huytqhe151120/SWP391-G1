package com.swp391.g1.dto.response;

import com.swp391.g1.model.Enum.ActivityNature;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.Enum.ParticipantStatus;

import java.time.LocalDateTime;

public class StudentActivityAttendanceDTO {

    private int participantId;
    private int activityId;
    private String activityCode;
    private String activityName;
    private int semesterId;
    private String semesterCode;
    private String semesterName;
    private int activityTypeId;
    private String activityTypeCode;
    private String activityTypeName;
    private int responsibleDepartmentId;
    private String responsibleDepartmentCode;
    private String responsibleDepartmentName;
    private ActivityNature nature;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String address;
    private ActivityStatus activityStatus;
    private ApprovalStatus approvalStatus;
    private ParticipantStatus attendanceStatus;
    private LocalDateTime createdAt;

    public StudentActivityAttendanceDTO() {
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public String getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(String activityCode) {
        this.activityCode = activityCode;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(int semesterId) {
        this.semesterId = semesterId;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public void setSemesterCode(String semesterCode) {
        this.semesterCode = semesterCode;
    }

    public String getSemesterName() {
        return semesterName;
    }

    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public int getActivityTypeId() {
        return activityTypeId;
    }

    public void setActivityTypeId(int activityTypeId) {
        this.activityTypeId = activityTypeId;
    }

    public String getActivityTypeCode() {
        return activityTypeCode;
    }

    public void setActivityTypeCode(String activityTypeCode) {
        this.activityTypeCode = activityTypeCode;
    }

    public String getActivityTypeName() {
        return activityTypeName;
    }

    public void setActivityTypeName(String activityTypeName) {
        this.activityTypeName = activityTypeName;
    }

    public int getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public void setResponsibleDepartmentId(int responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
    }

    public String getResponsibleDepartmentCode() {
        return responsibleDepartmentCode;
    }

    public void setResponsibleDepartmentCode(String responsibleDepartmentCode) {
        this.responsibleDepartmentCode = responsibleDepartmentCode;
    }

    public String getResponsibleDepartmentName() {
        return responsibleDepartmentName;
    }

    public void setResponsibleDepartmentName(String responsibleDepartmentName) {
        this.responsibleDepartmentName = responsibleDepartmentName;
    }

    public ActivityNature getNature() {
        return nature;
    }

    public void setNature(ActivityNature nature) {
        this.nature = nature;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public ParticipantStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(ParticipantStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
