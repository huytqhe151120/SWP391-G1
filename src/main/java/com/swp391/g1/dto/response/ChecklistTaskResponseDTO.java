package com.swp391.g1.dto.response;

import com.swp391.g1.model.ChecklistTaskStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChecklistTaskResponseDTO {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final int id;
    private final String code;
    private final String name;
    private final String description;
    private final LocalDateTime deadline;
    private final String status;

    public ChecklistTaskResponseDTO(
            int id, String code, String name, String description, LocalDateTime deadline, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
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
}
