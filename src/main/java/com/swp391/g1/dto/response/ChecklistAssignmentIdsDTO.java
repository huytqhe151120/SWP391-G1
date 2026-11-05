package com.swp391.g1.dto.response;

import java.util.List;

public class ChecklistAssignmentIdsDTO {

    private final List<Integer> staffIds;
    private final List<Integer> partnerStaffIds;
    private final List<Integer> prerequisiteTaskIds;

    public ChecklistAssignmentIdsDTO(List<Integer> staffIds, List<Integer> partnerStaffIds,
                                     List<Integer> prerequisiteTaskIds) {
        this.staffIds = List.copyOf(staffIds);
        this.partnerStaffIds = List.copyOf(partnerStaffIds);
        this.prerequisiteTaskIds = List.copyOf(prerequisiteTaskIds);
    }

    public List<Integer> getStaffIds() {
        return staffIds;
    }

    public List<Integer> getPartnerStaffIds() {
        return partnerStaffIds;
    }

    public List<Integer> getPrerequisiteTaskIds() {
        return prerequisiteTaskIds;
    }
}
