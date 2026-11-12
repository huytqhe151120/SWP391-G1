package com.swp391.g1.model;

public class AcademicProgram {
    private int id;
    private Integer responsibleMajorId;
    private String code;
    private String name;
    private String status;

    public AcademicProgram() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getResponsibleMajorId() {
        return responsibleMajorId;
    }

    public void setResponsibleMajorId(Integer responsibleMajorId) {
        this.responsibleMajorId = responsibleMajorId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
