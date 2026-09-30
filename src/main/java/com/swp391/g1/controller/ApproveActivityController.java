package com.swp391.g1.controller;

import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/activity/approval")
public class ApproveActivityController extends HttpServlet {

    private final IExtracurricularActivityService activityService =
            new ExtracurricularActivityServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String action = request.getParameter("action");

            boolean success;
            String message;

            if ("submit".equals(action)) {
                success = activityService.submitForApproval(id);
                message = "Đã gửi hoạt động đi duyệt.";
            } else if ("approve".equals(action)) {
                success = activityService.approveActivity(id);
                message = "Đã duyệt hoạt động.";
            } else if ("reject".equals(action)) {
                success = activityService.rejectActivity(id);
                message = "Đã từ chối hoạt động.";
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action.");
                return;
            }

            if (!success) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Activity not found.");
                return;
            }
            request.getSession().setAttribute("successMessage", message);
            response.sendRedirect(request.getContextPath() + "/activities");
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid activity ID.");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activities");
        } catch (IllegalStateException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to update approval status.");
        }
    }
}