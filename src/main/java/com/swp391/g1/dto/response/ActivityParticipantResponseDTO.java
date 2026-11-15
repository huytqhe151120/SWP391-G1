package com.swp391.g1.dto.response;

import com.swp391.g1.model.Enum.ParticipantStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class ActivityParticipantResponseDTO {

    private int participantId;
    private int studentId;
    private String studentCode;
    private String studentName;
    private String departmentName;
    private String academicProgramName;
    private String majorName;
    private String mainClassCode;
    private ParticipantStatus attendanceStatus;
    private int sessionNumber = 1;
    private int totalSessions = 1;
    private ParticipantStatus sessionAttendanceStatus = ParticipantStatus.NOT_YET;
    private BigDecimal sessionAttendanceScore = BigDecimal.ZERO;
    private int attendedSessionCount;
    private int absentSessionCount;
    private int markedSessionCount;
    private String registrationStatus;
    private String participationRole = "PARTICIPANT";

    public String getParticipationRole() {
        return participationRole;
    }

    public void setParticipationRole(String participationRole) {
        this.participationRole = participationRole;
    }

    public boolean isOrganizer() {
        return "ORGANIZER".equals(participationRole);
    }
    private Long registrationBy;
    private LocalDateTime createdAt;
    private boolean removable;

    // Grading & score fields
    private BigDecimal attendanceScore;
    private BigDecimal participationEvalScore;
    private String participationFeedback;
    private String participationGradedBy;
    private LocalDateTime participationGradedAt;
    private BigDecimal participationScore;
    private BigDecimal summaryScore;
    private BigDecimal averageScore;
    private BigDecimal orgLevelCoefficient;
    private BigDecimal earlyRegCoefficient;
    private BigDecimal finalPoint;

    public ActivityParticipantResponseDTO() {
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getAcademicProgramName() {
        return academicProgramName;
    }

    public void setAcademicProgramName(String academicProgramName) {
        this.academicProgramName = academicProgramName;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getMainClassCode() {
        return mainClassCode;
    }

    public void setMainClassCode(String mainClassCode) {
        this.mainClassCode = mainClassCode;
    }

    public ParticipantStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(ParticipantStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public int getSessionNumber() {
        return sessionNumber > 0 ? sessionNumber : 1;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber > 0 ? sessionNumber : 1;
    }

    public int getTotalSessions() {
        return totalSessions > 0 ? totalSessions : 1;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions > 0 ? totalSessions : 1;
    }

    public ParticipantStatus getSessionAttendanceStatus() {
        return sessionAttendanceStatus != null ? sessionAttendanceStatus : ParticipantStatus.NOT_YET;
    }

    public void setSessionAttendanceStatus(ParticipantStatus sessionAttendanceStatus) {
        this.sessionAttendanceStatus = sessionAttendanceStatus != null ? sessionAttendanceStatus : ParticipantStatus.NOT_YET;
    }

    public BigDecimal getSessionAttendanceScore() {
        return sessionAttendanceScore != null ? sessionAttendanceScore : BigDecimal.ZERO;
    }

    public void setSessionAttendanceScore(BigDecimal sessionAttendanceScore) {
        this.sessionAttendanceScore = sessionAttendanceScore != null ? sessionAttendanceScore : BigDecimal.ZERO;
    }

    public int getAttendedSessionCount() {
        return attendedSessionCount;
    }

    public void setAttendedSessionCount(int attendedSessionCount) {
        this.attendedSessionCount = Math.max(0, attendedSessionCount);
    }

    public int getAbsentSessionCount() {
        return absentSessionCount;
    }

    public void setAbsentSessionCount(int absentSessionCount) {
        this.absentSessionCount = Math.max(0, absentSessionCount);
    }

    public int getMarkedSessionCount() {
        return markedSessionCount;
    }

    public void setMarkedSessionCount(int markedSessionCount) {
        this.markedSessionCount = Math.max(0, markedSessionCount);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** True when the participant has never been marked, so removing it loses no attendance data. */
    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public Long getRegistrationBy() {
        return registrationBy;
    }

    public void setRegistrationBy(Long registrationBy) {
        this.registrationBy = registrationBy;
    }

    public BigDecimal getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(BigDecimal attendanceScore) {
        this.attendanceScore = attendanceScore;
    }

    public BigDecimal getParticipationEvalScore() {
        return participationEvalScore;
    }

    public void setParticipationEvalScore(BigDecimal participationEvalScore) {
        this.participationEvalScore = participationEvalScore;
    }

    public String getParticipationFeedback() {
        return participationFeedback;
    }

    public void setParticipationFeedback(String participationFeedback) {
        this.participationFeedback = participationFeedback;
    }

    public String getParticipationGradedBy() {
        return participationGradedBy;
    }

    public void setParticipationGradedBy(String participationGradedBy) {
        this.participationGradedBy = participationGradedBy;
    }

    public LocalDateTime getParticipationGradedAt() {
        return participationGradedAt;
    }

    public void setParticipationGradedAt(LocalDateTime participationGradedAt) {
        this.participationGradedAt = participationGradedAt;
    }

    public BigDecimal getParticipationScore() {
        return participationScore;
    }

    public void setParticipationScore(BigDecimal participationScore) {
        this.participationScore = participationScore;
    }

    public BigDecimal getSummaryScore() {
        return summaryScore;
    }

    public void setSummaryScore(BigDecimal summaryScore) {
        this.summaryScore = summaryScore;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public BigDecimal getOrgLevelCoefficient() {
        return orgLevelCoefficient;
    }

    public void setOrgLevelCoefficient(BigDecimal orgLevelCoefficient) {
        this.orgLevelCoefficient = orgLevelCoefficient;
    }

    public BigDecimal getEarlyRegCoefficient() {
        return earlyRegCoefficient;
    }

    public void setEarlyRegCoefficient(BigDecimal earlyRegCoefficient) {
        this.earlyRegCoefficient = earlyRegCoefficient;
    }

    public BigDecimal getFinalPoint() {
        return finalPoint;
    }

    public void setFinalPoint(BigDecimal finalPoint) {
        this.finalPoint = finalPoint;
    }

    public boolean isPresentStatus() {
        return attendedSessionCount > 0
                || attendanceStatus == ParticipantStatus.ATTENDED
                || attendanceStatus == ParticipantStatus.IN_LATE
                || attendanceStatus == ParticipantStatus.OUT_EARLY;
    }

    public boolean isGraded() {
        return participationEvalScore != null;
    }

    public String getFormattedSessionAttendanceScore() {
        return formatScore(getSessionAttendanceScore());
    }

    public String getSessionProgressText() {
        return attendedSessionCount + "/" + getTotalSessions() + " buổi";
    }

    public String getFormattedAttendanceScore() {
        return formatScore(attendanceScore);
    }

    public String getFormattedAttendanceScoreOneDecimal() {
        BigDecimal value = attendanceScore != null ? attendanceScore : BigDecimal.ZERO;
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    public String getFormattedEvalScore() {
        return participationEvalScore == null ? "" : formatScore(participationEvalScore);
    }

    public String getFormattedParticipationScore() {
        return formatScore(participationScore);
    }

    public String getFormattedFinalPoint() {
        return formatScore(finalPoint);
    }

    private static String formatScore(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal stripped = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }
}
