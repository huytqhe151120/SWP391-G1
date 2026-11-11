package com.swp391.g1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ActivityOrganizationLevel {
    private int id;
    private String code;
    private String name;
    private BigDecimal coefficient;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ActivityOrganizationLevel() {
    }

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

    public BigDecimal getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(BigDecimal coefficient) {
        this.coefficient = coefficient == null
                ? null
                : coefficient.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedCoefficient() {
        if (coefficient == null) {
            return "1.00";
        }
        return coefficient.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public String getDisplayLabel() {
        return (name == null ? "" : name) + " (x" + getFormattedCoefficient() + ")";
    }
}
