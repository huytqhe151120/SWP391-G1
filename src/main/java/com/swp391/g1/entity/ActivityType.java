package com.swp391.g1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_types")
public class ActivityType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "plus_point", nullable = false)
    private double plusPoint;

    @Column(name = "penalty_point", nullable = false)
    private double penaltyPoint;

    protected ActivityType() {
    }

    public ActivityType(
            String code,
            String name,
            double plusPoint,
            double penaltyPoint
    ) {
        this.code = code;
        this.name = name;
        this.plusPoint = plusPoint;
        this.penaltyPoint = penaltyPoint;
    }

    public ActivityType(
            String code,
            String name,
            String description,
            double plusPoint,
            double penaltyPoint
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.plusPoint = plusPoint;
        this.penaltyPoint = penaltyPoint;
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
}
