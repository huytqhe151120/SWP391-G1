package com.swp391.g1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a student's appeal request for re-evaluation of a final point
 * for a completed extracurricular activity.
 */
public class Appeal {

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    private int id;
    private int studentId;
    private int activityId;
    private String reason;
    private String status; // PENDING, APPROVED, REJECTED
    private Integer reviewedBy;       // staff_id, nullable
    private LocalDateTime reviewedAt; // nullable
    private BigDecimal oldPoint;      // nullable
    private BigDecimal newPoint;      // nullable, set when APPROVED
    private String reviewNote;        // nullable
    private LocalDateTime createdAt;

    // ── JOIN display fields ──────────────────────────────────────
    private String studentName;
    private String studentCode;
    private String activityName;
    private String activityCode;
    private String reviewerName;

    public Appeal() {}

    // ── Getters & Setters ────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public BigDecimal getOldPoint() { return oldPoint; }
    public void setOldPoint(BigDecimal oldPoint) { this.oldPoint = oldPoint; }

    public BigDecimal getNewPoint() { return newPoint; }
    public void setNewPoint(BigDecimal newPoint) { this.newPoint = newPoint; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── JOIN display ─────────────────────────────────────────────

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getActivityCode() { return activityCode; }
    public void setActivityCode(String activityCode) { this.activityCode = activityCode; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    // ── Convenience helpers ───────────────────────────────────────

    public boolean isPending()  { return "PENDING".equals(status); }
    public boolean isApproved() { return "APPROVED".equals(status); }
    public boolean isRejected() { return "REJECTED".equals(status); }
}
