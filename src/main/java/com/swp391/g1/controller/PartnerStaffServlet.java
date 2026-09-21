package com.swp391.g1.controller;

import com.swp391.g1.model.PartnerStaff;
import com.swp391.g1.service.PartnerStaffService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/partner-staff")
public class PartnerStaffServlet extends HttpServlet {

    private final PartnerStaffService staffService = new PartnerStaffService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = valueOrDefault(request.getParameter("action"), "list");

        try {
            switch (action) {
                case "options" -> showOptions(request, response);
                case "detail" -> showDetail(request, response);
                case "create" -> showForm(request, response, null);
                case "edit" -> showForm(request, response,
                        staffService.getById(parseId(request.getParameter("id"))));
                default -> showList(request, response);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to access partner staff data.");
        }
    }

    private void showOptions(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int partnerId = parseId(request.getParameter("partnerId"));
        List<PartnerStaff> staffList = staffService.getByPartnerId(partnerId);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(toJson(staffList));
    }

    private String toJson(List<PartnerStaff> staffList) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < staffList.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            PartnerStaff staff = staffList.get(index);
            json.append("{\"id\":").append(staff.getId())
                    .append(",\"fullName\":\"").append(escapeJson(staff.getFullName()))
                    .append("\",\"position\":\"").append(escapeJson(staff.getPosition()))
                    .append("\"}");
        }
        return json.append(']').toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
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
                    "Unable to save partner staff data.");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String partnerId = request.getParameter("partnerId");
        if (partnerId == null || partnerId.isBlank()) {
            request.setAttribute("staffList", staffService.getAll());
        } else {
            request.setAttribute("staffList", staffService.getByPartnerId(parseId(partnerId)));
        }
        request.getRequestDispatcher("/WEB-INF/views/partner-staff/list.jsp").forward(request, response);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        PartnerStaff staff = staffService.getById(parseId(request.getParameter("id")));
        if (staff == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Partner staff not found.");
            return;
        }
        request.setAttribute("staff", staff);
        request.getRequestDispatcher("/WEB-INF/views/partner-staff/detail.jsp")
                .forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, PartnerStaff staff)
            throws ServletException, IOException {
        request.setAttribute("staff", staff);
        request.getRequestDispatcher("/WEB-INF/views/partner-staff/form.jsp")
                .forward(request, response);
    }

    private void create(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        PartnerStaff staff = readStaff(request);
        if (!staffService.create(staff)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid partner staff data.");
            return;
        }
        redirectToList(request, response);
    }

    private void update(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        PartnerStaff staff = readStaff(request);
        staff.setId(parseId(request.getParameter("id")));
        if (!staffService.update(staff)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid partner staff data.");
            return;
        }
        redirectToList(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        if (!staffService.delete(parseId(request.getParameter("id")))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Partner staff not found.");
            return;
        }
        redirectToList(request, response);
    }

    private PartnerStaff readStaff(HttpServletRequest request) {
        String active = request.getParameter("isActive");
        return new PartnerStaff(
                request.getParameter("fullName"),
                request.getParameter("email"),
                request.getParameter("phone"),
                active == null ? null : Boolean.valueOf(active),
                parseId(request.getParameter("partnerId")),
                null);
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

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void redirectToList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/partner-staff");
    }
}