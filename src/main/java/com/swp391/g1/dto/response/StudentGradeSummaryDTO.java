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
    private int attendedActivityCount;
    private int registeredActivityCount;
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

    public String getMainClassCode() {
        return mainClassCode;
    }

    public void setMainClassCode(String mainClassCode) {
        this.mainClassCode = mainClassCode;
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

    public String getParticipationRatioText() {
        return attendedActivityCount + "/" + registeredActivityCount;
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
