package com.swp391.g1.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Date-time conventions: everything is stored and shown in GMT+7. */
public final class DateTimeUtil {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private DateTimeUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** dd/MM/yyyy HH:mm, empty string for null. */
    public static String format(LocalDateTime value) {
        return value == null ? "" : value.format(DISPLAY);
    }

    /** yyyy-MM-ddTHH:mm for {@code <input type="datetime-local">}. */
    public static String formatInput(LocalDateTime value) {
        return value == null ? "" : value.format(INPUT);
    }
}
