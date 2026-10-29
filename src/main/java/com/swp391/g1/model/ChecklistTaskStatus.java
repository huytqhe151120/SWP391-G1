package com.swp391.g1.model;

import java.util.Set;

public final class ChecklistTaskStatus {

    public static final String COMPLETED = "Đã hoàn thành";
    public static final String IN_PROGRESS = "Đang hoàn thành";
    public static final String NOT_COMPLETED = "Chưa hoàn thành";

    private static final Set<String> VALUES = Set.of(COMPLETED, IN_PROGRESS, NOT_COMPLETED);

    private ChecklistTaskStatus() {
    }

    public static boolean isValid(String status) {
        return VALUES.contains(status);
    }

    public static String normalize(String status) {
        if (COMPLETED.equals(status) || "COMPLETED".equalsIgnoreCase(status)) {
            return COMPLETED;
        }
        if (IN_PROGRESS.equals(status) || "Đang thực hiện".equals(status)
                || "IN_PROGRESS".equalsIgnoreCase(status) || "ONGOING".equalsIgnoreCase(status)) {
            return IN_PROGRESS;
        }
        return NOT_COMPLETED;
    }
}
