package com.swp391.g1.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StudentGradeSummaryDTO {
    private int studentId;
    private String studentCode;
    private String studentName;
    private String departmentName;
    private String majorName;
    private String academicProgramName;
    private String mainClassCode;
    private int semesterId;
    private String semesterCode;
    private String semesterName;
    private int attendedActivityCount;
    private int registeredActivityCount;
    private BigDecimal totalAttendanceScore = BigDecimal.ZERO;
    private BigDecimal totalParticipationEvalScore = BigDecimal.ZERO;
    private BigDecimal totalParticipationScore = BigDecimal.ZERO;
    private BigDecimal totalSummaryScore = BigDecimal.ZERO;
    private BigDecimal totalAverageScore = BigDecimal.ZERO;
    private BigDecimal totalPlusPoints = BigDecimal.ZERO;
    private BigDecimal totalMinusPoints = BigDecimal.ZERO;
    private BigDecimal totalAccumulatedPoints = BigDecimal.ZERO;

    public StudentGradeSummaryDTO() {
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

    public String getCurriculumCode() {
        return academicProgramName;
    }

    public String getMainClassCode() {
        return mainClassCode;
    }

    public void setMainClassCode(String mainClassCode) {
        this.mainClassCode = mainClassCode;
    }

    public int getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(int semesterId) {
        this.semesterId = semesterId;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public void setSemesterCode(String semesterCode) {
        this.semesterCode = semesterCode;
    }

    public String getSemesterName() {
        return semesterName;
    }

    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public int getAttendedActivityCount() {
        return attendedActivityCount;
    }

    public void setAttendedActivityCount(int attendedActivityCount) {
        this.attendedActivityCount = attendedActivityCount;
    }

    public int getRegisteredActivityCount() {
        return registeredActivityCount;
    }

    public void setRegisteredActivityCount(int registeredActivityCount) {
        this.registeredActivityCount = registeredActivityCount;
    }

    public BigDecimal getTotalAttendanceScore() {
        return totalAttendanceScore;
    }

    public void setTotalAttendanceScore(BigDecimal totalAttendanceScore) {
        this.totalAttendanceScore = totalAttendanceScore == null ? BigDecimal.ZERO : totalAttendanceScore;
    }

    public BigDecimal getTotalParticipationEvalScore() {
        return totalParticipationEvalScore;
    }

    public void setTotalParticipationEvalScore(BigDecimal totalParticipationEvalScore) {
        this.totalParticipationEvalScore = totalParticipationEvalScore == null ? BigDecimal.ZERO : totalParticipationEvalScore;
    }

    public BigDecimal getTotalParticipationScore() {
        return totalParticipationScore;
    }

    public void setTotalParticipationScore(BigDecimal totalParticipationScore) {
        this.totalParticipationScore = totalParticipationScore == null ? BigDecimal.ZERO : totalParticipationScore;
    }

    public BigDecimal getTotalSummaryScore() {
        return totalSummaryScore;
    }

    public void setTotalSummaryScore(BigDecimal totalSummaryScore) {
        this.totalSummaryScore = totalSummaryScore == null ? BigDecimal.ZERO : totalSummaryScore;
    }

    public BigDecimal getTotalAverageScore() {
        return totalAverageScore;
    }

    public void setTotalAverageScore(BigDecimal totalAverageScore) {
        this.totalAverageScore = totalAverageScore == null ? BigDecimal.ZERO : totalAverageScore;
    }

    public BigDecimal getTotalPlusPoints() {
        return totalPlusPoints;
    }

    public void setTotalPlusPoints(BigDecimal totalPlusPoints) {
        this.totalPlusPoints = totalPlusPoints == null ? BigDecimal.ZERO : totalPlusPoints;
    }

    public BigDecimal getTotalMinusPoints() {
        return totalMinusPoints;
    }

    public void setTotalMinusPoints(BigDecimal totalMinusPoints) {
        this.totalMinusPoints = totalMinusPoints == null ? BigDecimal.ZERO : totalMinusPoints;
    }

    public BigDecimal getTotalAccumulatedPoints() {
        return totalAccumulatedPoints;
    }

    public void setTotalAccumulatedPoints(BigDecimal totalAccumulatedPoints) {
        this.totalAccumulatedPoints = totalAccumulatedPoints == null ? BigDecimal.ZERO : totalAccumulatedPoints;
    }

    public BigDecimal getAccumulatedPoints() {
        return totalAccumulatedPoints;
    }

    public String getParticipationRatioText() {
        return attendedActivityCount + "/" + registeredActivityCount;
    }

    public String getFormattedAttendanceScore() {
        return formatScore(totalAttendanceScore);
    }

    public String getFormattedTotalAttendanceScore() {
        return formatScore(totalAttendanceScore);
    }

    public String getFormattedParticipationEvalScore() {
        return formatScore(totalParticipationEvalScore);
    }

    public String getFormattedTotalParticipationEvalScore() {
        return formatScore(totalParticipationEvalScore);
    }

    public String getFormattedParticipationScore() {
        return formatScore(totalParticipationScore);
    }

    public String getFormattedTotalParticipationScore() {
        return formatScore(totalParticipationScore);
    }

    public String getFormattedSummaryScore() {
        return formatScore(totalSummaryScore);
    }

    public String getFormattedTotalSummaryScore() {
        return formatScore(totalSummaryScore);
    }

    public String getFormattedAverageScore() {
        return formatScore(totalAverageScore);
    }

    public String getFormattedTotalAverageScore() {
        return formatScore(totalAverageScore);
    }

    public String getFormattedPlusPoints() {
        String s = formatScore(totalPlusPoints);
        if (totalPlusPoints != null && totalPlusPoints.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + s;
        }
        return s;
    }

    public String getFormattedMinusPoints() {
        String s = formatScore(totalMinusPoints);
        if (totalMinusPoints != null && totalMinusPoints.compareTo(BigDecimal.ZERO) > 0) {
            return "-" + s;
        }
        return "0";
    }

    public String getFormattedAccumulatedPoints() {
        return formatScore(totalAccumulatedPoints);
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
