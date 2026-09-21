package com.swp391.g1.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class PartnerStaff implements Serializable {

    private static final long serialVersionUID = 1L;

    // Các trường dữ liệu (Fields)
    private Integer id;
    private String fullName;    // Mandatory
    private String email;       // Mandatory
    private String phone;       // Mandatory
    private String position;    // Optional
    private Boolean isActive;   // Mandatory
    private Integer partnerId;   // Mandatory (Khóa ngoại trỏ sang Partner)
    private Timestamp createdAt; // Mandatory

    // 1. Constructor KHÔNG tham số
    public PartnerStaff() {
    }

    // 2. Constructor với các tham số BẮT BUỘC
    public PartnerStaff(String fullName, String email, String phone, Boolean isActive, Integer partnerId, Timestamp createdAt) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.isActive = isActive;
        this.partnerId = partnerId;
        this.createdAt = createdAt;
    }

    // 3. Constructor FULL tham số
    public PartnerStaff(Integer id, String fullName, String email, String phone, String position, Boolean isActive, Integer partnerId, Timestamp createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.isActive = isActive;
        this.partnerId = partnerId;
        this.createdAt = createdAt;
    }

    // Getter và Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Integer getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PartnerStaff{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", position='" + position + '\'' +
                ", isActive=" + isActive +
                ", partnerId=" + partnerId +
                ", createdAt=" + createdAt +
                '}';
    }
}