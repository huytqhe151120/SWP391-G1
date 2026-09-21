package com.swp391.g1.controller;

import com.swp391.g1.model.Partner;
import com.swp391.g1.service.PartnerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/partners")
public class PartnerServlet extends HttpServlet {

    private final PartnerService partnerService = new PartnerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = valueOrDefault(request.getParameter("action"), "list");

        try {
            switch (action) {
                case "detail" -> showDetail(request, response);
                case "create" -> showForm(request, response, null);
                case "edit" -> showForm(request, response,
                        partnerService.getById(parseId(request.getParameter("id"))));
                default -> showList(request, response);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to access partner data.");
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
                    "Unable to save partner data.");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("partners", partnerService.getAll());
        request.getRequestDispatcher("/WEB-INF/views/partner/list.jsp").forward(request, response);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Partner partner = partnerService.getById(parseId(request.getParameter("id")));
        if (partner == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Partner not found.");
            return;
        }
        request.setAttribute("partner", partner);
        request.getRequestDispatcher("/WEB-INF/views/partner/detail.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Partner partner)
            throws ServletException, IOException {
        request.setAttribute("partner", partner);
        request.getRequestDispatcher("/WEB-INF/views/partner/form.jsp").forward(request, response);
    }

    private void create(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        Partner partner = readPartner(request);
        if (!partnerService.create(partner)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid partner data.");
            return;
        }
        redirectToList(request, response);
    }

    private void update(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        Partner partner = readPartner(request);
        partner.setId(parseId(request.getParameter("id")));
        if (!partnerService.update(partner)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid partner data.");
            return;
        }
        redirectToList(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        if (!partnerService.delete(parseId(request.getParameter("id")))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Partner not found.");
            return;
        }
        redirectToList(request, response);
    }

    private Partner readPartner(HttpServletRequest request) {
        return new Partner(
                request.getParameter("name"),
                request.getParameter("email"),
                request.getParameter("phone"),
                request.getParameter("address"),
                request.getParameter("status"),
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
        response.sendRedirect(request.getContextPath() + "/partners");
    }
}