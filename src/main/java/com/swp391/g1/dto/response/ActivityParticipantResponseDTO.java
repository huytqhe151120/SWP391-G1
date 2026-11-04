package com.swp391.g1.dto.response;

import com.swp391.g1.model.Enum.ParticipantStatus;

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
    private LocalDateTime createdAt;
    private boolean removable;

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
}
