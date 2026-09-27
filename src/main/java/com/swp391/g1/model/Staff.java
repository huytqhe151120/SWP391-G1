package com.swp391.g1.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Staff implements Serializable {
    private int id;
    private String code;
    private String name;
    private LocalDate dob;
    private Boolean gender;
    private String status;
    private Integer accountId;
    private String departmentInfo;

    public Staff() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public Boolean getGender() { return gender; }
    public void setGender(Boolean gender) { this.gender = gender; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public String getDepartmentInfo() { return departmentInfo; }
    public void setDepartmentInfo(String departmentInfo) { this.departmentInfo = departmentInfo; }
}
