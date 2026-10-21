package com.swp391.g1.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Application dates use Vietnam time; database activity timestamps use UTC. */
public final class DateTimeUtil {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private DateTimeUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }

    public static LocalDateTime toUtc(LocalDateTime vietnamTime) {
        return vietnamTime == null
                ? null
                : vietnamTime.atZone(ZONE).withZoneSameInstant(UTC).toLocalDateTime();
    }

    public static LocalDateTime fromUtc(LocalDateTime utcTime) {
        return utcTime == null
                ? null
                : utcTime.atZone(UTC).withZoneSameInstant(ZONE).toLocalDateTime();
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
