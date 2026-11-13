package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IRecruitmentApplicationDAO;
import com.swp391.g1.dao.IRecruitmentRoundDAO;
import com.swp391.g1.model.RecruitmentApplication;
import com.swp391.g1.model.RecruitmentInterview;
import com.swp391.g1.model.RecruitmentPosition;
import com.swp391.g1.model.RecruitmentRound;
import com.swp391.g1.service.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecruitmentServiceImplTest {
    private IRecruitmentRoundDAO rounds;
    private IRecruitmentApplicationDAO applications;
    private INotificationService notifications;
    private RecruitmentServiceImpl service;

    @BeforeEach
    void setUp() {
        rounds = mock(IRecruitmentRoundDAO.class);
        applications = mock(IRecruitmentApplicationDAO.class);
        notifications = mock(INotificationService.class);
        service = new RecruitmentServiceImpl(rounds, applications, notifications);
    }

    @Test
    void lookupByActivitySynchronizesStatusesAndRejectsInvalidIds() {
        RecruitmentRound round = openRound();
        when(rounds.findByActivityId(2)).thenReturn(round);
        assertNull(service.getRoundByActivity(0));
        verifyNoInteractions(rounds);
        assertSame(round, service.getRoundByActivity(2));
        var order = inOrder(rounds);
        order.verify(rounds).synchronizeStatuses();
        order.verify(rounds).findByActivityId(2);
    }

    @Test
    void reapplicationDoesNotDeleteWithdrawnRecordBeforeAtomicInsert() {
        RecruitmentRound round = openRound();
        RecruitmentApplication withdrawn = application("WITHDRAWN");
        when(rounds.findById(1)).thenReturn(round);
        when(rounds.isActivityEligible(2)).thenReturn(true);
        when(applications.findByRoundAndStudent(1, 3)).thenReturn(withdrawn);

        assertNull(service.apply(1, 3, 4, null, Map.of(), ""));
        verify(applications).insert(any(), eq(round.getUpdatedAt()));
        verify(applications, never()).deleteWithdrawn(anyInt());
    }

    @Test
    void cannotApplyForFullPosition() {
        RecruitmentRound round = openRound();
        round.getPositions().get(0).setAcceptedCount(1);
        when(rounds.findById(1)).thenReturn(round);
        when(rounds.isActivityEligible(2)).thenReturn(true);

        assertNotNull(service.apply(1, 3, 4, null, Map.of(), ""));
        verify(applications, never()).insert(any(), any());
    }

    @Test
    void acceptedOrganizerNeedsReasonAndMustBeMoreThan48HoursBeforeStart() {
        RecruitmentApplication app = application("ACCEPTED");
        when(applications.findById(7)).thenReturn(app);
        app.setActivityStart(LocalDateTime.now().plusHours(47));
        assertNotNull(service.withdraw(7, 3, "Bận"));
        app.setActivityStart(LocalDateTime.now().plusHours(49));
        assertNotNull(service.withdraw(7, 3, " "));
        verify(applications, never()).withdraw(anyInt(), anyList(), any());
        when(applications.withdraw(eq(7), anyList(), eq("Bận"))).thenReturn(true);
        assertNull(service.withdraw(7, 3, "Bận"));
        verify(notifications).notifyActivityStaff(eq(2), eq("RECRUITMENT_ORGANIZER_WITHDRAWN"),
                anyString(), anyString(), anyString());
    }

    @Test
    void invitedApplicantCannotWithdraw() {
        when(applications.findById(7)).thenReturn(application("INTERVIEW_INVITED"));
        assertNotNull(service.withdraw(7, 3, ""));
        verify(applications, never()).withdraw(anyInt(), anyList(), any());
    }

    @Test
    void cannotCompleteInterviewBeforeScheduledTime() {
        RecruitmentApplication app = application("INTERVIEW_INVITED");
        RecruitmentInterview interview = new RecruitmentInterview();
        interview.setScheduledAt(LocalDateTime.now().plusHours(1));
        app.setInterview(interview);
        when(applications.findById(7)).thenReturn(app);
        assertNotNull(service.completeInterview(7, "Đạt"));
        verify(applications, never()).completeInterview(anyInt(), any());
    }

    @Test
    void openingRoundRequiresBothDates() {
        RecruitmentRound round = openRound();
        round.setStatus("DRAFT");
        when(rounds.findById(1)).thenReturn(round);
        when(rounds.isActivityEligible(2)).thenReturn(true);
        assertNotNull(service.openRound(1));
        verify(rounds, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    void fullOrConcurrentlyChangedPositionDoesNotSendAcceptanceNotification() {
        RecruitmentRound round = openRound();
        round.setInterviewRequired(false);
        RecruitmentApplication app = application("PENDING");
        when(applications.findById(7)).thenReturn(app);
        when(rounds.findById(1)).thenReturn(round);
        when(applications.accept(7, List.of("PENDING"), 4)).thenReturn(false);
        assertNotNull(service.accept(7, 4));
        verify(notifications, never()).notifyStudent(anyInt(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void reminderUsesAtomicClaimAndNotification() {
        RecruitmentInterview interview = new RecruitmentInterview();
        interview.setId(8);
        interview.setApplicationId(7);
        interview.setConfirmDeadline(LocalDateTime.now().plusHours(12));
        when(applications.findInterviewsNeedingReminder(24)).thenReturn(List.of(interview));
        when(applications.findById(7)).thenReturn(application("INTERVIEW_INVITED"));
        when(applications.sendInterviewReminderIfDue(eq(interview), eq(24), anyString(), anyString(), anyString()))
                .thenReturn(true);
        assertEquals(1, service.sendInterviewReminders());
        verify(notifications, never()).notifyStudent(anyInt(), anyString(), anyString(), anyString(), anyString());
    }

    private static RecruitmentRound openRound() {
        RecruitmentRound round = new RecruitmentRound();
        round.setId(1);
        round.setActivityId(2);
        round.setStatus("OPEN");
        round.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        RecruitmentPosition position = new RecruitmentPosition();
        position.setId(4);
        position.setQuota(1);
        position.setName("Hậu cần");
        round.setPositions(List.of(position));
        return round;
    }

    private static RecruitmentApplication application(String status) {
        RecruitmentApplication app = new RecruitmentApplication();
        app.setId(7);
        app.setRoundId(1);
        app.setActivityId(2);
        app.setStudentId(3);
        app.setStatus(status);
        app.setActivityStart(LocalDateTime.now().plusDays(4));
        return app;
    }
}
