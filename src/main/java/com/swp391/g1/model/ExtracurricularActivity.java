package com.swp391.g1.model;

import java.sql.Timestamp;

public class ExtracurricularActivity {

    private int id;
    private int semesterId;
    private String activityType;
    private String code;
    private String name;
    private int responsibleDepartmentId;
    private int responsibleStaffId;
    private int partnerId;
    private int partnerStaffId;
    private double bonusPoint;
    private double penaltyPoint;
    private String address;
    private String description;
    private String activityStatus;
    private String approvalStatus;
    private Timestamp createdAt;

    // Default constructor (bắt buộc đối với JavaBean)
    public ExtracurricularActivity() {
    }

    // Constructor đầy đủ tham số
    public ExtracurricularActivity(int semesterId, 
        String activityType, 
        String code, 
        String name,
        int responsibleDepartmentId, 
        int responsibleStaffId, 
        int partnerId,                           
        int partnerStaffId, 
        double bonusPoint, 
        double penaltyPoint,
        String address, 
        String description, 
        String activityStatus, 
        String approvalStatus,
        Timestamp createdAt) {
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
        this.createdAt = createdAt;
    }

    // Getters and Setters
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

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
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

    public int getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(int partnerId) {
        this.partnerId = partnerId;
    }

    public int getPartnerStaffId() {
        return partnerStaffId;
    }

    public void setPartnerStaffId(int partnerStaffId) {
        this.partnerStaffId = partnerStaffId;
    }

    public double getBonusPoint() {
        return bonusPoint;
    }

    public void setBonusPoint(double bonusPoint) {
        this.bonusPoint = bonusPoint;
    }

    public double getPenaltyPoint() {
        return penaltyPoint;
    }

    public void setPenaltyPoint(double penaltyPoint) {
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

    public String getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
