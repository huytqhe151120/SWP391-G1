package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.RegistrationApplication;
import com.swp391.g1.model.RegistrationForm;
import com.swp391.g1.model.RegistrationFormField;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IRegistrationFormService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.service.impl.RegistrationFormServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if ("myApplications".equals(action)) {
                if (!"STUDENT".equals(user.getType())) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                if (student == null) {
                    resp.sendError(403, "Không tìm thấy hồ sơ sinh viên.");
                    return;
                }
                req.setAttribute("applications", service.getApplicationsForStudent(student.getId()));
                req.getRequestDispatcher("/WEB-INF/views/registration/my-applications.jsp").forward(req, resp);
                return;
            }

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
                RegistrationForm f = service.getOpenFormById(id(req), student.getId());
                if (f == null) {
                    String regStatus = service.getRegistrationStatus(id(req), student.getId());
                    String message = "CANCELLED".equals(regStatus) ? "cancelledBefore"
                            : (regStatus != null && !"WITHDRAWN".equals(regStatus)) ? "alreadyRegistered"
                            : service.isOpenFormAtCapacity(id(req)) ? "capacityReached" : "registrationUnavailable";
                    req.getSession().setAttribute("errorMessage", registrationMessage(message));
                    resp.sendRedirect(req.getContextPath() + "/activities");
                    return;
                }
                Map<String, String> saved = service.getSavedProfile(student.getId());
                for (RegistrationFormField field : f.getFields()) {
                    field.setValue(saved.get(field.getFieldKey()));
                }
                req.setAttribute("student", student);
                req.setAttribute("personalInfo", personalInfo(student));
                req.setAttribute("savedProfile", saved);
                req.setAttribute("form", f);
                req.getRequestDispatcher("/WEB-INF/views/registration/student-form.jsp").forward(req, resp);
                return;
            }
            if ("myApplication".equals(action)) {
                if (!"STUDENT".equals(user.getType())) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                RegistrationApplication application = service.getApplicationById(id(req));
                if (student == null || application == null || application.getStudentId() != student.getId()) {
                    resp.sendError(404);
                    return;
                }
                req.setAttribute("application", application);
                req.setAttribute("form", service.getById(application.getFormId()));
                req.setAttribute("isStudent", true);
                req.getRequestDispatcher("/WEB-INF/views/registration/application-detail.jsp").forward(req, resp);
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
                req.setAttribute("applications", service.getApplications(f.getId()));
                req.getRequestDispatcher("/WEB-INF/views/registration/detail.jsp").forward(req, resp);
                return;
            }
            if ("create".equals(action) || "edit".equals(action)) {
                RegistrationForm f = "edit".equals(action) ? service.getById(id(req)) : new RegistrationForm();
                if (f == null) {
                    resp.sendError(404);
                    return;
                }

                if ("edit".equals(action) && !f.isQuick() && f.getRegistrationCount() > 0) {
                    req.getSession().setAttribute("errorMessage",
                            "Form đã có sinh viên đăng ký nên không thể chỉnh sửa.");
                    String backTo = "detail".equals(req.getParameter("from"))
                            ? "/registration-forms?action=view&id=" + f.getId()
                            : "/registration-forms";
                    resp.sendRedirect(req.getContextPath() + backTo);
                    return;
                }
                if ("create".equals(action)) {
                    int activityId = parseInt(req.getParameter("activityId"));
                    if (activityId > 0) {
                        var activity = activityService.getActivityById(activityId);
                        if (activity == null) {
                            resp.sendError(404, "Không tìm thấy hoạt động.");
                            return;
                        }
                        if (!activity.isRegistrationFormRequired()) {
                            req.getSession().setAttribute("errorMessage",
                                    "Hoạt động này không bắt buộc điền form nên không cần tạo form đăng ký.");
                            resp.sendRedirect(req.getContextPath() + "/activities?action=detail&id=" + activityId);
                            return;
                        }
                        if (activity.getActivityStatus() != ActivityStatus.UPCOMING) {
                            req.getSession().setAttribute("errorMessage",
                                    "Không thể tạo form đăng ký vì hoạt động không ở trạng thái Chưa diễn ra.");
                            resp.sendRedirect(req.getContextPath()
                                    + "/activities?action=detail&id=" + activityId);
                            return;
                        }
                        RegistrationForm existing = service.getByActivityId(activityId);
                        if (existing != null) {
                            resp.sendRedirect(req.getContextPath()
                                    + "/registration-forms?action=view&id=" + existing.getId());
                            return;
                        }
                        f.setActivityId(activityId);
                        // Auto-set openAt and closeAt based on activity time
                        if (activity.getStartTime() != null) {
                            f.setOpenAt(activity.getStartTime().minusDays(7)); // Open 7 days before activity
                            f.setCloseAt(activity.getStartTime().minusHours(1)); // Close 1 hour before activity
                        }
                    }
                }
                req.setAttribute("form", f);
                setFormPageAttributes(req, f);
                req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
                return;
            }
            if ("application".equals(action)) {
                RegistrationApplication application = service.getApplicationById(id(req));
                if (application == null) {
                    resp.sendError(404);
                    return;
                }
                req.setAttribute("application", application);
                req.setAttribute("form", service.getById(application.getFormId()));
                req.getRequestDispatcher("/WEB-INF/views/registration/application-detail.jsp").forward(req, resp);
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
                RegistrationForm form = service.getById(id(req));
                if (form == null) { resp.sendError(404); return; }
                List<RegistrationFormField> answers = new ArrayList<>();
                for (RegistrationFormField field : form.getFields()) {
                    String value = req.getParameter("field_" + field.getFieldKey());
                    if ("CHECKBOX".equals(field.getFieldType())) {
                        value = "true".equals(value) ? "true" : "false";
                    }
                    field.setValue(value);
                    answers.add(field);
                }
                Map<String, String> submittedInfo = personalInfo(student);
                error = service.registerWithAnswers(id(req), student.getId(), req.getParameter("note"), answers,
                        submittedInfo);
                if (error == null) {
                    req.getSession().setAttribute("successMessage", "Đăng ký thành công.");
                    resp.sendRedirect(req.getContextPath() + "/registration-forms?action=myApplications");
                    return;
                }
                if (error.startsWith("Không thể đăng ký:")) {
                    String regStatus = service.getRegistrationStatus(id(req), student.getId());
                    String message = "CANCELLED".equals(regStatus) ? "cancelledBefore"
                            : (regStatus != null && !"WITHDRAWN".equals(regStatus)) ? "alreadyRegistered"
                            : service.isOpenFormAtCapacity(id(req)) ? "capacityReached" : "registrationUnavailable";
                    req.getSession().setAttribute("errorMessage", registrationMessage(message));
                    resp.sendRedirect(req.getContextPath() + "/activities");
                    return;
                }
                req.setAttribute("error", error);
                RegistrationForm f = service.getById(id(req));
                if (f != null) {
                    for (RegistrationFormField field : f.getFields()) {
                        String value = req.getParameter("field_" + field.getFieldKey());
                        field.setValue("CHECKBOX".equals(field.getFieldType())
                                ? ("true".equals(value) ? "true" : "false")
                                : value);
                    }
                }
                req.setAttribute("student", student);
                req.setAttribute("personalInfo", submittedInfo);
                req.setAttribute("savedProfile", service.getSavedProfile(student.getId()));
                req.setAttribute("form", f);
                req.getRequestDispatcher("/WEB-INF/views/registration/student-form.jsp").forward(req, resp);
                return;
            }
            if ("registerQuick".equals(action)) {
                if (!"STUDENT".equals(user.getType())) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                if (student == null) {
                    resp.sendError(403, "Không tìm thấy hồ sơ sinh viên.");
                    return;
                }
                int activityId = parseInt(req.getParameter("activityId"));
                error = service.registerQuick(activityId, student.getId(), req.getParameter("note"));
                if (error == null) {
                    req.getSession().setAttribute("successMessage", "Đăng ký nhanh thành công.");
                    resp.sendRedirect(req.getContextPath() + "/registration-forms?action=myApplications");
                    return;
                }
                req.getSession().setAttribute("errorMessage", error);
                resp.sendRedirect(req.getContextPath() + "/activities?action=detail&id=" + activityId);
                return;
            }
            if ("cancelRegistration".equals(action)) {
                if (!"STUDENT".equals(user.getType())) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                if (student == null) {
                    resp.sendError(403, "Không tìm thấy hồ sơ sinh viên.");
                    return;
                }
                int registrationId = id(req);
                RegistrationApplication application = service.getApplicationById(registrationId);
                if (application == null || application.getStudentId() != student.getId()) {
                    resp.sendError(404);
                    return;
                }
                error = service.cancelRegistration(registrationId);
                if (error == null) {
                    RegistrationApplication after = service.getApplicationById(registrationId);
                    boolean withdrawn = after != null && "WITHDRAWN".equals(after.getStatus());
                    req.getSession().setAttribute("successMessage", withdrawn
                            ? "Đã rút đơn đăng ký. Bạn có thể đăng ký lại nếu hoạt động còn mở."
                            : "Đã hủy đăng ký. Bạn không thể đăng ký lại hoạt động này.");
                } else {
                    req.getSession().setAttribute("errorMessage", error);
                }
                resp.sendRedirect(req.getContextPath() + "/registration-forms?action=myApplications");
                return;
            }
            if (!AccessPolicy.canManageActivities(user)) {
                resp.sendError(403);
                return;
            }
            if ("approveApplication".equals(action) || "rejectApplication".equals(action)
                    || "undoApplicationApproval".equals(action) || "undoApplicationRejection".equals(action)) {
                int registrationId = parseInt(req.getParameter("registrationId"));
                error = switch (action) {
                    case "approveApplication" -> service.reviewApplication(registrationId);
                    case "rejectApplication" -> service.rejectApplication(registrationId);
                    case "undoApplicationApproval" -> service.undoApplicationApproval(registrationId);
                    default -> service.undoApplicationRejection(registrationId);
                };
                RegistrationApplication application = service.getApplicationById(registrationId);
                if (application == null) {
                    resp.sendError(404);
                    return;
                }
                String message = error == null
                        ? switch (action) {
                    case "approveApplication" -> "approved";
                    case "rejectApplication" -> "rejected";
                    case "undoApplicationRejection" -> "rejectionUndone";
                    default -> "approvalUndone";
                }
                        : error.contains("đã đủ số lượng tối đa") ? "capacityError"
                        : error.contains("được gửi trước") ? "fifoError" : "reviewError";
                resp.sendRedirect(req.getContextPath() + "/registration-forms?action=application&id="
                        + application.getId() + "&msg=" + message);
                return;
            }
            if ("delete".equals(action)) {
                error = service.delete(id(req));
                if (error != null) {
                    req.getSession().setAttribute("errorMessage", error);
                }
                resp.sendRedirect(req.getContextPath() + "/registration-forms" + (error == null ? "?msg=deleted" : ""));
                return;
            }
            String rawId = req.getParameter("id");
            int formId = parseInt(rawId);
            boolean invalidFormId = false;
            if (rawId != null && !rawId.isBlank()) {
                try {
                    invalidFormId = Integer.parseInt(rawId.trim()) < 0;
                } catch (NumberFormatException e) {
                    invalidFormId = true;
                }
            }
            if (invalidFormId) {
                error = "Form đăng ký không hợp lệ.";
                RegistrationForm invalidForm = new RegistrationForm();
                invalidForm.setActivityId(parseInt(req.getParameter("activityId")));
                invalidForm.setTitle(req.getParameter("title"));
                invalidForm.setDescription(req.getParameter("description"));
                req.setAttribute("error", error);
                setFormPageAttributes(req, invalidForm);
                req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
                return;
            }
            RegistrationForm f = new RegistrationForm();
            f.setId(formId);
            f.setActivityId(parseInt(req.getParameter("activityId")));
            f.setTitle(req.getParameter("title"));
            f.setDescription(req.getParameter("description"));
            if (f.getId() > 0) {
                RegistrationForm existing = service.getById(f.getId());
                if (existing == null) {
                    resp.sendError(404);
                    return;
                }
                if (existing.isQuick()) {
                    // Form đăng ký nhanh: staff chỉ chỉnh mô tả và thời gian
                    f.setActivityId(existing.getActivityId());
                    f.setActivityName(existing.getActivityName());
                    f.setTitle(existing.getTitle());
                    f.setQuick(true);
                    String quickOpen = req.getParameter("openAt");
                    String quickClose = req.getParameter("closeAt");
                    f.setOpenAt(parseDate(quickOpen));
                    f.setCloseAt(parseDate(quickClose));
                    if (hasInvalidDate(quickOpen, f.getOpenAt())) {
                        error = "Thời gian mở đăng ký không hợp lệ.";
                    } else if (hasInvalidDate(quickClose, f.getCloseAt())) {
                        error = "Thời gian đóng đăng ký không hợp lệ.";
                    } else {
                        error = service.saveQuickForm(f);
                    }
                    if (error == null) {
                        resp.sendRedirect(req.getContextPath() + "/registration-forms?msg=saved");
                        return;
                    }
                    req.setAttribute("error", error);
                    req.setAttribute("form", f);
                    setFormPageAttributes(req, f);
                    req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
                    return;
                }
                if (existing.getRegistrationCount() > 0) {
                    String referer = req.getHeader("referer");
                    String errorMessage = "Form đã có sinh viên đăng ký nên không thể chỉnh sửa.";
                    if (referer != null && referer.contains("action=view")) {
                        req.setAttribute("errorMessage", errorMessage);
                        req.setAttribute("form", existing);
                        req.getRequestDispatcher("/WEB-INF/views/registration/detail.jsp").forward(req, resp);
                    } else {
                        req.getSession().setAttribute("errorMessage", errorMessage);
                        resp.sendRedirect(req.getContextPath() + "/registration-forms?msg=editError");
                    }
                    return;
                }
                f.setFields(existing.getFields());
                f.setRegistrationCount(existing.getRegistrationCount());
            }
            String openAt = req.getParameter("openAt");
            String closeAt = req.getParameter("closeAt");
            f.setOpenAt(parseDate(openAt));
            f.setCloseAt(parseDate(closeAt));
            if (hasInvalidDate(openAt, f.getOpenAt())) {
                error = "Thời gian mở đăng ký không hợp lệ.";
            } else if (hasInvalidDate(closeAt, f.getCloseAt())) {
                error = "Thời gian đóng đăng ký không hợp lệ.";
            } else {
                error = service.save(f);
            }
            if (error == null) {
                resp.sendRedirect(req.getContextPath() + "/registration-forms?msg=saved");
                return;
            }
            req.setAttribute("error", error);
            req.setAttribute("form", f);
            setFormPageAttributes(req, f);
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

    private static boolean hasInvalidDate(String raw, LocalDateTime parsed) {
        return raw != null && !raw.isBlank() && parsed == null;
    }

    private void setFormPageAttributes(HttpServletRequest request, RegistrationForm form) {
        request.setAttribute("form", form);
        List<Integer> usedActivityIds = service.getAll().stream()
                .filter(existing -> existing.getId() != form.getId())
                .map(RegistrationForm::getActivityId)
                .toList();
        var approvedActivities = activityService.getAllActivities().stream()
                .filter(activity -> activity.getApprovalStatus() == ApprovalStatus.APPROVED)
                .filter(activity -> activity.getActivityStatus() == ActivityStatus.UPCOMING)
                .filter(activity -> activity.isRegistrationFormRequired()
                        || activity.getId() == form.getActivityId())
                .toList();
        request.setAttribute("hasEligibleApprovedActivities", !approvedActivities.isEmpty());
        request.setAttribute("activities", approvedActivities.stream()
                .filter(activity -> !usedActivityIds.contains(activity.getId()))
                .toList());
        request.setAttribute("minimumRegistrationTime",
                DateTimeUtil.formatInput(DateTimeUtil.now().truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)));
    }

    private static Map<String, String> personalInfo(Student student) {
        Map<String, String> values = new HashMap<>();
        values.put("studentCode", student.getCode());
        values.put("studentName", student.getName());
        values.put("classCode", student.getMainClassCode());
        values.put("email", student.getEmail());
        return values;
    }

    private static String registrationMessage(String code) {
        return switch (code) {
            case "alreadyRegistered" -> "Bạn đã đăng ký hoạt động này.";
            case "capacityReached" -> "Hoạt động đã đủ số lượng đăng ký.";
            case "cancelledBefore" ->
                    "Bạn đã hủy đăng ký hoạt động này sau khi được duyệt nên không thể đăng ký lại. Vui lòng liên hệ ban tổ chức nếu cần.";
            default -> "Hoạt động hiện chưa mở đăng ký hoặc đã đóng.";
        };
    }
}