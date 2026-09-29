package com.swp391.g1.controller;

import com.swp391.g1.dao.MessageDAO;
import com.swp391.g1.dao.StudentDAO;
import com.swp391.g1.model.Message;
import com.swp391.g1.model.Student;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * OrganizerInboxServlet - Row 19 in SonCN's assignment
 * Staff/Organizer views all direct messages from students and replies.
 *
 * URL patterns:
 *   GET  /organizer-inbox                           -> dashboard: list all student conversations
 *   GET  /organizer-inbox?action=chat&studentId={}  -> open chat with specific student
 *   POST /organizer-inbox?action=reply              -> send reply to student
 */
@WebServlet("/organizer-inbox")
public class OrganizerInboxServlet extends HttpServlet {

    // MOCK: In production, get from HttpSession
    private static final int MOCK_STAFF_ID = 1;

    private final MessageDAO messageDAO = new MessageDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("chat".equals(action)) {
            // Open conversation with a specific student
            String studentIdParam = request.getParameter("studentId");
            if (studentIdParam == null) {
                response.sendRedirect(request.getContextPath() + "/organizer-inbox");
                return;
            }
            int studentId = Integer.parseInt(studentIdParam);
            Student student = studentDAO.getById(studentId);
            List<Message> messages = messageDAO.getConversation(studentId, MOCK_STAFF_ID);

            // Mark student's messages as read
            messageDAO.markAsRead(studentId, MOCK_STAFF_ID);

            request.setAttribute("student", student);
            request.setAttribute("messages", messages);
            request.setAttribute("mockStaffId", MOCK_STAFF_ID);

            forward(request, response, "/WEB-INF/views/inbox/organizer-chat.jsp");
        } else {
            // Dashboard: latest message per student conversation
            List<Message> conversations = messageDAO.getLatestMessagePerStudent(MOCK_STAFF_ID);
            int unreadCount = messageDAO.countUnread(MOCK_STAFF_ID);

            request.setAttribute("conversations", conversations);
            request.setAttribute("unreadCount", unreadCount);
            request.setAttribute("mockStaffId", MOCK_STAFF_ID);

            forward(request, response, "/WEB-INF/views/inbox/organizer-inbox.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if ("reply".equals(action)) {
            int studentId = Integer.parseInt(request.getParameter("studentId"));
            String content = request.getParameter("content");

            if (content != null && !content.trim().isEmpty()) {
                Message msg = new Message();
                msg.setStudentId(studentId);
                msg.setStaffId(MOCK_STAFF_ID);
                msg.setSenderType("STAFF");
                msg.setContent(content.trim());
                messageDAO.send(msg);
            }

            response.sendRedirect(request.getContextPath() + "/organizer-inbox?action=chat&studentId=" + studentId);
        } else {
            response.sendRedirect(request.getContextPath() + "/organizer-inbox");
        }
    }

    private void forward(HttpServletRequest req, HttpServletResponse res, String path)
            throws ServletException, IOException {
        RequestDispatcher rd = req.getRequestDispatcher(path);
        rd.forward(req, res);
    }
}
