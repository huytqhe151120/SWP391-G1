package com.swp391.g1.dto.response;

import java.util.List;

public class ChecklistTaskDetailResponseDTO {

    private final ChecklistTaskResponseDTO task;
    private final List<ChecklistAssigneeResponseDTO> staff;
    private final List<ChecklistAssigneeResponseDTO> partnerStaff;
    private final List<ChecklistTaskResponseDTO> prerequisiteTasks;
    private final List<ChecklistTaskResponseDTO> nextTasks;

    public ChecklistTaskDetailResponseDTO(ChecklistTaskResponseDTO task,
                                          List<ChecklistAssigneeResponseDTO> staff,
                                          List<ChecklistAssigneeResponseDTO> partnerStaff,
                                          List<ChecklistTaskResponseDTO> prerequisiteTasks,
                                          List<ChecklistTaskResponseDTO> nextTasks) {
        this.task = task;
        this.staff = List.copyOf(staff);
        this.partnerStaff = List.copyOf(partnerStaff);
        this.prerequisiteTasks = List.copyOf(prerequisiteTasks);
        this.nextTasks = List.copyOf(nextTasks);
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

    public List<ChecklistTaskResponseDTO> getPrerequisiteTasks() {
        return prerequisiteTasks;
    }

    public List<ChecklistTaskResponseDTO> getNextTasks() {
        return nextTasks;
    }
}
