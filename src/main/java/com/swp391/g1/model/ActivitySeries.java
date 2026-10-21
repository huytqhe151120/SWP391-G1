package com.swp391.g1.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ActivitySeries {
    private int id;
    private String seriesCode;
    private String name;
    private int activityTypeId;
    private int responsibleDepartmentId;
    private int responsibleStaffId;
    private String description;
    private boolean registrationFormRequired;
    private Enum.RecurrenceMode recurrenceMode;
    private Integer recurrenceInterval;
    private LocalDate seriesStartDate;
    private LocalDate seriesEndDate;
    private Integer maxOccurrences;
    private Integer occurrenceDurationMinutes;
    private LocalTime timeOfDay;
    private Integer semesterId;
    private Enum.ApprovalStatus approvalStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ActivitySeries() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSeriesCode() {
        return seriesCode;
    }

    public void setSeriesCode(String seriesCode) {
        this.seriesCode = seriesCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getActivityTypeId() {
        return activityTypeId;
    }

    public void setActivityTypeId(int activityTypeId) {
        this.activityTypeId = activityTypeId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRegistrationFormRequired() {
        return registrationFormRequired;
    }

    public void setRegistrationFormRequired(boolean registrationFormRequired) {
        this.registrationFormRequired = registrationFormRequired;
    }

    public Enum.RecurrenceMode getRecurrenceMode() {
        return recurrenceMode;
    }

    public void setRecurrenceMode(Enum.RecurrenceMode recurrenceMode) {
        this.recurrenceMode = recurrenceMode;
    }

    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(Integer recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    public LocalDate getSeriesStartDate() {
        return seriesStartDate;
    }

    public void setSeriesStartDate(LocalDate seriesStartDate) {
        this.seriesStartDate = seriesStartDate;
    }

    public LocalDate getSeriesEndDate() {
        return seriesEndDate;
    }

    public void setSeriesEndDate(LocalDate seriesEndDate) {
        this.seriesEndDate = seriesEndDate;
    }

    public Integer getMaxOccurrences() {
        return maxOccurrences;
    }

    public void setMaxOccurrences(Integer maxOccurrences) {
        this.maxOccurrences = maxOccurrences;
    }

    public Integer getOccurrenceDurationMinutes() {
        return occurrenceDurationMinutes;
    }

    public void setOccurrenceDurationMinutes(Integer occurrenceDurationMinutes) {
        this.occurrenceDurationMinutes = occurrenceDurationMinutes;
    }

    public LocalTime getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(LocalTime timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public Integer getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Integer semesterId) {
        this.semesterId = semesterId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
