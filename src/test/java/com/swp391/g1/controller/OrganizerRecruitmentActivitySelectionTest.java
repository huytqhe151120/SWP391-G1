package com.swp391.g1.controller;

import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.RecruitmentRound;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.IRecruitmentService;
import com.swp391.g1.service.IRegistrationFormService;
import com.swp391.g1.service.IStudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizerRecruitmentActivitySelectionTest {
    private IRecruitmentService recruitment;
    private IExtracurricularActivityService activities;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private OrganizerRecruitmentServlet servlet;

    @BeforeEach
    void setUp() {
        recruitment = mock(IRecruitmentService.class);
        activities = mock(IExtracurricularActivityService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        servlet = new OrganizerRecruitmentServlet(recruitment, activities,
                mock(IStudentService.class), mock(IRegistrationFormService.class));
        when(request.getContextPath()).thenReturn("/app");
    }

    @Test
    void noActivityShowsSelectorWithoutLoadingRound() throws Exception {
        assertEquals("staff-list.jsp", servlet.managerView(request, response, null));
        verify(request).setAttribute("activity", null);
        verify(request).setAttribute("round", null);
        verify(recruitment, never()).getRoundByActivity(anyInt());
    }

    @Test
    void selectedActivityShowsRoundAndCannotCreateAnother() throws Exception {
        var activity = new ExtracurricularActivityResponseDTO();
        activity.setId(42);
        var round = new RecruitmentRound();
        round.setActivityId(42);
        when(request.getParameter("activityId")).thenReturn("42");
        when(activities.getActivityById(42)).thenReturn(activity);
        when(recruitment.getRoundByActivity(42)).thenReturn(round);
        assertEquals("staff-list.jsp", servlet.managerView(request, response, null));
        verify(request).setAttribute("activity", activity);
        verify(request).setAttribute("round", round);
        verify(recruitment, never()).getSelectableActivities(null);
    }

    @Test
    void missingRoundShowsActivityRegardlessOfEligibility() throws Exception {
        var activity = new ExtracurricularActivityResponseDTO();
        activity.setId(42);
        when(request.getParameter("activityId")).thenReturn("42");
        when(activities.getActivityById(42)).thenReturn(activity);
        assertEquals("staff-list.jsp", servlet.managerView(request, response, null));
        verify(request).setAttribute("round", null);
        verify(recruitment, never()).getSelectableActivities(null);
    }

    @Test
    void nonexistentActivityReturnsNotFound() throws Exception {
        when(request.getParameter("activityId")).thenReturn("42");
        assertNull(servlet.managerView(request, response, null));
        verify(response).sendError(eq(404), anyString());
    }

    @Test
    void createUsesSelectedActivityAndExistingRoundRedirects() throws Exception {
        when(request.getParameter("activityId")).thenReturn("42");
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        var round = new RecruitmentRound();
        round.setActivityId(42);
        when(recruitment.getRoundByActivity(42)).thenReturn(round);
        assertNull(servlet.managerView(request, response, "create"));
        verify(response).sendRedirect("/app/organizer-recruitment?activityId=42");
    }

    @Test
    void eligibleCreationPreselectsActivity() throws Exception {
        when(request.getParameter("activityId")).thenReturn("42");
        var eligible = new RecruitmentRound();
        eligible.setActivityId(42);
        when(activities.getActivityById(42)).thenReturn(new ExtracurricularActivityResponseDTO());
        when(recruitment.getSelectableActivities(null)).thenReturn(List.of(eligible));
        when(recruitment.getSelectableActivities(42)).thenReturn(List.of(eligible));
        assertEquals("staff-form.jsp", servlet.managerView(request, response, "create"));
        var round = org.mockito.ArgumentCaptor.forClass(RecruitmentRound.class);
        verify(request).setAttribute(eq("round"), round.capture());
        assertEquals(42, round.getValue().getActivityId());
    }

    @Test
    void ineligibleActivityCannotStartCreation() throws Exception {
        when(request.getParameter("activityId")).thenReturn("42");
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        ExtracurricularActivityResponseDTO activity = new ExtracurricularActivityResponseDTO();
        activity.setApprovalStatus(ApprovalStatus.PENDING);
        activity.setActivityStatus(ActivityStatus.COMPLETED);
        activity.setStartTime(LocalDateTime.now().minusDays(1));
        when(activities.getActivityById(42)).thenReturn(activity);
        when(recruitment.getSelectableActivities(null)).thenReturn(List.of());
        assertNull(servlet.managerView(request, response, "create"));
        verify(response).sendRedirect("/app/organizer-recruitment?activityId=42");
        verify(session).setAttribute(eq("errorMessage"),
                argThat(message -> message.toString().contains("chưa được duyệt")
                        && message.toString().contains("không ở trạng thái Chưa diễn ra")
                        && message.toString().contains("đã đến hoặc đã qua")));
    }

    @Test
    void eligibilityMessageDistinguishesMissingStartAndFallback() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 14, 8, 0);
        var activity = new ExtracurricularActivityResponseDTO();
        activity.setApprovalStatus(ApprovalStatus.APPROVED);
        activity.setActivityStatus(ActivityStatus.UPCOMING);
        assertTrue(OrganizerRecruitmentServlet.creationEligibilityMessage(activity, now)
                .contains("chưa có thời gian bắt đầu"));
        activity.setStartTime(now);
        assertTrue(OrganizerRecruitmentServlet.creationEligibilityMessage(activity, now)
                .contains("đã đến hoặc đã qua"));
        activity.setStartTime(now.plusDays(1));
        assertTrue(OrganizerRecruitmentServlet.creationEligibilityMessage(activity, now)
                .contains("không còn đủ điều kiện"));
    }

    @Test
    void fallbackSelectorTakesPrecedenceWithoutJavascript() throws Exception {
        when(request.getParameter("activityId")).thenReturn("10");
        when(request.getParameter("activityIdFallback")).thenReturn("42");
        when(activities.getActivityById(42)).thenReturn(new ExtracurricularActivityResponseDTO());
        assertEquals("staff-list.jsp", servlet.managerView(request, response, null));
        verify(activities).getActivityById(42);
    }
}
