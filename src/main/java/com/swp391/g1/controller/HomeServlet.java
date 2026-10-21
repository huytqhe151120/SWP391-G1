package com.swp391.g1.controller;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.util.AuthContext;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private final IExtracurricularActivityService activityService;

    public HomeServlet() {
        this.activityService = new ExtracurricularActivityServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        var currentUser = AuthContext.getCurrentUser(request);

        // UI hints — server-side enforcement is still in each servlet
        request.setAttribute("canManageActivities", AccessPolicy.canManageActivities(currentUser));
        request.setAttribute("canManageAccounts", AccessPolicy.canManageAccounts(currentUser));
        request.setAttribute("canManageQuestions", AccessPolicy.canManageQuestions(currentUser));
        request.setAttribute("canUseStudentQuestions", AccessPolicy.canUseStudentQuestions(currentUser));
        request.setAttribute("canUseSupportInbox", AccessPolicy.canUseSupportInbox(currentUser));
        request.setAttribute("canManageAppeals", AccessPolicy.canManageAppeals(currentUser));
        request.setAttribute("canUseStudentAppeals", AccessPolicy.canUseStudentAppeals(currentUser));

        String type = currentUser != null ? currentUser.getType() : "";
        request.setAttribute("isAdmin", "ADMIN".equals(type));
        request.setAttribute("isOrganizer", "ADMIN".equals(type));
        request.setAttribute("isStaff", "STAFF".equals(type));
        request.setAttribute("isStudent", "STUDENT".equals(type));
        request.setAttribute("isPartnerStaff", "PARTNER_STAFF".equals(type));
        request.setAttribute("currentUser", currentUser);

        String roleLabel = switch (type) {
            case "ADMIN" -> "Quản trị viên";
            case "STAFF" -> "Nhân viên nhà trường";
            case "PARTNER_STAFF" -> "Nhân viên đối tác";
            case "STUDENT" -> "Sinh viên";
            default -> "Khách / Thành viên";
        };
        request.setAttribute("roleLabel", roleLabel);

        try {
            var approved = activityService.getAllActivities().stream()
                    .filter(a -> a.getApprovalStatus() == com.swp391.g1.model.Enum.ApprovalStatus.APPROVED)
                    .toList();
            var now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            var upcoming = approved.stream()
                    .filter(a -> a.getActivityStatus() == com.swp391.g1.model.Enum.ActivityStatus.UPCOMING)
                    .filter(a -> a.getStartTime() != null && a.getStartTime().isAfter(now))
                    .sorted(java.util.Comparator.comparing(ExtracurricularActivityResponseDTO::getStartTime))
                    .toList();
            var ongoing = approved.stream()
                    .filter(a -> a.getActivityStatus() == com.swp391.g1.model.Enum.ActivityStatus.ONGOING)
                    .sorted(java.util.Comparator.comparing(ExtracurricularActivityResponseDTO::getStartTime,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .toList();
            request.setAttribute("activityCount", approved.size());
            request.setAttribute("upcomingCount", upcoming.size());
            request.setAttribute("departmentCount", approved.stream()
                    .map(ExtracurricularActivityResponseDTO::getResponsibleDepartmentId).distinct().count());
            request.setAttribute("upcomingActivities", upcoming.stream().limit(6).toList());
            request.setAttribute("ongoingActivities", ongoing.stream().limit(6).toList());
            request.setAttribute("featuredActivity", !upcoming.isEmpty() ? upcoming.get(0)
                    : ongoing.isEmpty() ? null : ongoing.get(0));
        } catch (Exception e) {
            log("Unable to load homepage activities", e);
            request.setAttribute("homeDataError", "Không thể tải dữ liệu hoạt động. Vui lòng thử lại sau.");
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/home/home.jsp");
        dispatcher.forward(request, response);
    }
}
