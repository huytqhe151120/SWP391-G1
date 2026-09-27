package com.swp391.g1.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Student implements Serializable {
    private int id;
    private Integer mainClassId;
    private String mainClassCode;
    private String code;
    private String name;
    private LocalDate dob;
    private Boolean gender;
    private String email;
    private String phoneNumber;
    private String status;

    public Student() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getMainClassId() { return mainClassId; }
    public void setMainClassId(Integer mainClassId) { this.mainClassId = mainClassId; }

    public String getMainClassCode() { return mainClassCode; }
    public void setMainClassCode(String mainClassCode) { this.mainClassCode = mainClassCode; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public Boolean getGender() { return gender; }
    public void setGender(Boolean gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
