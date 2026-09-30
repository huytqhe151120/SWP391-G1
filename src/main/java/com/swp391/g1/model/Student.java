package com.swp391.g1.model;

public class Student {
    private int id;
    private int mainClassId;
    private String code;
    private String name;
    private String dob;
    private String gender;
    private String email;
    private String phoneNumber;
    private String status;
    private int accountId;

    public Student() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMainClassId() { return mainClassId; }
    public void setMainClassId(int mainClassId) { this.mainClassId = mainClassId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
}
