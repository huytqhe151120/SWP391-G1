package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.RegistrationForm;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IRegistrationFormService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.service.impl.RegistrationFormServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.AuthContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@WebServlet("/registration-forms")
public class RegistrationFormServlet extends HttpServlet {
    private final IRegistrationFormService service = new RegistrationFormServiceImpl();
    private final IStudentService studentService = new StudentServiceImpl();
    private final ExtracurricularActivityServiceImpl activityService = new ExtracurricularActivityServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(req);
        String action = param(req, "action", "list");
        try {
            if ("student".equals(action)) {
                req.setAttribute("forms", service.getOpenForms());
                req.getRequestDispatcher("/WEB-INF/views/registration/student-list.jsp").forward(req, resp);
                return;
            }
            if ("register".equals(action)) {
                if (!"STUDENT".equals(user.getType())) {
                    resp.sendError(403);
                    return;
                }
                RegistrationForm f = service.getById(id(req));
                if (f == null) {
                    resp.sendError(404);
                    return;
                }
                req.setAttribute("form", f);
                req.getRequestDispatcher("/WEB-INF/views/registration/student-form.jsp").forward(req, resp);
                return;
            }
            if (!AccessPolicy.canManageActivities(user)) {
                resp.sendError(403, "Chỉ STAFF/ADMIN được quản lý form đăng ký.");
                return;
            }
            if ("view".equals(action)) {
                RegistrationForm f = service.getById(id(req));
                if (f == null) {
                    resp.sendError(404);
                    return;
                }
                req.setAttribute("form", f);
                req.getRequestDispatcher("/WEB-INF/views/registration/detail.jsp").forward(req, resp);
                return;
            }
            if ("create".equals(action) || "edit".equals(action)) {
                RegistrationForm f = "edit".equals(action) ? service.getById(id(req)) : new RegistrationForm();
                if (f == null) {
                    resp.sendError(404);
                    return;
                }
                req.setAttribute("form", f);
                req.setAttribute("activities", activityService.getAllActivities());
                req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("forms", service.getAll());
            req.getRequestDispatcher("/WEB-INF/views/registration/list.jsp").forward(req, resp);
        } catch (RuntimeException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        CurrentUser user = AuthContext.getCurrentUser(req);
        String action = param(req, "action", "");
        String error = null;
        try {
            if ("register".equals(action)) {
                if (!"STUDENT".equals(user.getType())) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                if (student == null) {
                    resp.sendError(403, "Không tìm thấy hồ sơ sinh viên.");
                    return;
                }
                error = service.register(id(req), student.getId(), req.getParameter("note"));
                if (error == null) {
                    resp.sendRedirect(req.getContextPath() + "/registration-forms?action=student&msg=registered");
                    return;
                }
                req.setAttribute("error", error);
                RegistrationForm f = service.getById(id(req));
                req.setAttribute("form", f);
                req.getRequestDispatcher("/WEB-INF/views/registration/student-form.jsp").forward(req, resp);
                return;
            }
            if (!AccessPolicy.canManageActivities(user)) {
                resp.sendError(403);
                return;
            }
            if ("status".equals(action)) {
                error = service.updateStatus(id(req), req.getParameter("status"));
                resp.sendRedirect(req.getContextPath() + "/registration-forms?msg="
                        + (error == null ? "statusUpdated" : "statusError"));
                return;
            }
            if ("delete".equals(action)) {
                error = service.delete(id(req));
                resp.sendRedirect(req.getContextPath() + "/registration-forms?msg=" + (error == null ? "deleted" : "deleteError"));
                return;
            }
            RegistrationForm f = new RegistrationForm();
            f.setId(id(req));
            f.setActivityId(parseInt(req.getParameter("activityId")));
            f.setTitle(req.getParameter("title"));
            f.setDescription(req.getParameter("description"));
            f.setStatus(req.getParameter("status"));
            f.setOpenAt(parseDate(req.getParameter("openAt")));
            f.setCloseAt(parseDate(req.getParameter("closeAt")));
            error = service.save(f);
            if (error == null) {
                resp.sendRedirect(req.getContextPath() + "/registration-forms?msg=saved");
                return;
            }
            req.setAttribute("error", error);
            req.setAttribute("form", f);
            req.setAttribute("activities", activityService.getAllActivities());
            req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
        } catch (RuntimeException e) {
            throw new ServletException(e);
        }
    }

    private static String param(HttpServletRequest r, String key, String fallback) {
        String v = r.getParameter(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static int id(HttpServletRequest r) {
        return parseInt(r.getParameter("id"));
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
