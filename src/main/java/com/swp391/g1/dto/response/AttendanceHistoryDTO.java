package com.swp391.g1.dto.response;

import com.swp391.g1.model.Enum.ParticipantStatus;

import java.time.LocalDateTime;

public class AttendanceHistoryDTO {

    private long id;
    private int participantId;
    private int sessionNumber = 1;
    private ParticipantStatus oldStatus = ParticipantStatus.NOT_YET;
    private ParticipantStatus newStatus = ParticipantStatus.NOT_YET;
    private Integer createdById;
    private String createdBy;
    private String createdByCode;
    private String createdByName;
    private String createdByUnit;
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber <= 0 ? 1 : sessionNumber;
    }

    public ParticipantStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(ParticipantStatus oldStatus) {
        this.oldStatus = oldStatus == null ? ParticipantStatus.NOT_YET : oldStatus;
    }

    public ParticipantStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ParticipantStatus newStatus) {
        this.newStatus = newStatus == null ? ParticipantStatus.NOT_YET : newStatus;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public String getCreatedBy() {
        return createdByName != null && !createdByName.isBlank() ? createdByName : createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByCode() {
        return createdByCode;
    }

    public void setCreatedByCode(String createdByCode) {
        this.createdByCode = createdByCode;
    }

    public String getCreatedByName() {
        return createdByName != null && !createdByName.isBlank() ? createdByName : createdBy;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByUnit() {
        return createdByUnit;
    }

    public void setCreatedByUnit(String createdByUnit) {
        this.createdByUnit = createdByUnit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
