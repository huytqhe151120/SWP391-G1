package com.swp391.g1.controller;

import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.RecruitmentRound;
import com.swp391.g1.model.RecruitmentApplication;
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
        verify(recruitment).getApplications(round.getId(), null, null);
        verify(request).setAttribute("apps", List.of());
        verify(recruitment, never()).getSelectableActivities(null);
    }

    @Test
    void legacyApplicationLinksShowSelectorAndFilterTheSelectedRound() throws Exception {
        var activity = new ExtracurricularActivityResponseDTO();
        activity.setId(42);
        var round = new RecruitmentRound();
        round.setId(7);
        round.setActivityId(42);
        var first = new RecruitmentApplication();
        first.setId(10);
        first.setRoundId(7);
        first.setStatus("PENDING");
        first.setChoice1PositionId(4);
        var second = new RecruitmentApplication();
        second.setId(11);
        second.setRoundId(7);
        second.setStatus("REJECTED");
        second.setChoice1PositionId(4);
        when(request.getParameter("id")).thenReturn("7");
        when(request.getParameter("status")).thenReturn("PENDING");
        when(request.getParameter("positionId")).thenReturn("4");
        when(request.getParameter("appId")).thenReturn("10");
        when(recruitment.getRound(7)).thenReturn(round);
        when(activities.getActivityById(42)).thenReturn(activity);
        when(recruitment.getApplications(7, null, null)).thenReturn(List.of(first, second));
        when(recruitment.getApplication(10)).thenReturn(first);
        assertEquals("staff-list.jsp", servlet.managerView(request, response, "applications"));
        verify(request).setAttribute("activity", activity);
        verify(request).setAttribute("apps", List.of(first));
        verify(request).setAttribute("allCount", 2);
        verify(request).setAttribute("selected", first);
    }

    @Test
    void selectedApplicationFromAnotherRoundIsNotDisplayed() throws Exception {
        var activity = new ExtracurricularActivityResponseDTO();
        activity.setId(42);
        var round = new RecruitmentRound();
        round.setId(7);
        round.setActivityId(42);
        var other = new RecruitmentApplication();
        other.setRoundId(8);
        when(request.getParameter("activityId")).thenReturn("42");
        when(request.getParameter("appId")).thenReturn("10");
        when(activities.getActivityById(42)).thenReturn(activity);
        when(recruitment.getRoundByActivity(42)).thenReturn(round);
        when(recruitment.getApplication(10)).thenReturn(other);
        assertEquals("staff-list.jsp", servlet.managerView(request, response, null));
        verify(request).setAttribute("selected", null);
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
                        && message.toString().contains("không ở trạng thái Chưa diễn ra")));
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
                .contains("không ở trạng thái Chưa diễn ra"));
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
