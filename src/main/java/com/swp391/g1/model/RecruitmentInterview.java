package com.swp391.g1.model;

import java.time.LocalDateTime;

public class RecruitmentInterview {
    private int id;
    private int applicationId;
    private LocalDateTime scheduledAt;
    private String format;
    private String location;
    private String note;
    private LocalDateTime confirmDeadline;
    private LocalDateTime confirmedAt;
    private LocalDateTime reminderSentAt;
    private String status;
    private String evaluation;
    private Integer createdBy;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getConfirmDeadline() { return confirmDeadline; }
    public void setConfirmDeadline(LocalDateTime confirmDeadline) { this.confirmDeadline = confirmDeadline; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(LocalDateTime reminderSentAt) { this.reminderSentAt = reminderSentAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEvaluation() { return evaluation; }
    public void setEvaluation(String evaluation) { this.evaluation = evaluation; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    /** Nhãn hiển thị "Quá hạn xác nhận", không lưu trong database. */
    public boolean isConfirmOverdue() {
        return RecruitmentStatus.INTERVIEW_SCHEDULED.equals(status)
                && confirmDeadline != null && confirmDeadline.isBefore(LocalDateTime.now());
    }
}
