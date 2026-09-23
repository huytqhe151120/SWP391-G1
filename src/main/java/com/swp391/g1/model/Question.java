package com.swp391.g1.model;

import java.sql.Timestamp;

public class Question {
    private int id;
    private String title;
    private String content;
    private boolean isAnonymous;
    private String status; // 'PENDING', 'ANSWERED', 'REJECTED'
    private int studentId;
    private Timestamp createdAt;

    public Question() {}

    public Question(String title, String content, boolean isAnonymous, int studentId) {
        this.title = title;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.studentId = studentId;
        this.status = "PENDING";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}