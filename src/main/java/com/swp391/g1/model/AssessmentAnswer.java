package com.swp391.g1.model;

import java.time.LocalDateTime;

/**
 * A student's answer to one question within one Attempt.
 * MCQ: selected_option_id set, answer_text null.
 * Essay: answer_text set, selected_option_id null.
 */
public class AssessmentAnswer {

    public enum GradingStatus {
        PENDING_GRADING,
        GRADED
    }

    private int id;
    private int attemptId;
    private int questionId;
    private Integer selectedOptionId; // nullable, MCQ only
    private String answerText;        // nullable, Essay only
    private Double awardedScore;      // nullable until graded
    private String feedback;          // nullable, Essay grader feedback
    private GradingStatus gradingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AssessmentAnswer() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public Integer getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Integer selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public Double getAwardedScore() {
        return awardedScore;
    }

    public void setAwardedScore(Double awardedScore) {
        this.awardedScore = awardedScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public GradingStatus getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(GradingStatus gradingStatus) {
        this.gradingStatus = gradingStatus;
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
