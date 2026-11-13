package com.swp391.g1.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecruitmentApplication {
    private int id;
    private int roundId;
    private int studentId;
    private String studentCode;
    private String studentName;
    private String className;
    private String roundTitle;
    private int activityId;
    private String activityName;
    private int choice1PositionId;
    private String choice1Name;
    private Integer choice2PositionId;
    private String choice2Name;
    private Integer acceptedPositionId;
    private String acceptedPositionName;
    private String status;
    private String note;
    private String internalNote;
    private String decisionReason;
    private String withdrawReason;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activityStart;
    private RecruitmentInterview interview;
    private List<RecruitmentAnswer> answers = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRoundId() { return roundId; }
    public void setRoundId(int roundId) { this.roundId = roundId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getRoundTitle() { return roundTitle; }
    public void setRoundTitle(String roundTitle) { this.roundTitle = roundTitle; }
    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public int getChoice1PositionId() { return choice1PositionId; }
    public void setChoice1PositionId(int choice1PositionId) { this.choice1PositionId = choice1PositionId; }
    public String getChoice1Name() { return choice1Name; }
    public void setChoice1Name(String choice1Name) { this.choice1Name = choice1Name; }
    public Integer getChoice2PositionId() { return choice2PositionId; }
    public void setChoice2PositionId(Integer choice2PositionId) { this.choice2PositionId = choice2PositionId; }
    public String getChoice2Name() { return choice2Name; }
    public void setChoice2Name(String choice2Name) { this.choice2Name = choice2Name; }
    public Integer getAcceptedPositionId() { return acceptedPositionId; }
    public void setAcceptedPositionId(Integer acceptedPositionId) { this.acceptedPositionId = acceptedPositionId; }
    public String getAcceptedPositionName() { return acceptedPositionName; }
    public void setAcceptedPositionName(String acceptedPositionName) { this.acceptedPositionName = acceptedPositionName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public String getWithdrawReason() { return withdrawReason; }
    public void setWithdrawReason(String withdrawReason) { this.withdrawReason = withdrawReason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getActivityStart() { return activityStart; }
    public void setActivityStart(LocalDateTime activityStart) { this.activityStart = activityStart; }
    public RecruitmentInterview getInterview() { return interview; }
    public void setInterview(RecruitmentInterview interview) { this.interview = interview; }
    public List<RecruitmentAnswer> getAnswers() { return answers; }
    public void setAnswers(List<RecruitmentAnswer> answers) {
        this.answers = answers == null ? new ArrayList<>() : new ArrayList<>(answers);
    }
}
