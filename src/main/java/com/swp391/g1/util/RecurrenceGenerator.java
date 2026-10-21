package com.swp391.g1.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import com.swp391.g1.model.Enum;

/**
 * Thuật toán sinh ngày occurrence cho recurrence MONTHLY và YEARLY.
 * Luôn tính từ mốc gốc (series_start_date), không cộng dồn từ occurrence trước.
 */
public class RecurrenceGenerator {

    private RecurrenceGenerator() {
    }

    /**
     * Sinh danh sách ngày occurrence cho recurrence MONTHLY.
     *
     * @param startDate  ngày bắt đầu của series (mốc gốc)
     * @param interval   N (tháng), >= 1
     * @param endDate    ngày kết thúc của series (có thể null)
     * @param maxOccurrences số lần tối đa (có thể null)
     * @return danh sách ngày occurrence (không bao giờ rỗng nếu có ít nhất 1 ngày)
     */
    public static List<LocalDate> generateMonthly(LocalDate startDate, int interval,
            LocalDate endDate, Integer maxOccurrences) {
        List<LocalDate> dates = new ArrayList<>();
        int k = 0;
        while (true) {
            LocalDate target = addMonths(startDate, k * interval);
            if (endDate != null && target.isAfter(endDate)) {
                break;
            }
            if (maxOccurrences != null && dates.size() >= maxOccurrences) {
                break;
            }
            dates.add(target);
            k++;
        }
        return dates;
    }

    /**
     * Sinh danh sách ngày occurrence cho recurrence YEARLY.
     *
     * @param startDate  ngày bắt đầu của series (mốc gốc)
     * @param interval   N (năm), >= 1
     * @param endDate    ngày kết thúc của series (có thể null)
     * @param maxOccurrences số lần tối đa (có thể null)
     * @return danh sách ngày occurrence (không bao giờ rỗng nếu có ít nhất 1 ngày)
     */
    public static List<LocalDate> generateYearly(LocalDate startDate, int interval,
            LocalDate endDate, Integer maxOccurrences) {
        List<LocalDate> dates = new ArrayList<>();
        int k = 0;
        while (true) {
            int targetYear = startDate.getYear() + k * interval;
            int day = startDate.getDayOfMonth();
            int month = startDate.getMonthValue();
            // Clamp 29/02 sang 28/02 nếu năm mục tiêu không phải năm nhuận
            if (month == 2 && day == 29 && !isLeapYear(targetYear)) {
                day = 28;
            }
            LocalDate target = LocalDate.of(targetYear, month, day);
            if (endDate != null && target.isAfter(endDate)) {
                break;
            }
            if (maxOccurrences != null && dates.size() >= maxOccurrences) {
                break;
            }
            dates.add(target);
            k++;
        }
        return dates;
    }

    /**
     * Tính LocalDateTime bắt đầu của occurrence từ ngày và thời gian trong ngày.
     */
    public static LocalDateTime buildStartTime(LocalDate date, LocalTime timeOfDay) {
        return LocalDateTime.of(date, timeOfDay);
    }

    /**
     * Tính LocalDateTime kết thúc bằng cách cộng duration (phút) vào startTime.
     */
    public static LocalDateTime buildEndTime(LocalDateTime startTime, int durationMinutes) {
        return startTime.plusMinutes(durationMinutes);
    }

    private static LocalDate addMonths(LocalDate date, int months) {
        return date.plusMonths(months);
    }

    private static boolean isLeapYear(int year) {
        return Year.isLeap(year);
    }
}
