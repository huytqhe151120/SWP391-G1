package com.swp391.g1.util;

import com.swp391.g1.model.Enum.ParticipantStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityScoreCalculatorTest {

    @Test
    void computeSessionScore_mapsEveryStatusAccurately() {
        assertEquals(new BigDecimal("5.00"), ActivityScoreCalculator.computeSessionScore(ParticipantStatus.ATTENDED));
        assertEquals(new BigDecimal("3.00"), ActivityScoreCalculator.computeSessionScore(ParticipantStatus.IN_LATE));
        assertEquals(new BigDecimal("3.00"), ActivityScoreCalculator.computeSessionScore(ParticipantStatus.OUT_EARLY));
        assertEquals(new BigDecimal("-5.00"), ActivityScoreCalculator.computeSessionScore(ParticipantStatus.ABSENT));
        assertEquals(new BigDecimal("0.00"), ActivityScoreCalculator.computeSessionScore(ParticipantStatus.NOT_YET));
        assertEquals(new BigDecimal("0.00"), ActivityScoreCalculator.computeSessionScore(null));
    }

    @Test
    void resolveOverallStatus_followsPriorityOrder() {
        assertEquals(ParticipantStatus.ATTENDED, ActivityScoreCalculator.resolveOverallStatus(
                List.of(ParticipantStatus.ABSENT, ParticipantStatus.OUT_EARLY, ParticipantStatus.IN_LATE, ParticipantStatus.ATTENDED)));
        assertEquals(ParticipantStatus.IN_LATE, ActivityScoreCalculator.resolveOverallStatus(
                List.of(ParticipantStatus.ABSENT, ParticipantStatus.OUT_EARLY, ParticipantStatus.IN_LATE)));
        assertEquals(ParticipantStatus.OUT_EARLY, ActivityScoreCalculator.resolveOverallStatus(
                List.of(ParticipantStatus.ABSENT, ParticipantStatus.OUT_EARLY)));
        assertEquals(ParticipantStatus.ABSENT, ActivityScoreCalculator.resolveOverallStatus(
                List.of(ParticipantStatus.NOT_YET, ParticipantStatus.ABSENT)));
        assertEquals(ParticipantStatus.NOT_YET, ActivityScoreCalculator.resolveOverallStatus(List.of()));
    }

    @Test
    void summarizeSessions_ignoresSessionsAboveTotalSessionsAndAveragesProperly() {
        Map<Integer, ParticipantStatus> sessions = Map.of(
                1, ParticipantStatus.ATTENDED,   // +5
                2, ParticipantStatus.IN_LATE,    // +3
                3, ParticipantStatus.ABSENT,     // -5
                4, ParticipantStatus.ATTENDED    // ignored because totalSessions = 3
        );

        ActivityScoreCalculator.SessionStats stats =
                ActivityScoreCalculator.summarizeSessions(sessions, 3, ParticipantStatus.NOT_YET);

        assertEquals(ParticipantStatus.ATTENDED, stats.overallStatus());
        assertEquals(2, stats.attendedSessionCount());
        assertEquals(1, stats.absentSessionCount());
        assertEquals(3, stats.markedSessionCount());
        // (5 + 3 - 5) / 3 = 1.00
        assertEquals(new BigDecimal("1.00"), stats.attendanceScore());
    }

    @Test
    void summarizeSessions_usesLegacyFallbackWhenSessionMapIsEmpty() {
        ActivityScoreCalculator.SessionStats stats =
                ActivityScoreCalculator.summarizeSessions(Map.of(), 2, ParticipantStatus.ATTENDED);

        assertEquals(ParticipantStatus.ATTENDED, stats.overallStatus());
        assertEquals(1, stats.attendedSessionCount());
        assertEquals(0, stats.absentSessionCount());
        assertEquals(1, stats.markedSessionCount());
        // 5.00 / 2 = 2.50
        assertEquals(new BigDecimal("2.50"), stats.attendanceScore());
    }

    @Test
    void calculate_presentOrganizerRegisteredWithSummary() {
        Map<Integer, ParticipantStatus> sessions = Map.of(
                1, ParticipantStatus.ATTENDED,
                2, ParticipantStatus.OUT_EARLY
        );

        ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                sessions,
                2,
                ParticipantStatus.NOT_YET,
                new BigDecimal("4.50"),
                new BigDecimal("9.00"),
                true,
                new BigDecimal("1.20"),
                "REGISTERED",
                "ORGANIZER"
        );

        // attendance = (5 + 3) / 2 = 4.00
        assertEquals(new BigDecimal("4.00"), breakdown.attendanceScore());
        assertEquals(new BigDecimal("4.50"), breakdown.effectiveEvalScore());
        // participation = 4.00 + 4.50 = 8.50
        assertEquals(new BigDecimal("8.50"), breakdown.participationScore());
        // average = (8.50 + 9.00) / 2 = 8.75
        assertEquals(new BigDecimal("8.75"), breakdown.averageScore());
        assertEquals(new BigDecimal("1.20"), breakdown.orgLevelCoefficient());
        assertEquals(new BigDecimal("1.10"), breakdown.earlyRegCoefficient());
        assertEquals(new BigDecimal("1.50"), breakdown.organizerCoefficient());
        // final = 8.75 * 1.20 * 1.10 * 1.50 = 17.325 -> 17.33
        assertEquals(new BigDecimal("17.33"), breakdown.finalPoint());
    }

    @Test
    void calculate_presentWithoutSummaryScoreFallsBackToParticipationScore() {
        ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                Map.of(1, ParticipantStatus.ATTENDED),
                1,
                ParticipantStatus.NOT_YET,
                new BigDecimal("3.00"),
                null,
                true,
                null,
                "STAFF_ENROLLED",
                "PARTICIPANT"
        );

        assertEquals(new BigDecimal("5.00"), breakdown.attendanceScore());
        assertEquals(new BigDecimal("8.00"), breakdown.participationScore());
        assertNull(breakdown.summaryScore());
        assertEquals(new BigDecimal("8.00"), breakdown.averageScore());
        assertEquals(new BigDecimal("1.00"), breakdown.orgLevelCoefficient());
        assertEquals(new BigDecimal("1.00"), breakdown.earlyRegCoefficient());
        assertEquals(new BigDecimal("1.00"), breakdown.organizerCoefficient());
        assertEquals(new BigDecimal("8.00"), breakdown.finalPoint());
    }

    @Test
    void calculate_absentIgnoresEvalScoreAndDoesNotMultiplyCoefficients() {
        ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                Map.of(1, ParticipantStatus.ABSENT, 2, ParticipantStatus.ABSENT),
                2,
                ParticipantStatus.ABSENT,
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                true,
                new BigDecimal("1.50"),
                "REGISTERED",
                "ORGANIZER"
        );

        assertEquals(ParticipantStatus.ABSENT, breakdown.overallStatus());
        assertEquals(new BigDecimal("-5.00"), breakdown.attendanceScore());
        assertEquals(new BigDecimal("0.00"), breakdown.effectiveEvalScore());
        assertEquals(new BigDecimal("-5.00"), breakdown.participationScore());
        assertEquals(new BigDecimal("-5.00"), breakdown.averageScore());
        assertEquals(new BigDecimal("-5.00"), breakdown.finalPoint());
    }

    @Test
    void calculate_notYetReturnsZeros() {
        ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                Map.of(),
                3,
                ParticipantStatus.NOT_YET,
                new BigDecimal("4.00"),
                null,
                false,
                new BigDecimal("1.20"),
                "REGISTERED",
                "PARTICIPANT"
        );

        assertEquals(ParticipantStatus.NOT_YET, breakdown.overallStatus());
        assertEquals(new BigDecimal("0.00"), breakdown.attendanceScore());
        assertEquals(new BigDecimal("0.00"), breakdown.participationScore());
        assertEquals(new BigDecimal("0.00"), breakdown.averageScore());
        assertEquals(new BigDecimal("0.00"), breakdown.finalPoint());
    }

    @Test
    void isRemovable_enforcesAllSafetyRules() {
        assertTrue(ActivityScoreCalculator.isRemovable("PARTICIPANT", ParticipantStatus.NOT_YET, 0, false));
        assertFalse(ActivityScoreCalculator.isRemovable("ORGANIZER", ParticipantStatus.NOT_YET, 0, false));
        assertFalse(ActivityScoreCalculator.isRemovable("PARTICIPANT", ParticipantStatus.ATTENDED, 0, false));
        assertFalse(ActivityScoreCalculator.isRemovable("PARTICIPANT", ParticipantStatus.NOT_YET, 1, false));
        assertFalse(ActivityScoreCalculator.isRemovable("PARTICIPANT", ParticipantStatus.NOT_YET, 0, true));
    }
}
