package com.swp391.g1.model;
import java.math.BigDecimal;

public class ActivityType {
        private int id; //[cite: 1]
        private String code; //[cite: 1]
        private String name; //[cite: 1]
        private String description; //[cite: 1]
        private BigDecimal bonusPoint; //[cite: 1]
        private BigDecimal penaltyPoint; //[cite: 1]
        private Enum.CommonStatus status; //[cite: 1]

    // Constructor with no parameter
    public ActivityType() {
    }

    //Getter and Setter
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Enum.CommonStatus getStatus() {
        return status;
    }

    public void setStatus(Enum.CommonStatus status) {
        this.status = status;
    }
}
