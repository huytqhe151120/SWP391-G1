package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Enum.ParticipantStatus;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityCollaborationInvitationService;
import com.swp391.g1.service.IActivityParticipantService;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.impl.ActivityCollaborationInvitationServiceImpl;
import com.swp391.g1.service.impl.ActivityParticipantServiceImpl;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@WebServlet(name = "ParticipationGradingServlet", urlPatterns = {"/participation-grading"})
public class ParticipationGradingServlet extends HttpServlet {

    private static final Set<String> POST_ACTIONS = Set.of("batchGrade", "batchGradeAjax", "saveGradeAjax");
    private static final Set<String> AJAX_ACTIONS = Set.of("batchGradeAjax", "saveGradeAjax");
    private static final List<Integer> PAGE_SIZES = List.of(30, 40, 50);
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final IExtracurricularActivityService activityService;
    private final IActivityCollaborationInvitationService invitationService;
    private final IActivityParticipantService participantService;

    public ParticipationGradingServlet() {
        this.activityService = new ExtracurricularActivityServiceImpl();
        this.invitationService = new ActivityCollaborationInvitationServiceImpl();
        this.participantService = new ActivityParticipantServiceImpl();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!AccessPolicy.canTakeAttendance(AuthContext.getCurrentUser(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ tài khoản ADMIN, STAFF hoặc PARTNER_STAFF mới được chấm điểm tham gia.");
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CurrentUser currentUser = AuthContext.getCurrentUser(request);
            List<ExtracurricularActivityResponseDTO> activities = getVisibleActivities(currentUser);
            request.setAttribute("activities", activities);

            int activityId = parseId(request.getParameter("activityId"));
            if (activityId > 0) {
                ExtracurricularActivityResponseDTO selectedActivity = findById(activities, activityId);
                if (selectedActivity == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Bạn không có quyền chấm điểm tham gia hoạt động này.");
                    return;
                }
                List<ActivityParticipantResponseDTO> allParticipants =
                        participantService.getParticipants(activityId, 1);

                long eligibleCount = 0;
                long gradedCount = 0;
                long ungradedCount = 0;
                long ineligibleCount = 0;

                Set<String> departments = new TreeSet<>();
                Set<String> programs = new TreeSet<>();
                Set<String> majors = new TreeSet<>();
                Set<String> classes = new TreeSet<>();

                for (ActivityParticipantResponseDTO participant : allParticipants) {
                    addIfPresent(departments, participant.getDepartmentName());
                    addIfPresent(programs, participant.getAcademicProgramName());
                    addIfPresent(majors, participant.getMajorName());
                    addIfPresent(classes, participant.getMainClassCode());

                    if (participant.isPresentStatus()) {
                        eligibleCount++;
                        if (participant.isGraded()) {
                            gradedCount++;
                        } else {
                            ungradedCount++;
                        }
                    } else {
                        ineligibleCount++;
                    }
                }

                GradingFilter filter = GradingFilter.from(request);
                List<ActivityParticipantResponseDTO> filteredParticipants = new ArrayList<>();
                for (ActivityParticipantResponseDTO participant : allParticipants) {
                    if (filter.matches(participant)) {
                        filteredParticipants.add(participant);
                    }
                }

                int pageSize = parsePageSize(request.getParameter("pageSize"));
                int filteredCount = filteredParticipants.size();
                int totalPages = Math.max(1, (int) Math.ceil((double) filteredCount / pageSize));
                int currentPage = Math.min(Math.max(parseId(request.getParameter("page")), 1), totalPages);
                int fromIndex = Math.min((currentPage - 1) * pageSize, filteredCount);
                int toIndex = Math.min(fromIndex + pageSize, filteredCount);
                List<ActivityParticipantResponseDTO> participants =
                        new ArrayList<>(filteredParticipants.subList(fromIndex, toIndex));

                request.setAttribute("selectedActivity", selectedActivity);
                request.setAttribute("selectedActivityId", activityId);
                request.setAttribute("participants", participants);
                request.setAttribute("filteredParticipantCount", filteredCount);
                request.setAttribute("pageSize", pageSize);
                request.setAttribute("pageSizes", PAGE_SIZES);
                request.setAttribute("currentPage", currentPage);
                request.setAttribute("totalPages", totalPages);
                request.setAttribute("firstParticipantNumber", filteredCount == 0 ? 0 : fromIndex + 1);
                request.setAttribute("lastParticipantNumber", toIndex);
                request.setAttribute("pagingQuery", pagingQuery(currentPage, pageSize));
                request.setAttribute("totalParticipantCount", allParticipants.size());
                request.setAttribute("eligibleCount", eligibleCount);
                request.setAttribute("gradedCount", gradedCount);
                request.setAttribute("ungradedCount", ungradedCount);
                request.setAttribute("ineligibleCount", ineligibleCount);
                request.setAttribute("canTakeAttendance", participantService.canTakeAttendance(selectedActivity));
                request.setAttribute("departments", departments);
                request.setAttribute("academicPrograms", programs);
                request.setAttribute("majors", majors);
                request.setAttribute("mainClasses", classes);
                setFilterAttributes(request, filter);
            }
            request.setAttribute("participantStatuses", ParticipantStatus.values());
            request.getRequestDispatcher("/WEB-INF/views/participation-grading/list.jsp").forward(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        int activityId = parseId(request.getParameter("activityId"));
        String action = request.getParameter("action");
        boolean isAjax = action != null && AJAX_ACTIONS.contains(action);
        if (action == null || !POST_ACTIONS.contains(action) || activityId <= 0) {
            if (isAjax) {
                json(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"ok\":false,\"error\":\"Yêu cầu chấm điểm không hợp lệ.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/participation-grading");
            }
            return;
        }

        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        if (findById(getVisibleActivities(currentUser), activityId) == null) {
            if (isAjax) {
                json(response, HttpServletResponse.SC_FORBIDDEN,
                        "{\"ok\":false,\"error\":\"Bạn không có quyền chấm điểm tham gia hoạt động này.\"}");
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền chấm điểm tham gia hoạt động này.");
            }
            return;
        }

        if (isAjax) {
            saveGradesAjax(request, response, activityId, currentUser);
            return;
        }

        HttpSession session = request.getSession();
        try {
            List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
            BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
            String feedback = request.getParameter("feedback");
            int graded = participantService.saveParticipationGrades(
                    activityId, participantIds, evalScore, feedback, currentUser);
            session.setAttribute("successMessage", graded == 0
                    ? "Không có sinh viên có mặt nào trong danh sách chọn để chấm điểm tham gia."
                    : "Đã chấm điểm đánh giá tham gia cho " + graded + " sinh viên.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi chấm điểm tham gia: " + e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/participation-grading?activityId=" + activityId
                + GradingFilter.from(request).toQuery()
                + pagingQuery(parseId(request.getParameter("page")), parsePageSize(request.getParameter("pageSize"))));
    }

    private void saveGradesAjax(HttpServletRequest request, HttpServletResponse response,
            int activityId, CurrentUser currentUser) throws IOException {
        try {
            List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
            BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
            String feedback = request.getParameter("feedback");
            int updated = participantService.saveParticipationGrades(
                    activityId, participantIds, evalScore, feedback, currentUser);
            List<ActivityParticipantResponseDTO> allParticipants = participantService.getParticipants(activityId, 1);
            StringBuilder body = new StringBuilder("{\"ok\":true,\"updated\":").append(updated)
                    .append(",\"participants\":{");
            appendParticipantScoresJson(body, allParticipants, Set.copyOf(participantIds));
            body.append("}}");
            json(response, HttpServletResponse.SC_OK, body.toString());
        } catch (IllegalArgumentException e) {
            json(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            json(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"ok\":false,\"error\":\"Đã xảy ra lỗi khi lưu điểm tham gia.\"}");
        }
    }

    private void appendParticipantScoresJson(StringBuilder body,
            List<ActivityParticipantResponseDTO> allParticipants, Set<Integer> targetIds) {
        boolean first = true;
        for (ActivityParticipantResponseDTO p : allParticipants) {
            if (targetIds != null && !targetIds.isEmpty() && !targetIds.contains(p.getParticipantId())) {
                continue;
            }
            if (!first) {
                body.append(',');
            }
            first = false;
            body.append('"').append(p.getParticipantId()).append("\":{")
                    .append("\"present\":").append(p.isPresentStatus())
                    .append(",\"attendedSessionCount\":").append(p.getAttendedSessionCount())
                    .append(",\"totalSessions\":").append(p.getTotalSessions())
                    .append(",\"attendanceScore\":\"").append(escapeJson(p.getFormattedAttendanceScore())).append('"')
                    .append(",\"evalScore\":\"").append(escapeJson(p.getFormattedEvalScore())).append('"')
                    .append(",\"feedback\":\"").append(escapeJson(p.getParticipationFeedback())).append('"')
                    .append(",\"gradedBy\":\"").append(escapeJson(p.getParticipationGradedBy())).append('"')
                    .append(",\"participationScore\":\"").append(escapeJson(p.getFormattedParticipationScore())).append('"')
                    .append(",\"finalPoint\":\"").append(escapeJson(p.getFormattedFinalPoint())).append('"')
                    .append('}');
        }
    }

    private BigDecimal parseScore(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập điểm đánh giá tham gia (0 - 5).");
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Điểm đánh giá tham gia không hợp lệ.");
        }
    }

    private static void json(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String pagingQuery(int page, int pageSize) {
        return "&page=" + Math.max(page, 1) + "&pageSize=" + pageSize;
    }

    private int parsePageSize(String value) {
        int pageSize = parseId(value);
        return PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    private void setFilterAttributes(HttpServletRequest request, GradingFilter filter) {
        request.setAttribute("keyword", filter.keyword);
        request.setAttribute("selectedDepartment", filter.department);
        request.setAttribute("selectedAcademicProgram", filter.academicProgram);
        request.setAttribute("selectedMajor", filter.major);
        request.setAttribute("selectedMainClass", filter.mainClass);
        request.setAttribute("selectedGradingStatus", filter.gradingStatus);
        request.setAttribute("filtered", filter.activeCount() > 0);
        request.setAttribute("activeFilterCount", filter.activeCount());
        request.setAttribute("filterQuery", filter.toQuery());
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private List<ExtracurricularActivityResponseDTO> getVisibleActivities(CurrentUser user) {
        Set<Integer> visibleActivityIds = invitationService.getAttendanceActivityIds(user);
        List<ExtracurricularActivityResponseDTO> activities = new ArrayList<>();
        for (ExtracurricularActivityResponseDTO activity : activityService.getActivitiesVisibleTo(user)) {
            if (visibleActivityIds == null || visibleActivityIds.contains(activity.getId())) {
                activities.add(activity);
            }
        }
        return activities;
    }

    private ExtracurricularActivityResponseDTO findById(List<ExtracurricularActivityResponseDTO> activities,
            int activityId) {
        for (ExtracurricularActivityResponseDTO activity : activities) {
            if (activity.getId() == activityId) {
                return activity;
            }
        }
        return null;
    }

    private List<Integer> parseIds(String[] values) {
        List<Integer> ids = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                int id = parseId(value);
                if (id > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private int parseId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final class GradingFilter {
        private final String keyword;
        private final String department;
        private final String academicProgram;
        private final String major;
        private final String mainClass;
        private final String gradingStatus;
        private final String normalizedKeyword;

        private GradingFilter(HttpServletRequest request) {
            this.keyword = trimToEmpty(request.getParameter("keyword"));
            this.department = trimToEmpty(request.getParameter("department"));
            this.academicProgram = trimToEmpty(request.getParameter("academicProgram"));
            this.major = trimToEmpty(request.getParameter("major"));
            this.mainClass = trimToEmpty(request.getParameter("mainClass"));
            this.gradingStatus = parseGradingStatus(request.getParameter("gradingStatus"));
            this.normalizedKeyword = normalizeText(keyword);
        }

        static GradingFilter from(HttpServletRequest request) {
            return new GradingFilter(request);
        }

        boolean matches(ActivityParticipantResponseDTO participant) {
            if (!normalizedKeyword.isEmpty()
                    && !normalizeText(participant.getStudentCode()).contains(normalizedKeyword)
                    && !normalizeText(participant.getStudentName()).contains(normalizedKeyword)) {
                return false;
            }
            if (!matchesValue(department, participant.getDepartmentName())
                    || !matchesValue(academicProgram, participant.getAcademicProgramName())
                    || !matchesValue(major, participant.getMajorName())
                    || !matchesValue(mainClass, participant.getMainClassCode())) {
                return false;
            }
            return switch (gradingStatus) {
                case "ELIGIBLE" -> participant.isPresentStatus();
                case "GRADED" -> participant.isPresentStatus() && participant.isGraded();
                case "UNGRADED" -> participant.isPresentStatus() && !participant.isGraded();
                case "INELIGIBLE" -> !participant.isPresentStatus();
                default -> true;
            };
        }

        int activeCount() {
            return (normalizedKeyword.isEmpty() ? 0 : 1) + (department.isEmpty() ? 0 : 1)
                    + (academicProgram.isEmpty() ? 0 : 1) + (major.isEmpty() ? 0 : 1)
                    + (mainClass.isEmpty() ? 0 : 1) + (gradingStatus.isEmpty() ? 0 : 1);
        }

        String toQuery() {
            StringBuilder query = new StringBuilder();
            appendParam(query, "keyword", keyword);
            appendParam(query, "department", department);
            appendParam(query, "academicProgram", academicProgram);
            appendParam(query, "major", major);
            appendParam(query, "mainClass", mainClass);
            appendParam(query, "gradingStatus", gradingStatus);
            return query.toString();
        }

        private static boolean matchesValue(String selected, String value) {
            return selected.isEmpty() || (value != null && selected.equals(value.trim()));
        }

        private static void appendParam(StringBuilder query, String name, String value) {
            if (!value.isEmpty()) {
                query.append('&').append(name).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }

        private static String trimToEmpty(String value) {
            return value == null ? "" : value.trim();
        }

        private static String normalizeText(String value) {
            if (value == null) {
                return "";
            }
            return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace('đ', 'd').replace('Đ', 'D')
                    .toLowerCase(Locale.ROOT);
        }

        private static String parseGradingStatus(String value) {
            String candidate = trimToEmpty(value);
            return switch (candidate) {
                case "ELIGIBLE", "GRADED", "UNGRADED", "INELIGIBLE" -> candidate;
                default -> "";
            };
        }
    }
}
