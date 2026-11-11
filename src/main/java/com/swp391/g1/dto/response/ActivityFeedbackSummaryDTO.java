package com.swp391.g1.dto.response;

import java.time.LocalDateTime;

public class ActivityFeedbackSummaryDTO {
    private int activityId;
    private String activityCode;
    private String activityName;
    private String activityTypeName;
    private String semesterName;
    private String departmentName;
    private LocalDateTime endTime;
    private int participantCount;
    private int submittedCount;
    private int pendingCount;
    private double averageRating;

    public int getActivityId() { return activityId; }
    public void setActivityId(int value) { activityId = value; }
    public String getActivityCode() { return activityCode; }
    public void setActivityCode(String value) { activityCode = value; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String value) { activityName = value; }
    public String getActivityTypeName() { return activityTypeName; }
    public void setActivityTypeName(String value) { activityTypeName = value; }
    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String value) { semesterName = value; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String value) { departmentName = value; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime value) { endTime = value; }
    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int value) { participantCount = value; }
    public int getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(int value) { submittedCount = value; }
    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int value) { pendingCount = value; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double value) { averageRating = value; }
}
