package com.swp391.g1.dto.response;

public class ChecklistAssigneeResponseDTO {

    private final int id;
    private final String code;
    private final String name;
    private final String companyName;

    public ChecklistAssigneeResponseDTO(int id, String code, String name, String companyName) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.companyName = companyName;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCompanyName() {
        return companyName;
    }
}
