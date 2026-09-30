package com.swp391.g1.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExtracurricularActivity {
    private int id; //[cite: 1]
    private int semesterId; //[cite: 1]
    private String code; //[cite: 1]
    private String name; //[cite: 1]
    private int responsibleDepartmentId; //[cite: 1]
    private int responsibleStaffId; //[cite: 1]
    private Integer partnerCompanyId; // Nullable[cite: 1]
    private Integer partnerStaffId; // Nullable[cite: 1]
    private BigDecimal bonusPoint; //[cite: 1]
    private BigDecimal penaltyPoint; //[cite: 1]
    private String address; //[cite: 1]
    private String description; //[cite: 1]
    private Enum.ActivityStatus activityStatus; //[cite: 1]
    private Enum.ApprovalStatus approvalStatus; //[cite: 1]
    private LocalDateTime createdAt; //[cite: 1]
    private int activityTypeId; //[cite: 1]
    private LocalDateTime startTime; //[cite: 1]
    private LocalDateTime endTime;

    // Constructor with no parameter
    public ExtracurricularActivity() {
    }

    //Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(int semesterId) {
        this.semesterId = semesterId;
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

    public int getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public void setResponsibleDepartmentId(int responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
    }

    public int getResponsibleStaffId() {
        return responsibleStaffId;
    }

    public void setResponsibleStaffId(int responsibleStaffId) {
        this.responsibleStaffId = responsibleStaffId;
    }

    public Integer getPartnerCompanyId() {
        return partnerCompanyId;
    }

    public void setPartnerCompanyId(Integer partnerCompanyId) {
        this.partnerCompanyId = partnerCompanyId;
    }

    public Integer getPartnerStaffId() {
        return partnerStaffId;
    }

    public void setPartnerStaffId(Integer partnerStaffId) {
        this.partnerStaffId = partnerStaffId;
    }

    public BigDecimal getBonusPoint() {
        return bonusPoint;
    }

    public void setBonusPoint(BigDecimal bonusPoint) {
        this.bonusPoint = bonusPoint;
    }

    public BigDecimal getPenaltyPoint() {
        return penaltyPoint;
    }

    public void setPenaltyPoint(BigDecimal penaltyPoint) {
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

    public Enum.ActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(Enum.ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
    }

    public Enum.ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(Enum.ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getActivityTypeId() {
        return activityTypeId;
    }

    public void setActivityTypeId(int activityTypeId) {
        this.activityTypeId = activityTypeId;
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
}
