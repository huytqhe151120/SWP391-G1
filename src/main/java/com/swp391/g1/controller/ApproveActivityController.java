package com.swp391.g1.controller;

import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;



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

            if (action == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action.");
                return;
            }

            boolean success;
            String message;

            switch (action) {
                case "submit":
                    success = activityService.submitForApproval(id);
                    message = "Đã gửi hoạt động đi duyệt.";
                    break;
                case "approve":
                    success = activityService.approveActivity(id);
                    message = "Đã duyệt hoạt động.";
                    break;
                case "reject":
                    success = activityService.rejectActivity(id);
                    message = "Đã từ chối hoạt động.";
                    break;
                case "revokeApproval":
                    success = activityService.revokeApproval(id);
                    message = "Đã hủy duyệt, hoạt động quay về trạng thái chờ duyệt.";
                    break;
                case "revokeRejection":
                    success = activityService.revokeRejection(id);
                    message = "Đã hủy từ chối, hoạt động quay về trạng thái chờ duyệt.";
                    break;
                default:
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
//            LOGGER.log(Level.SEVERE, "Approval action failed.", e);
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//                    "Unable to update approval status.");
        }
    }
}
