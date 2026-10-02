package com.swp391.g1.controller;

import com.swp391.g1.dao.MessageDAO;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Message;
import com.swp391.g1.model.Staff;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.IStaffService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.StaffServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.AuthContext;

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

    private final MessageDAO messageDAO = new MessageDAO();
    private final IStaffService staffService = new StaffServiceImpl();
    private final IStudentService studentService = new StudentServiceImpl();

    /** Lấy student_id từ session. Trả về -1 nếu không hợp lệ. */
    private int resolveStudentId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return -1;
        }
        if (!"STUDENT".equals(user.getType())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ sinh viên mới được truy cập trang này.");
            return -1;
        }
        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy thông tin sinh viên.");
            return -1;
        }
        return student.getId();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int studentId = resolveStudentId(request, response);
        if (studentId == -1) return;

        // The inbox is a master-detail screen: the recipient list remains
        // visible even when a conversation is selected on the right.
        request.setAttribute("staffList", staffService.getAllStaffs());
        request.setAttribute("recentMessages", messageDAO.getStudentMessages(studentId));
        request.setAttribute("currentStudentId", studentId);

        String action = request.getParameter("action");

        if ("chat".equals(action)) {
            // Open conversation with a specific staff
            String staffIdParam = request.getParameter("staffId");
            if (staffIdParam == null) {
                response.sendRedirect(request.getContextPath() + "/student-inbox");
                return;
            }
            int staffId;
            try {
                staffId = Integer.parseInt(staffIdParam);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã nhân viên không hợp lệ.");
                return;
            }
            Staff staff = staffService.getStaffById(staffId);
            if (staff == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy nhân viên.");
                return;
            }
            List<Message> messages = messageDAO.getConversation(studentId, staffId);

            request.setAttribute("selectedStaff", staff);
            request.setAttribute("messages", messages);
        }

        forward(request, response, "/WEB-INF/views/inbox/student-inbox.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        int studentId = resolveStudentId(request, response);
        if (studentId == -1) return;

        String action = request.getParameter("action");

        if ("send".equals(action)) {
            int staffId;
            try {
                staffId = Integer.parseInt(request.getParameter("staffId"));
            } catch (NumberFormatException | NullPointerException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã nhân viên không hợp lệ.");
                return;
            }
            Staff staff = staffService.getStaffById(staffId);
            if (staff == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy nhân viên.");
                return;
            }
            String content = request.getParameter("content");

            if (content != null && !content.trim().isEmpty() && content.trim().length() <= 4000) {
                Message msg = new Message();
                msg.setStudentId(studentId);
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
