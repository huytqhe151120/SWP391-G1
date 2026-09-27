package com.swp391.g1.model;

import java.io.Serializable;

public class ActivityType implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String code;         // Mandatory
    private String name;         // Mandatory
    private String description;  // Optional
    private double plusPoint;    // Mandatory
    private double penaltyPoint; // Mandatory

    // 1. Constructor KHÔNG tham số (Java Bean Spec)
    public ActivityType() {
    }

    // 2. Constructor với các tham số BẮT BUỘC (Required-args)
    public ActivityType(String code, String name, double plusPoint, double penaltyPoint) {
        this.code = code;
        this.name = name;
        this.plusPoint = plusPoint;
        this.penaltyPoint = penaltyPoint;
    }

    // 3. Constructor FULL tham số (All-args)
    public ActivityType(Integer id, String code, String name, String description, double plusPoint, double penaltyPoint) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.plusPoint = plusPoint;
        this.penaltyPoint = penaltyPoint;
    }

    // Getter và Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPlusPoint() {
        return plusPoint;
    }

    public void setPlusPoint(double plusPoint) {
        this.plusPoint = plusPoint;
    }

    public double getPenaltyPoint() {
        return penaltyPoint;
    }

    public void setPenaltyPoint(double penaltyPoint) {
        this.penaltyPoint = penaltyPoint;
    }

    @Override
    public String toString() {
        return "ActivityType{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", plusPoint=" + plusPoint +
                ", penaltyPoint=" + penaltyPoint +
                '}';
    }
}
