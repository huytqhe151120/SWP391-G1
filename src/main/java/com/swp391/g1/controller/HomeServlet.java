package com.swp391.g1.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.util.AuthContext;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private final IExtracurricularActivityService activityService;

    public HomeServlet() {
        this.activityService = new ExtracurricularActivityServiceImpl();
    }

    HomeServlet(IExtracurricularActivityService activityService) {
        this.activityService = activityService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        var currentUser = AuthContext.getCurrentUser(request);

        request.setAttribute("roleLabel", currentUser == null ? "" : currentUser.getRoleLabel());

        // UI hints — server-side enforcement is still in each servlet
        request.setAttribute("canManageAccounts", AccessPolicy.canManageAccounts(currentUser));
        request.setAttribute("canManageQuestions", AccessPolicy.canManageQuestions(currentUser));
        request.setAttribute("canUseStudentQuestions", AccessPolicy.canUseStudentQuestions(currentUser));
        request.setAttribute("canUseSupportInbox", AccessPolicy.canUseSupportInbox(currentUser));

        String type = currentUser != null ? currentUser.getType() : "";
        // ADMIN = Organizer: có QA management + Organizer inbox
        request.setAttribute("isOrganizer", "ADMIN".equals(type));
        // STAFF: có một số tính năng riêng
        request.setAttribute("isStaff", "STAFF".equals(type));
        // STUDENT: có Student inbox
        request.setAttribute("isStudent", "STUDENT".equals(type));

        try {
            List<ExtracurricularActivityResponseDTO> approvedActivities =
                    activityService.getAllActivities().stream()
                            .filter(activity -> activity.getApprovalStatus() == ApprovalStatus.APPROVED)
                            .collect(Collectors.toList());

            Comparator<ExtracurricularActivityResponseDTO> byStartTime =
                    Comparator.comparing(ExtracurricularActivityResponseDTO::getStartTime,
                            Comparator.nullsLast(Comparator.naturalOrder()));
            List<ExtracurricularActivityResponseDTO> upcomingActivities = approvedActivities.stream()
                    .filter(activity -> activity.getActivityStatus() == ActivityStatus.UPCOMING)
                    .sorted(byStartTime)
                    .collect(Collectors.toList());
            List<ExtracurricularActivityResponseDTO> ongoingActivities = approvedActivities.stream()
                    .filter(activity -> activity.getActivityStatus() == ActivityStatus.ONGOING)
                    .sorted(byStartTime)
                    .collect(Collectors.toList());

            Set<Integer> responsibleDepartments = approvedActivities.stream()
                    .map(ExtracurricularActivityResponseDTO::getResponsibleDepartmentId)
                    .collect(Collectors.toSet());

            request.setAttribute("activityCount", approvedActivities.size());
            request.setAttribute("upcomingCount", upcomingActivities.size());
            request.setAttribute("departmentCount", responsibleDepartments.size());
            request.setAttribute("featuredActivity", !ongoingActivities.isEmpty()
                    ? ongoingActivities.get(0)
                    : upcomingActivities.isEmpty() ? null : upcomingActivities.get(0));
            request.setAttribute("upcomingActivities", upcomingActivities);
            request.setAttribute("ongoingActivities", ongoingActivities);
        } catch (IllegalStateException e) {
            getServletContext().log("Unable to load home page data.", e);
            request.setAttribute("homeDataError", "Không thể tải dữ liệu hoạt động lúc này.");
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/home/home.jsp");
        dispatcher.forward(request, response);
    }
}
