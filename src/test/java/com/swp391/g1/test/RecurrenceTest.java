package com.swp391.g1.test;

import com.swp391.g1.util.RecurrenceGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class RecurrenceTest {

    static int passed = 0;
    static int failed = 0;

    static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + name);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== RecurrenceGenerator Tests ===\n");

        // --- Monthly tests ---
        System.out.println("-- Monthly (start 2026-01-15, interval 1, no end, max 3) --");
        List<LocalDate> m1 = RecurrenceGenerator.generateMonthly(LocalDate.of(2026, 1, 15), 1, null, 3);
        check("monthly returns exactly 3 dates", m1.size() == 3);
        check("monthly date[0] = 2026-01-15", m1.get(0).equals(LocalDate.of(2026, 1, 15)));
        check("monthly date[1] = 2026-02-15", m1.get(1).equals(LocalDate.of(2026, 2, 15)));
        check("monthly date[2] = 2026-03-15", m1.get(2).equals(LocalDate.of(2026, 3, 15)));

        System.out.println("-- Monthly interval=2 (start 2026-01-15, max 3) --");
        List<LocalDate> m2 = RecurrenceGenerator.generateMonthly(LocalDate.of(2026, 1, 15), 2, null, 3);
        check("monthly interval=2 returns 3 dates", m2.size() == 3);
        check("monthly interval=2 date[0] = 2026-01-15", m2.get(0).equals(LocalDate.of(2026, 1, 15)));
        check("monthly interval=2 date[1] = 2026-03-15", m2.get(1).equals(LocalDate.of(2026, 3, 15)));
        check("monthly interval=2 date[2] = 2026-05-15", m2.get(2).equals(LocalDate.of(2026, 5, 15)));

        System.out.println("-- Monthly with endDate (start 2026-01-15, end 2026-02-15, interval 1) --");
        List<LocalDate> m3 = RecurrenceGenerator.generateMonthly(LocalDate.of(2026, 1, 15), 1, LocalDate.of(2026, 2, 15), null);
        check("monthly with endDate returns 2 dates", m3.size() == 2);
        check("monthly with endDate date[1] = 2026-02-15", m3.get(1).equals(LocalDate.of(2026, 2, 15)));

        System.out.println("-- Monthly no drift check (start 2026-01-31, interval 1, max 3) --");
        List<LocalDate> m4 = RecurrenceGenerator.generateMonthly(LocalDate.of(2026, 1, 31), 1, null, 3);
        check("monthly Jan 31 -> Feb 28 (clamp)", m4.get(1).equals(LocalDate.of(2026, 2, 28)));
        check("monthly Jan 31 -> Mar 31", m4.get(2).equals(LocalDate.of(2026, 3, 31)));

        // --- Yearly tests ---
        System.out.println("\n-- Yearly (start 2026-03-10, interval 1, max 3) --");
        List<LocalDate> y1 = RecurrenceGenerator.generateYearly(LocalDate.of(2026, 3, 10), 1, null, 3);
        check("yearly returns exactly 3 dates", y1.size() == 3);
        check("yearly date[0] = 2026-03-10", y1.get(0).equals(LocalDate.of(2026, 3, 10)));
        check("yearly date[1] = 2027-03-10", y1.get(1).equals(LocalDate.of(2027, 3, 10)));
        check("yearly date[2] = 2028-03-10", y1.get(2).equals(LocalDate.of(2028, 3, 10)));

        System.out.println("-- Yearly 29/02 -> 28/02 in non-leap year (start 2024-02-29, interval 1, max 3) --");
        List<LocalDate> y2 = RecurrenceGenerator.generateYearly(LocalDate.of(2024, 2, 29), 1, null, 3);
        check("yearly 2024-02-29 -> 2025-02-28 (clamp)", y2.get(1).equals(LocalDate.of(2025, 2, 28)));
        check("yearly 2024-02-29 -> 2026-02-28 (clamp)", y2.get(2).equals(LocalDate.of(2026, 2, 28)));

        System.out.println("-- Yearly interval=2 (start 2026-03-10, max 3) --");
        List<LocalDate> y3 = RecurrenceGenerator.generateYearly(LocalDate.of(2026, 3, 10), 2, null, 3);
        check("yearly interval=2 returns 3 dates", y3.size() == 3);
        check("yearly interval=2 date[0] = 2026-03-10", y3.get(0).equals(LocalDate.of(2026, 3, 10)));
        check("yearly interval=2 date[1] = 2028-03-10", y3.get(1).equals(LocalDate.of(2028, 3, 10)));
        check("yearly interval=2 date[2] = 2030-03-10", y3.get(2).equals(LocalDate.of(2030, 3, 10)));

        System.out.println("-- Yearly with endDate (start 2026-03-10, end 2027-03-10, interval 1) --");
        List<LocalDate> y4 = RecurrenceGenerator.generateYearly(LocalDate.of(2026, 3, 10), 1, LocalDate.of(2027, 3, 10), null);
        check("yearly with endDate returns 2 dates", y4.size() == 2);
        check("yearly with endDate date[1] = 2027-03-10", y4.get(1).equals(LocalDate.of(2027, 3, 10)));

        // --- buildStartTime / buildEndTime ---
        System.out.println("\n-- buildStartTime / buildEndTime --");
        LocalDateTime start = RecurrenceGenerator.buildStartTime(LocalDate.of(2026, 1, 15), LocalTime.of(8, 30));
        check("buildStartTime = 2026-01-15 08:30", start.equals(LocalDateTime.of(2026, 1, 15, 8, 30)));
        LocalDateTime end = RecurrenceGenerator.buildEndTime(start, 90);
        check("buildEndTime (+90min) = 2026-01-15 10:00", end.equals(LocalDateTime.of(2026, 1, 15, 10, 0)));

        System.out.println("\n=== Ket qua: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
