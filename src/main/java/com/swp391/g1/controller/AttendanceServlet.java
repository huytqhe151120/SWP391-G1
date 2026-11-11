package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@WebServlet(name = "AttendanceServlet", urlPatterns = {"/attendance"})
public class AttendanceServlet extends HttpServlet {

    private static final String STATUS_PARAM_PREFIX = "status_";
    private static final Set<String> POST_ACTIONS = Set.of(
            "save", "saveAjax", "saveStudentSession", "batchGrade", "batchGradeAjax", "saveGradeAjax",
            "addStudents", "addGroup", "remove");
    private static final Set<String> AJAX_ACTIONS = Set.of("saveAjax", "batchGradeAjax", "saveGradeAjax");
    private static final Set<String> ADD_ACTIONS = Set.of("addStudents", "addGroup");
    private static final List<Integer> PAGE_SIZES = List.of(30, 40, 50);
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final IExtracurricularActivityService activityService;
    private final IActivityCollaborationInvitationService invitationService;
    private final IActivityParticipantService participantService;

    public AttendanceServlet() {
        this.activityService = new ExtracurricularActivityServiceImpl();
        this.invitationService = new ActivityCollaborationInvitationServiceImpl();
        this.participantService = new ActivityParticipantServiceImpl();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!AccessPolicy.canTakeAttendance(AuthContext.getCurrentUser(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ tài khoản ADMIN, STAFF hoặc PARTNER_STAFF mới được điểm danh sinh viên.");
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
            String action = request.getParameter("action");
            if ("detail".equals(action)) {
                handleParticipantDetailGet(request, response, activities, activityId);
                return;
            }
            // The add-students form lives inside the list page; action=add only opens it.
            boolean openAddPanel = "add".equals(action);
            if (openAddPanel && activityId <= 0) {
                response.sendRedirect(request.getContextPath() + "/attendance");
                return;
            }
            if (activityId > 0) {
                ExtracurricularActivityResponseDTO selectedActivity = findById(activities, activityId);
                if (selectedActivity == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Bạn không có quyền điểm danh hoạt động này.");
                    return;
                }
                int totalSessions = Math.max(1, selectedActivity.getAttendanceSessionCount());
                int requestedSession = parseId(request.getParameter("sessionNumber"));
                int selectedSessionNumber = requestedSession <= 0
                        ? 1
                        : Math.min(requestedSession, totalSessions);

                List<ActivityParticipantResponseDTO> allParticipants =
                        participantService.getParticipants(activityId, selectedSessionNumber);
                Map<ParticipantStatus, Long> statusCounts = countByStatus(allParticipants);
                Set<String> departments = new TreeSet<>();
                Set<String> programs = new TreeSet<>();
                Set<String> majors = new TreeSet<>();
                Set<String> classes = new TreeSet<>();
                for (ActivityParticipantResponseDTO participant : allParticipants) {
                    addIfPresent(departments, participant.getDepartmentName());
                    addIfPresent(programs, participant.getAcademicProgramName());
                    addIfPresent(majors, participant.getMajorName());
                    addIfPresent(classes, participant.getMainClassCode());
                }

                ParticipantFilter filter = ParticipantFilter.from(request);
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
                request.setAttribute("selectedSessionNumber", selectedSessionNumber);
                request.setAttribute("totalSessions", totalSessions);
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
                request.setAttribute("statusCounts", statusCounts);
                request.setAttribute("canTakeAttendance", participantService.canTakeAttendance(selectedActivity));
                request.setAttribute("departments", departments);
                request.setAttribute("academicPrograms", programs);
                request.setAttribute("majors", majors);
                request.setAttribute("mainClasses", classes);
                setFilterAttributes(request, filter);
                boolean canManageParticipants = AccessPolicy.canManageParticipants(currentUser)
                        && participantService.canManageParticipants(selectedActivity);
                request.setAttribute("canManageParticipants", canManageParticipants);
                if (canManageParticipants) {
                    setCandidateAttributes(request, participantService.getCandidates(activityId));
                    request.setAttribute("openAddPanel", openAddPanel);
                } else if (openAddPanel) {
                    if (!AccessPolicy.canManageParticipants(currentUser)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Bạn không có quyền thêm hoặc xóa sinh viên trong danh sách tham gia.");
                        return;
                    }
                    request.getSession().setAttribute("errorMessage",
                            "Không thể thay đổi danh sách sinh viên của hoạt động đã hủy.");
                    response.sendRedirect(request.getContextPath() + "/attendance?activityId=" + activityId
                            + "&sessionNumber=" + selectedSessionNumber
                            + filter.toQuery() + pagingQuery(currentPage, pageSize));
                    return;
                }
            }
            request.setAttribute("participantStatuses", ParticipantStatus.values());
            request.getRequestDispatcher("/WEB-INF/views/attendance/list.jsp").forward(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    private void handleParticipantDetailGet(HttpServletRequest request, HttpServletResponse response,
            List<ExtracurricularActivityResponseDTO> activities, int activityId)
            throws ServletException, IOException {
        if (activityId <= 0) {
            response.sendRedirect(request.getContextPath() + "/attendance");
            return;
        }
        ExtracurricularActivityResponseDTO selectedActivity = findById(activities, activityId);
        if (selectedActivity == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền xem chi tiết điểm danh của hoạt động này.");
            return;
        }
        int totalSessions = Math.max(1, selectedActivity.getAttendanceSessionCount());
        int requestedSession = parseId(request.getParameter("sessionNumber"));
        int selectedSessionNumber = requestedSession <= 0
                ? 1
                : Math.min(requestedSession, totalSessions);

        List<ActivityParticipantResponseDTO> allParticipants =
                participantService.getParticipants(activityId, selectedSessionNumber);
        int participantId = parseId(request.getParameter("participantId"));
        int studentId = parseId(request.getParameter("studentId"));

        ActivityParticipantResponseDTO selectedParticipant = null;
        for (ActivityParticipantResponseDTO p : allParticipants) {
            if ((participantId > 0 && p.getParticipantId() == participantId)
                    || (participantId <= 0 && studentId > 0 && p.getStudentId() == studentId)) {
                selectedParticipant = p;
                break;
            }
        }
        if (selectedParticipant == null && !allParticipants.isEmpty()) {
            selectedParticipant = allParticipants.get(0);
        }
        if (selectedParticipant == null) {
            request.getSession().setAttribute("errorMessage",
                    "Không tìm thấy sinh viên trong danh sách điểm danh của hoạt động này.");
            response.sendRedirect(request.getContextPath() + "/attendance?activityId=" + activityId
                    + "&sessionNumber=" + selectedSessionNumber);
            return;
        }

        request.setAttribute("selectedActivity", selectedActivity);
        request.setAttribute("selectedActivityId", activityId);
        request.setAttribute("selectedSessionNumber", selectedSessionNumber);
        request.setAttribute("totalSessions", totalSessions);
        request.setAttribute("participant", selectedParticipant);
        request.setAttribute("allParticipants", allParticipants);
        request.setAttribute("sessionAttendances",
                participantService.getSessionAttendances(activityId, selectedParticipant.getParticipantId(), totalSessions));
        request.setAttribute("attendanceHistory",
                participantService.getAttendanceHistory(activityId, selectedParticipant.getParticipantId()));
        request.setAttribute("canTakeAttendance", participantService.canTakeAttendance(selectedActivity));
        request.setAttribute("participantStatuses", ParticipantStatus.values());
        request.getRequestDispatcher("/WEB-INF/views/attendance/detail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        int activityId = parseId(request.getParameter("activityId"));
        int sessionNumber = Math.max(1, parseId(request.getParameter("sessionNumber")));
        String action = request.getParameter("action");
        boolean isAjax = action != null && AJAX_ACTIONS.contains(action);
        // Set.of(...).contains(null) throws, so check null first.
        if (action == null || !POST_ACTIONS.contains(action) || activityId <= 0) {
            if (isAjax) {
                json(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"ok\":false,\"error\":\"Yêu cầu điểm danh không hợp lệ.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/attendance");
            }
            return;
        }

        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        if ((ADD_ACTIONS.contains(action) || "remove".equals(action))
                && !AccessPolicy.canManageParticipants(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền thêm hoặc xóa sinh viên trong danh sách tham gia.");
            return;
        }
        if (findById(getVisibleActivities(currentUser), activityId) == null) {
            if (isAjax) {
                json(response, HttpServletResponse.SC_FORBIDDEN,
                        "{\"ok\":false,\"error\":\"Bạn không có quyền điểm danh hoạt động này.\"}");
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền điểm danh hoạt động này.");
            }
            return;
        }

        if ("saveAjax".equals(action)) {
            saveAttendanceAjax(request, response, activityId, sessionNumber, currentUser);
            return;
        }
        if ("batchGradeAjax".equals(action) || "saveGradeAjax".equals(action)) {
            saveGradesAjax(request, response, activityId, sessionNumber, currentUser);
            return;
        }

        HttpSession session = request.getSession();
        boolean failed = false;
        try {
            switch (action) {
                case "save" -> {
                    int changed = participantService.saveAttendance(activityId, sessionNumber, readStatuses(request), currentUser);
                    session.setAttribute("successMessage", changed == 0
                            ? "Không có thay đổi nào về trạng thái điểm danh buổi " + sessionNumber + "."
                            : "Đã cập nhật điểm danh buổi " + sessionNumber + " cho " + changed + " sinh viên.");
                }
                case "saveStudentSession" -> {
                    int participantId = parseId(request.getParameter("participantId"));
                    int targetSession = Math.max(1, parseId(request.getParameter("targetSessionNumber")));
                    String newStatus = request.getParameter("attendanceStatusValue");
                    Map<Integer, String> statusMap = new LinkedHashMap<>();
                    if (participantId > 0 && newStatus != null) {
                        statusMap.put(participantId, newStatus);
                    }
                    int changed = participantService.saveAttendance(activityId, targetSession, statusMap, currentUser);
                    session.setAttribute("successMessage", changed == 0
                            ? "Trạng thái điểm danh buổi " + targetSession + " không thay đổi."
                            : "Đã cập nhật điểm danh buổi " + targetSession + " của sinh viên.");
                    response.sendRedirect(request.getContextPath() + "/attendance?action=detail&activityId=" + activityId
                            + "&participantId=" + participantId + "&sessionNumber=" + sessionNumber);
                    return;
                }
                case "batchGrade" -> {
                    List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
                    BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
                    String feedback = request.getParameter("feedback");
                    int graded = participantService.saveParticipationGrades(
                            activityId, participantIds, evalScore, feedback, currentUser);
                    session.setAttribute("successMessage", graded == 0
                            ? "Không có sinh viên có mặt nào trong danh sách chọn để chấm điểm tham gia."
                            : "Đã chấm điểm đánh giá tham gia cho " + graded + " sinh viên.");
                }
                case "addStudents" -> setAddedMessage(session, participantService.addStudents(activityId,
                        parseIds(request.getParameterValues("studentId")),
                        currentUser == null ? 1 : currentUser.getId()));
                case "addGroup" -> setAddedMessage(session, participantService.addStudentsByGroup(activityId,
                        parseId(request.getParameter("groupDepartmentId")),
                        parseId(request.getParameter("groupMajorId")),
                        parseId(request.getParameter("groupProgramId")),
                        parseId(request.getParameter("groupClassId")),
                        currentUser == null ? 1 : currentUser.getId()));
                case "remove" -> {
                    List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
                    int removed = participantService.removeParticipants(activityId, participantIds);
                    String message = "Đã xóa " + removed + " sinh viên khỏi danh sách.";
                    if (removed < participantIds.size()) {
                        message += " Bỏ qua " + (participantIds.size() - removed)
                                + " sinh viên đã được điểm danh.";
                    }
                    session.setAttribute("successMessage", message);
                }
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
            failed = true;
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật danh sách: " + e.getMessage());
            failed = true;
        }
        if ("saveStudentSession".equals(action)) {
            int participantId = parseId(request.getParameter("participantId"));
            response.sendRedirect(request.getContextPath() + "/attendance?action=detail&activityId=" + activityId
                    + "&participantId=" + participantId + "&sessionNumber=" + sessionNumber);
            return;
        }
        // A failed add reopens the add panel of the list page; everything else just returns to the list.
        // Both keep the filters the user was working with.
        boolean backToAddPage = failed && ADD_ACTIONS.contains(action);
        response.sendRedirect(request.getContextPath() + "/attendance?"
                + (backToAddPage ? "action=add&" : "") + "activityId=" + activityId
                + "&sessionNumber=" + sessionNumber
                + ParticipantFilter.from(request).toQuery()
                + pagingQuery(parseId(request.getParameter("page")), parsePageSize(request.getParameter("pageSize"))));
    }

    private void saveAttendanceAjax(HttpServletRequest request, HttpServletResponse response,
            int activityId, int sessionNumber, CurrentUser currentUser) throws IOException {
        try {
            Map<Integer, String> statuses = readStatuses(request);
            int changed = participantService.saveAttendance(activityId, sessionNumber, statuses, currentUser);
            List<ActivityParticipantResponseDTO> allParticipants = participantService.getParticipants(activityId, sessionNumber);
            Map<ParticipantStatus, Long> counts = countByStatus(allParticipants);
            StringBuilder body = new StringBuilder("{\"ok\":true,\"changed\":").append(changed).append(",\"counts\":{");
            boolean first = true;
            for (ParticipantStatus status : ParticipantStatus.values()) {
                if (!first) {
                    body.append(',');
                }
                first = false;
                body.append('"').append(status.name()).append("\":").append(counts.getOrDefault(status, 0L));
            }
            body.append("},\"participants\":{");
            appendParticipantScoresJson(body, allParticipants, statuses.keySet());
            body.append("}}");
            json(response, HttpServletResponse.SC_OK, body.toString());
        } catch (IllegalArgumentException e) {
            json(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            json(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"ok\":false,\"error\":\"Đã xảy ra lỗi khi lưu điểm danh.\"}");
        }
    }

    private void saveGradesAjax(HttpServletRequest request, HttpServletResponse response,
            int activityId, int sessionNumber, CurrentUser currentUser) throws IOException {
        try {
            List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
            BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
            String feedback = request.getParameter("feedback");
            int updated = participantService.saveParticipationGrades(
                    activityId, participantIds, evalScore, feedback, currentUser);
            List<ActivityParticipantResponseDTO> allParticipants = participantService.getParticipants(activityId, sessionNumber);
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
                    .append(",\"removable\":").append(p.isRemovable())
                    .append(",\"sessionAttendanceScore\":\"").append(escapeJson(p.getFormattedSessionAttendanceScore())).append('"')
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

    private Map<ParticipantStatus, Long> countByStatus(List<ActivityParticipantResponseDTO> participants) {
        Map<ParticipantStatus, Long> statusCounts = new EnumMap<>(ParticipantStatus.class);
        for (ParticipantStatus status : ParticipantStatus.values()) {
            statusCounts.put(status, 0L);
        }
        for (ActivityParticipantResponseDTO participant : participants) {
            statusCounts.merge(participant.getSessionAttendanceStatus(), 1L, Long::sum);
        }
        return statusCounts;
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

    /** Query string fragment that keeps the current page; the page number is clamped when the list is shown. */
    private String pagingQuery(int page, int pageSize) {
        return "&page=" + Math.max(page, 1) + "&pageSize=" + pageSize;
    }

    private int parsePageSize(String value) {
        int pageSize = parseId(value);
        return PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    private void setFilterAttributes(HttpServletRequest request, ParticipantFilter filter) {
        request.setAttribute("keyword", filter.keyword);
        request.setAttribute("selectedDepartment", filter.department);
        request.setAttribute("selectedAcademicProgram", filter.academicProgram);
        request.setAttribute("selectedMajor", filter.major);
        request.setAttribute("selectedMainClass", filter.mainClass);
        request.setAttribute("selectedAttendanceStatus", filter.attendanceStatus);
        request.setAttribute("filtered", filter.activeCount() > 0);
        request.setAttribute("activeFilterCount", filter.activeCount());
        // Already URL-encoded, so it can be appended to links as is.
        request.setAttribute("filterQuery", filter.toQuery());
    }

    private void setAddedMessage(HttpSession session, int added) {
        session.setAttribute("successMessage", added == 0
                ? "Không có sinh viên mới nào được thêm."
                : "Đã thêm " + added + " sinh viên vào danh sách.");
    }

    /** Builds the candidate list and the Khoa / Ngành / CTĐT / Lớp options of the add-by-group form. */
    private void setCandidateAttributes(HttpServletRequest request, List<ParticipantCandidateDTO> candidates) {
        Map<Integer, GroupOption> departments = new HashMap<>();
        Map<Integer, GroupOption> majors = new HashMap<>();
        Map<Integer, GroupOption> programs = new HashMap<>();
        Map<Integer, GroupOption> classes = new HashMap<>();
        for (ParticipantCandidateDTO c : candidates) {
            if (c.getDepartmentId() > 0) {
                departments.computeIfAbsent(c.getDepartmentId(), id -> new GroupOption(id,
                        c.getDepartmentName(), id, 0, 0)).count++;
            }
            if (c.getMajorId() > 0) {
                majors.computeIfAbsent(c.getMajorId(), id -> new GroupOption(id,
                        c.getMajorName(), c.getDepartmentId(), id, 0)).count++;
            }
            if (c.getAcademicProgramId() > 0) {
                programs.computeIfAbsent(c.getAcademicProgramId(), id -> new GroupOption(id,
                        c.getAcademicProgramName(), c.getDepartmentId(), c.getMajorId(), id)).count++;
            }
            if (c.getMainClassId() > 0) {
                classes.computeIfAbsent(c.getMainClassId(), id -> new GroupOption(id,
                        c.getMainClassCode(), c.getDepartmentId(), c.getMajorId(),
                        c.getAcademicProgramId())).count++;
            }
        }
        request.setAttribute("candidates", candidates);
        request.setAttribute("groupDepartments", sortedByName(departments));
        request.setAttribute("groupMajors", sortedByName(majors));
        request.setAttribute("groupPrograms", sortedByName(programs));
        request.setAttribute("groupClasses", sortedByName(classes));
    }

    private List<GroupOption> sortedByName(Map<Integer, GroupOption> options) {
        List<GroupOption> list = new ArrayList<>(options.values());
        list.sort(Comparator.comparing(GroupOption::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
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

    /** Collects status_<participantId> parameters into participantId -> status name. */
    private Map<Integer, String> readStatuses(HttpServletRequest request) {
        Map<Integer, String> statuses = new LinkedHashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith(STATUS_PARAM_PREFIX)) {
                int participantId = parseId(name.substring(STATUS_PARAM_PREFIX.length()));
                if (participantId > 0) {
                    statuses.put(participantId, request.getParameter(name));
                }
            }
        }
        return statuses;
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

    /**
     * One Khoa / Ngành / CTĐT / Lớp option of the add-by-group form. The parent ids drive the cascading
     * selects and count is the number of candidates in the group.
     */
    public static final class GroupOption {
        private final int id;
        private final String name;
        private final int departmentId;
        private final int majorId;
        private final int programId;
        private int count;

        private GroupOption(int id, String name, int departmentId, int majorId, int programId) {
            this.id = id;
            this.name = name == null || name.isBlank() ? "#" + id : name.trim();
            this.departmentId = departmentId;
            this.majorId = majorId;
            this.programId = programId;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getDepartmentId() {
            return departmentId;
        }

        public int getMajorId() {
            return majorId;
        }

        public int getProgramId() {
            return programId;
        }

        public int getCount() {
            return count;
        }
    }

    /** Search and filter criteria for the participant table, read from request parameters. */
    private static final class ParticipantFilter {
        private final String keyword;
        private final String department;
        private final String academicProgram;
        private final String major;
        private final String mainClass;
        private final String attendanceStatus;
        private final String normalizedKeyword;

        private ParticipantFilter(HttpServletRequest request) {
            this.keyword = trimToEmpty(request.getParameter("keyword"));
            this.department = trimToEmpty(request.getParameter("department"));
            this.academicProgram = trimToEmpty(request.getParameter("academicProgram"));
            this.major = trimToEmpty(request.getParameter("major"));
            this.mainClass = trimToEmpty(request.getParameter("mainClass"));
            this.attendanceStatus = parseStatusName(request.getParameter("attendanceStatus"));
            this.normalizedKeyword = normalizeText(keyword);
        }

        static ParticipantFilter from(HttpServletRequest request) {
            return new ParticipantFilter(request);
        }

        boolean matches(ActivityParticipantResponseDTO participant) {
            if (!normalizedKeyword.isEmpty()
                    && !normalizeText(participant.getStudentCode()).contains(normalizedKeyword)
                    && !normalizeText(participant.getStudentName()).contains(normalizedKeyword)) {
                return false;
            }
            return matchesValue(department, participant.getDepartmentName())
                    && matchesValue(academicProgram, participant.getAcademicProgramName())
                    && matchesValue(major, participant.getMajorName())
                    && matchesValue(mainClass, participant.getMainClassCode())
                    && (attendanceStatus.isEmpty()
                            || attendanceStatus.equals(String.valueOf(participant.getSessionAttendanceStatus())));
        }

        int activeCount() {
            return (normalizedKeyword.isEmpty() ? 0 : 1) + (department.isEmpty() ? 0 : 1)
                    + (academicProgram.isEmpty() ? 0 : 1) + (major.isEmpty() ? 0 : 1)
                    + (mainClass.isEmpty() ? 0 : 1) + (attendanceStatus.isEmpty() ? 0 : 1);
        }

        String toQuery() {
            StringBuilder query = new StringBuilder();
            appendParam(query, "keyword", keyword);
            appendParam(query, "department", department);
            appendParam(query, "academicProgram", academicProgram);
            appendParam(query, "major", major);
            appendParam(query, "mainClass", mainClass);
            appendParam(query, "attendanceStatus", attendanceStatus);
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

        private static String parseStatusName(String value) {
            String candidate = trimToEmpty(value);
            for (ParticipantStatus status : ParticipantStatus.values()) {
                if (status.name().equals(candidate)) {
                    return candidate;
                }
            }
            return "";
        }
    }
}
