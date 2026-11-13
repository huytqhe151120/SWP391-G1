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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@WebServlet("/registration-forms")
public class RegistrationFormServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_OF_BIRTH_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final IRegistrationFormService service = new RegistrationFormServiceImpl();
    private final IStudentService studentService = new StudentServiceImpl();
    private final ExtracurricularActivityServiceImpl activityService = new ExtracurricularActivityServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(req);
        String action = param(req, "action", "list");
        try {
            if ("register".equals(action)) {
                if (!AccessPolicy.hasType(user, "STUDENT")) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                if (student == null) {
                    resp.sendError(403, "Không tìm thấy hồ sơ sinh viên.");
                    return;
                }
                String returnTo = safeReturnTo(req, req.getContextPath() + "/activities");
                RegistrationForm f = service.getOpenFormById(id(req), student.getId());
                if (f == null) {
                    RegistrationForm formCheck = service.getById(id(req));
                    String message;
                    if (formCheck != null && "DRAFT".equals(formCheck.getStatus())) {
                        message = "formNotOpen";
                    } else if (formCheck != null && "CLOSED".equals(formCheck.getStatus())) {
                        message = "formClosed";
                    } else {
                        String regStatus = service.getRegistrationStatus(id(req), student.getId());
                        message = "CANCELLED".equals(regStatus) ? "cancelledBefore"
                                : (regStatus != null && !"WITHDRAWN".equals(regStatus)) ? "alreadyRegistered"
                                : service.isOpenFormAtCapacity(id(req)) ? "capacityReached" : "registrationUnavailable";
                    }
                    req.getSession().setAttribute("errorMessage", registrationMessage(message));
                    resp.sendRedirect(returnTo);
                    return;
                }
                Map<String, String> saved = service.getSavedProfile(student.getId());
                for (RegistrationFormField field : f.getFields()) {
                    String savedValue = saved.get(field.getFieldKey());
                    field.setValue(savedValue);
                    if ("MULTISELECT".equals(field.getFieldType()) && savedValue != null
                            && !savedValue.isBlank()) {
                        field.setSelectedOptions(Arrays.asList(savedValue.split("\\R")));
                    }
                }
                req.setAttribute("student", student);
                req.setAttribute("personalInfo", personalInfo(student));
                req.setAttribute("savedProfile", saved);
                req.setAttribute("form", f);
                req.setAttribute("activity", activityService.getActivityById(f.getActivityId()));
                req.setAttribute("returnTo", returnTo);
                req.getRequestDispatcher("/WEB-INF/views/registration/student-form.jsp").forward(req, resp);
                return;
            }
            if ("myApplication".equals(action)) {
                if (!AccessPolicy.hasType(user, "STUDENT")) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                RegistrationApplication application = service.getApplicationById(id(req));
                if (student == null || application == null || application.getStudentId() != student.getId()) {
                    resp.sendError(404);
                    return;
                }
                RegistrationForm form = service.getById(application.getFormId());
                req.setAttribute("application", application);
                req.setAttribute("form", form);
                req.setAttribute("applicationDob", applicationDateOfBirth(application));
                setApplicationQuestionFields(req, form, application);
                req.setAttribute("isStudent", true);
                req.setAttribute("returnTo", safeReturnTo(req,
                        req.getContextPath() + "/registration-forms?action=myApplications"));
                req.getRequestDispatcher("/WEB-INF/views/registration/application-detail.jsp").forward(req, resp);
                return;
            }
            if (!AccessPolicy.canManageActivities(user)) {
                resp.sendError(403, "Chỉ STAFF/ADMIN được quản lý form đăng ký.");
                return;
            }
            if ("create".equals(action) || "edit".equals(action)) {
                RegistrationForm f = "edit".equals(action) ? service.getById(id(req)) : new RegistrationForm();
                if (f == null) {
                    resp.sendError(404);
                    return;
                }

                String returnToFallback = "edit".equals(action) && f.getId() > 0
                        ? req.getContextPath() + "/registration-forms?action=view&id=" + f.getId()
                        : req.getContextPath() + "/registration-forms";
                if ("create".equals(action)) {
                    int activityId = parseInt(req.getParameter("activityId"));
                    if (activityId > 0) {
                        returnToFallback = "detail".equals(req.getParameter("from"))
                                ? req.getContextPath() + "/registration-forms?action=view&activityId=" + activityId
                                : req.getContextPath() + "/activities?action=detail&id=" + activityId;
                    }
                }
                String returnTo = safeReturnTo(req, returnToFallback);

                if ("edit".equals(action) && !f.isQuick() && f.getRegistrationCount() > 0) {
                    req.getSession().setAttribute("errorMessage",
                            "Form đã có sinh viên đăng ký nên không thể chỉnh sửa.");
                    resp.sendRedirect(returnTo);
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
                                    "Hoạt động này không bắt buộc điền form nên không cần tạo đợt đăng ký.");
                            resp.sendRedirect(returnTo);
                            return;
                        }
                        if (activity.getActivityStatus() != ActivityStatus.UPCOMING) {
                            req.getSession().setAttribute("errorMessage",
                                    "Không thể tạo đợt đăng ký vì hoạt động không ở trạng thái Chưa diễn ra.");
                            resp.sendRedirect(returnTo);
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
                            LocalDateTime defaultOpenAt = activity.getStartTime().minusDays(7);
                            LocalDateTime earliestOpenAt = DateTimeUtil.now().truncatedTo(ChronoUnit.MINUTES)
                                    .plusMinutes(1);
                            f.setOpenAt(defaultOpenAt.isBefore(earliestOpenAt) ? earliestOpenAt : defaultOpenAt);
                            f.setCloseAt(activity.getStartTime().minusHours(24));
                        }
                    }
                }
                req.setAttribute("form", f);
                req.setAttribute("returnTo", returnTo);
                setFormPageAttributes(req, f);
                req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
                return;
            }
            if ("application".equals(action)) {
                int appId = id(req);
                int formId = parseInt(req.getParameter("formId"));
                RegistrationApplication application = appId > 0 ? service.getApplicationById(appId) : null;
                if (application == null && formId <= 0) {
                    resp.sendError(404);
                    return;
                }
                RegistrationForm form = service.getById(application != null ? application.getFormId() : formId);
                if (form == null) {
                    resp.sendError(404);
                    return;
                }
                List<RegistrationApplication> applications = service.getApplications(form.getId());
                if (application == null && !applications.isEmpty()) {
                    application = service.getApplicationById(applications.get(0).getId());
                }
                req.setAttribute("application", application);
                req.setAttribute("applications", applications);
                req.setAttribute("form", form);
                if (application != null) {
                    req.setAttribute("applicationDob", applicationDateOfBirth(application));
                    setApplicationQuestionFields(req, form, application);
                    req.setAttribute("hasEarlierPending",
                            "SUBMITTED".equals(application.getStatus())
                                    && service.hasEarlierPendingApplication(application.getId()));
                }
                req.getRequestDispatcher("/WEB-INF/views/registration/application-detail.jsp").forward(req, resp);
                return;
            }

            int formId = id(req);
            int activityId = parseInt(req.getParameter("activityId"));
            var allActivities = activityService.getAllActivities();
            Map<Integer, com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO> activityMap = new HashMap<>();
            for (var act : allActivities) {
                activityMap.put(act.getId(), act);
            }
            var requiredFormActivities = allActivities.stream()
                    .filter(com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO::isRegistrationFormRequired)
                    .toList();

            RegistrationForm f = null;
            com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO selectedActivity = null;
            if (formId > 0) {
                f = service.getById(formId);
                if (f == null) {
                    resp.sendError(404);
                    return;
                }
                activityId = f.getActivityId();
                selectedActivity = activityMap.getOrDefault(activityId,
                        activityService.getActivityById(activityId));
            } else if (activityId > 0) {
                selectedActivity = activityMap.getOrDefault(activityId,
                        activityService.getActivityById(activityId));
                if (selectedActivity == null) {
                    resp.sendError(404);
                    return;
                }
                f = service.getByActivityId(activityId);
                if (f != null) {
                    resp.sendRedirect(req.getContextPath() + "/registration-forms?action=view&id=" + f.getId());
                    return;
                }
            }

            LocalDateTime now = DateTimeUtil.now();
            boolean extendableForm = f != null && !f.isQuick() && f.getRegistrationCount() > 0
                    && f.getCloseAt() != null && !f.getCloseAt().isAfter(now);
            req.setAttribute("extendableForm", extendableForm);
            req.setAttribute("minimumExtensionTime",
                    DateTimeUtil.formatInput(now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)));
            req.setAttribute("activities", requiredFormActivities);
            req.setAttribute("activityMap", activityMap);
            req.setAttribute("form", f);
            req.setAttribute("activity", selectedActivity);
            req.setAttribute("selectedActivityId", activityId);
            String returnToFallback = activityId > 0
                    ? req.getContextPath() + "/activities?action=detail&id=" + activityId + "#tab-registration"
                    : req.getContextPath() + "/activities";
            req.setAttribute("returnTo", safeReturnTo(req, returnToFallback));
            req.setAttribute("applications", f != null ? service.getApplications(f.getId()) : List.of());
            req.getRequestDispatcher("/WEB-INF/views/registration/detail.jsp").forward(req, resp);
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
                if (!AccessPolicy.hasType(user, "STUDENT")) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                if (student == null) {
                    resp.sendError(403, "Không tìm thấy hồ sơ sinh viên.");
                    return;
                }
                String returnTo = safeReturnTo(req, req.getContextPath() + "/activities");
                RegistrationForm form = service.getById(id(req));
                if (form == null) { resp.sendError(404); return; }
                List<RegistrationFormField> answers = new ArrayList<>();
                for (RegistrationFormField field : form.getFields()) {
                    String[] values = req.getParameterValues("field_" + field.getFieldKey());
                    String value;
                    if ("MULTISELECT".equals(field.getFieldType())) {
                        List<String> selected = values == null ? List.of() : Arrays.asList(values);
                        field.setSelectedOptions(selected);
                        value = String.join("\n", selected);
                    } else {
                        value = values == null || values.length == 0 ? null : values[0];
                    }
                    if ("CHECKBOX".equals(field.getFieldType())) {
                        value = "true".equals(value) ? "true" : "false";
                    }
                    field.setValue(value);
                    answers.add(field);
                }
                Map<String, String> submittedInfo = personalInfo(student);
                error = service.registerWithAnswers(id(req), student.getId(), req.getParameter("note"), answers,
                        submittedInfo, "true".equals(req.getParameter("attendanceCommitment")));
                if (error == null) {
                    req.getSession().setAttribute("successMessage", "Đăng ký thành công.");
                    resp.sendRedirect(returnTo);
                    return;
                }
                req.setAttribute("error", error);
                req.setAttribute("note", req.getParameter("note"));
                RegistrationForm f = service.getById(id(req));
                if (f != null) {
                    for (RegistrationFormField field : f.getFields()) {
                        String[] values = req.getParameterValues("field_" + field.getFieldKey());
                        String value;
                        if ("MULTISELECT".equals(field.getFieldType())) {
                            List<String> selected = values == null ? List.of() : Arrays.asList(values);
                            field.setSelectedOptions(selected);
                            value = String.join("\n", selected);
                        } else {
                            value = values == null || values.length == 0 ? null : values[0];
                        }
                        field.setValue("CHECKBOX".equals(field.getFieldType())
                                ? ("true".equals(value) ? "true" : "false")
                                : value);
                    }
                }
                req.setAttribute("student", student);
                req.setAttribute("personalInfo", submittedInfo);
                req.setAttribute("savedProfile", service.getSavedProfile(student.getId()));
                req.setAttribute("form", f);
                req.setAttribute("activity", f == null ? null : activityService.getActivityById(f.getActivityId()));
                req.setAttribute("attendanceCommitment",
                        "true".equals(req.getParameter("attendanceCommitment")));
                req.setAttribute("returnTo", returnTo);
                req.getRequestDispatcher("/WEB-INF/views/registration/student-form.jsp").forward(req, resp);
                return;
            }
            if ("registerQuick".equals(action)) {
                if (!AccessPolicy.hasType(user, "STUDENT")) {
                    resp.sendError(403);
                    return;
                }
                Student student = studentService.getStudentByAccountId(user.getId());
                if (student == null) {
                    resp.sendError(403, "Không tìm thấy hồ sơ sinh viên.");
                    return;
                }
                int activityId = parseInt(req.getParameter("activityId"));
                String returnTo = safeReturnTo(req, req.getContextPath() + "/activities");
                error = service.registerQuick(activityId, student.getId(), req.getParameter("note"));
                if (error == null) {
                    req.getSession().setAttribute("successMessage", "Đăng ký nhanh thành công.");
                    resp.sendRedirect(returnTo);
                    return;
                }
                req.getSession().setAttribute("errorMessage", error);
                resp.sendRedirect(returnTo);
                return;
            }
            if ("cancelRegistration".equals(action)) {
                if (!AccessPolicy.hasType(user, "STUDENT")) {
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
                String returnTo = safeReturnTo(req,
                        req.getContextPath() + "/registration-forms?action=myApplications");
                if (error == null) {
                    RegistrationApplication after = service.getApplicationById(registrationId);
                    boolean withdrawn = after != null && "WITHDRAWN".equals(after.getStatus());
                    req.getSession().setAttribute("successMessage", withdrawn
                            ? "Đã rút đơn đăng ký. Bạn có thể đăng ký lại nếu hoạt động còn mở."
                            : "Đã hủy đăng ký. Bạn không thể đăng ký lại hoạt động này.");
                } else {
                    req.getSession().setAttribute("errorMessage", error);
                }
                resp.sendRedirect(returnTo);
                return;
            }
            if (!AccessPolicy.canManageActivities(user)) {
                resp.sendError(403);
                return;
            }
            if ("bulkRejectApplications".equals(action) || "bulkApproveApplications".equals(action)) {
                int formId = parseInt(req.getParameter("formId"));
                String[] rawIds = req.getParameterValues("registrationIds");
                List<Integer> registrationIds = rawIds == null ? List.of()
                        : Arrays.stream(rawIds).map(RegistrationFormServlet::parseInt).toList();
                Set<Integer> selectedSet = new HashSet<>(registrationIds);
                boolean seenUnselectedSubmitted = false;
                boolean outOfOrderBulk = false;
                for (RegistrationApplication app : service.getApplications(formId)) {
                    if ("SUBMITTED".equals(app.getStatus())) {
                        if (selectedSet.contains(app.getId())) {
                            if (seenUnselectedSubmitted) {
                                outOfOrderBulk = true;
                                break;
                            }
                        } else {
                            seenUnselectedSubmitted = true;
                        }
                    }
                }
                boolean rejecting = "bulkRejectApplications".equals(action);
                error = rejecting
                        ? service.rejectApplications(formId, registrationIds, req.getParameter("rejectionReason"))
                        : service.approveApplications(formId, registrationIds);
                if (error == null) {
                    req.getSession().setAttribute("successMessage",
                            "Đã " + (rejecting ? "từ chối " : "duyệt ") + registrationIds.size()
                                    + " đơn" + (rejecting ? " với cùng một lý do." : "."));
                    if (outOfOrderBulk) {
                        req.getSession().setAttribute("warningMessage",
                                "Cảnh báo: Còn đơn chờ xử lý được gửi trước chưa được xử lý. Hãy lưu ý xử lý các đơn theo thứ tự gửi.");
                    }
                } else {
                    req.getSession().setAttribute("errorMessage", error);
                }
                resp.sendRedirect(req.getContextPath() + "/registration-forms?action=view&id="
                        + formId + "&tab=applications");
                return;
            }
            if ("approveApplication".equals(action) || "rejectApplication".equals(action)
                    || "undoApplicationApproval".equals(action) || "undoApplicationRejection".equals(action)) {
                int registrationId = parseInt(req.getParameter("registrationId"));
                boolean outOfOrder = ("approveApplication".equals(action) || "rejectApplication".equals(action))
                        && service.hasEarlierPendingApplication(registrationId);
                error = switch (action) {
                    case "approveApplication" -> service.reviewApplication(registrationId);
                    case "rejectApplication" -> service.rejectApplication(
                            registrationId, req.getParameter("rejectionReason"));
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
                        : "rejectApplication".equals(action) && error.toLowerCase().contains("lý do") ? "reasonError"
                        : error.contains("đã đủ số lượng tối đa") ? "capacityError"
                        : error.contains("được gửi trước") ? "fifoError" : "reviewError";
                resp.sendRedirect(req.getContextPath() + "/registration-forms?action=application&id="
                        + application.getId() + "&msg=" + message + (error == null && outOfOrder ? "&fifoWarn=1" : ""));
                return;
            }
            if ("extendFormClose".equals(action)) {
                int formId = parseInt(req.getParameter("formId"));
                error = service.extendCloseTime(formId, parseDate(req.getParameter("closeAt")));
                req.getSession().setAttribute(error == null ? "successMessage" : "errorMessage",
                        error == null ? "Đã gia hạn đợt đăng ký." : error);
                resp.sendRedirect(req.getContextPath() + (formId > 0
                        ? "/registration-forms?action=view&id=" + formId
                        : "/registration-forms"));
                return;
            }
            if ("delete".equals(action)) {
                int delFormId = id(req);
                RegistrationForm existingForm = service.getById(delFormId);
                int delActivityId = existingForm != null ? existingForm.getActivityId() : 0;
                error = service.delete(delFormId);
                if (error != null) {
                    req.getSession().setAttribute("errorMessage", error);
                    resp.sendRedirect(req.getContextPath() + (delFormId > 0
                            ? "/registration-forms?action=view&id=" + delFormId
                            : "/registration-forms"));
                } else {
                    req.getSession().setAttribute("successMessage", "Đã xóa form.");
                    resp.sendRedirect(req.getContextPath() + (delActivityId > 0
                            ? "/registration-forms?action=view&activityId=" + delActivityId
                            : "/registration-forms"));
                }
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
                invalidForm.setFields(readQuestionFields(req));
                req.setAttribute("error", error);
                req.setAttribute("returnTo", safeReturnTo(req, req.getContextPath() + "/registration-forms"));
                setFormPageAttributes(req, invalidForm);
                req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
                return;
            }
            RegistrationForm f = new RegistrationForm();
            f.setId(formId);
            f.setActivityId(parseInt(req.getParameter("activityId")));
            f.setTitle(req.getParameter("title"));
            f.setDescription(req.getParameter("description"));
            f.setFields(readQuestionFields(req));
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
                        req.getSession().setAttribute("successMessage", "Đã lưu đợt đăng ký.");
                        resp.sendRedirect(req.getContextPath() + "/registration-forms?action=view&id=" + f.getId());
                        return;
                    }
                    req.setAttribute("error", error);
                    req.setAttribute("form", f);
                    req.setAttribute("returnTo", safeReturnTo(req,
                            req.getContextPath() + "/registration-forms?action=view&id=" + f.getId()));
                    setFormPageAttributes(req, f);
                    req.getRequestDispatcher("/WEB-INF/views/registration/form.jsp").forward(req, resp);
                    return;
                }
                if (existing.getRegistrationCount() > 0) {
                    req.getSession().setAttribute("errorMessage",
                            "Form đã có sinh viên đăng ký nên không thể chỉnh sửa.");
                    resp.sendRedirect(req.getContextPath() + "/registration-forms?action=view&id=" + f.getId());
                    return;
                }
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
                req.getSession().setAttribute("successMessage", "Đã lưu đợt đăng ký.");
                RegistrationForm savedForm = f.getId() > 0
                        ? service.getById(f.getId())
                        : service.getByActivityId(f.getActivityId());
                String viewUrl = savedForm != null
                        ? req.getContextPath() + "/registration-forms?action=view&id=" + savedForm.getId()
                        : req.getContextPath() + "/registration-forms";
                resp.sendRedirect(viewUrl);
                return;
            }
            req.setAttribute("error", error);
            req.setAttribute("form", f);
            req.setAttribute("returnTo", safeReturnTo(req, req.getContextPath() + "/registration-forms"));
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

    private static String safeReturnTo(HttpServletRequest request, String fallback) {
        String candidate = request.getParameter("returnTo");
        if (candidate == null || candidate.isBlank() || candidate.indexOf('\\') >= 0
                || candidate.indexOf('\r') >= 0 || candidate.indexOf('\n') >= 0) {
            return fallback;
        }
        int queryStart = candidate.indexOf('?');
        int fragmentStart = candidate.indexOf('#');
        int pathEnd = queryStart < 0 ? candidate.length() : queryStart;
        if (fragmentStart >= 0 && fragmentStart < pathEnd) {
            pathEnd = fragmentStart;
        }
        String path = candidate.substring(0, pathEnd);
        String contextPath = request.getContextPath();
        if (!(path.equals(contextPath + "/activities")
                || path.equals(contextPath + "/registration-forms"))) {
            return fallback;
        }
        return candidate;
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

    private static List<RegistrationFormField> readQuestionFields(HttpServletRequest request) {
        int questionCount = parseInt(request.getParameter("questionCount"));
        if (questionCount < 0 || questionCount > 10) {
            List<RegistrationFormField> overflow = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                RegistrationFormField field = new RegistrationFormField();
                field.setFieldKey("question_" + (i + 1));
                field.setLabel("Câu hỏi " + (i + 1));
                field.setFieldType("TEXTAREA");
                overflow.add(field);
            }
            return overflow;
        }

        List<RegistrationFormField> fields = new ArrayList<>();
        for (int i = 0; i < questionCount; i++) {
            RegistrationFormField field = new RegistrationFormField();
            field.setFieldKey("question_" + (i + 1));
            field.setLabel(request.getParameter("questionLabel_" + i));
            String type = request.getParameter("questionType_" + i);
            field.setFieldType(type == null ? "" : type);
            field.setRequired("true".equals(request.getParameter("questionRequired_" + i)));
            field.setSortOrder(i);

            if ("SELECT".equals(type) || "MULTISELECT".equals(type)) {
                int choiceCount = parseInt(request.getParameter("choiceCount_" + i));
                if (choiceCount < 0 || choiceCount > 20) {
                    field.setOptions("");
                } else {
                    List<String> choices = new ArrayList<>();
                    for (int choice = 0; choice < choiceCount; choice++) {
                        String value = request.getParameter("choice_" + i + "_" + choice);
                        choices.add(value == null ? "" : value.trim());
                    }
                    field.setOptions(String.join("\n", choices));
                }
            }
            fields.add(field);
        }
        return fields;
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
        request.setAttribute("openAtInput", DateTimeUtil.formatInput(form.getOpenAt()));
        request.setAttribute("closeAtInput", DateTimeUtil.formatInput(form.getCloseAt()));
    }

    private static void setApplicationQuestionFields(HttpServletRequest request, RegistrationForm form,
                                                     RegistrationApplication application) {
        List<RegistrationFormField> questions = application.getAnswers().stream()
                .filter(answer -> answer.getFieldKey() != null
                        && answer.getFieldKey().startsWith("question_"))
                .toList();
        if (questions.isEmpty() && form != null) {
            questions = form.getFields().stream()
                    .filter(field -> field.getFieldKey() != null
                            && field.getFieldKey().startsWith("question_"))
                    .toList();
        }
        request.setAttribute("questionFields", questions);
    }

    private static String applicationDateOfBirth(RegistrationApplication application) {
        return application.getAnswers().stream()
                .filter(answer -> "application_dob".equals(answer.getFieldKey()))
                .map(RegistrationFormField::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .map(RegistrationFormServlet::formatDateOfBirth)
                .orElse(null);
    }

    private static Map<String, String> personalInfo(Student student) {
        Map<String, String> values = new HashMap<>();
        values.put("studentCode", student.getCode());
        values.put("studentName", student.getName());
        values.put("classCode", student.getMainClassCode());
        values.put("email", student.getEmail());
        values.put("phoneNumber", student.getPhoneNumber());
        values.put("dob", student.getDob());
        values.put("dobDisplay", formatDateOfBirth(student.getDob()));
        return values;
    }

    private static String formatDateOfBirth(String dob) {
        if (dob == null || dob.isBlank()) return "";
        try {
            return LocalDate.parse(dob).format(DATE_OF_BIRTH_DISPLAY);
        } catch (DateTimeParseException e) {
            return dob;
        }
    }

    private static String registrationMessage(String code) {
        return switch (code) {
            case "alreadyRegistered" -> "Bạn đã đăng ký hoạt động này.";
            case "capacityReached" -> "Hoạt động đã đủ số lượng đăng ký.";
            case "cancelledBefore" ->
                    "Bạn đã hủy đăng ký hoạt động này sau khi được duyệt nên không thể đăng ký lại. Vui lòng liên hệ ban tổ chức nếu cần.";
            case "formNotOpen" -> "Đợt đăng ký chưa mở. Vui lòng quay lại sau khi thời gian mở đăng ký bắt đầu.";
            case "formClosed" -> "Đợt đăng ký đã đóng.";
            default -> "Hoạt động hiện chưa mở đăng ký hoặc đã đóng.";
        };
    }
}
