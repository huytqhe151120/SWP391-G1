package com.swp391.g1.dto.request;

public class PartnerStaffRequestDTO {
    private int id;
    private String companyId; // Receive String from form, then convert to int in the controller
    private String code;
    private String name;
    private String dob;       // Receive date in YYYY-MM-DD format from <input type="date">
    private String gender;    // "Male" / "Female" or "1" / "0"
    private String position;
    private String status;

    //Constructor with no parameter
    public PartnerStaffRequestDTO() {}

    //Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
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

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
