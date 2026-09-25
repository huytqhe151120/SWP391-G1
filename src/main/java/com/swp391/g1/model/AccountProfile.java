package com.swp391.g1.model;

/**
 * Lightweight view of a profile row linked to an account through
 * student.account_id / staff.account_id / partner_staff.account_id.
 *
 * <p>Only fields that exist in the profile tables are used. email is only
 * populated for student profiles; position is only populated for
 * partner_staff profiles.
 */
public class AccountProfile {

    private String profileType;
    private String code;
    private String name;
    private String status;
    private String email;
    private String position;

    public String getProfileType() {
        return profileType;
    }

    public void setProfileType(String profileType) {
        this.profileType = profileType;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}