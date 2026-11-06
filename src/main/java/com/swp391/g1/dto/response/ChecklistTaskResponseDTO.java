package com.swp391.g1.dto.response;

import com.swp391.g1.model.ChecklistTaskStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChecklistTaskResponseDTO {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final int id;
    private final int activityId;
    private final String code;
    private final String name;
    private final String description;
    private final LocalDateTime deadline;
    private final String status;
    private final String assignees;
    private final String staffAssignees;
    private final String partnerStaffAssignees;
    private final String prerequisites;
    private final boolean prerequisitesCompleted;
    private final String phase;
    private final int progress;
    private final String priority;

    public ChecklistTaskResponseDTO(
            int id, int activityId, String code, String name, String description, LocalDateTime deadline,
            String status, String assignees, String staffAssignees, String partnerStaffAssignees,
            String prerequisites, boolean prerequisitesCompleted, String phase, int progress, String priority) {
        this.id = id;
        this.activityId = activityId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
        this.assignees = assignees;
        this.staffAssignees = staffAssignees;
        this.partnerStaffAssignees = partnerStaffAssignees;
        this.prerequisites = prerequisites;
        this.prerequisitesCompleted = prerequisitesCompleted;
        this.phase = phase;
        this.progress = progress;
        this.priority = priority;
    }

    public int getId() {
        return id;
    }

    public int getActivityId() {
        return activityId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public String getDeadlineDisplay() {
        return deadline == null ? "Chưa có hạn" : deadline.format(DEADLINE_FORMAT);
    }

    public String getStatus() {
        return status;
    }

    public String getStatusDisplay() {
        return status == null || status.isBlank() ? ChecklistTaskStatus.NOT_COMPLETED
                : ChecklistTaskStatus.normalize(status);
    }

    public String getStatusValue() {
        return ChecklistTaskStatus.normalize(status);
    }

    public String getAssignees() {
        return assignees;
    }

    public String getStaffAssignees() {
        return staffAssignees;
    }

    public String getPartnerStaffAssignees() {
        return partnerStaffAssignees;
    }

    public String getDeadlineFilterValue() {
        return deadline == null ? "" : deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public boolean isPrerequisitesCompleted() {
        return prerequisitesCompleted;
    }

    public String getPhase() {
        return phase;
    }

    public String getPhaseDisplay() {
        return switch (phase == null ? "" : phase) {
            case "BEFORE_EVENT" -> "Trước hoạt động";
            case "DURING_EVENT" -> "Trong hoạt động";
            case "AFTER_EVENT" -> "Sau hoạt động";
            default -> phase == null || phase.isBlank() ? "Chưa xác định" : phase;
        };
    }

    public int getProgress() {
        return progress;
    }

    public String getPriority() {
        return priority;
    }

    public String getPriorityDisplay() {
        return switch (priority == null ? "" : priority) {
            case "HIGH" -> "Cao";
            case "LOW" -> "Thấp";
            case "MEDIUM" -> "Trung bình";
            default -> priority == null || priority.isBlank() ? "Trung bình" : priority;
        };
    }
}
