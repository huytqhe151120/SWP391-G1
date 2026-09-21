package com.swp391.g1.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Partner implements Serializable {

    private static final long serialVersionUID = 1L;

    // Các trường dữ liệu (Fields)
    private int id;
    private String name;        // Mandatory
    private String email;       // Mandatory
    private String phone;       // Mandatory
    private String address;     // Optional
    private String status;      // Mandatory
    private Timestamp createdAt; // Mandatory

    // 1. Constructor KHÔNG tham số
    public Partner() {
    }

    // 2. Constructor với các tham số BẮT BUỘC
    public Partner(String name, String email, String phone, String status, Timestamp createdAt) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
    }

    // 3. Constructor FULL tham số
    public Partner(String name, String email, String phone, String address, String status, Timestamp createdAt) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getter và Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Partner{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

