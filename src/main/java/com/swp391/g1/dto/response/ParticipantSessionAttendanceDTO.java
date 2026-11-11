package com.swp391.g1.dto.response;

import com.swp391.g1.model.Enum.ParticipantStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ParticipantSessionAttendanceDTO {

    private int sessionNumber;
    private ParticipantStatus attendanceStatus = ParticipantStatus.NOT_YET;
    private BigDecimal sessionScore = BigDecimal.ZERO;
    private Integer updatedById;
    private String updatedBy;
    private String updatedByCode;
    private String updatedByName;
    private String updatedByUnit;
    private LocalDateTime updatedAt;

    public int getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public ParticipantStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(ParticipantStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus == null ? ParticipantStatus.NOT_YET : attendanceStatus;
    }

    public BigDecimal getSessionScore() {
        return sessionScore;
    }

    public void setSessionScore(BigDecimal sessionScore) {
        this.sessionScore = sessionScore == null ? BigDecimal.ZERO : sessionScore;
    }

    public Integer getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(Integer updatedById) {
        this.updatedById = updatedById;
    }

    public String getUpdatedBy() {
        return updatedByName != null && !updatedByName.isBlank() ? updatedByName : updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByCode() {
        return updatedByCode;
    }

    public void setUpdatedByCode(String updatedByCode) {
        this.updatedByCode = updatedByCode;
    }

    public String getUpdatedByName() {
        return updatedByName != null && !updatedByName.isBlank() ? updatedByName : updatedBy;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public String getUpdatedByUnit() {
        return updatedByUnit;
    }

    public void setUpdatedByUnit(String updatedByUnit) {
        this.updatedByUnit = updatedByUnit;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPresentStatus() {
        return attendanceStatus == ParticipantStatus.ATTENDED
                || attendanceStatus == ParticipantStatus.IN_LATE
                || attendanceStatus == ParticipantStatus.OUT_EARLY;
    }

    public String getFormattedSessionScore() {
        if (sessionScore == null) {
            return "0";
        }
        String plain = sessionScore.stripTrailingZeros().toPlainString();
        if (sessionScore.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + plain;
        }
        return plain;
    }
}
