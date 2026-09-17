package com.swp391.g1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "extracurricular_activities")
public class ExtracurricularActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;
    
    @Column(name = "activity_type", nullable = false)
    private String activityType;
    
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column (name = "responsible_department_id")
    private Long responsibleDepartmentId;

    @Column (name = "responsible_staff_id")
    private Long responsibleStaffId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column (name = "partner_staff_id")
    private Long partnerStaffId;

    @Column(name = "bonus_point", nullable = false)
    private Integer bonusPoint;

    @Column(name = "penalty_point", nullable = false)
    private Integer penaltyPoint;

    private String address;

    private String description;

    @Column (name = "activity_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExtracurricularActivityStatus activityStatus;

    @Column (name = "approval_status", nullable = false)
    private String approvalStatus;

    protected ExtracurricularActivity() {
    }

    public ExtracurricularActivity(
            Long semesterId,
            String activityType,
            String code,
            String name,
            Integer bonusPoint,
            Integer penaltyPoint,
            ExtracurricularActivityStatus activityStatus,
            String approvalStatus
    ) {
        this.semesterId = semesterId;
        this.activityType = activityType;
        this.code = code;
        this.name = name;
        this.bonusPoint = bonusPoint;
        this.penaltyPoint = penaltyPoint;
        this.activityStatus = activityStatus;
        this.approvalStatus = approvalStatus;
    }

    public ExtracurricularActivity(
            Long semesterId,
            String activityType,
            String code,
            String name,
            Long responsibleDepartmentId,
            Long responsibleStaffId,
            Long partnerId,
            Long partnerStaffId,
            Integer bonusPoint,
            Integer penaltyPoint,
            String address,
            String description,
            ExtracurricularActivityStatus activityStatus,
            String approvalStatus
    ) {
        this.semesterId = semesterId;
        this.activityType = activityType;
        this.code = code;
        this.name = name;
        this.responsibleDepartmentId = responsibleDepartmentId;
        this.responsibleStaffId = responsibleStaffId;
        this.partnerId = partnerId;
        this.partnerStaffId = partnerStaffId;
        this.bonusPoint = bonusPoint;
        this.penaltyPoint = penaltyPoint;
        this.address = address;
        this.description = description;
        this.activityStatus = activityStatus;
        this.approvalStatus = approvalStatus;
    }

    public Long getId() {
        return id;
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

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Long getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public void setResponsibleDepartmentId(Long responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
    }

    public Long getResponsibleStaffId() {
        return responsibleStaffId;
    }

    public void setResponsibleStaffId(Long responsibleStaffId) {
        this.responsibleStaffId = responsibleStaffId;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public Long getPartnerStaffId() {
        return partnerStaffId;
    }

    public void setPartnerStaffId(Long partnerStaffId) {
        this.partnerStaffId = partnerStaffId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public Integer getBonusPoint() {
        return bonusPoint;
    }

    public void setBonusPoint(Integer bonusPoint) {
        this.bonusPoint = bonusPoint;
    }

    public Integer getPenaltyPoint() {
        return penaltyPoint;
    }

    public void setPenaltyPoint(Integer penaltyPoint) {
        this.penaltyPoint = penaltyPoint;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExtracurricularActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(ExtracurricularActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
}
