package com.swp391.g1.model;

import java.time.LocalDateTime;

/**
 * One submitted (or in-progress) Attempt by a Student for one Assessment.
 * At most one submitted Attempt per Student per Assessment.
 */
public class AssessmentAttempt {

    public enum Status {
        IN_PROGRESS,
        SUBMITTED,
        COMPLETED
    }

    private int id;
    private int assessmentId;
    private int studentId;
    private Status status;
    private LocalDateTime submittedAt;
    private Double finalScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AssessmentAttempt() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(int assessmentId) {
        this.assessmentId = assessmentId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
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
