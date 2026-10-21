package com.swp391.g1.controller;

import com.swp391.g1.dao.IAppealDAO;
import com.swp391.g1.dao.impl.AppealDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Appeal;
import com.swp391.g1.model.Staff;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
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
import java.math.BigDecimal;
import java.util.List;

/**
 * AppealServlet — handles the Appeal Process workflow.
 *
 * URL patterns:
 *   GET  /appeal                       → Student: appeal form + own appeal list
 *   GET  /appeal?action=manage         → Staff/Admin: appeal management dashboard
 *   GET  /appeal?action=view&id={id}   → Staff/Admin: view single appeal detail
 *   POST /appeal?action=submit         → Student: submit new appeal
 *   POST /appeal?action=approve        → Staff: approve appeal + set new point
 *   POST /appeal?action=reject         → Staff: reject appeal with note
 */
@WebServlet("/appeal")
public class AppealServlet extends HttpServlet {

    private final IAppealDAO appealDAO                     = new AppealDAOImpl();
    private final IStaffService staffService               = new StaffServiceImpl();
    private final IStudentService studentService           = new StudentServiceImpl();

    // ── GET ──────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        CurrentUser user = AuthContext.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");

        if (AccessPolicy.canManageAppeals(user) && ("manage".equals(action) || "view".equals(action))) {
            handleStaffGet(request, response, user, action);
        } else if (AccessPolicy.canUseStudentAppeals(user)) {
            handleStudentGet(request, response, user);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền truy cập trang này.");
        }
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        CurrentUser user = AuthContext.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");

        switch (action == null ? "" : action) {
            case "submit"  -> handleStudentSubmit(request, response, user);
            case "approve" -> handleStaffApprove(request, response, user);
            case "reject"  -> handleStaffReject(request, response, user);
            default        -> response.sendRedirect(request.getContextPath() + "/appeal");
        }
    }

    // ── Student GET ───────────────────────────────────────────────────────────

    private void handleStudentGet(HttpServletRequest request, HttpServletResponse response,
                                  CurrentUser user)
            throws ServletException, IOException {

        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Không tìm thấy hồ sơ sinh viên.");
            return;
        }

        // Only completed activities for which this student has an official point.
        List<Appeal> eligibleActivities = appealDAO.getEligibleActivities(student.getId());

        // Student's existing appeals
        List<Appeal> myAppeals = appealDAO.getByStudentId(student.getId());

        request.setAttribute("student", student);
        request.setAttribute("eligibleActivities", eligibleActivities);
        request.setAttribute("myAppeals", myAppeals);

        if ("success".equals(request.getParameter("msg"))) {
            request.setAttribute("successMsg", "Yêu cầu phúc khảo đã được gửi thành công!");
        } else if ("duplicate".equals(request.getParameter("msg"))) {
            request.setAttribute("errorMsg", "Bạn đã có yêu cầu phúc khảo đang chờ cho hoạt động này.");
        } else if ("invalid_activity".equals(request.getParameter("msg"))) {
            request.setAttribute("errorMsg", "Bạn chưa có điểm cuối cùng hợp lệ cho hoạt động này.");
        } else if ("invalid_input".equals(request.getParameter("msg"))) {
            request.setAttribute("errorMsg", "Vui lòng chọn hoạt động và nhập lý do hợp lệ.");
        } else if ("error".equals(request.getParameter("msg"))) {
            request.setAttribute("errorMsg", "Không thể xử lý yêu cầu. Dữ liệu có thể đã được thay đổi; vui lòng tải lại trang.");
        }

        forward(request, response, "/WEB-INF/views/qa/student-appeal.jsp");
    }

    // ── Staff GET ─────────────────────────────────────────────────────────────

    private void handleStaffGet(HttpServletRequest request, HttpServletResponse response,
                                CurrentUser user, String action)
            throws ServletException, IOException {

        if (!AccessPolicy.canManageAppeals(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ ADMIN/STAFF mới được quản lý phúc khảo.");
            return;
        }

        if ("view".equals(action)) {
            // Single appeal detail
            Integer id = parsePositiveInt(request.getParameter("id"));
            if (id == null) {
                response.sendRedirect(request.getContextPath() + "/appeal?action=manage");
                return;
            }
            Appeal appeal = appealDAO.getById(id);
            if (appeal == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy yêu cầu phúc khảo.");
                return;
            }
            request.setAttribute("appeal", appeal);

            if ("approved".equals(request.getParameter("msg"))) {
                request.setAttribute("successMsg", "Đã chấp thuận yêu cầu phúc khảo.");
            } else if ("rejected".equals(request.getParameter("msg"))) {
                request.setAttribute("successMsg", "Đã từ chối yêu cầu phúc khảo.");
            } else if ("error".equals(request.getParameter("msg"))) {
                request.setAttribute("errorMsg", "Không thể xử lý yêu cầu phúc khảo hoặc yêu cầu đã được xử lý trước đó.");
            }

            forward(request, response, "/WEB-INF/views/qa/appeal-detail.jsp");

        } else {
            // Dashboard — list all with optional filter
            String filter = request.getParameter("filter");
            List<Appeal> appeals;
            if ("pending".equals(filter)) {
                appeals = appealDAO.getPending();
            } else {
                appeals = appealDAO.getAll();
            }
            request.setAttribute("appeals", appeals);
            request.setAttribute("filter", filter == null ? "all" : filter);

            if ("approved".equals(request.getParameter("msg"))) {
                request.setAttribute("successMsg", "Đã chấp thuận yêu cầu phúc khảo.");
            } else if ("rejected".equals(request.getParameter("msg"))) {
                request.setAttribute("successMsg", "Đã từ chối yêu cầu phúc khảo.");
            } else if ("error".equals(request.getParameter("msg"))) {
                request.setAttribute("errorMsg", "Không thể xử lý yêu cầu phúc khảo hoặc yêu cầu đã được xử lý trước đó.");
            }

            forward(request, response, "/WEB-INF/views/qa/appeal-management.jsp");
        }
    }

    // ── Student Submit ────────────────────────────────────────────────────────

    private void handleStudentSubmit(HttpServletRequest request, HttpServletResponse response,
                                     CurrentUser user)
            throws IOException {

        if (!AccessPolicy.canUseStudentAppeals(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ sinh viên mới có thể gửi yêu cầu phúc khảo.");
            return;
        }

        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy hồ sơ sinh viên.");
            return;
        }

        Integer activityId = parsePositiveInt(request.getParameter("activityId"));
        String reason      = normalize(request.getParameter("reason"));

        if (activityId == null || reason.isEmpty() || reason.length() > 4000) {
            response.sendRedirect(request.getContextPath() + "/appeal?msg=invalid_input");
            return;
        }

        boolean eligible = appealDAO.getEligibleActivities(student.getId()).stream()
                .anyMatch(item -> item.getActivityId() == activityId);
        if (!eligible) {
            response.sendRedirect(request.getContextPath() + "/appeal?msg=invalid_activity");
            return;
        }

        // Prevent duplicate pending appeal
        if (appealDAO.hasPendingAppeal(student.getId(), activityId)) {
            response.sendRedirect(request.getContextPath() + "/appeal?msg=duplicate");
            return;
        }

        Appeal appeal = new Appeal();
        appeal.setStudentId(student.getId());
        appeal.setActivityId(activityId);
        appeal.setReason(reason);
        boolean saved = appealDAO.insert(appeal);
        response.sendRedirect(request.getContextPath() + "/appeal?msg=" + (saved ? "success" : "error"));
    }

    // ── Staff Approve ─────────────────────────────────────────────────────────

    private void handleStaffApprove(HttpServletRequest request, HttpServletResponse response,
                                    CurrentUser user)
            throws IOException {

        if (!AccessPolicy.canManageAppeals(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int staffId = resolveStaffId(request, response, user);
        if (staffId == -1) return;

        Integer appealId = parsePositiveInt(request.getParameter("appealId"));
        String  note     = normalize(request.getParameter("reviewNote"));
        BigDecimal newPoint = parseBigDecimal(request.getParameter("newPoint"));

        if (appealId == null || newPoint == null || !isValidPoint(newPoint)
                || note.length() > 2000) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "ID, điểm mới hoặc ghi chú phúc khảo không hợp lệ.");
            return;
        }

        boolean ok = appealDAO.approve(appealId, staffId, newPoint, note.isEmpty() ? null : note);
        response.sendRedirect(request.getContextPath() + "/appeal?action=manage&msg=" + (ok ? "approved" : "error"));
    }

    // ── Staff Reject ──────────────────────────────────────────────────────────

    private void handleStaffReject(HttpServletRequest request, HttpServletResponse response,
                                   CurrentUser user)
            throws IOException {

        if (!AccessPolicy.canManageAppeals(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int staffId = resolveStaffId(request, response, user);
        if (staffId == -1) return;

        Integer appealId = parsePositiveInt(request.getParameter("appealId"));
        String  note     = normalize(request.getParameter("reviewNote"));

        if (appealId == null || note.isEmpty() || note.length() > 2000) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "ID hoặc lý do từ chối không hợp lệ.");
            return;
        }

        boolean ok = appealDAO.reject(appealId, staffId, note.isEmpty() ? null : note);
        response.sendRedirect(request.getContextPath() + "/appeal?action=manage&msg=" + (ok ? "rejected" : "error"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves the staff ID from the current authenticated user.
     * ADMIN accounts that have no staff row fall back to the first staff record
     * (consistent with AnswerQuestionServlet).
     */
    private int resolveStaffId(HttpServletRequest request, HttpServletResponse response,
                               CurrentUser user) throws IOException {
        Staff staff = staffService.getStaffByAccountId(user.getId());
        if (staff == null && "ADMIN".equals(user.getType())) {
            staff = staffService.getFirstStaff();
        }
        if (staff == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Tài khoản STAFF chưa được liên kết với hồ sơ nhân viên.");
            return -1;
        }
        return staff.getId();
    }

    private void forward(HttpServletRequest req, HttpServletResponse res, String path)
            throws ServletException, IOException {
        RequestDispatcher rd = req.getRequestDispatcher(path);
        rd.forward(req, res);
    }

    private static Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static boolean isValidPoint(BigDecimal point) {
        return point.compareTo(BigDecimal.ZERO) >= 0
                && point.compareTo(BigDecimal.TEN) <= 0
                && point.scale() <= 2;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
