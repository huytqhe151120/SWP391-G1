package com.swp391.g1.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Lớp tiện ích xử lý ngày giờ thống nhất cho toàn ứng dụng.
 * Nghiệp vụ hiển thị và nhập liệu sử dụng múi giờ Việt Nam ({@code Asia/Ho_Chi_Minh}, GMT+7),
 * còn các mốc thời gian hoạt động lưu trong cơ sở dữ liệu sử dụng chuẩn UTC.
 */
public final class DateTimeUtil {

    // Múi giờ chuẩn của ứng dụng (Việt Nam, GMT+7)
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    // Múi giờ UTC dùng để lưu trữ và so sánh mốc thời gian trong DB
    private static final ZoneId UTC = ZoneOffset.UTC;
    // Định dạng hiển thị ngày giờ rút gọn trên giao diện (dd/MM/yyyy HH:mm)
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // Định dạng hiển thị đầy đủ giây theo giờ Việt Nam cho mốc Instant (ví dụ thời điểm đăng nhập)
    private static final DateTimeFormatter LOGIN_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZONE);
    // Định dạng chuẩn HTML5 cho thẻ <input type="datetime-local">
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /**
     * Constructor private nhằm ngăn khởi tạo đối tượng từ lớp tiện ích chỉ chứa hàm static.
     */
    private DateTimeUtil() {
    }

    /**
     * Lấy thời điểm hiện tại theo múi giờ Việt Nam ({@link #ZONE}).
     *
     * @return {@link LocalDateTime} hiện tại ở GMT+7
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /**
     * Lấy thời điểm hiện tại theo múi giờ UTC.
     *
     * @return {@link LocalDateTime} hiện tại ở UTC
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }

    /**
     * Chuyển đổi mốc thời gian từ múi giờ Việt Nam (GMT+7) sang múi giờ UTC để lưu xuống cơ sở dữ liệu.
     *
     * @param vietnamTime Thời gian theo múi giờ Việt Nam
     * @return Thời gian tương ứng theo UTC, hoặc {@code null} nếu đầu vào là {@code null}
     */
    public static LocalDateTime toUtc(LocalDateTime vietnamTime) {
        // Nếu đầu vào null thì trả về null; ngược lại gắn múi giờ GMT+7 rồi quy đổi cùng thời điểm sang UTC
        return vietnamTime == null
                ? null
                : vietnamTime.atZone(ZONE).withZoneSameInstant(UTC).toLocalDateTime();
    }

    /**
     * Chuyển đổi mốc thời gian từ múi giờ UTC (đọc từ DB) sang múi giờ Việt Nam (GMT+7) để xử lý và hiển thị.
     *
     * @param utcTime Thời gian theo múi giờ UTC
     * @return Thời gian tương ứng theo giờ Việt Nam, hoặc {@code null} nếu đầu vào là {@code null}
     */
    public static LocalDateTime fromUtc(LocalDateTime utcTime) {
        // Nếu đầu vào null thì trả về null; ngược lại gắn múi giờ UTC rồi quy đổi cùng thời điểm sang GMT+7
        return utcTime == null
                ? null
                : utcTime.atZone(UTC).withZoneSameInstant(ZONE).toLocalDateTime();
    }

    /**
     * Định dạng {@link LocalDateTime} sang chuỗi {@code dd/MM/yyyy HH:mm} để hiển thị trên UI.
     *
     * @param value Giá trị ngày giờ cần định dạng
     * @return Chuỗi đã định dạng, hoặc chuỗi rỗng {@code ""} nếu {@code value} là {@code null}
     */
    public static String format(LocalDateTime value) {
        // Trả về chuỗi rỗng nếu null để tránh hiển thị "null" trên JSP
        return value == null ? "" : value.format(DISPLAY);
    }

    /**
     * Định dạng {@link Instant} sang chuỗi {@code dd/MM/yyyy HH:mm:ss} theo múi giờ Việt Nam.
     *
     * @param value Mốc thời gian Instant cần định dạng
     * @return Chuỗi đã định dạng theo giờ Việt Nam, hoặc chuỗi rỗng {@code ""} nếu {@code value} là {@code null}
     */
    public static String format(Instant value) {
        // Trả về chuỗi rỗng nếu null, ngược lại áp dụng formatter đã gắn múi giờ Việt Nam
        return value == null ? "" : LOGIN_DISPLAY.format(value);
    }

    /**
     * Định dạng {@link LocalDateTime} sang chuỗi {@code yyyy-MM-dd'T'HH:mm} để bind vào thẻ {@code <input type="datetime-local">}.
     *
     * @param value Giá trị ngày giờ cần định dạng
     * @return Chuỗi định dạng input HTML5, hoặc chuỗi rỗng {@code ""} nếu {@code value} là {@code null}
     */
    public static String formatInput(LocalDateTime value) {
        // Trả về chuỗi rỗng nếu null, ngược lại định dạng theo chuẩn datetime-local
        return value == null ? "" : value.format(INPUT);
    }
}
