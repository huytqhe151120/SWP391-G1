package com.swp391.g1.model;

public class MainClass {
    private int id;
    private Integer modeOfStudyId;
    private Integer applicableCohortId;
    private Integer responsibleAcademicProgramId;
    private String code;
    private String status;

    public MainClass() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getModeOfStudyId() {
        return modeOfStudyId;
    }

    public void setModeOfStudyId(Integer modeOfStudyId) {
        this.modeOfStudyId = modeOfStudyId;
    }

    public Integer getApplicableCohortId() {
        return applicableCohortId;
    }

    public void setApplicableCohortId(Integer applicableCohortId) {
        this.applicableCohortId = applicableCohortId;
    }

    public Integer getResponsibleAcademicProgramId() {
        return responsibleAcademicProgramId;
    }

    public void setResponsibleAcademicProgramId(Integer responsibleAcademicProgramId) {
        this.responsibleAcademicProgramId = responsibleAcademicProgramId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
