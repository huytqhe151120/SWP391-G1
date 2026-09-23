package com.swp391.g1.controller;

import com.swp391.g1.model.Question;
import com.swp391.g1.service.QAService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

@WebServlet("/qa")
public class StudentQAServlet extends HttpServlet {
    private final QAService qaService = new QAService();

    // GET /qa: Xem danh sách Q&A đã xuất bản (Task 1)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        List<Question> questions = qaService.getPublishedQuestions();
        request.setAttribute("questions", questions);
        
        // Forward sang trang JSP theo quy định README
        request.getRequestDispatcher("/WEB-INF/views/qa/student-qa.jsp").forward(request, response);
    }

    // POST /qa: Gửi câu hỏi mới (Task 2)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        boolean isAnonymous = "on".equals(request.getParameter("isAnonymous"));
        
        // Tạm thời lấy studentId từ Session (hoặc mock bằng 1 nếu chưa login)
        HttpSession session = request.getSession();
        Integer studentId = (Integer) session.getAttribute("userId");
        if (studentId == null) studentId = 1; 

        boolean success = qaService.submitQuestion(title, content, isAnonymous, studentId);
        
        if (success) {
            request.setAttribute("message", "Gửi câu hỏi thành công! Vui lòng chờ Ban tổ chức duyệt.");
        } else {
            request.setAttribute("error", "Lỗi: Tiêu đề và nội dung không được để trống!");
        }
        
        doGet(request, response);
    }
}