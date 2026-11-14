package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.RecruitmentApplication;
import com.swp391.g1.model.RecruitmentInterview;
import com.swp391.g1.model.RecruitmentPosition;
import com.swp391.g1.model.RecruitmentQuestion;
import com.swp391.g1.model.RecruitmentRound;
import com.swp391.g1.model.RecruitmentStatus;
import com.swp391.g1.model.RegistrationApplication;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IRecruitmentService;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.IRegistrationFormService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.RecruitmentServiceImpl;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tuyển ban tổ chức.
 *
 *   Sinh viên: GET /organizer-recruitment, ?action=apply&roundId=, ?action=myApplications[&id=]
 *              POST action=apply | withdraw | confirm
 *   Staff/Admin: GET /organizer-recruitment, ?action=create, ?action=edit&id=, ?action=applications
 *              POST action=save | open | close | delete
 */
@WebServlet("/organizer-recruitment")
public class OrganizerRecruitmentServlet extends HttpServlet {

    private static final String VIEW_DIR = "/WEB-INF/views/recruitment/";
    private static final String PAGE = "/organizer-recruitment";
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private static final Map<String, String> APP_LABELS = Map.of(
            RecruitmentStatus.APP_PENDING, "Chờ xét",
            RecruitmentStatus.APP_INTERVIEW_INVITED, "Mời phỏng vấn",
            RecruitmentStatus.APP_INTERVIEWED, "Đã phỏng vấn",
            RecruitmentStatus.APP_ACCEPTED, "Trúng tuyển",
            RecruitmentStatus.APP_REJECTED, "Không đạt",
            RecruitmentStatus.APP_WITHDRAWN, "Đã rút");
    private static final Map<String, String> APP_BADGES = Map.of(
            RecruitmentStatus.APP_PENDING, "status-yellow",
            RecruitmentStatus.APP_INTERVIEW_INVITED, "status-yellow",
            RecruitmentStatus.APP_INTERVIEWED, "status-blue",
            RecruitmentStatus.APP_ACCEPTED, "status-green",
            RecruitmentStatus.APP_REJECTED, "status-red",
            RecruitmentStatus.APP_WITHDRAWN, "status-gray");
    private static final Map<String, String> ROUND_LABELS = Map.of(
            RecruitmentStatus.ROUND_DRAFT, "Bản nháp",
            RecruitmentStatus.ROUND_OPEN, "Đang nhận hồ sơ",
            RecruitmentStatus.ROUND_CLOSED, "Đã đóng");
    private static final Map<String, String> ROUND_BADGES = Map.of(
            RecruitmentStatus.ROUND_DRAFT, "status-gray",
            RecruitmentStatus.ROUND_OPEN, "status-green",
            RecruitmentStatus.ROUND_CLOSED, "status-red");
    private static final Pattern STUDENT_YEAR = Pattern.compile("^[A-Za-z]{2}(\\d{2})");

    private final IRecruitmentService service;
    private final IExtracurricularActivityService activityService;
    private final IStudentService studentService;
    private final IRegistrationFormService registrationService;

    public OrganizerRecruitmentServlet() {
        this(new RecruitmentServiceImpl(), new ExtracurricularActivityServiceImpl(),
                new StudentServiceImpl(), new RegistrationFormServiceImpl());
    }

    OrganizerRecruitmentServlet(IRecruitmentService service, IExtracurricularActivityService activityService,
                                IStudentService studentService, IRegistrationFormService registrationService) {
        this.service = service;
        this.activityService = activityService;
        this.studentService = studentService;
        this.registrationService = registrationService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = authorize(request, response);
        if (user == null) {
            return;
        }
        request.setAttribute("appLabels", APP_LABELS);
        request.setAttribute("appBadges", APP_BADGES);
        request.setAttribute("roundLabels", ROUND_LABELS);
        request.setAttribute("roundBadges", ROUND_BADGES);
        String action = request.getParameter("action");
        String view = "STUDENT".equals(user.getType())
                ? studentView(request, response, user, action)
                : managerView(request, response, action);
        if (view != null) {
            request.getRequestDispatcher(VIEW_DIR + view).forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        CurrentUser user = authorize(request, response);
        if (user == null) {
            return;
        }
        String action = request.getParameter("action");
        if ("STUDENT".equals(user.getType())) {
            studentPost(request, response, user, action);
        } else {
            managerPost(request, response, user, action);
        }
    }

    private CurrentUser authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        if (!"STUDENT".equals(user.getType()) && !AccessPolicy.canManageActivities(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
            return null;
        }
        return user;
    }

    // ---------------------------------------------------------------- Sinh viên

    private String studentView(HttpServletRequest request, HttpServletResponse response, CurrentUser user,
                               String action) throws IOException {
        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy hồ sơ sinh viên của tài khoản này.");
            return null;
        }
        if ("apply".equals(action)) {
            return applyView(request, response, student);
        }
        if ("myApplications".equals(action)) {
            return myApplicationsView(request, student);
        }
        List<RecruitmentRound> rounds = service.getOpenRounds();
        List<RecruitmentApplication> active = activeApplications(service.getStudentApplications(student.getId()));
        Set<Integer> appliedRoundIds = new HashSet<>();
        for (RecruitmentApplication app : active) {
            appliedRoundIds.add(app.getRoundId());
        }
        request.setAttribute("rounds", rounds);
        request.setAttribute("appliedRoundIds", appliedRoundIds);
        request.setAttribute("appliedApplications", active);
        return "student-list.jsp";
    }

    private String applyView(HttpServletRequest request, HttpServletResponse response, Student student)
            throws IOException {
        int roundId = intParam(request, "roundId");
        RecruitmentRound round = null;
        for (RecruitmentRound open : service.getOpenRounds()) {
            if (open.getId() == roundId) {
                round = service.getRound(roundId);
            }
        }
        if (round == null) {
            flash(request, "errorMessage", "Đợt tuyển không tồn tại, đã đóng hoặc chưa mở.");
            response.sendRedirect(request.getContextPath() + PAGE);
            return null;
        }
        if (service.getActiveApplication(roundId, student.getId()) != null) {
            flash(request, "errorMessage", "Bạn đã nộp hồ sơ cho đợt tuyển này.");
            response.sendRedirect(request.getContextPath() + PAGE + "?action=myApplications");
            return null;
        }
        request.setAttribute("round", round);
        request.setAttribute("student", student);
        request.setAttribute("cohort", cohort(student.getCode()));
        request.setAttribute("hasParticipation", service.hasActiveParticipation(round.getActivityId(), student.getId()));
        if (request.getAttribute("answerMap") == null) {
            request.setAttribute("answerMap", new HashMap<Integer, List<String>>());
        }
        return "student-apply.jsp";
    }

    private String myApplicationsView(HttpServletRequest request, Student student) {
        List<RecruitmentApplication> applications = service.getStudentApplications(student.getId());
        List<Map<String, Object>> rows = new ArrayList<>();
        int joinCount = 0;
        for (RegistrationApplication join : registrationService.getApplicationsForStudent(student.getId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", "JOIN");
            row.put("activityName", join.getActivityName());
            row.put("submittedAt", join.getSubmittedAt());
            row.put("status", join.getStatus());
            row.put("url", request.getContextPath() + "/registration-forms?action=myApplication&id=" + join.getId());
            rows.add(row);
            joinCount++;
        }
        for (RecruitmentApplication app : applications) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", "BTC");
            row.put("activityName", app.getActivityName());
            row.put("submittedAt", app.getSubmittedAt());
            row.put("status", app.getStatus());
            row.put("url", request.getContextPath() + PAGE + "?action=myApplications&id=" + app.getId() + "#progress");
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (LocalDateTime) r.get("submittedAt"),
                Comparator.nullsLast(Comparator.reverseOrder())));

        RecruitmentApplication selected = null;
        int selectedId = intParam(request, "id");
        for (RecruitmentApplication app : applications) {
            if (app.getId() == selectedId) {
                selected = service.getStudentApplication(selectedId, student.getId());
            }
        }
        if (selected == null) {
            for (RecruitmentApplication app : applications) {
                if (!RecruitmentStatus.APP_WITHDRAWN.equals(app.getStatus())) {
                    selected = service.getStudentApplication(app.getId(), student.getId());
                    break;
                }
            }
        }
        request.setAttribute("rows", rows);
        request.setAttribute("joinCount", joinCount);
        request.setAttribute("organizerCount", applications.size());
        request.setAttribute("selected", selected);
        if (selected != null) {
            request.setAttribute("steps", buildSteps(selected));
            request.setAttribute("selectedAcceptedLocked", selected.getActivityStart() == null
                    || !LocalDateTime.now().isBefore(selected.getActivityStart()
                    .minusHours(RecruitmentStatus.WITHDRAW_ACCEPTED_HOURS_BEFORE_ACTIVITY)));
        }
        return "my-applications.jsp";
    }

    private void studentPost(HttpServletRequest request, HttpServletResponse response, CurrentUser user,
                             String action) throws ServletException, IOException {
        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy hồ sơ sinh viên của tài khoản này.");
            return;
        }
        String ctx = request.getContextPath();
        if ("apply".equals(action)) {
            int roundId = intParam(request, "roundId");
            RecruitmentRound round = service.getRound(roundId);
            Map<Integer, List<String>> answers = new HashMap<>();
            if (round != null) {
                for (RecruitmentQuestion question : round.getQuestions()) {
                    String[] values = request.getParameterValues("q_" + question.getId());
                    answers.put(question.getId(), values == null ? List.of() : List.of(values));
                }
            }
            String error = service.apply(roundId, student.getId(), integerParam(request, "wish1"),
                    integerParam(request, "wish2"), answers, request.getParameter("note"));
            if (error == null) {
                flash(request, "successMessage", "Đã nộp hồ sơ ứng tuyển ban tổ chức.");
                response.sendRedirect(ctx + PAGE + "?action=myApplications");
                return;
            }
            request.setAttribute("error", error);
            request.setAttribute("answerMap", answers);
            request.setAttribute("appLabels", APP_LABELS);
            String view = applyView(request, response, student);
            if (view != null) {
                request.getRequestDispatcher(VIEW_DIR + view).forward(request, response);
            }
            return;
        }
        int applicationId = intParam(request, "applicationId");
        String error;
        String success;
        if ("withdraw".equals(action)) {
            error = service.withdraw(applicationId, student.getId(), request.getParameter("reason"));
            success = "Đã rút hồ sơ ứng tuyển.";
        } else if ("confirm".equals(action)) {
            error = service.confirmInterview(applicationId, student.getId());
            success = "Đã xác nhận tham dự phỏng vấn.";
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        flash(request, error == null ? "successMessage" : "errorMessage", error == null ? success : error);
        response.sendRedirect(ctx + PAGE + "?action=myApplications&id=" + applicationId + "#progress");
    }

    private static List<RecruitmentApplication> activeApplications(List<RecruitmentApplication> all) {
        List<RecruitmentApplication> list = new ArrayList<>();
        for (RecruitmentApplication app : all) {
            if (!RecruitmentStatus.APP_WITHDRAWN.equals(app.getStatus())) {
                list.add(app);
            }
        }
        return list;
    }

    private static String cohort(String studentCode) {
        if (studentCode == null) {
            return "";
        }
        Matcher m = STUDENT_YEAR.matcher(studentCode);
        return m.find() ? "K" + m.group(1) : "";
    }

    /** Bốn bước của thẻ tiến trình: state là done, current, fail hoặc rỗng. */
    private static List<Map<String, String>> buildSteps(RecruitmentApplication app) {
        String status = app.getStatus();
        RecruitmentInterview iv = app.getInterview();
        boolean hasInterview = iv != null && !RecruitmentStatus.INTERVIEW_CANCELLED.equals(iv.getStatus());
        String[] state = {"done", "", "", ""};
        String[] sub = {format(app.getSubmittedAt()), "", "", "Chưa có"};
        switch (status) {
            case RecruitmentStatus.APP_PENDING -> {
                state[1] = "current";
                sub[1] = "Đang chờ xét";
            }
            case RecruitmentStatus.APP_INTERVIEW_INVITED -> {
                state[1] = "done";
                sub[1] = "Đạt vòng hồ sơ";
                state[2] = "current";
                sub[2] = iv != null && RecruitmentStatus.INTERVIEW_CONFIRMED.equals(iv.getStatus())
                        ? "Đã xác nhận" : "Chờ bạn xác nhận";
            }
            case RecruitmentStatus.APP_INTERVIEWED -> {
                state[1] = "done";
                sub[1] = "Đạt vòng hồ sơ";
                state[2] = "done";
                sub[2] = "Đã phỏng vấn";
                state[3] = "current";
                sub[3] = "Đang chờ kết quả";
            }
            case RecruitmentStatus.APP_ACCEPTED -> {
                state[1] = "done";
                sub[1] = "Đạt vòng hồ sơ";
                state[2] = hasInterview ? "done" : "";
                sub[2] = hasInterview ? "Đã phỏng vấn" : "Không phỏng vấn";
                state[3] = "done";
                sub[3] = "Trúng tuyển" + (app.getAcceptedPositionName() == null ? "" : ": " + app.getAcceptedPositionName());
            }
            case RecruitmentStatus.APP_REJECTED -> {
                state[1] = hasInterview ? "done" : "fail";
                sub[1] = hasInterview ? "Đạt vòng hồ sơ" : "Không đạt";
                if (hasInterview) {
                    state[2] = iv != null && RecruitmentStatus.INTERVIEW_DONE.equals(iv.getStatus()) ? "done" : "";
                    state[3] = "fail";
                    sub[3] = "Không đạt";
                }
            }
            default -> {
                state[3] = "fail";
                sub[3] = "Đã rút hồ sơ";
            }
        }
        String[] labels = {"Đã nộp hồ sơ", "Xét hồ sơ", "Phỏng vấn", "Kết quả"};
        List<Map<String, String>> steps = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, String> step = new LinkedHashMap<>();
            step.put("no", String.valueOf(i + 1));
            step.put("label", labels[i]);
            step.put("state", state[i]);
            step.put("sub", sub[i]);
            steps.add(step);
        }
        return steps;
    }

    private static String format(LocalDateTime value) {
        return value == null ? "" : DISPLAY_FORMAT.format(value);
    }

    // ------------------------------------------------------------ Staff / Admin

    String managerView(HttpServletRequest request, HttpServletResponse response, String action)
            throws IOException {
        if ("create".equals(action) || "edit".equals(action)) {
            RecruitmentRound round;
            if ("edit".equals(action)) {
                round = service.getRound(intParam(request, "id"));
                if (round == null) {
                    flash(request, "errorMessage", "Không tìm thấy đợt tuyển.");
                    response.sendRedirect(request.getContextPath() + PAGE);
                    return null;
                }
            } else {
                round = new RecruitmentRound();
                int activityId = intParam(request, "activityId");
                if (activityId <= 0) {
                    response.sendRedirect(request.getContextPath() + PAGE);
                    return null;
                }
                RecruitmentRound existing = service.getRoundByActivity(activityId);
                if (existing != null) {
                    flash(request, "errorMessage", "Hoạt động này đã có đợt tuyển ban tổ chức.");
                    response.sendRedirect(request.getContextPath() + PAGE + "?activityId=" + activityId);
                    return null;
                }
                ExtracurricularActivityResponseDTO activity = activityService.getActivityById(activityId);
                if (activity == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hoạt động.");
                    return null;
                }
                boolean eligible = service.getSelectableActivities(null).stream()
                        .anyMatch(candidate -> candidate.getActivityId() == activityId);
                if (!eligible) {
                    flash(request, "errorMessage", creationEligibilityMessage(activity, DateTimeUtil.now()));
                    response.sendRedirect(request.getContextPath() + PAGE + "?activityId=" + activityId);
                    return null;
                }
                round.setActivityId(activityId);
            }
            prepareForm(request, round);
            return "staff-form.jsp";
        }
        var activities = activityService.getAllActivities();
        RecruitmentRound round = null;
        int activityId;
        if ("applications".equals(action)) {
            round = service.getRound(intParam(request, "id"));
            if (round == null) {
                flash(request, "errorMessage", "Không tìm thấy đợt tuyển.");
                response.sendRedirect(request.getContextPath() + PAGE);
                return null;
            }
            activityId = round.getActivityId();
        } else {
            activityId = request.getParameter("activityIdFallback") == null
                    ? intParam(request, "activityId") : intParam(request, "activityIdFallback");
        }
        var activity = activityId > 0 ? activityService.getActivityById(activityId) : null;
        if (activityId > 0 && activity == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hoạt động.");
            return null;
        }
        if (round == null && activity != null) {
            round = service.getRoundByActivity(activityId);
        }
        request.setAttribute("activities", activities);
        request.setAttribute("activity", activity);
        request.setAttribute("round", round);
        if (round != null) {
            prepareApplications(request, round);
        }
        return "staff-list.jsp";
    }

    static String creationEligibilityMessage(ExtracurricularActivityResponseDTO activity, LocalDateTime now) {
        List<String> reasons = new ArrayList<>();
        if (activity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            reasons.add("hoạt động chưa được duyệt");
        }
        boolean notUpcoming = activity.getActivityStatus() != ActivityStatus.UPCOMING;
        boolean started = activity.getStartTime() != null && !activity.getStartTime().isAfter(now);
        if (notUpcoming || started) {
            reasons.add("hoạt động không ở trạng thái Chưa diễn ra");
        }
        if (activity.getStartTime() == null) {
            reasons.add("hoạt động chưa có thời gian bắt đầu");
        }
        if (reasons.isEmpty()) {
            return "Không thể tạo đợt tuyển vì hoạt động không còn đủ điều kiện. Vui lòng tải lại trang và kiểm tra.";
        }
        return "Không thể tạo đợt tuyển vì " + String.join("; ", reasons) + ".";
    }


    private void prepareApplications(HttpServletRequest request, RecruitmentRound round) {
        String status = request.getParameter("status");
        Integer positionId = integerParam(request, "positionId");
        List<RecruitmentApplication> all = service.getApplications(round.getId(), null, null);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RecruitmentApplication a : all) {
            counts.merge(a.getStatus(), 1, Integer::sum);
        }
        List<RecruitmentApplication> shown = new ArrayList<>();
        for (RecruitmentApplication a : all) {
            boolean statusOk = status == null || status.isBlank() || "all".equals(status) || status.equals(a.getStatus());
            boolean positionOk = positionId == null || positionId == a.getChoice1PositionId()
                    || positionId.equals(a.getChoice2PositionId()) || positionId.equals(a.getAcceptedPositionId());
            if (statusOk && positionOk) {
                shown.add(a);
            }
        }
        RecruitmentApplication selected = null;
        int selectedId = intParam(request, "appId");
        if (selectedId > 0) {
            selected = service.getApplication(selectedId);
            if (selected != null && selected.getRoundId() != round.getId()) {
                selected = null;
            }
        }
        Map<Integer, Integer> remaining = new HashMap<>();
        for (RecruitmentPosition p : round.getPositions()) {
            remaining.put(p.getId(), p.getRemaining());
        }
        Map<String, String> inputs = new HashMap<>();
        if (selected != null && selected.getInterview() != null
                && RecruitmentStatus.APP_INTERVIEW_INVITED.equals(selected.getStatus())) {
            RecruitmentInterview iv = selected.getInterview();
            inputs.put("scheduledAt", iv.getScheduledAt() == null ? "" : INPUT_FORMAT.format(iv.getScheduledAt()));
            inputs.put("confirmDeadline", iv.getConfirmDeadline() == null ? "" : INPUT_FORMAT.format(iv.getConfirmDeadline()));
        }
        request.setAttribute("apps", shown);
        request.setAttribute("allCount", all.size());
        request.setAttribute("counts", counts);
        request.setAttribute("statusFilter", status == null || status.isBlank() ? "all" : status);
        request.setAttribute("positionFilter", positionId);
        request.setAttribute("selected", selected);
        request.setAttribute("remaining", remaining);
        request.setAttribute("ivInputs", inputs);
        request.setAttribute("interviewCanComplete", selected != null && selected.getInterview() != null
                && selected.getInterview().getScheduledAt() != null
                && !selected.getInterview().getScheduledAt().isAfter(LocalDateTime.now()));
        request.setAttribute("activityStarted", selected != null && selected.getActivityStart() != null
                && !selected.getActivityStart().isAfter(LocalDateTime.now()));
    }

    private RecruitmentInterview parseInterview(HttpServletRequest request) {
        RecruitmentInterview iv = new RecruitmentInterview();
        try {
            iv.setScheduledAt(parseTime(request.getParameter("scheduledAt")));
            iv.setConfirmDeadline(parseTime(request.getParameter("confirmDeadline")));
        } catch (DateTimeParseException e) {
            return null;
        }
        iv.setFormat(request.getParameter("format"));
        iv.setLocation(request.getParameter("location"));
        iv.setNote(request.getParameter("note"));
        return iv;
    }

    private boolean reviewPost(HttpServletRequest request, HttpServletResponse response, CurrentUser user,
                               String action) throws IOException {
        int roundId = intParam(request, "roundId");
        int appId = intParam(request, "applicationId");
        String error;
        String success;
        switch (action) {
            case "invite" -> {
                String[] ids = request.getParameterValues("ids");
                List<Integer> targets = new ArrayList<>();
                if (ids != null) {
                    for (String id : ids) {
                        if (parseInt(id) > 0) {
                            targets.add(parseInt(id));
                        }
                    }
                }
                if (targets.isEmpty() && appId > 0) {
                    targets.add(appId);
                }
                if (targets.isEmpty()) {
                    error = "Vui lòng chọn hồ sơ cần mời phỏng vấn.";
                    success = null;
                    break;
                }
                int ok = 0;
                String firstError = null;
                for (int id : targets) {
                    RecruitmentInterview iv = parseInterview(request);
                    String result = iv == null ? "Thời gian nhập không hợp lệ." : service.invite(id, user.getId(), iv);
                    if (result == null) {
                        ok++;
                    } else if (firstError == null) {
                        firstError = result;
                    }
                }
                error = ok == targets.size() ? null : (ok == 0 ? firstError
                        : "Đã mời " + ok + "/" + targets.size() + " hồ sơ. Lỗi: " + firstError);
                success = "Đã gửi lời mời phỏng vấn cho " + ok + " hồ sơ.";
                if (ids != null && ids.length > 0) {
                    appId = 0;
                }
            }
            case "remind" -> {
                error = service.remindInterview(appId);
                success = "Đã nhắc sinh viên xác nhận lịch.";
            }
            case "cancelInterview" -> {
                error = service.cancelInterview(appId);
                success = "Đã hủy lịch phỏng vấn.";
            }
            case "completeInterview" -> {
                error = service.completeInterview(appId, request.getParameter("evaluation"));
                success = "Đã ghi nhận buổi phỏng vấn.";
            }
            case "accept" -> {
                error = service.accept(appId, intParam(request, "positionId"));
                success = "Đã cho trúng tuyển.";
            }
            case "reject" -> {
                error = service.reject(appId, request.getParameter("reason"));
                success = "Đã loại hồ sơ.";
            }
            case "revert" -> {
                error = service.revertDecision(appId);
                success = "Đã thu hồi quyết định.";
            }
            case "note" -> {
                error = service.saveInternalNote(appId, request.getParameter("internalNote"));
                success = "Đã lưu ghi chú nội bộ.";
            }
            default -> {
                return false;
            }
        }
        flash(request, error == null ? "successMessage" : "errorMessage", error == null ? success : error);
        response.sendRedirect(request.getContextPath() + PAGE + "?action=applications&id=" + roundId
                + (appId > 0 ? "&appId=" + appId : "")
                + (request.getParameter("status") == null ? "" : "&status=" + request.getParameter("status"))
                + (intParam(request, "filterPositionId") > 0 ? "&positionId=" + intParam(request, "filterPositionId") : "")
                + (intParam(request, "page") > 0 ? "&page=" + intParam(request, "page") : ""));
        return true;
    }

    private void prepareForm(HttpServletRequest request, RecruitmentRound round) {
        List<RecruitmentRound> activities = service.getSelectableActivities(round.getActivityId() > 0 ? round.getActivityId() : null);
        Map<Integer, String> participationOpen = new HashMap<>();
        for (RecruitmentRound activity : activities) {
            LocalDateTime openAt = service.getParticipationOpenAt(activity.getActivityId());
            if (openAt != null) {
                participationOpen.put(activity.getActivityId(), format(openAt));
            }
        }
        request.setAttribute("round", round);
        request.setAttribute("activities", activities);
        request.setAttribute("participationOpen", participationOpen);
        request.setAttribute("locked", round.getId() > 0 && service.isStructureLocked(round.getId()));
        request.setAttribute("openAtValue", round.getOpenAt() == null ? "" : INPUT_FORMAT.format(round.getOpenAt()));
        request.setAttribute("closeAtValue", round.getCloseAt() == null ? "" : INPUT_FORMAT.format(round.getCloseAt()));
        request.setAttribute("initialJson", structureJson(round));
    }

    private void managerPost(HttpServletRequest request, HttpServletResponse response, CurrentUser user,
                             String action) throws ServletException, IOException {
        String ctx = request.getContextPath();
        if ("save".equals(action)) {
            RecruitmentRound round = parseRound(request);
            String error = round == null ? "Thời gian nhập không hợp lệ." : service.saveRound(round, user.getId(),
                    "open".equals(request.getParameter("intent")));
            if (error == null) {
                flash(request, "successMessage", "Đã lưu đợt tuyển.");
                response.sendRedirect(ctx + PAGE + "?activityId=" + round.getActivityId());
                return;
            }
            if (round == null) {
                round = new RecruitmentRound();
                round.setId(intParam(request, "id"));
                round.setActivityId(intParam(request, "activityId"));
            }
            request.setAttribute("error", error);
            prepareForm(request, round);
            request.getRequestDispatcher(VIEW_DIR + "staff-form.jsp").forward(request, response);
            return;
        }
        int id = intParam(request, "id");
        if (action != null && reviewPost(request, response, user, action)) {
            return;
        }
        RecruitmentRound target = service.getRound(id);
        int activityId = target == null ? 0 : target.getActivityId();
        String error;
        String success;
        switch (action == null ? "" : action) {
            case "open" -> {
                error = service.openRound(id);
                success = "Đã mở nhận hồ sơ.";
            }
            case "close" -> {
                error = service.closeRound(id);
                success = "Đã đóng đợt tuyển.";
            }
            case "delete" -> {
                error = service.deleteRound(id);
                success = "Đã xóa đợt tuyển.";
            }
            default -> {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        flash(request, error == null ? "successMessage" : "errorMessage", error == null ? success : error);
        response.sendRedirect(ctx + PAGE + (activityId > 0 ? "?activityId=" + activityId : ""));
    }

    /** Trả về null khi thời gian nhập sai định dạng. */
    private static RecruitmentRound parseRound(HttpServletRequest request) {
        RecruitmentRound round = new RecruitmentRound();
        round.setId(intParam(request, "id"));
        round.setActivityId(intParam(request, "activityId"));
        round.setTitle(request.getParameter("title"));
        round.setDescription(request.getParameter("description"));
        round.setInterviewRequired(request.getParameter("interview") != null);
        try {
            round.setOpenAt(parseTime(request.getParameter("openAt")));
            round.setCloseAt(parseTime(request.getParameter("closeAt")));
        } catch (DateTimeParseException e) {
            return null;
        }
        String[] names = valuesOf(request, "posName");
        String[] totals = valuesOf(request, "posTotal");
        List<RecruitmentPosition> positions = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            RecruitmentPosition position = new RecruitmentPosition();
            position.setName(names[i]);
            position.setQuota(i < totals.length ? parseInt(totals[i]) : 0);
            positions.add(position);
        }
        round.setPositions(positions);
        String[] texts = valuesOf(request, "qText");
        String[] types = valuesOf(request, "qType");
        String[] required = valuesOf(request, "qRequired");
        String[] options = valuesOf(request, "qOptions");
        List<RecruitmentQuestion> questions = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            RecruitmentQuestion question = new RecruitmentQuestion();
            question.setQuestionText(texts[i]);
            question.setQuestionType(i < types.length ? types[i] : RecruitmentStatus.QUESTION_TEXT);
            question.setRequired(i >= required.length || !"false".equals(required[i]));
            question.setOptions(i < options.length ? options[i] : null);
            questions.add(question);
        }
        round.setQuestions(questions);
        return round;
    }

    private static LocalDateTime parseTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value.trim(), INPUT_FORMAT);
    }

    private static String[] valuesOf(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    private static String structureJson(RecruitmentRound round) {
        StringBuilder sb = new StringBuilder("{\"positions\":[");
        for (int i = 0; i < round.getPositions().size(); i++) {
            RecruitmentPosition p = round.getPositions().get(i);
            sb.append(i > 0 ? "," : "").append("{\"name\":").append(quote(p.getName()))
                    .append(",\"quota\":").append(p.getQuota()).append('}');
        }
        sb.append("],\"questions\":[");
        for (int i = 0; i < round.getQuestions().size(); i++) {
            RecruitmentQuestion q = round.getQuestions().get(i);
            sb.append(i > 0 ? "," : "").append("{\"type\":").append(quote(q.getQuestionType()))
                    .append(",\"text\":").append(quote(q.getQuestionText()))
                    .append(",\"required\":").append(q.isRequired()).append(",\"options\":[");
            List<String> options = q.getOptionList();
            for (int j = 0; j < options.size(); j++) {
                sb.append(j > 0 ? "," : "").append(quote(options.get(j)));
            }
            sb.append("]}");
        }
        return sb.append("]}").toString();
    }

    /** Chuỗi JSON an toàn để nhúng trong thẻ script (không chứa dấu "<"). */
    private static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                case '\u2028' -> sb.append("\\u2028");
                case '\u2029' -> sb.append("\\u2029");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    // ------------------------------------------------------------------ Chung

    private static void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private static int intParam(HttpServletRequest request, String name) {
        return parseInt(request.getParameter(name));
    }

    private static Integer integerParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        int parsed = parseInt(value);
        return parsed > 0 ? parsed : null;
    }

    private static int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
