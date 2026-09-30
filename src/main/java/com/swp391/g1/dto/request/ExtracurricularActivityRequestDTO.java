package com.swp391.g1.dto.request;

public class ExtracurricularActivityRequestDTO {
    private String id;
    private String code;
    private String name;
    private String bonusPoint;
    private String penaltyPoint;
    private String address;
    private String description;
    private String startTime;                // Received as String from form, then convert to LocalDateTime in the controller
    private String endTime;                  // Received as String from form, then convert to LocalDateTime in the controller
    private String semesterId;               // Received as String from form, then convert to int in the controller
    private String activityTypeId;           // Received as String from form, then convert to int in the controller
    private String responsibleDepartmentId;  // Received as String from form, then convert to int in the controller
    private String responsibleStaffId;       // Received as String from form, then convert to int in the controller
    private String partnerCompanyId;         // Received as String from form, then convert to int in the controller
    private String partnerStaffId;           // Received as String from form, then convert to int in the controller

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

    public String getBonusPoint() {
        return bonusPoint;
    }

    public void setBonusPoint(String bonusPoint) {
        this.bonusPoint = bonusPoint;
    }

    public String getPenaltyPoint() {
        return penaltyPoint;
    }

    public void setPenaltyPoint(String penaltyPoint) {
        this.penaltyPoint = penaltyPoint;
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
}
