package com.swp391.g1.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row for the standalone "Bài đánh giá" listing pages (staff manage list and
 * student take list). Combines assessment metadata with the owning activity
 * name plus per-context counters/attempt status.
 */
public class AssessmentListItem {

    private int assessmentId;
    private int activityId;
    private String activityCode;
    private String activityName;
    private String title;
    private boolean published;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;

    // Staff view
    private int totalAttempts;
    private int pendingGrading;

    // Student view
    private String attemptStatus;   // null if not started
    private BigDecimal finalScore;  // null if not graded
    private boolean canTake;

    public int getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(int assessmentId) {
        this.assessmentId = assessmentId;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public String getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(String activityCode) {
        this.activityCode = activityCode;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDateTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public int getPendingGrading() {
        return pendingGrading;
    }

    public void setPendingGrading(int pendingGrading) {
        this.pendingGrading = pendingGrading;
    }

    public String getAttemptStatus() {
        return attemptStatus;
    }

    public void setAttemptStatus(String attemptStatus) {
        this.attemptStatus = attemptStatus;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public boolean isCanTake() {
        return canTake;
    }

    public void setCanTake(boolean canTake) {
        this.canTake = canTake;
    }
}
