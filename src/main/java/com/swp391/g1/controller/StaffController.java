package com.swp391.g1.controller;

import com.swp391.g1.model.Staff;
import com.swp391.g1.service.IStaffService;
import com.swp391.g1.service.impl.StaffServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/staff")
public class StaffController extends HttpServlet {

    private final IStaffService staffService =
            new StaffServiceImpl();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Lấy danh sách Staff từ database
            List<Staff> staffList = staffService.getAllStaffs();

            // Đưa danh sách sang JSP
            request.setAttribute("staffList", staffList);

            // Hiển thị Staff List
            request.getRequestDispatcher(
                    "/WEB-INF/views/staff/list-staff.jsp"
            ).forward(request, response);

        } catch (Exception e) {
            throw new ServletException(
                    "Không thể tải danh sách nhân viên.",
                    e
            );
        }
    }
}