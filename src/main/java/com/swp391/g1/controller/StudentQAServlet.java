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

@WebServlet("/qa")
public class StudentQAServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(StudentQAServlet.class.getName());
    private final QAService qaService = new QAService();

    // GET /qa: Xem danh sách Q&A đã xuất bản (Task 1)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        loadPublishedQuestions(request);
        request.getRequestDispatcher("/WEB-INF/views/qa/student-qa.jsp").forward(request, response);
    }

    // POST /qa: Gửi câu hỏi mới (Task 2)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        boolean isAnonymous = "on".equals(request.getParameter("isAnonymous"));
        
        // Tạm thời lấy studentId từ Session (hoặc mock bằng 1 nếu chưa login)
        HttpSession session = request.getSession();
        Integer studentId = (Integer) session.getAttribute("userId");
        if (studentId == null) studentId = 1; 

        try {
            boolean success = qaService.submitQuestion(title, content, isAnonymous, studentId);
            if (success) {
                request.setAttribute("message", "Gửi câu hỏi thành công! Vui lòng chờ Ban tổ chức duyệt.");
            } else {
                request.setAttribute("error", "Lỗi: Tiêu đề và nội dung không được để trống!");
            }
        } catch (SQLException | IllegalStateException ex) {
            LOGGER.log(Level.SEVERE, "Could not submit student question.", ex);
            request.setAttribute("error", "Không thể gửi câu hỏi do chưa kết nối được cơ sở dữ liệu.");
        }
        loadPublishedQuestions(request);
        request.getRequestDispatcher("/WEB-INF/views/qa/student-qa.jsp").forward(request, response);
    }

    private void loadPublishedQuestions(HttpServletRequest request) {
        try {
            List<Question> questions = qaService.getPublishedQuestions();
            request.setAttribute("questions", questions);
        } catch (SQLException | IllegalStateException ex) {
            LOGGER.log(Level.SEVERE, "Could not load published questions.", ex);
            request.setAttribute("questions", Collections.emptyList());
            request.setAttribute("databaseError",
                    "Chưa thể tải danh sách câu hỏi. Hãy cấu hình src/main/resources/ConnectDB.properties "
                            + "theo ConnectDB.properties.example và kiểm tra SQL Server đang hoạt động.");
        }
    }
}