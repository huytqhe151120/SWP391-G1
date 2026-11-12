package com.swp391.g1.model;

/**
 * A question inside an Assessment. Type MCQ or ESSAY.
 */
public class AssessmentQuestion {

    public enum QuestionType {
        MCQ,
        ESSAY
    }

    private int id;
    private int assessmentId;
    private QuestionType questionType;
    private String content;
    private double maxScore;
    private int displayOrder;
    private java.util.List<AssessmentOption> options;

    public AssessmentQuestion() {
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

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public java.util.List<AssessmentOption> getOptions() {
        return options;
    }

    public void setOptions(java.util.List<AssessmentOption> options) {
        this.options = options;
    }
}
