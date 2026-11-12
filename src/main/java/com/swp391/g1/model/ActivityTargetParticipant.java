package com.swp391.g1.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ActivityTargetParticipant {
    private long id;
    private int activityId;
    private Integer departmentId;
    private Integer cohortId;
    private Integer majorId;
    private Integer academicProgramId;
    private Integer mainClassId;
    private LocalDateTime createdAt;

    private String departmentCode;
    private String departmentName;
    private String cohortCode;
    private String cohortName;
    private String majorCode;
    private String majorName;
    private String academicProgramCode;
    private String academicProgramName;
    private String mainClassCode;

    public ActivityTargetParticipant() {
    }

    public ActivityTargetParticipant(Integer departmentId, Integer cohortId, Integer majorId,
                                     Integer academicProgramId, Integer mainClassId) {
        this.departmentId = departmentId;
        this.cohortId = cohortId;
        this.majorId = majorId;
        this.academicProgramId = academicProgramId;
        this.mainClassId = mainClassId;
    }

    public boolean isAllSchool() {
        return departmentId == null
                && cohortId == null
                && majorId == null
                && academicProgramId == null
                && mainClassId == null;
    }

    public String getDisplayLabel() {
        if (isAllSchool()) {
            return "Toàn trường";
        }
        List<String> parts = new ArrayList<>();
        if (departmentName != null && !departmentName.isBlank()) {
            parts.add(departmentName);
        }
        if (cohortName != null && !cohortName.isBlank()) {
            parts.add(cohortName);
        } else if (cohortCode != null && !cohortCode.isBlank()) {
            parts.add("Khóa " + cohortCode);
        }
        if (majorName != null && !majorName.isBlank()) {
            parts.add("Ngành " + majorName);
        }
        if (academicProgramName != null && !academicProgramName.isBlank()) {
            parts.add("CTĐT " + academicProgramName);
        }
        if (mainClassCode != null && !mainClassCode.isBlank()) {
            parts.add("Lớp " + mainClassCode);
        }
        return parts.isEmpty() ? "Theo nhóm chỉ định" : String.join(" • ", parts);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getCohortId() {
        return cohortId;
    }

    public void setCohortId(Integer cohortId) {
        this.cohortId = cohortId;
    }

    public Integer getMajorId() {
        return majorId;
    }

    public void setMajorId(Integer majorId) {
        this.majorId = majorId;
    }

    public Integer getAcademicProgramId() {
        return academicProgramId;
    }

    public void setAcademicProgramId(Integer academicProgramId) {
        this.academicProgramId = academicProgramId;
    }

    public Integer getMainClassId() {
        return mainClassId;
    }

    public void setMainClassId(Integer mainClassId) {
        this.mainClassId = mainClassId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getCohortCode() {
        return cohortCode;
    }

    public void setCohortCode(String cohortCode) {
        this.cohortCode = cohortCode;
    }

    public String getCohortName() {
        return cohortName;
    }

    public void setCohortName(String cohortName) {
        this.cohortName = cohortName;
    }

    public String getMajorCode() {
        return majorCode;
    }

    public void setMajorCode(String majorCode) {
        this.majorCode = majorCode;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getAcademicProgramCode() {
        return academicProgramCode;
    }

    public void setAcademicProgramCode(String academicProgramCode) {
        this.academicProgramCode = academicProgramCode;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityTargetParticipant that)) return false;
        return Objects.equals(departmentId, that.departmentId)
                && Objects.equals(cohortId, that.cohortId)
                && Objects.equals(majorId, that.majorId)
                && Objects.equals(academicProgramId, that.academicProgramId)
                && Objects.equals(mainClassId, that.mainClassId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departmentId, cohortId, majorId, academicProgramId, mainClassId);
    }
}
