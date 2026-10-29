package com.swp391.g1.dto.response;

import java.util.List;

public class ChecklistTaskDetailResponseDTO {

    private final ChecklistTaskResponseDTO task;
    private final List<ChecklistAssigneeResponseDTO> staff;
    private final List<ChecklistAssigneeResponseDTO> partnerStaff;

    public ChecklistTaskDetailResponseDTO(ChecklistTaskResponseDTO task,
                                          List<ChecklistAssigneeResponseDTO> staff,
                                          List<ChecklistAssigneeResponseDTO> partnerStaff) {
        this.task = task;
        this.staff = List.copyOf(staff);
        this.partnerStaff = List.copyOf(partnerStaff);
    }

    public ChecklistTaskResponseDTO getTask() {
        return task;
    }

    public List<ChecklistAssigneeResponseDTO> getStaff() {
        return staff;
    }

    public List<ChecklistAssigneeResponseDTO> getPartnerStaff() {
        return partnerStaff;
    }
}
