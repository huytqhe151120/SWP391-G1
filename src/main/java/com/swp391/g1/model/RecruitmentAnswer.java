package com.swp391.g1.model;

public class RecruitmentAnswer {
    private int id;
    private int applicationId;
    private int questionId;
    private String questionText;
    private String answerValue;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getAnswerValue() { return answerValue; }
    public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }
}
