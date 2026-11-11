package com.swp391.g1.dto.response;

import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ParticipantStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class StudentActivityGradeDTO {
    private int participantId;
    private int studentId;
    private String studentCode;
    private String studentName;
    private String mainClassCode;
    private String departmentName;
    private String majorName;
    private String academicProgramName;

    private int activityId;
    private String activityCode;
    private String activityName;
    private String semesterName;
    private ActivityStatus activityStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean summaryRequired;
    private String organizationLevelName;
    private BigDecimal orgLevelCoefficient;

    private String registrationStatus;
    private BigDecimal earlyRegCoefficient;

    private ParticipantStatus attendanceStatus;
    private int attendedSessionCount;
    private int totalSessions = 1;
    private BigDecimal attendanceScore;
    private String attendanceMarkedBy;
    private LocalDateTime attendanceMarkedAt;

    private BigDecimal participationEvalScore;
    private String participationFeedback;
    private String participationGradedBy;
    private LocalDateTime participationGradedAt;

    private BigDecimal participationScore;
    private BigDecimal summaryScore;
    private String summaryFeedback;
    private String summaryGradedBy;
    private LocalDateTime summaryGradedAt;

    private BigDecimal averageScore;
    private BigDecimal accumulatedScore;

    public StudentActivityGradeDTO() {
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

    public String getMainClassCode() {
        return mainClassCode;
    }

    public void setMainClassCode(String mainClassCode) {
        this.mainClassCode = mainClassCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getAcademicProgramName() {
        return academicProgramName;
    }

    public void setAcademicProgramName(String academicProgramName) {
        this.academicProgramName = academicProgramName;
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

    public String getSemesterName() {
        return semesterName;
    }

    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public ActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isSummaryRequired() {
        return summaryRequired;
    }

    public void setSummaryRequired(boolean summaryRequired) {
        this.summaryRequired = summaryRequired;
    }

    public String getOrganizationLevelName() {
        return organizationLevelName;
    }

    public void setOrganizationLevelName(String organizationLevelName) {
        this.organizationLevelName = organizationLevelName;
    }

    public BigDecimal getOrgLevelCoefficient() {
        return orgLevelCoefficient;
    }

    public void setOrgLevelCoefficient(BigDecimal orgLevelCoefficient) {
        this.orgLevelCoefficient = orgLevelCoefficient;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public BigDecimal getEarlyRegCoefficient() {
        return earlyRegCoefficient;
    }

    public void setEarlyRegCoefficient(BigDecimal earlyRegCoefficient) {
        this.earlyRegCoefficient = earlyRegCoefficient;
    }

    public ParticipantStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(ParticipantStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public int getAttendedSessionCount() {
        return attendedSessionCount;
    }

    public void setAttendedSessionCount(int attendedSessionCount) {
        this.attendedSessionCount = Math.max(0, attendedSessionCount);
    }

    public int getTotalSessions() {
        return totalSessions > 0 ? totalSessions : 1;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions > 0 ? totalSessions : 1;
    }

    public BigDecimal getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(BigDecimal attendanceScore) {
        this.attendanceScore = attendanceScore;
    }

    public String getAttendanceMarkedBy() {
        return attendanceMarkedBy;
    }

    public void setAttendanceMarkedBy(String attendanceMarkedBy) {
        this.attendanceMarkedBy = attendanceMarkedBy;
    }

    public LocalDateTime getAttendanceMarkedAt() {
        return attendanceMarkedAt;
    }

    public void setAttendanceMarkedAt(LocalDateTime attendanceMarkedAt) {
        this.attendanceMarkedAt = attendanceMarkedAt;
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

    public String getSummaryFeedback() {
        return summaryFeedback;
    }

    public void setSummaryFeedback(String summaryFeedback) {
        this.summaryFeedback = summaryFeedback;
    }

    public String getSummaryGradedBy() {
        return summaryGradedBy;
    }

    public void setSummaryGradedBy(String summaryGradedBy) {
        this.summaryGradedBy = summaryGradedBy;
    }

    public LocalDateTime getSummaryGradedAt() {
        return summaryGradedAt;
    }

    public void setSummaryGradedAt(LocalDateTime summaryGradedAt) {
        this.summaryGradedAt = summaryGradedAt;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public BigDecimal getAccumulatedScore() {
        return accumulatedScore;
    }

    public void setAccumulatedScore(BigDecimal accumulatedScore) {
        this.accumulatedScore = accumulatedScore;
    }

    public boolean isPresent() {
        return attendanceStatus == ParticipantStatus.ATTENDED
                || attendanceStatus == ParticipantStatus.IN_LATE
                || attendanceStatus == ParticipantStatus.OUT_EARLY;
    }

    public String getRegistrationTypeLabel() {
        if ("REGISTERED".equals(registrationStatus)) {
            return "Đăng ký trước (x1.1)";
        } else if ("AUTO_ENROLLED".equals(registrationStatus)) {
            return "Ghi danh tự động (x1.0)";
        }
        return "Bổ sung / Thường (x1.0)";
    }

    public String getFormattedAttendanceScore() {
        return formatScore(attendanceScore);
    }

    public String getFormattedEvalScore() {
        return participationEvalScore == null ? "0" : formatScore(participationEvalScore);
    }

    public String getFormattedParticipationScore() {
        return formatScore(participationScore);
    }

    public String getFormattedSummaryScore() {
        if (!summaryRequired) {
            return "-";
        }
        return summaryScore == null ? "Chưa chấm" : formatScore(summaryScore);
    }

    public String getFormattedAverageScore() {
        return formatScore(averageScore);
    }

    public String getFormattedAccumulatedScore() {
        return formatScore(accumulatedScore);
    }

    public String getFormattedOrgLevelCoefficient() {
        return formatScore(orgLevelCoefficient == null ? BigDecimal.ONE : orgLevelCoefficient);
    }

    public String getFormattedEarlyRegCoefficient() {
        return formatScore(earlyRegCoefficient == null ? BigDecimal.ONE : earlyRegCoefficient);
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
