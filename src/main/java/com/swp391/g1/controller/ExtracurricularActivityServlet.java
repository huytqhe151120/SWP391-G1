package com.swp391.g1.controller;

import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.service.PartnerService;
import com.swp391.g1.service.PartnerStaffService;
import com.swp391.g1.service.ExtracurricularActivityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/activities")
public class ExtracurricularActivityServlet extends HttpServlet {

    private final ExtracurricularActivityService activityService = new ExtracurricularActivityService();
    private final PartnerService partnerService = new PartnerService();
    private final PartnerStaffService partnerStaffService = new PartnerStaffService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = valueOrDefault(request.getParameter("action"), "list");

        try {
            switch (action) {
                case "detail" -> showDetail(request, response);
                case "create" -> showForm(request, response, null);
                case "edit" -> showForm(request, response,
                        activityService.getById(parseId(request.getParameter("id"))));
                default -> showList(request, response);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to access activity data.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = valueOrDefault(request.getParameter("action"), "create");

        try {
            switch (action) {
                case "update" -> update(request, response);
                case "delete" -> delete(request, response);
                default -> create(request, response);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to save activity data.");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("activities", activityService.getAll());
        request.getRequestDispatcher("/WEB-INF/views/activity/list.jsp").forward(request, response);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        ExtracurricularActivity activity = activityService.getById(parseId(request.getParameter("id")));
        if (activity == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Activity not found.");
            return;
        }
        request.setAttribute("activity", activity);
        request.getRequestDispatcher("/WEB-INF/views/activity/detail.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          ExtracurricularActivity activity)
            throws ServletException, IOException, SQLException {
        request.setAttribute("activity", activity);
        request.setAttribute("partners", partnerService.getAll());
        int partnerId = activity == null ? 0 : activity.getPartnerId();
        request.setAttribute("partnerStaffOptions",
                partnerId > 0 ? partnerStaffService.getByPartnerId(partnerId) : java.util.List.of());
        request.getRequestDispatcher("/WEB-INF/views/activity/form.jsp").forward(request, response);
    }

    private void create(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        ExtracurricularActivity activity = readActivity(request);
        if (!activityService.create(activity)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid activity data.");
            return;
        }
        redirectToList(request, response);
    }

    private void update(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        ExtracurricularActivity activity = readActivity(request);
        activity.setId(parseId(request.getParameter("id")));
        if (!activityService.update(activity)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid activity data.");
            return;
        }
        redirectToList(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        if (!activityService.delete(parseId(request.getParameter("id")))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Activity not found.");
            return;
        }
        redirectToList(request, response);
    }

    private ExtracurricularActivity readActivity(HttpServletRequest request) {
        ExtracurricularActivity activity = new ExtracurricularActivity();
        activity.setSemesterId(parseId(request.getParameter("semesterId")));
        activity.setActivityType(request.getParameter("activityType"));
        activity.setCode(request.getParameter("code"));
        activity.setName(request.getParameter("name"));
        activity.setResponsibleDepartmentId(parseId(request.getParameter("responsibleDepartmentId")));
        activity.setResponsibleStaffId(parseId(request.getParameter("responsibleStaffId")));
        activity.setPartnerId(parseId(request.getParameter("partnerId")));
        activity.setPartnerStaffId(parseId(request.getParameter("partnerStaffId")));
        activity.setBonusPoint(parseDouble(request.getParameter("bonusPoint")));
        activity.setPenaltyPoint(parseDouble(request.getParameter("penaltyPoint")));
        activity.setAddress(request.getParameter("address"));
        activity.setDescription(request.getParameter("description"));
        activity.setActivityStatus(request.getParameter("activityStatus"));
        activity.setApprovalStatus(request.getParameter("approvalStatus"));
        return activity;
    }

    private int parseId(String value) {
        try {
            int id = Integer.parseInt(value);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException | NullPointerException ignored) {
            // Convert invalid request parameters to a consistent HTTP 400 response.
        }
        throw new IllegalArgumentException("A positive id is required.");
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException exception) {
            throw new IllegalArgumentException("A valid number is required.");
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void redirectToList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/activities");
    }
}