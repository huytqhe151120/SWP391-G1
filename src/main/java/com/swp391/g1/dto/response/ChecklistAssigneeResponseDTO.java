package com.swp391.g1.dto.response;

import com.swp391.g1.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChecklistAssigneeResponseDTO {

    private static final DateTimeFormatter ASSIGNED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final int id;
    private final String code;
    private final String name;
    private final String companyName;
    private final String status;
    private final String position;
    private final LocalDateTime assignedAt;

    public ChecklistAssigneeResponseDTO(int id, String code, String name, String companyName,
                                        String status, String position, LocalDateTime assignedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.companyName = companyName;
        this.status = status;
        this.position = position;
        this.assignedAt = assignedAt == null ? null : DateTimeUtil.fromUtc(assignedAt);
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

    public String getStatus() {
        return status;
    }

    public String getStatusDisplay() {
        return "ACTIVE".equalsIgnoreCase(status) ? "Đang hoạt động"
                : status == null || status.isBlank() ? "Chưa xác định" : status;
    }

    public String getPosition() {
        return position;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public String getAssignedAtDisplay() {
        return assignedAt == null ? "Chưa ghi nhận" : assignedAt.format(ASSIGNED_AT_FORMAT);
    }
}
