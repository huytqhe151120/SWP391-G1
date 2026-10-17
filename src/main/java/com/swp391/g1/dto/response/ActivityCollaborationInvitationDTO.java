package com.swp391.g1.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityCollaborationInvitationDTO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private int activityId;
    private int partnerCompanyId;
    private String partnerCompanyCode;
    private String partnerCompanyName;
    private Integer partnerStaffId;
    private String partnerStaffCode;
    private String partnerStaffName;
    private String partnerStaffPosition;
    private String approvalStatus;
    private String createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private String collaborationStatus;
    private String invitationType;
    private String message;
    private boolean canRespond;
    private boolean canRevoke;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public int getPartnerCompanyId() {
        return partnerCompanyId;
    }

    public void setPartnerCompanyId(int partnerCompanyId) {
        this.partnerCompanyId = partnerCompanyId;
    }

    public String getPartnerCompanyCode() {
        return partnerCompanyCode;
    }

    public void setPartnerCompanyCode(String partnerCompanyCode) {
        this.partnerCompanyCode = partnerCompanyCode;
    }

    public String getPartnerCompanyName() {
        return partnerCompanyName;
    }

    public void setPartnerCompanyName(String partnerCompanyName) {
        this.partnerCompanyName = partnerCompanyName;
    }

    public Integer getPartnerStaffId() {
        return partnerStaffId;
    }

    public void setPartnerStaffId(Integer partnerStaffId) {
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

    public String getPartnerStaffPosition() {
        return partnerStaffPosition;
    }

    public void setPartnerStaffPosition(String partnerStaffPosition) {
        this.partnerStaffPosition = partnerStaffPosition;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCollaborationStatus() {
        return collaborationStatus;
    }

    public void setCollaborationStatus(String collaborationStatus) {
        this.collaborationStatus = collaborationStatus;
    }

    public String getInvitationType() {
        return invitationType;
    }

    public void setInvitationType(String invitationType) {
        this.invitationType = invitationType;
    }

    public boolean isTermination() {
        return "TERMINATION".equalsIgnoreCase(invitationType);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCanRevoke() {
        return canRevoke;
    }

    public void setCanRevoke(boolean canRevoke) {
        this.canRevoke = canRevoke;
    }

    public boolean isCanRespond() {
        return canRespond;
    }

    public void setCanRespond(boolean canRespond) {
        this.canRespond = canRespond;
    }

    public String getFormattedCreatedAt() {
        return createdAt == null ? "—" : createdAt.format(FORMATTER);
    }
}
