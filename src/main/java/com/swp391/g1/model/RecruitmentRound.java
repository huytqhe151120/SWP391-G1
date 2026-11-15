package com.swp391.g1.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecruitmentRound {
    private int id;
    private int activityId;
    private String activityName;
    private String title;
    private String description;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private String status;
    private boolean interviewRequired = true;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int applicationCount;
    private List<RecruitmentPosition> positions = new ArrayList<>();
    private List<RecruitmentQuestion> questions = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getOpenAt() { return openAt; }
    public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; }
    public LocalDateTime getCloseAt() { return closeAt; }
    public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isInterviewRequired() { return interviewRequired; }
    public void setInterviewRequired(boolean interviewRequired) { this.interviewRequired = interviewRequired; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getApplicationCount() { return applicationCount; }
    public int getTotalQuota() { return positions.stream().mapToInt(RecruitmentPosition::getQuota).sum(); }
    public int getTotalRemaining() { return positions.stream().mapToInt(RecruitmentPosition::getRemaining).sum(); }
    public void setApplicationCount(int applicationCount) { this.applicationCount = applicationCount; }
    public List<RecruitmentPosition> getPositions() { return positions; }
    public void setPositions(List<RecruitmentPosition> positions) {
        this.positions = positions == null ? new ArrayList<>() : new ArrayList<>(positions);
    }
    public List<RecruitmentQuestion> getQuestions() { return questions; }
    public void setQuestions(List<RecruitmentQuestion> questions) {
        this.questions = questions == null ? new ArrayList<>() : new ArrayList<>(questions);
    }
}
