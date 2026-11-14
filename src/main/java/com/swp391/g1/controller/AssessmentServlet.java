package com.swp391.g1.controller;

import com.swp391.g1.dao.IAssessmentAnswerDAO;
import com.swp391.g1.dao.IAssessmentAttemptDAO;
import com.swp391.g1.dao.IAssessmentDAO;
import com.swp391.g1.dao.IAssessmentOptionDAO;
import com.swp391.g1.dao.IAssessmentQuestionDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IStudentDAO;
import com.swp391.g1.dao.impl.AssessmentAnswerDAOImpl;
import com.swp391.g1.dao.impl.AssessmentAttemptDAOImpl;
import com.swp391.g1.dao.impl.AssessmentDAOImpl;
import com.swp391.g1.dao.impl.AssessmentOptionDAOImpl;
import com.swp391.g1.dao.impl.AssessmentQuestionDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.StudentDAOImpl;
import com.swp391.g1.dto.AssessmentListItem;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Assessment;
import com.swp391.g1.model.AssessmentAnswer;
import com.swp391.g1.model.AssessmentAttempt;
import com.swp391.g1.model.AssessmentOption;
import com.swp391.g1.model.AssessmentQuestion;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.AssessmentServiceException;
import com.swp391.g1.service.IAssessmentService;
import com.swp391.g1.service.impl.AssessmentServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.common.DBContext;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.util.DateTimeUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@WebServlet(name = "AssessmentServlet", urlPatterns = {"/assessment"})
public class AssessmentServlet extends HttpServlet {

    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private static final String CSRF_SESSION_KEY = "com.swp391.g1.assessment.csrfToken";

    private final IAssessmentService assessmentService = new AssessmentServiceImpl();
    private final IAssessmentDAO assessmentDAO = new AssessmentDAOImpl();
    private final IAssessmentQuestionDAO questionDAO = new AssessmentQuestionDAOImpl();
    private final IAssessmentOptionDAO optionDAO = new AssessmentOptionDAOImpl();
    private final IAssessmentAttemptDAO attemptDAO = new AssessmentAttemptDAOImpl();
    private final IAssessmentAnswerDAO answerDAO = new AssessmentAnswerDAOImpl();
    private final IExtracurricularActivityDAO activityDAO = new ExtracurricularActivityDAOImpl();
    private final IStudentDAO studentDAO = new StudentDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        request.setAttribute("csrfToken", getCsrfToken(request));

        try {
            switch (action) {
                case "list":
                    showList(request, response, user);
                    break;
                case "create":
                    redirectLegacyCreate(request, response);
                    break;
                case "edit":
                    redirectLegacyEdit(request, response, user);
                    break;
                case "manage":
                    showManagePage(request, response, user);
                    break;
                case "questions":
                    redirectLegacyQuestions(request, response, user);
                    break;
                case "options":
                    redirectLegacyOptions(request, response, user);
                    break;
                case "submissions":
                    showSubmissions(request, response, user);
                    break;
                case "grade":
                    showGradingPage(request, response, user);
                    break;
                case "take":
                    showTakePage(request, response, user);
                    break;
                case "result":
                    showResultPage(request, response, user);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/assessment?action=list");
                    break;
            }
        } catch (AssessmentServiceException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/assessment?action=list");
        }
    }

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
        if (action == null) {
            action = "";
        }

        if (!isValidCsrfToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token không hợp lệ.");
            return;
        }

        try {
            switch (action) {
                case "saveFull":
                    handleSaveFull(request, response, user);
                    break;
                case "create":
                    handleCreate(request, response, user);
                    break;
                case "update":
                    handleUpdate(request, response, user);
                    break;
                case "publish":
                    handlePublish(request, response, user);
                    break;
                case "delete":
                    handleDelete(request, response, user);
                    break;
                case "addQuestion":
                    handleAddQuestion(request, response, user);
                    break;
                case "updateQuestion":
                    handleUpdateQuestion(request, response, user);
                    break;
                case "deleteQuestion":
                    handleDeleteQuestion(request, response, user);
                    break;
                case "addOption":
                    handleAddOption(request, response, user);
                    break;
                case "updateOption":
                    handleUpdateOption(request, response, user);
                    break;
                case "deleteOption":
                    handleDeleteOption(request, response, user);
                    break;
                case "startAttempt":
                    handleStartAttempt(request, response, user);
                    break;
                case "saveAnswers":
                    handleSaveAnswers(request, response, user);
                    break;
                case "submitAttempt":
                    handleSubmitAttempt(request, response, user);
                    break;
                case "gradeEssay":
                    handleGradeEssay(request, response, user);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/assessment?action=list");
                    break;
            }
        } catch (AssessmentServiceException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", e.getMessage());
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isBlank()) {
                response.sendRedirect(referer);
            } else {
                response.sendRedirect(request.getContextPath() + "/assessment?action=list");
            }
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        if (AccessPolicy.canManageActivities(user)) {
            List<AssessmentListItem> items = assessmentService.listAssessmentsForStaff(user);
            setAssessmentTimeDisplays(request, items);

            int publishedCount = 0;
            int totalSubmissionsCount = 0;
            int totalPendingGradingCount = 0;
            for (AssessmentListItem item : items) {
                if (item.isPublished()) {
                    publishedCount++;
                }
                totalSubmissionsCount += item.getTotalAttempts();
                totalPendingGradingCount += item.getPendingGrading();
            }

            request.setAttribute("assessments", items);
            request.setAttribute("publishedCount", publishedCount);
            request.setAttribute("draftCount", items.size() - publishedCount);
            request.setAttribute("totalSubmissionsCount", totalSubmissionsCount);
            request.setAttribute("totalPendingGradingCount", totalPendingGradingCount);
            request.getRequestDispatcher("/WEB-INF/views/assessment/staff/list.jsp")
                    .forward(request, response);
            return;
        }

        Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem danh sách bài đánh giá.");
            return;
        }

        List<AssessmentListItem> items = assessmentService.listAssessmentsForStudent(student.getId());
        setAssessmentTimeDisplays(request, items);

        LocalDateTime now = DateTimeUtil.now();
        Map<Integer, Boolean> expiredMap = new HashMap<>();
        Map<Integer, Boolean> notOpenYetMap = new HashMap<>();
        int pendingTakeCount = 0;
        int waitingGradeCount = 0;
        int completedCount = 0;

        for (AssessmentListItem item : items) {
            boolean canTake = assessmentService.canStudentTake(item.getAssessmentId(), student.getId());
            item.setCanTake(canTake);

            boolean notOpenYet = item.getOpenTime() != null && now.isBefore(item.getOpenTime());
            boolean expired = item.getCloseTime() != null && now.isAfter(item.getCloseTime());
            notOpenYetMap.put(item.getAssessmentId(), notOpenYet);
            expiredMap.put(item.getAssessmentId(), expired);

            String st = item.getAttemptStatus();
            if ("COMPLETED".equals(st)) {
                completedCount++;
            } else if ("SUBMITTED".equals(st)) {
                waitingGradeCount++;
            } else {
                pendingTakeCount++;
            }
        }

        request.setAttribute("assessments", items);
        request.setAttribute("expiredMap", expiredMap);
        request.setAttribute("notOpenYetMap", notOpenYetMap);
        request.setAttribute("pendingTakeCount", pendingTakeCount);
        request.setAttribute("waitingGradeCount", waitingGradeCount);
        request.setAttribute("completedCount", completedCount);
        request.getRequestDispatcher("/WEB-INF/views/assessment/student/list.jsp")
                .forward(request, response);
    }

    private void setAssessmentTimeDisplays(HttpServletRequest request,
            List<AssessmentListItem> items) {
        Map<Integer, String> openTimeDisplay = new HashMap<>();
        Map<Integer, String> closeTimeDisplay = new HashMap<>();
        for (AssessmentListItem item : items) {
            openTimeDisplay.put(item.getAssessmentId(), DateTimeUtil.format(item.getOpenTime()));
            closeTimeDisplay.put(item.getAssessmentId(), DateTimeUtil.format(item.getCloseTime()));
        }
        request.setAttribute("openTimeDisplay", openTimeDisplay);
        request.setAttribute("closeTimeDisplay", closeTimeDisplay);
    }

    private void redirectLegacyCreate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int activityId = parseId(request.getParameter("activityId"));
        String target = request.getContextPath() + "/assessment?action=manage&mode=create"
                + (activityId > 0 ? "&activityId=" + activityId : "");
        response.sendRedirect(target);
    }

    private void redirectLegacyEdit(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int activityId = parseId(request.getParameter("activityId"));
        int id = parseId(request.getParameter("id"));
        if (activityId <= 0 && id > 0) {
            Assessment assessment = assessmentService.getAssessmentById(id, user);
            if (assessment != null) {
                activityId = assessment.getActivityId();
            }
        }
        String target = request.getContextPath() + "/assessment?action=manage"
                + (activityId > 0 ? "&activityId=" + activityId + "&mode=edit" : "");
        response.sendRedirect(target);
    }

    private void redirectLegacyQuestions(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        int activityId = parseId(request.getParameter("activityId"));
        if (activityId <= 0 && assessmentId > 0) {
            Assessment assessment = assessmentService.getAssessmentById(assessmentId, user);
            if (assessment != null) {
                activityId = assessment.getActivityId();
            }
        }
        String target = request.getContextPath() + "/assessment?action=manage"
                + (activityId > 0 ? "&activityId=" + activityId + "&mode=edit#question-builder" : "");
        response.sendRedirect(target);
    }

    private void redirectLegacyOptions(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int questionId = parseId(request.getParameter("questionId"));
        int activityId = 0;
        if (questionId > 0) {
            AssessmentQuestion question = questionDAO.findById(questionId);
            if (question != null) {
                Assessment assessment = assessmentService.getAssessmentById(question.getAssessmentId(), user);
                if (assessment != null) {
                    activityId = assessment.getActivityId();
                }
            }
        }
        String target = request.getContextPath() + "/assessment?action=manage"
                + (activityId > 0 ? "&activityId=" + activityId + "&mode=edit#question-builder" : "");
        response.sendRedirect(target);
    }

    private void showManagePage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        if (!AccessPolicy.canManageActivities(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý bài đánh giá.");
            return;
        }

        List<ExtracurricularActivityResponseDTO> scopedActivities = AccessPolicy.hasType(user, "ADMIN")
                ? activityDAO.findAllWithDetails()
                : activityDAO.findAllWithDetailsForStaff(user.getId(), user.getRole());

        Map<Integer, ExtracurricularActivityResponseDTO> activityMap = new HashMap<>();
        for (ExtracurricularActivityResponseDTO act : scopedActivities) {
            activityMap.put(act.getId(), act);
        }

        Set<Integer> assessedActivityIds = new HashSet<>();
        for (Assessment existing : assessmentDAO.findAll()) {
            assessedActivityIds.add(existing.getActivityId());
        }

        int activityId = parseId(request.getParameter("activityId"));
        int idParam = parseId(request.getParameter("id"));
        Assessment assessment = null;
        ExtracurricularActivityResponseDTO selectedActivity = null;

        if (idParam > 0 && activityId <= 0) {
            assessment = assessmentService.getAssessmentById(idParam, user);
            if (assessment != null) {
                activityId = assessment.getActivityId();
            }
        }

        if (activityId > 0) {
            selectedActivity = activityMap.get(activityId);
            if (selectedActivity == null) {
                ExtracurricularActivity rawAct = activityDAO.findById(activityId);
                if (rawAct == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hoạt động.");
                    return;
                }
                if (!AccessPolicy.hasType(user, "ADMIN")
                        && !activityDAO.isInStaffActivityScope(activityId, user.getId(), user.getRole())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập hoạt động này.");
                    return;
                }
                selectedActivity = new ExtracurricularActivityResponseDTO();
                selectedActivity.setId(rawAct.getId());
                selectedActivity.setCode(rawAct.getCode());
                selectedActivity.setName(rawAct.getName());
            }
            if (assessment == null) {
                assessment = assessmentService.getAssessmentByActivityId(activityId, user);
            }
        }

        List<AssessmentQuestion> questions = List.of();
        double totalMaxScore = 0;
        List<AssessmentAttempt> submissions = List.of();
        Map<Integer, String> submittedAtDisplay = new HashMap<>();
        Map<Integer, Student> studentMap = new HashMap<>();
        int pendingGradingCount = 0;
        int completedCount = 0;
        int inProgressCount = 0;

        if (assessment != null) {
            questions = assessmentService.listQuestions(assessment.getId(), user);
            for (AssessmentQuestion q : questions) {
                totalMaxScore += q.getMaxScore();
                if (q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                    q.setOptions(optionDAO.findByQuestionId(q.getId()));
                }
            }

            submissions = attemptDAO.findByAssessmentId(assessment.getId());
            for (AssessmentAttempt sub : submissions) {
                submittedAtDisplay.put(sub.getId(), DateTimeUtil.format(sub.getSubmittedAt()));
                if (!studentMap.containsKey(sub.getStudentId())) {
                    Student st = studentDAO.findById(sub.getStudentId());
                    if (st != null) {
                        studentMap.put(sub.getStudentId(), st);
                    }
                }
                if (sub.getStatus() == AssessmentAttempt.Status.SUBMITTED) {
                    pendingGradingCount++;
                } else if (sub.getStatus() == AssessmentAttempt.Status.COMPLETED) {
                    completedCount++;
                } else {
                    inProgressCount++;
                }
            }
        }

        request.setAttribute("activities", scopedActivities);
        request.setAttribute("assessedActivityIds", assessedActivityIds);
        request.setAttribute("selectedActivityId", activityId);
        request.setAttribute("assessment", assessment);
        request.setAttribute("activity", selectedActivity);
        request.setAttribute("questions", questions);
        request.setAttribute("totalMaxScore", totalMaxScore);
        request.setAttribute("hasAttempts", !submissions.isEmpty());
        request.setAttribute("submissions", submissions);
        request.setAttribute("submittedAtDisplay", submittedAtDisplay);
        request.setAttribute("studentMap", studentMap);
        request.setAttribute("totalAttempts", submissions.size());
        request.setAttribute("pendingGradingCount", pendingGradingCount);
        request.setAttribute("completedCount", completedCount);
        request.setAttribute("inProgressCount", inProgressCount);
        request.setAttribute("openTimeDisplay", assessment != null ? DateTimeUtil.format(assessment.getOpenTime()) : "");
        request.setAttribute("closeTimeDisplay", assessment != null ? DateTimeUtil.format(assessment.getCloseTime()) : "");
        request.setAttribute("openTimeInput", assessment != null && assessment.getOpenTime() != null
                ? DT_FORMAT.format(assessment.getOpenTime()) : "");
        request.setAttribute("closeTimeInput", assessment != null && assessment.getCloseTime() != null
                ? DT_FORMAT.format(assessment.getCloseTime()) : "");
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/manage.jsp")
                .forward(request, response);
    }

    private void showSubmissions(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int activityId = parseId(request.getParameter("activityId"));
        if (activityId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID hoạt động không hợp lệ.");
            return;
        }
        Assessment assessment = assessmentService.getAssessmentByActivityId(activityId, user);
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        List<AssessmentAttempt> submissions = assessmentService.listSubmissionsForGrading(activityId, user);

        Map<Integer, String> submittedAtDisplay = new HashMap<>();
        Map<Integer, Student> studentMap = new HashMap<>();
        int completedCount = 0;
        int pendingCount = 0;
        int inProgressCount = 0;

        for (AssessmentAttempt s : submissions) {
            submittedAtDisplay.put(s.getId(), DateTimeUtil.format(s.getSubmittedAt()));
            if (!studentMap.containsKey(s.getStudentId())) {
                Student st = studentDAO.findById(s.getStudentId());
                if (st != null) {
                    studentMap.put(s.getStudentId(), st);
                }
            }
            if (s.getStatus() == AssessmentAttempt.Status.COMPLETED) {
                completedCount++;
            } else if (s.getStatus() == AssessmentAttempt.Status.SUBMITTED) {
                pendingCount++;
            } else {
                inProgressCount++;
            }
        }

        request.setAttribute("activityId", activityId);
        request.setAttribute("assessment", assessment);
        request.setAttribute("activity", activity);
        request.setAttribute("submissions", submissions);
        request.setAttribute("submittedAtDisplay", submittedAtDisplay);
        request.setAttribute("studentMap", studentMap);
        request.setAttribute("completedCount", completedCount);
        request.setAttribute("pendingCount", pendingCount);
        request.setAttribute("inProgressCount", inProgressCount);
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/submissions.jsp")
                .forward(request, response);
    }

    private void showGradingPage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int attemptId = parseId(request.getParameter("attemptId"));
        if (attemptId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID bài làm không hợp lệ.");
            return;
        }
        AssessmentAttempt attempt = assessmentService.getAttemptForGrading(attemptId, user);
        if (attempt == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy bài làm.");
            return;
        }
        Assessment assessment = assessmentDAO.findById(attempt.getAssessmentId());
        ExtracurricularActivity activity = assessment != null ? activityDAO.findById(assessment.getActivityId()) : null;
        Student student = studentDAO.findById(attempt.getStudentId());

        List<AssessmentAnswer> rawAnswers = assessmentService.getAttemptAnswers(attemptId, attempt.getStudentId());
        List<AssessmentAnswer> enrichedAnswers = buildEnrichedAnswers(attempt.getAssessmentId(), attemptId, rawAnswers);

        double totalMaxScore = 0;
        for (AssessmentAnswer ans : enrichedAnswers) {
            if (ans.getQuestion() != null) {
                totalMaxScore += ans.getQuestion().getMaxScore();
            }
        }

        request.setAttribute("attempt", attempt);
        request.setAttribute("assessment", assessment);
        request.setAttribute("activity", activity);
        request.setAttribute("student", student);
        request.setAttribute("submittedAtDisplay", DateTimeUtil.format(attempt.getSubmittedAt()));
        request.setAttribute("totalMaxScore", totalMaxScore);
        request.setAttribute("answers", enrichedAnswers);
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/grade.jsp")
                .forward(request, response);
    }

    private void showTakePage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        if (assessmentId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID bài đánh giá không hợp lệ.");
            return;
        }
        Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy thông tin sinh viên.");
            return;
        }
        if (!assessmentService.canStudentTake(assessmentId, student.getId())) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", "Bạn chưa đủ điều kiện làm bài hoặc bài đánh giá đã đóng.");
            response.sendRedirect(request.getContextPath() + "/assessment?action=list");
            return;
        }
        AssessmentAttempt attempt = assessmentService.startAttempt(assessmentId, student.getId());
        Assessment assessment = assessmentService.getAssessmentForStudentAttempt(assessmentId, student.getId());
        if (assessment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy bài đánh giá hoặc bài làm không thuộc về bạn.");
            return;
        }
        ExtracurricularActivity activity = activityDAO.findById(assessment.getActivityId());

        List<AssessmentQuestion> questions = questionDAO.findByAssessmentId(assessmentId);
        double totalMaxScore = 0;
        for (AssessmentQuestion q : questions) {
            totalMaxScore += q.getMaxScore();
            if (q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                q.setOptions(optionDAO.findByQuestionId(q.getId()));
            }
        }

        List<AssessmentAnswer> existingAnswers = assessmentService.getAttemptAnswers(attempt.getId(), student.getId());
        Map<Integer, AssessmentAnswer> savedAnswerMap = new HashMap<>();
        for (AssessmentAnswer ans : existingAnswers) {
            savedAnswerMap.put(ans.getQuestionId(), ans);
        }

        request.setAttribute("activityId", assessment.getActivityId());
        request.setAttribute("activity", activity);
        request.setAttribute("assessment", assessment);
        request.setAttribute("assessmentId", assessmentId);
        request.setAttribute("attempt", attempt);
        request.setAttribute("questions", questions);
        request.setAttribute("totalMaxScore", totalMaxScore);
        request.setAttribute("savedAnswerMap", savedAnswerMap);
        request.setAttribute("openTimeDisplay", DateTimeUtil.format(assessment.getOpenTime()));
        request.setAttribute("closeTimeDisplay", DateTimeUtil.format(assessment.getCloseTime()));
        request.getRequestDispatcher("/WEB-INF/views/assessment/student/take.jsp")
                .forward(request, response);
    }

    private void showResultPage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        if (assessmentId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID bài đánh giá không hợp lệ.");
            return;
        }
        Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy thông tin sinh viên.");
            return;
        }
        AssessmentAttempt attempt = assessmentService.getMyAttempt(assessmentId, student.getId());
        if (attempt == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Bạn chưa có bài làm cho bài đánh giá này.");
            return;
        }
        Assessment assessment = assessmentDAO.findById(assessmentId);
        ExtracurricularActivity activity = assessment != null ? activityDAO.findById(assessment.getActivityId()) : null;

        List<AssessmentAnswer> rawAnswers = assessmentService.getAttemptAnswers(attempt.getId(), student.getId());
        List<AssessmentAnswer> enrichedAnswers = buildEnrichedAnswers(assessmentId, attempt.getId(), rawAnswers);

        double totalMaxScore = 0;
        for (AssessmentAnswer ans : enrichedAnswers) {
            if (ans.getQuestion() != null) {
                totalMaxScore += ans.getQuestion().getMaxScore();
            }
        }

        request.setAttribute("assessment", assessment);
        request.setAttribute("activity", activity);
        request.setAttribute("activityId", assessment != null ? assessment.getActivityId() : 0);
        request.setAttribute("attempt", attempt);
        request.setAttribute("submittedAtDisplay", DateTimeUtil.format(attempt.getSubmittedAt()));
        request.setAttribute("totalMaxScore", totalMaxScore);
        request.setAttribute("answers", enrichedAnswers);
        request.getRequestDispatcher("/WEB-INF/views/assessment/student/result.jsp")
                .forward(request, response);
    }

    private List<AssessmentAnswer> buildEnrichedAnswers(int assessmentId, int attemptId,
            List<AssessmentAnswer> rawAnswers) {
        List<AssessmentQuestion> questions = questionDAO.findByAssessmentId(assessmentId);
        Map<Integer, AssessmentAnswer> byQuestionId = new HashMap<>();
        for (AssessmentAnswer ans : rawAnswers) {
            byQuestionId.put(ans.getQuestionId(), ans);
        }

        List<AssessmentAnswer> result = new ArrayList<>();
        for (AssessmentQuestion q : questions) {
            List<AssessmentOption> options = List.of();
            if (q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                options = optionDAO.findByQuestionId(q.getId());
                q.setOptions(options);
            }

            AssessmentAnswer ans = byQuestionId.get(q.getId());
            if (ans == null) {
                ans = new AssessmentAnswer();
                ans.setAttemptId(attemptId);
                ans.setQuestionId(q.getId());
                ans.setGradingStatus(q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ
                        ? AssessmentAnswer.GradingStatus.GRADED
                        : AssessmentAnswer.GradingStatus.PENDING_GRADING);
            }
            ans.setQuestion(q);

            if (q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                AssessmentOption chosen = null;
                if (ans.getSelectedOptionId() != null) {
                    for (AssessmentOption opt : options) {
                        if (opt.getId() == ans.getSelectedOptionId()) {
                            chosen = opt;
                            break;
                        }
                    }
                }
                ans.setSelectedOption(chosen);
                if (ans.getAwardedScore() == null) {
                    ans.setAwardedScore(chosen != null && chosen.isCorrect() ? q.getMaxScore() : 0.0);
                }
            }
            result.add(ans);
        }
        return result;
    }

    private record ParsedOption(String optionText, boolean correct) {}

    private record ParsedQuestion(AssessmentQuestion.QuestionType type, String content,
                                  BigDecimal maxScore, List<ParsedOption> options) {}

    private void handleSaveFull(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        if (!AccessPolicy.canManageActivities(user)) {
            throw new AssessmentServiceException("Bạn không có quyền quản lý bài đánh giá.");
        }

        int id = parseId(request.getParameter("id"));
        int activityId = parseId(request.getParameter("activityId"));
        boolean publishAfterSave = Boolean.parseBoolean(request.getParameter("publishAfterSave"));

        Assessment existing = id > 0 ? assessmentDAO.findById(id) : null;
        if (id > 0 && existing == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        if (existing != null) {
            activityId = existing.getActivityId();
        }
        if (activityId <= 0) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", "Vui lòng chọn hoạt động ngoại khóa.");
            response.sendRedirect(request.getContextPath() + "/assessment?action=manage&mode=create");
            return;
        }

        ExtracurricularActivity rawAct = activityDAO.findById(activityId);
        if (rawAct == null) {
            throw new AssessmentServiceException("Không tìm thấy hoạt động.");
        }
        if (!AccessPolicy.hasType(user, "ADMIN")
                && !activityDAO.isInStaffActivityScope(activityId, user.getId(), user.getRole())) {
            throw new AssessmentServiceException("Bạn không có quyền quản lý hoạt động này.");
        }

        if (existing == null) {
            Assessment byActivity = assessmentDAO.findByActivityId(activityId);
            if (byActivity != null) {
                existing = byActivity;
                id = byActivity.getId();
            }
        }

        String modeOnError = existing != null ? "edit" : "create";
        String errorRedirectUrl = request.getContextPath()
                + "/assessment?action=manage&activityId=" + activityId + "&mode=" + modeOnError;

        String title = request.getParameter("title");
        if (title == null || title.trim().isEmpty()) {
            request.getSession().setAttribute("errorMessage", "Tiêu đề bài đánh giá không được để trống.");
            response.sendRedirect(errorRedirectUrl);
            return;
        }
        String description = request.getParameter("description");
        LocalDateTime openTime;
        LocalDateTime closeTime;
        try {
            openTime = parseDateTime(request.getParameter("openTime"));
            closeTime = parseDateTime(request.getParameter("closeTime"));
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(errorRedirectUrl);
            return;
        }
        if (openTime != null && closeTime != null && !closeTime.isAfter(openTime)) {
            request.getSession().setAttribute("errorMessage", "Thời gian đóng bài phải sau thời gian mở bài.");
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        boolean hasAttempts = existing != null && !attemptDAO.findByAssessmentId(existing.getId()).isEmpty();
        List<ParsedQuestion> parsedQuestions = new ArrayList<>();

        if (!hasAttempts) {
            int questionCount = parseId(request.getParameter("questionCount"));
            for (int i = 0; i < questionCount; i++) {
                String typeStr = request.getParameter("questionType_" + i);
                String content = request.getParameter("questionContent_" + i);
                String scoreStr = request.getParameter("questionScore_" + i);
                if (typeStr == null && content == null && scoreStr == null) {
                    continue;
                }

                int qNumber = parsedQuestions.size() + 1;
                if (content == null || content.trim().isEmpty()) {
                    request.getSession().setAttribute("errorMessage",
                            "Câu hỏi số " + qNumber + " chưa nhập nội dung.");
                    response.sendRedirect(errorRedirectUrl + "#question-builder");
                    return;
                }

                AssessmentQuestion.QuestionType qType;
                try {
                    qType = AssessmentQuestion.QuestionType.valueOf(typeStr != null ? typeStr.trim() : "MCQ");
                } catch (Exception e) {
                    qType = AssessmentQuestion.QuestionType.MCQ;
                }

                BigDecimal maxScore;
                try {
                    maxScore = new BigDecimal(scoreStr != null ? scoreStr.trim() : "0");
                } catch (NumberFormatException e) {
                    maxScore = BigDecimal.ZERO;
                }
                if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
                    request.getSession().setAttribute("errorMessage",
                            "Điểm tối đa của câu hỏi số " + qNumber + " phải lớn hơn 0.");
                    response.sendRedirect(errorRedirectUrl + "#question-builder");
                    return;
                }

                List<ParsedOption> options = new ArrayList<>();
                if (qType == AssessmentQuestion.QuestionType.MCQ) {
                    int choiceCount = parseId(request.getParameter("choiceCount_" + i));
                    int correctChoice = -1;
                    String correctStr = request.getParameter("correctChoice_" + i);
                    if (correctStr != null && !correctStr.trim().isEmpty()) {
                        try {
                            correctChoice = Integer.parseInt(correctStr.trim());
                        } catch (NumberFormatException ignored) {
                            correctChoice = -1;
                        }
                    }

                    int correctCount = 0;
                    for (int j = 0; j < choiceCount; j++) {
                        String optText = request.getParameter("choice_" + i + "_" + j);
                        if (optText == null || optText.trim().isEmpty()) {
                            request.getSession().setAttribute("errorMessage",
                                    "Phương án trả lời ở câu hỏi số " + qNumber + " không được để trống.");
                            response.sendRedirect(errorRedirectUrl + "#question-builder");
                            return;
                        }
                        boolean isCorrect = (j == correctChoice);
                        if (isCorrect) {
                            correctCount++;
                        }
                        options.add(new ParsedOption(optText.trim(), isCorrect));
                    }

                    if (options.size() < 2) {
                        request.getSession().setAttribute("errorMessage",
                                "Câu trắc nghiệm số " + qNumber + " cần có ít nhất 2 phương án trả lời.");
                        response.sendRedirect(errorRedirectUrl + "#question-builder");
                        return;
                    }
                    if (correctCount != 1) {
                        request.getSession().setAttribute("errorMessage",
                                "Vui lòng đánh dấu chính xác 1 đáp án đúng cho câu trắc nghiệm số " + qNumber + ".");
                        response.sendRedirect(errorRedirectUrl + "#question-builder");
                        return;
                    }
                }

                parsedQuestions.add(new ParsedQuestion(qType, content.trim(), maxScore, options));
            }
        }

        boolean targetPublished = publishAfterSave || (existing != null && existing.isPublished());
        if (targetPublished && !hasAttempts && parsedQuestions.isEmpty()) {
            request.getSession().setAttribute("errorMessage",
                    "Bài đánh giá cần có ít nhất 1 câu hỏi để công bố cho sinh viên.");
            response.sendRedirect(errorRedirectUrl + "#question-builder");
            return;
        }
        if (targetPublished && hasAttempts && existing != null && !existing.isPublished()) {
            List<AssessmentQuestion> dbQuestions = questionDAO.findByAssessmentId(existing.getId());
            if (dbQuestions.isEmpty()) {
                request.getSession().setAttribute("errorMessage",
                        "Không thể công bố bài đánh giá chưa có câu hỏi.");
                response.sendRedirect(errorRedirectUrl);
                return;
            }
        }

        boolean wasNew = (existing == null);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int savedAssessmentId;
                if (wasNew) {
                    String insertAssessmentSql = "INSERT INTO assessment (activity_id, title, description, open_time, close_time, is_published, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2), CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2))";
                    try (PreparedStatement ps = conn.prepareStatement(insertAssessmentSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, activityId);
                        ps.setString(2, title.trim());
                        ps.setString(3, description == null || description.trim().isEmpty() ? null : description.trim());
                        if (openTime != null) {
                            ps.setTimestamp(4, Timestamp.valueOf(openTime));
                        } else {
                            ps.setNull(4, Types.TIMESTAMP);
                        }
                        if (closeTime != null) {
                            ps.setTimestamp(5, Timestamp.valueOf(closeTime));
                        } else {
                            ps.setNull(5, Types.TIMESTAMP);
                        }
                        ps.setBoolean(6, targetPublished);
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("Không lấy được ID bài đánh giá vừa tạo.");
                            }
                            savedAssessmentId = keys.getInt(1);
                        }
                    }
                } else {
                    savedAssessmentId = existing.getId();
                    String updateAssessmentSql = "UPDATE assessment SET title = ?, description = ?, open_time = ?, close_time = ?, is_published = ?, "
                            + "updated_at = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2) WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateAssessmentSql)) {
                        ps.setString(1, title.trim());
                        ps.setString(2, description == null || description.trim().isEmpty() ? null : description.trim());
                        if (openTime != null) {
                            ps.setTimestamp(3, Timestamp.valueOf(openTime));
                        } else {
                            ps.setNull(3, Types.TIMESTAMP);
                        }
                        if (closeTime != null) {
                            ps.setTimestamp(4, Timestamp.valueOf(closeTime));
                        } else {
                            ps.setNull(4, Types.TIMESTAMP);
                        }
                        ps.setBoolean(5, targetPublished);
                        ps.setInt(6, savedAssessmentId);
                        ps.executeUpdate();
                    }
                }

                if (!hasAttempts) {
                    String deleteOptionsSql = "DELETE FROM assessment_option WHERE question_id IN (SELECT id FROM assessment_question WHERE assessment_id = ?)";
                    try (PreparedStatement ps = conn.prepareStatement(deleteOptionsSql)) {
                        ps.setInt(1, savedAssessmentId);
                        ps.executeUpdate();
                    }
                    String deleteQuestionsSql = "DELETE FROM assessment_question WHERE assessment_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteQuestionsSql)) {
                        ps.setInt(1, savedAssessmentId);
                        ps.executeUpdate();
                    }

                    String insertQuestionSql = "INSERT INTO assessment_question (assessment_id, question_type, content, max_score, display_order) VALUES (?, ?, ?, ?, ?)";
                    String insertOptionSql = "INSERT INTO assessment_option (question_id, option_text, is_correct, display_order) VALUES (?, ?, ?, ?)";

                    for (int qIdx = 0; qIdx < parsedQuestions.size(); qIdx++) {
                        ParsedQuestion pq = parsedQuestions.get(qIdx);
                        int insertedQuestionId;
                        try (PreparedStatement psQ = conn.prepareStatement(insertQuestionSql, Statement.RETURN_GENERATED_KEYS)) {
                            psQ.setInt(1, savedAssessmentId);
                            psQ.setString(2, pq.type().name());
                            psQ.setString(3, pq.content());
                            psQ.setBigDecimal(4, pq.maxScore());
                            psQ.setInt(5, qIdx + 1);
                            psQ.executeUpdate();
                            try (ResultSet qKeys = psQ.getGeneratedKeys()) {
                                if (!qKeys.next()) {
                                    throw new SQLException("Không lấy được ID câu hỏi vừa tạo.");
                                }
                                insertedQuestionId = qKeys.getInt(1);
                            }
                        }

                        if (pq.type() == AssessmentQuestion.QuestionType.MCQ) {
                            try (PreparedStatement psOpt = conn.prepareStatement(insertOptionSql)) {
                                for (int oIdx = 0; oIdx < pq.options().size(); oIdx++) {
                                    ParsedOption po = pq.options().get(oIdx);
                                    psOpt.setInt(1, insertedQuestionId);
                                    psOpt.setString(2, po.optionText());
                                    psOpt.setBoolean(3, po.correct());
                                    psOpt.setInt(4, oIdx + 1);
                                    psOpt.addBatch();
                                }
                                psOpt.executeBatch();
                            }
                        }
                    }
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi lưu bài đánh giá và danh sách câu hỏi.", e);
        }

        HttpSession session = request.getSession();
        if (publishAfterSave) {
            session.setAttribute("successMessage",
                    wasNew ? "Đã tạo và công bố bài đánh giá cho sinh viên." : "Đã lưu thay đổi và công bố bài đánh giá.");
        } else {
            session.setAttribute("successMessage",
                    wasNew ? "Đã tạo bài đánh giá và lưu danh sách câu hỏi thành công." : "Đã cập nhật bài đánh giá và câu hỏi thành công.");
        }
        response.sendRedirect(request.getContextPath() + "/assessment?action=manage&activityId=" + activityId);
    }

    private void redirectToManageByAssessmentId(HttpServletRequest request, HttpServletResponse response,
            int assessmentId, CurrentUser user) throws IOException {
        Assessment a = assessmentId > 0 ? assessmentService.getAssessmentById(assessmentId, user) : null;
        if (a != null) {
            response.sendRedirect(request.getContextPath()
                    + "/assessment?action=manage&activityId=" + a.getActivityId() + "&mode=edit#question-builder");
        } else {
            response.sendRedirect(request.getContextPath() + "/assessment?action=list");
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int activityId = parseId(request.getParameter("activityId"));
        if (activityId <= 0) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", "Vui lòng chọn hoạt động ngoại khóa.");
            response.sendRedirect(request.getContextPath() + "/assessment?action=manage&mode=create");
            return;
        }
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String openTimeStr = request.getParameter("openTime");
        String closeTimeStr = request.getParameter("closeTime");

        LocalDateTime openTime;
        LocalDateTime closeTime;
        try {
            openTime = parseDateTime(openTimeStr);
            closeTime = parseDateTime(closeTimeStr);
        } catch (IllegalArgumentException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/assessment?action=manage&mode=create&activityId=" + activityId);
            return;
        }

        assessmentService.createAssessment(activityId, user, title, description, openTime, closeTime);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã tạo bài đánh giá thành công.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=manage&activityId=" + activityId);
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID bài đánh giá không hợp lệ.");
            return;
        }
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String openTimeStr = request.getParameter("openTime");
        String closeTimeStr = request.getParameter("closeTime");

        LocalDateTime openTime;
        LocalDateTime closeTime;
        try {
            openTime = parseDateTime(openTimeStr);
            closeTime = parseDateTime(closeTimeStr);
        } catch (IllegalArgumentException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", e.getMessage());
            redirectToManageByAssessmentId(request, response, id, user);
            return;
        }

        assessmentService.updateAssessment(id, user, title, description, openTime, closeTime);
        Assessment updated = assessmentService.getAssessmentById(id, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã cập nhật thông tin bài đánh giá thành công.");

        if (updated != null) {
            response.sendRedirect(request.getContextPath() + "/assessment?action=manage&activityId=" + updated.getActivityId());
        } else {
            response.sendRedirect(request.getContextPath() + "/assessment?action=list");
        }
    }

    private void handlePublish(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID bài đánh giá không hợp lệ.");
            return;
        }
        boolean publish = Boolean.parseBoolean(request.getParameter("publish"));
        assessmentService.publishAssessment(id, publish, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage",
                publish ? "Đã công bố bài đánh giá cho sinh viên." : "Đã hủy công bố bài đánh giá.");

        Assessment a = assessmentService.getAssessmentById(id, user);
        int activityId = a != null ? a.getActivityId() : 0;
        response.sendRedirect(request.getContextPath() + "/assessment?action=manage&activityId=" + activityId);
    }

    private void handleAddQuestion(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        String typeStr = request.getParameter("questionType");
        String content = request.getParameter("content");
        double maxScore = parseDouble(request.getParameter("maxScore"), 0);
        int displayOrder = parseId(request.getParameter("displayOrder"));

        AssessmentQuestion.QuestionType type;
        try {
            type = AssessmentQuestion.QuestionType.valueOf(typeStr);
        } catch (Exception e) {
            type = AssessmentQuestion.QuestionType.MCQ;
        }

        assessmentService.addQuestion(assessmentId, user, type, content, maxScore, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã thêm câu hỏi thành công.");
        redirectToManageByAssessmentId(request, response, assessmentId, user);
    }

    private void handleUpdateQuestion(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int questionId = parseId(request.getParameter("questionId"));
        String typeStr = request.getParameter("questionType");
        String content = request.getParameter("content");
        double maxScore = parseDouble(request.getParameter("maxScore"), 0);
        int displayOrder = parseId(request.getParameter("displayOrder"));

        AssessmentQuestion.QuestionType type;
        try {
            type = AssessmentQuestion.QuestionType.valueOf(typeStr);
        } catch (Exception e) {
            type = AssessmentQuestion.QuestionType.MCQ;
        }

        assessmentService.updateQuestion(questionId, user, type, content, maxScore, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã cập nhật câu hỏi thành công.");

        AssessmentQuestion q = questionDAO.findById(questionId);
        int assessmentId = q != null ? q.getAssessmentId() : 0;
        redirectToManageByAssessmentId(request, response, assessmentId, user);
    }

    private void handleDeleteQuestion(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int questionId = parseId(request.getParameter("questionId"));
        AssessmentQuestion q = questionDAO.findById(questionId);
        int assessmentId = q != null ? q.getAssessmentId() : 0;
        assessmentService.deleteQuestion(questionId, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã xóa câu hỏi thành công.");
        redirectToManageByAssessmentId(request, response, assessmentId, user);
    }

    private void handleAddOption(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int questionId = parseId(request.getParameter("questionId"));
        String optionText = request.getParameter("optionText");
        boolean correct = Boolean.parseBoolean(request.getParameter("isCorrect"));
        int displayOrder = parseId(request.getParameter("displayOrder"));

        assessmentService.addOption(questionId, user, optionText, correct, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã thêm phương án trả lời thành công.");
        AssessmentQuestion q = questionDAO.findById(questionId);
        redirectToManageByAssessmentId(request, response, q != null ? q.getAssessmentId() : 0, user);
    }

    private void handleUpdateOption(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int optionId = parseId(request.getParameter("optionId"));
        String optionText = request.getParameter("optionText");
        boolean correct = Boolean.parseBoolean(request.getParameter("isCorrect"));
        int displayOrder = parseId(request.getParameter("displayOrder"));

        assessmentService.updateOption(optionId, user, optionText, correct, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã cập nhật phương án trả lời thành công.");

        AssessmentOption o = optionDAO.findById(optionId);
        AssessmentQuestion q = o != null ? questionDAO.findById(o.getQuestionId()) : null;
        redirectToManageByAssessmentId(request, response, q != null ? q.getAssessmentId() : 0, user);
    }

    private void handleDeleteOption(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int optionId = parseId(request.getParameter("optionId"));
        AssessmentOption o = optionDAO.findById(optionId);
        AssessmentQuestion q = o != null ? questionDAO.findById(o.getQuestionId()) : null;
        assessmentService.deleteOption(optionId, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã xóa phương án trả lời thành công.");
        redirectToManageByAssessmentId(request, response, q != null ? q.getAssessmentId() : 0, user);
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int id = parseId(request.getParameter("id"));
        Assessment a = assessmentService.getAssessmentById(id, user);
        if (a == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy bài đánh giá.");
            return;
        }
        boolean deleted = assessmentService.deleteAssessment(id, user);
        HttpSession session = request.getSession();
        if (deleted) {
            session.setAttribute("successMessage", "Đã xóa bài đánh giá thành công.");
            response.sendRedirect(request.getContextPath() + "/assessment?action=list");
        } else {
            session.setAttribute("errorMessage", "Không thể xóa bài đánh giá. Vui lòng thử lại.");
            response.sendRedirect(request.getContextPath() + "/assessment?action=manage&activityId=" + a.getActivityId());
        }
    }

    private void handleStartAttempt(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy thông tin sinh viên.");
            return;
        }
        assessmentService.startAttempt(assessmentId, student.getId());
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã khởi tạo bài làm.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=take&assessmentId=" + assessmentId);
    }

    private void handleSaveAnswers(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int attemptId = parseId(request.getParameter("attemptId"));

        Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy thông tin sinh viên.");
            return;
        }
        int studentId = student.getId();

        Map<Integer, Object> answers = extractQuestionAnswers(request);
        assessmentService.saveAnswers(attemptId, studentId, answers);

        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã lưu nháp câu trả lời của bạn.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=take&assessmentId="
                + parseId(request.getParameter("assessmentId")));
    }

    private void handleSubmitAttempt(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int attemptId = parseId(request.getParameter("attemptId"));

        Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không tìm thấy thông tin sinh viên.");
            return;
        }
        int studentId = student.getId();

        Map<Integer, Object> answers = extractQuestionAnswers(request);
        assessmentService.saveAnswers(attemptId, studentId, answers);

        AssessmentAttempt attempt = assessmentService.submitAttempt(attemptId, studentId);
        int assessmentId = attempt != null ? attempt.getAssessmentId() : 0;

        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã nộp bài đánh giá thành công.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=result&assessmentId=" + assessmentId);
    }

    private Map<Integer, Object> extractQuestionAnswers(HttpServletRequest request) {
        Map<Integer, Object> answers = new HashMap<>();
        for (String key : request.getParameterMap().keySet()) {
            if (key.startsWith("q_")) {
                try {
                    int questionId = Integer.parseInt(key.substring(2));
                    answers.put(questionId, request.getParameter(key));
                } catch (NumberFormatException ignored) {
                    // skip invalid keys
                }
            }
        }
        return answers;
    }

    private void handleGradeEssay(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int answerId = parseId(request.getParameter("answerId"));
        String fromParam = "manage".equals(request.getParameter("from")) ? "&from=manage" : "";
        String scoreStr = request.getParameter("score");
        BigDecimal score;
        try {
            score = new BigDecimal(scoreStr);
        } catch (NumberFormatException | NullPointerException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", "Điểm nhập vào không hợp lệ.");
            AssessmentAnswer ans = answerDAO.findById(answerId);
            int attemptId2 = ans != null ? ans.getAttemptId() : 0;
            response.sendRedirect(request.getContextPath() + "/assessment?action=grade&attemptId=" + attemptId2 + fromParam);
            return;
        }
        String feedback = request.getParameter("feedback");
        assessmentService.gradeEssay(answerId, score, feedback, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đã lưu điểm chấm câu tự luận thành công.");

        AssessmentAnswer ans = answerDAO.findById(answerId);
        int attemptId2 = ans != null ? ans.getAttemptId() : 0;
        response.sendRedirect(request.getContextPath() + "/assessment?action=grade&attemptId=" + attemptId2 + fromParam);
    }

    private Student getCurrentStudent(CurrentUser user) {
        return new StudentServiceImpl().getStudentByAccountId(user.getId());
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

    private String getCsrfToken(HttpServletRequest request) {
        var session = request.getSession();
        String token = (String) session.getAttribute(CSRF_SESSION_KEY);
        if (token == null || token.isEmpty()) {
            token = UUID.randomUUID().toString();
            session.setAttribute(CSRF_SESSION_KEY, token);
        }
        return token;
    }

    private boolean isValidCsrfToken(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(CSRF_SESSION_KEY);
        String supplied = request.getParameter("_csrf");
        if (supplied == null || supplied.isEmpty()) {
            return false;
        }
        return expected instanceof String token
                && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DT_FORMAT);
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng thời gian không hợp lệ: " + value, e);
        }
    }

    private double parseDouble(String value, double def) {
        if (value == null || value.trim().isEmpty()) {
            return def;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
