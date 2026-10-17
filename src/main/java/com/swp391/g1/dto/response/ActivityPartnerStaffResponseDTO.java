package com.swp391.g1.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityPartnerStaffResponseDTO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int activityId;
    private int partnerStaffId;
    private String partnerStaffCode;
    private String partnerStaffName;
    private String status;
    private String position;
    private String companyName;
    private int companyId;
    private LocalDateTime assignedOn;

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public int getPartnerStaffId() {
        return partnerStaffId;
    }

    public void setPartnerStaffId(int partnerStaffId) {
        this.partnerStaffId = partnerStaffId;
    }

    public String getPartnerStaffCode() {
        return partnerStaffCode;
    }

    public void setPartnerStaffCode(String partnerStaffCode) {
        this.partnerStaffCode = partnerStaffCode;
    }

    public String getPartnerStaffName() {
        return partnerStaffName;
    }

    public void setPartnerStaffName(String partnerStaffName) {
        this.partnerStaffName = partnerStaffName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public LocalDateTime getAssignedOn() {
        return assignedOn;
    }

    public void setAssignedOn(LocalDateTime assignedOn) {
        this.assignedOn = assignedOn;
    }

    public String getFormattedAssignedOn() {
        return assignedOn == null ? "—" : assignedOn.format(FORMATTER);
    }
}
