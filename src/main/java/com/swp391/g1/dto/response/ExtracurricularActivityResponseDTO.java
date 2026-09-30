package com.swp391.g1.dto.response;

import com.swp391.g1.model.Enum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExtracurricularActivityResponseDTO {
    private int id;
    private String code;
    private String name;
    private int semesterId;
    private int activityTypeId;
    private int responsibleDepartmentId;
    private int responsibleStaffId;
    private Integer partnerCompanyId;          // Nullable
    private Integer partnerStaffId;            // Nullable

    // The information has been JOIN/Mapping for display in the user interface.
    private String semesterCode;                  // Join from Semester
    private String semesterName;                  // Join from Semester
    private String activityTypeCode;              // Join from ActivityType
    private String activityTypeName;              // Join from ActivityType
    private String responsibleDepartmentCode;     // Join from Department
    private String responsibleDepartmentName;     // Join from Department
    private String responsibleStaffCode;          // Join from Staff
    private String responsibleStaffName;          // Join from Staff
    private String partnerCompanyCode;              // Join from PartnerCompany (nullable)
    private String partnerCompanyName;           // Join from PartnerCompany
    private String partnerStaffCode;                // Join from PartnerStaff (nullable)
    private String partnerStaffName;             // Join from PartnerStaff (nullable)
    private BigDecimal bonusPoint;
    private BigDecimal penaltyPoint;
    private String address;
    private String description;
    private Enum.ActivityStatus activityStatus;        // UPCOMING, ONGOING, COMPLETED, CANCELLED[cite: 1]
    private Enum.ApprovalStatus approvalStatus;        // DRAFT, PENDING, APPROVED, REJECTED[cite: 1]
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Constructor with no parameter
    public ExtracurricularActivityResponseDTO() {
    }

    // Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public String getSemesterName() {
        return semesterName;
    }

    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public String getActivityTypeName() {
        return activityTypeName;
    }

    public void setActivityTypeName(String activityTypeName) {
        this.activityTypeName = activityTypeName;
    }

    public String getResponsibleDepartmentName() {
        return responsibleDepartmentName;
    }

    public void setResponsibleDepartmentName(String responsibleDepartmentName) {
        this.responsibleDepartmentName = responsibleDepartmentName;
    }

    public String getResponsibleStaffName() {
        return responsibleStaffName;
    }

    public void setResponsibleStaffName(String responsibleStaffName) {
        this.responsibleStaffName = responsibleStaffName;
    }

    public String getPartnerCompanyName() {
        return partnerCompanyName;
    }

    public void setPartnerCompanyName(String partnerCompanyName) {
        this.partnerCompanyName = partnerCompanyName;
    }

    public String getPartnerStaffName() {
        return partnerStaffName;
    }

    public void setPartnerStaffName(String partnerStaffName) {
        this.partnerStaffName = partnerStaffName;
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

    public String getSemesterCode() {
        return semesterCode;
    }

    public void setSemesterCode(String semesterCode) {
        this.semesterCode = semesterCode;
    }

    public String getActivityTypeCode() {
        return activityTypeCode;
    }

    public void setActivityTypeCode(String activityTypeCode) {
        this.activityTypeCode = activityTypeCode;
    }

    public String getResponsibleDepartmentCode() {
        return responsibleDepartmentCode;
    }

    public void setResponsibleDepartmentCode(String responsibleDepartmentCode) {
        this.responsibleDepartmentCode = responsibleDepartmentCode;
    }

    public String getResponsibleStaffCode() {
        return responsibleStaffCode;
    }

    public void setResponsibleStaffCode(String responsibleStaffCode) {
        this.responsibleStaffCode = responsibleStaffCode;
    }

    public String getPartnerCompanyCode() {
        return partnerCompanyCode;
    }

    public void setPartnerCompanyCode(String partnerCompanyCode) {
        this.partnerCompanyCode = partnerCompanyCode;
    }

    public String getPartnerStaffCode() {
        return partnerStaffCode;
    }

    public void setPartnerStaffCode(String partnerStaffCode) {
        this.partnerStaffCode = partnerStaffCode;
    }

    public int getPartnerCompanyId() {
        return partnerCompanyId != null ? partnerCompanyId : 0;
    }
}
