package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityFeedbackAnswerDTO;
import com.swp391.g1.dto.response.ActivityFeedbackReportDTO;
import com.swp391.g1.dto.response.FeedbackActivityDTO;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.ActivityFeedbackUnavailableException;
import com.swp391.g1.service.IDepartmentService;
import com.swp391.g1.service.ISemesterService;
import com.swp391.g1.service.IActivityFeedbackService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.DepartmentServiceImpl;
import com.swp391.g1.service.impl.SemesterServiceImpl;
import com.swp391.g1.service.impl.ActivityFeedbackServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.Semester;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/activity-feedback")
public class ActivityFeedbackServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ActivityFeedbackServlet.class.getName());
    private final IActivityFeedbackService feedbackService = new ActivityFeedbackServiceImpl();
    private final IStudentService studentService = new StudentServiceImpl();
    private final ISemesterService semesterService = new SemesterServiceImpl();
    private final IDepartmentService departmentService = new DepartmentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        if (AccessPolicy.canSubmitActivityFeedback(user)) {
            if ("reminder".equals(request.getParameter("action"))) {
                writeReminderCount(user, response);
                return;
            }
            showStudentPage(request, response, user);
            return;
        }
        if (AccessPolicy.canManageActivityFeedback(user)) {
            showManagementPage(request, response, user);
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng feedback.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        CurrentUser user = AuthContext.getCurrentUser(request);
        if ("remind".equals(request.getParameter("action"))) {
            handleReminder(request, response, user);
            return;
        }
        if (!AccessPolicy.canSubmitActivityFeedback(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ sinh viên mới có thể gửi feedback.");
            return;
        }
        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa được liên kết với hồ sơ sinh viên.");
            return;
        }
        int activityId = parsePositiveId(request.getParameter("activityId"));
        ActivityFeedbackAnswerDTO answer = readAnswer(request);
        try {
            feedbackService.submit(student.getId(), activityId, answer);
            request.getSession().setAttribute("feedbackSuccess", "Feedback của bạn đã được gửi. Cảm ơn bạn!");
            response.sendRedirect(request.getContextPath() + "/activity-feedback");
        } catch (IllegalArgumentException | ActivityFeedbackUnavailableException e) {
            showStudentPage(request, response, user, activityId, answer, e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to submit activity feedback.", e);
            showStudentPage(request, response, user, activityId, answer,
                    "Không thể gửi feedback lúc này. Vui lòng thử lại sau.");
        }
    }

    private void handleReminder(HttpServletRequest request, HttpServletResponse response, CurrentUser user)
            throws IOException {
        if (!AccessPolicy.canManageActivityFeedback(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền gửi nhắc nhở feedback.");
            return;
        }
        int activityId = parsePositiveId(request.getParameter("activityId"));
        if (activityId == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hoạt động không hợp lệ.");
            return;
        }
        Integer staffAccountId = "ADMIN".equals(user.getType()) ? null : user.getId();
        try {
            int notifiedCount = feedbackService.remindPendingStudents(activityId, staffAccountId, user.getRole());
            request.getSession().setAttribute("feedbackSuccess",
                    "Đã gửi nhắc nhở tới " + notifiedCount + " sinh viên chưa feedback.");
        } catch (ActivityFeedbackUnavailableException e) {
            request.getSession().setAttribute("feedbackError", e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to send activity feedback reminders.", e);
            request.getSession().setAttribute("feedbackError",
                    "Không thể gửi nhắc nhở lúc này. Vui lòng thử lại sau.");
        }
        response.sendRedirect(request.getContextPath()
                + "/activity-feedback?action=report&activityId=" + activityId);
    }

    private void showStudentPage(HttpServletRequest request, HttpServletResponse response, CurrentUser user)
            throws ServletException, IOException {
        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa được liên kết với hồ sơ sinh viên.");
            return;
        }
        try {
            List<FeedbackActivityDTO> activities = feedbackService.getStudentActivities(student.getId());
            request.setAttribute("feedbackActivities", activities);
            request.setAttribute("pendingFeedbackCount", feedbackService.countPending(student.getId()));
            Object success = request.getSession().getAttribute("feedbackSuccess");
            if (success != null) {
                request.setAttribute("successMessage", success.toString());
                request.getSession().removeAttribute("feedbackSuccess");
            }
            String action = request.getParameter("action");
            int activityId = parsePositiveId(request.getParameter("activityId"));
            if ("write".equals(action) && activityId > 0) {
                showFeedbackForm(request, response, student.getId(), activityId, null, null);
                return;
            }
            if ("view".equals(action) && activityId > 0) {
                FeedbackActivityDTO selectedActivity = findActivity(activities, activityId);
                if (selectedActivity == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Chỉ có thể xem feedback của hoạt động bạn đã tham gia và đã kết thúc.");
                    return;
                }
                ActivityFeedbackAnswerDTO submitted = feedbackService.getStudentFeedback(student.getId(), activityId);
                if (submitted == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy feedback của hoạt động này.");
                    return;
                }
                request.setAttribute("selectedFeedback", submitted);
                request.setAttribute("selectedActivity", selectedActivity);
                request.getRequestDispatcher("/WEB-INF/views/feedback/student-form.jsp").forward(request, response);
                return;
            }
            request.getRequestDispatcher("/WEB-INF/views/feedback/student-list.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load student activity feedback.", e);
            request.setAttribute("errorMessage", "Không thể tải danh sách feedback. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    private void showFeedbackForm(HttpServletRequest request, HttpServletResponse response, int studentId,
                                  int activityId, ActivityFeedbackAnswerDTO submitted, String error)
            throws ServletException, IOException {
        List<FeedbackActivityDTO> activities = feedbackService.getStudentActivities(studentId);
        FeedbackActivityDTO selectedActivity = findActivity(activities, activityId);
        if (selectedActivity == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Chỉ có thể feedback hoạt động bạn đã tham gia và đã kết thúc.");
            return;
        }
        ActivityFeedbackAnswerDTO existing = feedbackService.getStudentFeedback(studentId, activityId);
        if (existing != null) {
            request.setAttribute("selectedFeedback", existing);
        } else {
            request.setAttribute("feedbackForm", submitted == null ? new ActivityFeedbackAnswerDTO() : submitted);
        }
        request.setAttribute("selectedActivity", selectedActivity);
        request.setAttribute("formError", error);
        request.getRequestDispatcher("/WEB-INF/views/feedback/student-form.jsp").forward(request, response);
    }

    private void showStudentPage(HttpServletRequest request, HttpServletResponse response, CurrentUser user,
                                 int activityId, ActivityFeedbackAnswerDTO submitted, String error)
            throws ServletException, IOException {
        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa được liên kết với hồ sơ sinh viên.");
            return;
        }
        try {
            showFeedbackForm(request, response, student.getId(), activityId, submitted, error);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to reload activity feedback form.", e);
            request.setAttribute("errorMessage", "Không thể tải biểu mẫu feedback. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    private void showManagementPage(HttpServletRequest request, HttpServletResponse response, CurrentUser user)
            throws ServletException, IOException {
        Integer staffAccountId = "ADMIN".equals(user.getType()) ? null : user.getId();
        try {
            transferFlashMessage(request, "feedbackSuccess", "successMessage");
            transferFlashMessage(request, "feedbackError", "errorMessage");
            int activityId = parsePositiveId(request.getParameter("activityId"));
            if ("report".equals(request.getParameter("action")) && activityId > 0) {
                String roleFilter = request.getParameter("role");
                ActivityFeedbackReportDTO report =
                        feedbackService.getActivityReport(activityId, staffAccountId, user.getRole(), roleFilter);
                if (report == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy báo cáo feedback hoạt động.");
                    return;
                }
                request.setAttribute("roleFilter", "ORGANIZER".equals(roleFilter) || "PARTICIPANT".equals(roleFilter)
                        ? roleFilter : "");
                request.setAttribute("feedbackReport", report);
                request.getRequestDispatcher("/WEB-INF/views/feedback/management-report.jsp").forward(request, response);
                return;
            }
            request.setAttribute("feedbackSummaries",
                    feedbackService.getActivitySummaries(staffAccountId, user.getRole()));
            request.setAttribute("feedbackSemesters", semesterService.getAllSemesters().stream()
                    .sorted(Comparator.comparing(Semester::getName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList());
            request.setAttribute("feedbackDepartments", departmentService.getAllDepartments().stream()
                    .sorted(Comparator.comparing(Department::getName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList());
            request.getRequestDispatcher("/WEB-INF/views/feedback/management-list.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load activity feedback management.", e);
            request.setAttribute("errorMessage", "Không thể tải báo cáo feedback. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    private static void transferFlashMessage(HttpServletRequest request, String sessionKey, String requestKey) {
        Object message = request.getSession().getAttribute(sessionKey);
        if (message != null) {
            request.setAttribute(requestKey, message.toString());
            request.getSession().removeAttribute(sessionKey);
        }
    }

    private void writeReminderCount(CurrentUser user, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            Student student = studentService.getStudentByAccountId(user.getId());
            if (student == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"Tài khoản chưa được liên kết với hồ sơ sinh viên.\"}");
                return;
            }
            int count = feedbackService.countPending(student.getId());
            response.getWriter().write("{\"count\":" + count + "}");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to load pending feedback reminder count.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Không thể tải số feedback đang chờ.\"}");
        }
    }

    private static FeedbackActivityDTO findActivity(List<FeedbackActivityDTO> activities, int activityId) {
        return activities.stream()
                .filter(activity -> activity.getActivityId() == activityId)
                .findFirst().orElse(null);
    }

    private static ActivityFeedbackAnswerDTO readAnswer(HttpServletRequest request) {
        ActivityFeedbackAnswerDTO answer = new ActivityFeedbackAnswerDTO();
        answer.setGeneralRating(parseRating(request.getParameter("generalRating")));
        answer.setContentRating(parseRating(request.getParameter("contentRating")));
        answer.setOrganizationRating(parseRating(request.getParameter("organizationRating")));
        answer.setVenueRating(parseRating(request.getParameter("venueRating")));
        answer.setBestPart(request.getParameter("bestPart"));
        answer.setImprovement(request.getParameter("improvement"));
        answer.setFavorite(request.getParameter("favorite"));
        answer.setSuggestions(request.getParameter("suggestions"));
        answer.setNote(request.getParameter("note"));
        return answer;
    }

    private static int parseRating(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parsePositiveId(String value) {
        try {
            int id = Integer.parseInt(value);
            return id > 0 ? id : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
