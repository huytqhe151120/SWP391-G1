package com.swp391.g1.controller;

import com.swp391.g1.dao.MessageDAO;
import com.swp391.g1.dao.StaffDAO;
import com.swp391.g1.model.Message;
import com.swp391.g1.model.Staff;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * StudentInboxServlet - Row 18 in SonCN's assignment
 * Student sends private messages to a specific organizer/staff.
 *
 * URL patterns:
 *   GET  /student-inbox                        -> show inbox (list of conversations)
 *   GET  /student-inbox?action=chat&staffId={} -> open chat with a specific staff
 *   POST /student-inbox?action=send            -> send a new message
 */
@WebServlet("/student-inbox")
public class StudentInboxServlet extends HttpServlet {

    // MOCK: In production, get from HttpSession
    private static final int MOCK_STUDENT_ID = 1;

    private final MessageDAO messageDAO = new MessageDAO();
    private final StaffDAO staffDAO = new StaffDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("chat".equals(action)) {
            // Open conversation with a specific staff
            String staffIdParam = request.getParameter("staffId");
            if (staffIdParam == null) {
                response.sendRedirect(request.getContextPath() + "/student-inbox");
                return;
            }
            int staffId = Integer.parseInt(staffIdParam);
            Staff staff = staffDAO.getById(staffId);
            List<Message> messages = messageDAO.getConversation(MOCK_STUDENT_ID, staffId);

            request.setAttribute("staff", staff);
            request.setAttribute("messages", messages);
            request.setAttribute("mockStudentId", MOCK_STUDENT_ID);

            forward(request, response, "/WEB-INF/views/inbox/student-chat.jsp");
        } else {
            // List all staff members + recent messages from this student
            List<Staff> staffList = staffDAO.getAll();
            List<Message> recentMessages = messageDAO.getStudentMessages(MOCK_STUDENT_ID);

            request.setAttribute("staffList", staffList);
            request.setAttribute("recentMessages", recentMessages);
            request.setAttribute("mockStudentId", MOCK_STUDENT_ID);

            forward(request, response, "/WEB-INF/views/inbox/student-inbox.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if ("send".equals(action)) {
            int staffId = Integer.parseInt(request.getParameter("staffId"));
            String content = request.getParameter("content");

            if (content != null && !content.trim().isEmpty()) {
                Message msg = new Message();
                msg.setStudentId(MOCK_STUDENT_ID);
                msg.setStaffId(staffId);
                msg.setSenderType("STUDENT");
                msg.setContent(content.trim());
                messageDAO.send(msg);
            }

            response.sendRedirect(request.getContextPath() + "/student-inbox?action=chat&staffId=" + staffId);
        } else {
            response.sendRedirect(request.getContextPath() + "/student-inbox");
        }
    }

    private void forward(HttpServletRequest req, HttpServletResponse res, String path)
            throws ServletException, IOException {
        RequestDispatcher rd = req.getRequestDispatcher(path);
        rd.forward(req, res);
    }
}
