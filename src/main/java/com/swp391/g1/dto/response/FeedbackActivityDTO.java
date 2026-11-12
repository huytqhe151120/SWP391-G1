package com.swp391.g1.dto.response;

import com.swp391.g1.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class FeedbackActivityDTO {
    private int activityId;
    private String activityCode;
    private String activityName;
    private String address;
    private LocalDateTime endTime;
    private boolean submitted;

    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }
    public String getActivityCode() { return activityCode; }
    public void setActivityCode(String activityCode) { this.activityCode = activityCode; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public long getDaysSinceEnd() {
        return endTime == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(endTime.toLocalDate(), DateTimeUtil.now().toLocalDate()));
    }
    public boolean isSubmitted() { return submitted; }
    public void setSubmitted(boolean submitted) { this.submitted = submitted; }
}
