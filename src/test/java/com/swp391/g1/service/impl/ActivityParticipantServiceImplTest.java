package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityParticipantDAO;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ParticipantStatus;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityParticipantServiceImplTest {

    private IActivityParticipantDAO participantDAO;
    private IExtracurricularActivityService activityService;
    private INotificationService notificationService;
    private ActivityParticipantServiceImpl service;

    @BeforeEach
    void setUp() {
        participantDAO = mock(IActivityParticipantDAO.class);
        activityService = mock(IExtracurricularActivityService.class);
        notificationService = mock(INotificationService.class);
        service = new ActivityParticipantServiceImpl(participantDAO, activityService, notificationService);
    }

    @Test
    void getParticipants_computesScoresAndClampsSessionInServiceLayer() {
        IActivityParticipantDAO.ParticipantRawData raw = new IActivityParticipantDAO.ParticipantRawData(
                10,
                100,
                "HE180001",
                "Nguyen Van A",
                "SE1801",
                "SE",
                "Software Engineering",
                "IT",
                ParticipantStatus.NOT_YET,
                "REGISTERED",
                "PARTICIPANT",
                1L,
                LocalDateTime.now(),
                new BigDecimal("4.00"),
                "Good",
                "Staff A",
                LocalDateTime.now(),
                null,
                2,
                false,
                new BigDecimal("1.20"),
                true,
                Map.of(1, ParticipantStatus.ATTENDED, 2, ParticipantStatus.IN_LATE)
        );
        when(participantDAO.findRawParticipantsByActivityId(5)).thenReturn(List.of(raw));

        // Yêu cầu session 99 nhưng hoạt động chỉ có 2 buổi -> clamp về buổi 2
        List<ActivityParticipantResponseDTO> list = service.getParticipants(5, 99);

        assertEquals(1, list.size());
        ActivityParticipantResponseDTO dto = list.get(0);
        assertEquals(2, dto.getSessionNumber());
        assertEquals(ParticipantStatus.ATTENDED, dto.getAttendanceStatus());
        assertEquals(ParticipantStatus.IN_LATE, dto.getSessionAttendanceStatus());
        assertEquals(new BigDecimal("3.00"), dto.getSessionAttendanceScore());
        // attendance = (5 + 3) / 2 = 4.00; participation = 4 + 4 = 8.00; final = 8 * 1.2 * 1.1 = 10.56
        assertEquals(new BigDecimal("4.00"), dto.getAttendanceScore());
        assertEquals(new BigDecimal("8.00"), dto.getParticipationScore());
        assertEquals(new BigDecimal("10.56"), dto.getFinalPoint());
        assertFalse(dto.isRemovable());
        verify(participantDAO, never()).findByActivityId(anyInt(), anyInt());
    }

    @Test
    void addStudents_usesBatchResultWithoutReloadingFullParticipantList() {
        ExtracurricularActivityResponseDTO activity = new ExtracurricularActivityResponseDTO();
        activity.setId(7);
        activity.setName("Tech Talk");
        activity.setActivityStatus(ActivityStatus.UPCOMING);
        activity.setMaxParticipants(50);

        when(activityService.getActivityById(7)).thenReturn(activity);
        when(participantDAO.insertParticipantsAndReturnAdded(eq(7), eq(List.of(101, 102)), eq(50), eq(1)))
                .thenReturn(new IActivityParticipantDAO.InsertParticipantsResult(false, List.of(101)));

        int added = service.addStudents(7, List.of(101, 102, 101));

        assertEquals(1, added);
        verify(notificationService, times(1)).notifyStudent(eq(101), eq("ACTIVITY_PARTICIPANT_ADDED"),
                anyString(), anyString(), eq("/activities?action=detail&id=7"));
        verify(notificationService, never()).notifyStudent(eq(102), anyString(), anyString(), anyString(), anyString());
        verify(participantDAO, never()).findByActivityId(anyInt());
    }

    @Test
    void addStudents_throwsWhenCapacityExceeded() {
        ExtracurricularActivityResponseDTO activity = new ExtracurricularActivityResponseDTO();
        activity.setId(7);
        activity.setName("Workshop");
        activity.setActivityStatus(ActivityStatus.UPCOMING);
        activity.setMaxParticipants(10);

        when(activityService.getActivityById(7)).thenReturn(activity);
        when(participantDAO.insertParticipantsAndReturnAdded(eq(7), eq(List.of(101, 102)), eq(10), eq(1)))
                .thenReturn(new IActivityParticipantDAO.InsertParticipantsResult(true, List.of()));
        when(participantDAO.countOccupiedByActivityId(7)).thenReturn(9);

        assertThrows(IllegalArgumentException.class, () -> service.addStudents(7, List.of(101, 102)));
        verify(notificationService, never()).notifyStudent(anyInt(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void removeParticipants_notifiesOnlyActuallyDeletedStudentsWithoutFullReload() {
        ExtracurricularActivityResponseDTO activity = new ExtracurricularActivityResponseDTO();
        activity.setId(9);
        activity.setName("Club Day");
        activity.setActivityStatus(ActivityStatus.UPCOMING);

        when(activityService.getActivityById(9)).thenReturn(activity);
        when(participantDAO.deleteRemovableAndReturnStudentIds(9, List.of(11, 12)))
                .thenReturn(List.of(201));

        int removed = service.removeParticipants(9, List.of(11, 12));

        assertEquals(1, removed);
        verify(notificationService, times(1)).notifyStudent(eq(201), eq("ACTIVITY_PARTICIPANT_REMOVED"),
                anyString(), anyString(), eq("/activities"));
        verify(participantDAO, never()).findByActivityId(anyInt());
    }

    @Test
    void saveParticipationGrades_notifiesOnlyActuallyGradedStudentsWithoutFullReload() {
        ExtracurricularActivityResponseDTO activity = new ExtracurricularActivityResponseDTO();
        activity.setId(15);
        activity.setName("Hackathon");
        activity.setActivityStatus(ActivityStatus.ONGOING);

        CurrentUser staff = new CurrentUser(55, "staff01", "STAFF", "STAFF", Instant.now(), "Staff One");

        when(activityService.getActivityById(15)).thenReturn(activity);
        when(participantDAO.updateParticipationGradesAndReturnStudentIds(
                eq(15), eq(List.of(1, 2)), eq(new BigDecimal("4.5")), eq("Great"), anyString(), eq(55)))
                .thenReturn(List.of(301));

        int graded = service.saveParticipationGrades(15, List.of(1, 2), new BigDecimal("4.5"), "Great", staff);

        assertEquals(1, graded);
        verify(notificationService, times(1)).notifyStudent(eq(301), eq("ACTIVITY_PARTICIPATION_GRADED"),
                anyString(), anyString(), eq("/activities?action=detail&id=15"));
        verify(participantDAO, never()).findByActivityId(anyInt());
    }

    @Test
    void saveAttendance_groupsNotificationsByTransition() {
        ExtracurricularActivityResponseDTO activity = new ExtracurricularActivityResponseDTO();
        activity.setId(20);
        activity.setName("Seminar");
        activity.setActivityStatus(ActivityStatus.ONGOING);
        activity.setAttendanceSessionCount(3);

        CurrentUser staff = new CurrentUser(88, "staff02", "STAFF", "STAFF", Instant.now(), "Staff Two");

        when(activityService.getActivityById(20)).thenReturn(activity);
        when(participantDAO.updateStatusesAndReturnChanges(eq(20), eq(2), any(), eq("88")))
                .thenReturn(List.of(
                        new IActivityParticipantDAO.AttendanceStatusChange(1, 1001, "NOT_YET", "ATTENDED"),
                        new IActivityParticipantDAO.AttendanceStatusChange(2, 1002, "NOT_YET", "ATTENDED"),
                        new IActivityParticipantDAO.AttendanceStatusChange(3, 1003, "ATTENDED", "ABSENT")
                ));

        int changed = service.saveAttendance(20, 2, Map.of(1, "ATTENDED", 2, "ATTENDED", 3, "ABSENT"), staff);

        assertEquals(3, changed);
        verify(notificationService).notifyAttendanceStatusChanged(
                20, "Seminar", 2, 3, "NOT_YET", "ATTENDED", List.of(1001, 1002));
        verify(notificationService).notifyAttendanceStatusChanged(
                20, "Seminar", 2, 3, "ATTENDED", "ABSENT", List.of(1003));
    }
}
