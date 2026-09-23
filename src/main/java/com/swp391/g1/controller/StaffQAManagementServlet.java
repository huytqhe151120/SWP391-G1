package com.swp391.g1.controller;

import com.swp391.g1.model.Question;
import com.swp391.g1.service.QAService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

@WebServlet("/staff/qa-management")
public class StaffQAManagementServlet extends HttpServlet {
    private final QAService qaService = new QAService();

    // GET /staff/qa-management: Ban tổ chức xem danh sách câu hỏi chờ duyệt (Task 3)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        List<Question> pendingQuestions = qaService.getPendingQuestionsForStaff();
        request.setAttribute("pendingQuestions", pendingQuestions);
        
        request.getRequestDispatcher("/WEB-INF/views/qa/staff-manage.jsp").forward(request, response);
    }
}