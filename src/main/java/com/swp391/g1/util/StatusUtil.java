package com.swp391.g1.util;

import java.util.Map;

/**
 * Lớp tiện ích ánh xạ các mã trạng thái (enum/chuỗi) sang nhãn tiếng Việt và lớp CSS màu sắc
 * phục vụ hiển thị đồng nhất trên giao diện người dùng.
 */
public final class StatusUtil {

    // Bảng ánh xạ bất biến từ mã trạng thái chuẩn sang nhãn hiển thị tiếng Việt
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("ACTIVE", "Đang hoạt động"),
            Map.entry("INACTIVE", "Không hoạt động"),
            Map.entry("BLOCKED", "Đã bị khóa"),
            Map.entry("UPCOMING", "Chưa diễn ra"),
            Map.entry("ONGOING", "Đang diễn ra"),
            Map.entry("COMPLETED", "Đã hoàn thành"),
            Map.entry("CANCELLED", "Đã bị hủy"),
            Map.entry("DRAFT", "Bản nháp"),
            Map.entry("PENDING", "Chờ duyệt"),
            Map.entry("APPROVED", "Đã duyệt"),
            Map.entry("REJECTED", "Đã từ chối"),
            Map.entry("NOT_YET", "Chưa điểm danh"),
            Map.entry("ATTENDED", "Có mặt"),
            Map.entry("ABSENT", "Vắng mặt"),
            Map.entry("IN_LATE", "Đi muộn"),
            Map.entry("OUT_EARLY", "Về sớm"),
            Map.entry("OPENING", "Đang mở đăng ký"),
            Map.entry("CLOSED", "Đã đóng đăng ký"),
            Map.entry("SUBMITTED", "Chờ duyệt"),
            Map.entry("WITHDRAWN", "Đã rút đơn"));

    /**
     * Constructor private nhằm ngăn khởi tạo đối tượng từ lớp tiện ích static.
     */
    private StatusUtil() {
    }

    /**
     * Trả về nhãn tiếng Việt tương ứng với một giá trị trạng thái (có thể là Enum hoặc String).
     *
     * @param status Đối tượng trạng thái cần chuyển đổi sang nhãn
     * @return Nhãn tiếng Việt tương ứng; trả về {@code "Chưa xác định"} nếu null, hoặc chính chuỗi gốc nếu chưa có trong bảng ánh xạ
     */
    public static String label(Object status) {
        // Trả về nhãn mặc định khi giá trị trạng thái bị null
        if (status == null) {
            return "Chưa xác định";
        }
        String key = status.toString();
        return LABELS.getOrDefault(key, key);
    }

    /**
     * Trả về tên lớp CSS màu sắc (badge/pill) tương ứng với trạng thái để hiển thị trực quan trên UI.
     *
     * @param status Đối tượng trạng thái cần lấy lớp CSS
     * @return Tên lớp CSS (ví dụ {@code status-green}, {@code status-yellow}, {@code status-red}, ...)
     */
    public static String css(Object status) {
        // Trả về màu xám trung tính nếu trạng thái là null
        if (status == null) {
            return "status-gray";
        }
        // Phân loại màu sắc theo nhóm ý nghĩa nghiệp vụ của từng mã trạng thái
        return switch (status.toString()) {
            // Nhóm trạng thái tích cực / hoàn tất / đã duyệt / có mặt -> xanh lá
            case "ACTIVE", "COMPLETED", "APPROVED", "ATTENDED" -> "status-green";
            // Nhóm trạng thái đang diễn ra / chờ xử lý / lưu ý (đi muộn, về sớm, bản nháp) -> vàng
            case "ONGOING", "PENDING", "SUBMITTED", "IN_LATE", "OUT_EARLY", "DRAFT" -> "status-yellow";
            // Nhóm trạng thái tiêu cực / hủy / từ chối / đóng / vắng / khóa -> đỏ
            case "CANCELLED", "REJECTED", "CLOSED", "ABSENT", "BLOCKED" -> "status-red";
            // Trạng thái đang mở đăng ký -> xanh lá
            case "OPENING" -> "status-green";
            // Trạng thái sinh viên đã rút đơn -> xanh dương
            case "WITHDRAWN" -> "status-blue";
            // Trạng thái không hoạt động -> màu mờ
            case "INACTIVE" -> "status-muted";
            // Các trạng thái chưa phân loại (ví dụ UPCOMING, NOT_YET) -> xám mặc định
            default -> "status-gray";
        };
    }
}
