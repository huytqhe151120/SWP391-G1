package com.swp391.g1.dto.response;

import java.util.List;

public record ChecklistTaskAlertDTO(
        String code,
        String name,
        String priority,
        String deadlineDisplay,
        String targetUrl,
        List<String> recipientEmails) {
}
