package com.swp391.g1.controller;

import com.swp391.g1.service.StaffService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/staff")
public class StaffServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            request.setAttribute("staffList", staffService.getAll());
            request.getRequestDispatcher("/WEB-INF/views/staff/list-staff.jsp")
                    .forward(request, response);
        } catch (SQLException exception) {
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to access staff data."
            );
        }
    }
}
