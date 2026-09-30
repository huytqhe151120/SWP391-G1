package com.swp391.g1.controller;

import com.swp391.g1.model.Question;
import com.swp391.g1.service.QAService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/staff/qa-management")
public class StaffQAManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(StaffQAManagementServlet.class.getName());
    private final QAService qaService = new QAService();

    // GET /staff/qa-management: Ban tổ chức xem danh sách câu hỏi chờ duyệt (Task 3)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            List<Question> pendingQuestions = qaService.getPendingQuestionsForStaff();
            request.setAttribute("pendingQuestions", pendingQuestions);
        } catch (SQLException | IllegalStateException ex) {
            LOGGER.log(Level.SEVERE, "Could not load pending questions.", ex);
            request.setAttribute("pendingQuestions", Collections.emptyList());
            request.setAttribute("databaseError",
                    "Chưa thể tải danh sách. Hãy cấu hình src/main/resources/ConnectDB.properties "
                            + "và kiểm tra SQL Server đang hoạt động.");
        }
        request.getRequestDispatcher("/WEB-INF/views/qa/staff-manage.jsp").forward(request, response);
    }
}