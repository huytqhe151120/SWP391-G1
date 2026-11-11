package com.swp391.g1.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityPartnerCompanyDTO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private int activityId;
    private int partnerCompanyId;
    private String partnerCompanyCode;
    private String partnerCompanyName;
    private int partnerStaffId;
    private String partnerStaffCode;
    private String partnerStaffName;
    private String partnerStaffPosition;
    private String collaborationStatus;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }
    public int getPartnerCompanyId() { return partnerCompanyId; }
    public void setPartnerCompanyId(int partnerCompanyId) { this.partnerCompanyId = partnerCompanyId; }
    public String getPartnerCompanyCode() { return partnerCompanyCode; }
    public void setPartnerCompanyCode(String partnerCompanyCode) { this.partnerCompanyCode = partnerCompanyCode; }
    public String getPartnerCompanyName() { return partnerCompanyName; }
    public void setPartnerCompanyName(String partnerCompanyName) { this.partnerCompanyName = partnerCompanyName; }
    public int getPartnerStaffId() { return partnerStaffId; }
    public void setPartnerStaffId(int partnerStaffId) { this.partnerStaffId = partnerStaffId; }
    public String getPartnerStaffCode() { return partnerStaffCode; }
    public void setPartnerStaffCode(String partnerStaffCode) { this.partnerStaffCode = partnerStaffCode; }
    public String getPartnerStaffName() { return partnerStaffName; }
    public void setPartnerStaffName(String partnerStaffName) { this.partnerStaffName = partnerStaffName; }
    public String getPartnerStaffPosition() { return partnerStaffPosition; }
    public void setPartnerStaffPosition(String partnerStaffPosition) { this.partnerStaffPosition = partnerStaffPosition; }
    public String getCollaborationStatus() { return collaborationStatus; }
    public void setCollaborationStatus(String collaborationStatus) { this.collaborationStatus = collaborationStatus; }
    public boolean isActive() { return "ACTIVE".equalsIgnoreCase(collaborationStatus); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedCreatedAt() {
        return createdAt == null ? "—" : createdAt.format(FORMATTER);
    }
}
