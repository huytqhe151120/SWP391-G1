package com.swp391.g1.dto.request;

import java.util.ArrayList;
import java.util.List;

public class ExtracurricularActivityRequestDTO {
    private String id;
    private String code;
    private String name;
    private String nature;                  // REQUIRE_REGISTRATION, VOLUNTARY or MANDATORY
    private String summaryRequired;         // "true" when the toggle is on
    private String maxParticipants;
    private String attendanceSessionCount = "1";
    private String address;
    private String description;
    private String startTime;                // Received as String from form, then convert to LocalDateTime in the controller
    private String endTime;                  // Received as String from form, then convert to LocalDateTime in the controller
    private String semesterId;               // Received as String from form, then convert to int in the controller
    private String activityTypeId;           // Received as String from form, then convert to int in the controller
    private String organizationLevelId;      // Received as String from form, then convert to int in the controller
    private String responsibleDepartmentId;  // Received as String from form, then convert to int in the controller
    private String responsibleStaffId;       // Received as String from form, then convert to int in the controller
    private String partnerCompanyId;         // Received as String from form, then convert to int in the controller
    private String partnerStaffId;           // Received as String from form, then convert to int in the controller
    private String registrationFormRequired; // "true" when the toggle is on
    private String isPublic;
    private String targetScope = "ALL";      // ALL or GROUP
    private List<String> targetDepartmentIds = new ArrayList<>();
    private List<String> targetCohortIds = new ArrayList<>();
    private List<String> targetMajorIds = new ArrayList<>();
    private List<String> targetProgramIds = new ArrayList<>();
    private List<String> targetClassIds = new ArrayList<>();

    public String getIsPublic() { return isPublic; }
    public void setIsPublic(String isPublic) { this.isPublic = isPublic; }

    public String getRegistrationFormRequired() {
        return registrationFormRequired;
    }

    public void setRegistrationFormRequired(String registrationFormRequired) {
        this.registrationFormRequired = registrationFormRequired;
    }

    public ExtracurricularActivityRequestDTO() {
    }

    // --- Getters & Setters ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getSummaryRequired() {
        return summaryRequired;
    }

    public void setSummaryRequired(String summaryRequired) {
        this.summaryRequired = summaryRequired;
    }

    public String getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(String maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getAttendanceSessionCount() {
        return attendanceSessionCount;
    }

    public void setAttendanceSessionCount(String attendanceSessionCount) {
        this.attendanceSessionCount = attendanceSessionCount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(String semesterId) {
        this.semesterId = semesterId;
    }

    public String getActivityTypeId() {
        return activityTypeId;
    }

    public void setActivityTypeId(String activityTypeId) {
        this.activityTypeId = activityTypeId;
    }

    public String getOrganizationLevelId() {
        return organizationLevelId;
    }

    public void setOrganizationLevelId(String organizationLevelId) {
        this.organizationLevelId = organizationLevelId;
    }

    public String getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public void setResponsibleDepartmentId(String responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
    }

    public String getResponsibleStaffId() {
        return responsibleStaffId;
    }

    public void setResponsibleStaffId(String responsibleStaffId) {
        this.responsibleStaffId = responsibleStaffId;
    }

    public String getPartnerCompanyId() {
        return partnerCompanyId;
    }

    public void setPartnerCompanyId(String partnerCompanyId) {
        this.partnerCompanyId = partnerCompanyId;
    }

    public String getPartnerStaffId() {
        return partnerStaffId;
    }

    public void setPartnerStaffId(String partnerStaffId) {
        this.partnerStaffId = partnerStaffId;
    }

    public String getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(String targetScope) {
        this.targetScope = targetScope;
    }

    public List<String> getTargetDepartmentIds() {
        return targetDepartmentIds;
    }

    public void setTargetDepartmentIds(List<String> targetDepartmentIds) {
        this.targetDepartmentIds = targetDepartmentIds == null ? new ArrayList<>() : targetDepartmentIds;
    }

    public List<String> getTargetCohortIds() {
        return targetCohortIds;
    }

    public void setTargetCohortIds(List<String> targetCohortIds) {
        this.targetCohortIds = targetCohortIds == null ? new ArrayList<>() : targetCohortIds;
    }

    public List<String> getTargetMajorIds() {
        return targetMajorIds;
    }

    public void setTargetMajorIds(List<String> targetMajorIds) {
        this.targetMajorIds = targetMajorIds == null ? new ArrayList<>() : targetMajorIds;
    }

    public List<String> getTargetProgramIds() {
        return targetProgramIds;
    }

    public void setTargetProgramIds(List<String> targetProgramIds) {
        this.targetProgramIds = targetProgramIds == null ? new ArrayList<>() : targetProgramIds;
    }

    public List<String> getTargetClassIds() {
        return targetClassIds;
    }

    public void setTargetClassIds(List<String> targetClassIds) {
        this.targetClassIds = targetClassIds == null ? new ArrayList<>() : targetClassIds;
    }
}
