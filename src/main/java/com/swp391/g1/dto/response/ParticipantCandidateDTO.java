package com.swp391.g1.dto.response;

/** An active student who is not yet in an activity's participant list. */
public class ParticipantCandidateDTO {

    private int studentId;
    private String studentCode;
    private String studentName;
    private int mainClassId;
    private String mainClassCode;
    private int academicProgramId;
    private String academicProgramName;
    private int majorId;
    private String majorName;
    private int departmentId;
    private String departmentName;

    public ParticipantCandidateDTO() {
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

    public int getMainClassId() {
        return mainClassId;
    }

    public void setMainClassId(int mainClassId) {
        this.mainClassId = mainClassId;
    }

    public String getMainClassCode() {
        return mainClassCode;
    }

    public void setMainClassCode(String mainClassCode) {
        this.mainClassCode = mainClassCode;
    }

    public int getAcademicProgramId() {
        return academicProgramId;
    }

    public void setAcademicProgramId(int academicProgramId) {
        this.academicProgramId = academicProgramId;
    }

    public String getAcademicProgramName() {
        return academicProgramName;
    }

    public void setAcademicProgramName(String academicProgramName) {
        this.academicProgramName = academicProgramName;
    }

    public int getMajorId() {
        return majorId;
    }

    public void setMajorId(int majorId) {
        this.majorId = majorId;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
