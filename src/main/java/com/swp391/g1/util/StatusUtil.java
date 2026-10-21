package com.swp391.g1.util;

import java.util.Map;

/** Vietnamese labels and color classes for status values shown in the UI. */
public final class StatusUtil {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("ACTIVE", "Đang hoạt động"),
            Map.entry("INACTIVE", "Không hoạt động"),
            Map.entry("UPCOMING", "Chưa diễn ra"),
            Map.entry("ONGOING", "Đang diễn ra"),
            Map.entry("COMPLETED", "Đã hoàn thành"),
            Map.entry("CANCELLED", "Đã bị hủy"),
            Map.entry("DRAFT", "Bản nháp"),
            Map.entry("PENDING", "Chờ duyệt"),
            Map.entry("APPROVED", "Đã duyệt"),
            Map.entry("REJECTED", "Đã từ chối"),
            Map.entry("OPENING", "Đang mở đăng ký"),
            Map.entry("CLOSED", "Đã đóng đăng ký"));

    private StatusUtil() {
    }

    public static String label(Object status) {
        if (status == null) {
            return "Chưa xác định";
        }
        String key = status.toString();
        return LABELS.getOrDefault(key, key);
    }

    public static String css(Object status) {
        if (status == null) {
            return "status-gray";
        }
        return switch (status.toString()) {
            case "ACTIVE", "COMPLETED", "APPROVED" -> "status-green";
            case "ONGOING", "PENDING" -> "status-yellow";
            case "CANCELLED", "REJECTED", "CLOSED" -> "status-red";
            case "OPENING" -> "status-green";
            case "DRAFT" -> "status-yellow";
            default -> "status-gray";
        };
    }
}
