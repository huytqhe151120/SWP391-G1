package com.swp391.g1.dto.response;

import java.util.List;

public class ChecklistAssignmentIdsDTO {

    private final List<Integer> staffIds;
    private final List<Integer> partnerStaffIds;

    public ChecklistAssignmentIdsDTO(List<Integer> staffIds, List<Integer> partnerStaffIds) {
        this.staffIds = List.copyOf(staffIds);
        this.partnerStaffIds = List.copyOf(partnerStaffIds);
    }

    public List<Integer> getStaffIds() {
        return staffIds;
    }

    public List<Integer> getPartnerStaffIds() {
        return partnerStaffIds;
    }
}
