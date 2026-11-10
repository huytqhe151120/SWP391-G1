package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.AssessmentAttempt;
import com.swp391.g1.model.AssessmentAnswer;
import com.swp391.g1.model.AssessmentQuestion;
import com.swp391.g1.model.AssessmentOption;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.AssessmentServiceException;
import com.swp391.g1.service.IAssessmentService;
import com.swp391.g1.service.impl.AssessmentServiceImpl;
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
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@WebServlet(name = "AssessmentServlet", urlPatterns = {"/assessment"})
public class AssessmentServlet extends HttpServlet {

    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private static final String CSRF_SESSION_KEY = "com.swp391.g1.assessment.csrfToken";

    private final IAssessmentService assessmentService = new AssessmentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "";

        request.setAttribute("csrfToken", getCsrfToken(request));

        try {
            switch (action) {
                case "create":
                    showCreateForm(request, response, user);
                    break;
                case "edit":
                    showEditForm(request, response, user);
                    break;
                case "manage":
                    showManagePage(request, response, user);
                    break;
                case "questions":
                    showQuestionManagement(request, response, user);
                    break;
                case "options":
                    showOptionManagement(request, response, user);
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
                    response.sendRedirect(request.getContextPath() + "/activities");
                    break;
            }
        } catch (AssessmentServiceException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activities");
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
        if (action == null) action = "";

        if (!isValidCsrfToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token khong hop le.");
            return;
        }

        try {
            switch (action) {
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
            }
        } catch (AssessmentServiceException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", e.getMessage());
            String referer = request.getHeader("Referer");
            if (referer != null) {
                response.sendRedirect(referer);
            } else {
                response.sendRedirect(request.getContextPath() + "/activities");
            }
        }
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int activityId = parseId(request.getParameter("activityId"));
        if (activityId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID hoat dong khong hop le.");
            return;
        }
        if (!AccessPolicy.canManageActivities(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Ban khong co quyen tao danh gia.");
            return;
        }
        request.setAttribute("activityId", activityId);
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/create.jsp")
                .forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh gia khong hop le.");
            return;
        }
        com.swp391.g1.model.Assessment assessment = assessmentService.getAssessmentById(id, user);
        if (assessment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay danh gia.");
            return;
        }
        request.setAttribute("assessment", assessment);
        request.setAttribute("openTimeDisplay", assessment.getOpenTime() != null
                ? DT_FORMAT.format(assessment.getOpenTime()) : "");
        request.setAttribute("closeTimeDisplay", assessment.getCloseTime() != null
                ? DT_FORMAT.format(assessment.getCloseTime()) : "");
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/edit.jsp")
                .forward(request, response);
    }

    private void showManagePage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int activityId = parseId(request.getParameter("activityId"));
        if (activityId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID hoat dong khong hop le.");
            return;
        }
        com.swp391.g1.model.Assessment assessment = assessmentService.getAssessmentByActivityId(activityId, user);
        if (assessment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay danh gia.");
            return;
        }
        request.setAttribute("assessment", assessment);
        request.setAttribute("openTimeDisplay", DateTimeUtil.format(assessment.getOpenTime()));
        request.setAttribute("closeTimeDisplay", DateTimeUtil.format(assessment.getCloseTime()));
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/manage.jsp")
                .forward(request, response);
    }

    private void showQuestionManagement(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        if (assessmentId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh gia khong hop le.");
            return;
        }
        var questions = assessmentService.listQuestions(assessmentId, user);
        request.setAttribute("assessmentId", assessmentId);
        request.setAttribute("questions", questions);
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/questions.jsp")
                .forward(request, response);
    }

    private void showOptionManagement(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int questionId = parseId(request.getParameter("questionId"));
        if (questionId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID cau hoi khong hop le.");
            return;
        }
        var options = assessmentService.listOptions(questionId, user);
        request.setAttribute("questionId", questionId);
        request.setAttribute("options", options);
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/options.jsp")
                .forward(request, response);
    }

    private void showSubmissions(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int activityId = parseId(request.getParameter("activityId"));
        if (activityId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID hoat dong khong hop le.");
            return;
        }
        var submissions = assessmentService.listSubmissionsForGrading(activityId, user);
        java.util.Map<Integer, String> submittedAtDisplay = new java.util.HashMap<>();
        for (var s : submissions) {
            submittedAtDisplay.put(s.getId(), DateTimeUtil.format(s.getSubmittedAt()));
        }
        request.setAttribute("activityId", activityId);
        request.setAttribute("submissions", submissions);
        request.setAttribute("submittedAtDisplay", submittedAtDisplay);
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/submissions.jsp")
                .forward(request, response);
    }

    private void showGradingPage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int attemptId = parseId(request.getParameter("attemptId"));
        if (attemptId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID bai lam khong hop le.");
            return;
        }
        var attempt = assessmentService.getAttemptForGrading(attemptId, user);
        if (attempt == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay bai lam.");
            return;
        }
        var answers = assessmentService.getAttemptAnswers(attemptId, attempt.getStudentId());
        request.setAttribute("attempt", attempt);
        request.setAttribute("answers", answers);
        request.getRequestDispatcher("/WEB-INF/views/assessment/staff/grade.jsp")
                .forward(request, response);
    }

    private void showTakePage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        if (assessmentId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh gia khong hop le.");
            return;
        }
        com.swp391.g1.model.Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong tim thay thong tin sinh vien.");
            return;
        }
        if (!assessmentService.canStudentTake(assessmentId, student.getId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Ban khong du dieu kien lam bai.");
            return;
        }
        var attempt = assessmentService.startAttempt(assessmentId, student.getId());
        com.swp391.g1.model.Assessment assessment = assessmentService.getAssessmentForStudentAttempt(assessmentId, student.getId());
        if (assessment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay danh gia hoac bai lam khong thuoc ban.");
            return;
        }
        request.setAttribute("activityId", assessment.getActivityId());
        var questions = new com.swp391.g1.dao.impl.AssessmentQuestionDAOImpl()
                .findByAssessmentId(assessmentId);
        for (var q : questions) {
            if (q.getQuestionType() == com.swp391.g1.model.AssessmentQuestion.QuestionType.MCQ) {
                q.setOptions(new com.swp391.g1.dao.impl.AssessmentOptionDAOImpl()
                        .findByQuestionId(q.getId()));
            }
        }
        request.setAttribute("assessmentId", assessmentId);
        request.setAttribute("attempt", attempt);
        request.setAttribute("questions", questions);
        request.getRequestDispatcher("/WEB-INF/views/assessment/student/take.jsp")
                .forward(request, response);
    }

    private void showResultPage(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws ServletException, IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        if (assessmentId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh gia khong hop le.");
            return;
        }
        com.swp391.g1.model.Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong tim thay thong tin sinh vien.");
            return;
        }
        var attempt = assessmentService.getMyAttempt(assessmentId, student.getId());
        if (attempt == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Chua co bai lam.");
            return;
        }
        var answers = assessmentService.getAttemptAnswers(attempt.getId(), student.getId());
        request.setAttribute("attempt", attempt);
        request.setAttribute("answers", answers);
        request.getRequestDispatcher("/WEB-INF/views/assessment/student/result.jsp")
                .forward(request, response);
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int activityId = parseId(request.getParameter("activityId"));
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
            response.sendRedirect(request.getContextPath() + "/assessment?action=create&activityId=" + activityId);
            return;
        }

        assessmentService.createAssessment(activityId, user, title, description, openTime, closeTime);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da tao danh gia thanh cong.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=manage&activityId=" + activityId);
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh gia khong hop le.");
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
            response.sendRedirect(request.getContextPath() + "/assessment?action=edit&id=" + id);
            return;
        }

        assessmentService.updateAssessment(id, user, title, description, openTime, closeTime);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da cap nhat danh gia thanh cong.");

        response.sendRedirect(request.getContextPath() + "/assessment?action=questions&assessmentId=" + id);
    }

    private void handlePublish(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh gia khong hop le.");
            return;
        }
        boolean publish = Boolean.parseBoolean(request.getParameter("publish"));
        assessmentService.publishAssessment(id, publish, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage",
                publish ? "Da cong bo danh gia." : "Da huy cong bo danh gia.");

        com.swp391.g1.model.Assessment a = assessmentService.getAssessmentById(id, user);
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

        com.swp391.g1.model.AssessmentQuestion.QuestionType type;
        try { type = com.swp391.g1.model.AssessmentQuestion.QuestionType.valueOf(typeStr); }
        catch (Exception e) { type = com.swp391.g1.model.AssessmentQuestion.QuestionType.MCQ; }

        assessmentService.addQuestion(assessmentId, user, type, content, maxScore, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da them cau hoi thanh cong.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=questions&assessmentId=" + assessmentId);
    }

    private void handleUpdateQuestion(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int questionId = parseId(request.getParameter("questionId"));
        String typeStr = request.getParameter("questionType");
        String content = request.getParameter("content");
        double maxScore = parseDouble(request.getParameter("maxScore"), 0);
        int displayOrder = parseId(request.getParameter("displayOrder"));

        com.swp391.g1.model.AssessmentQuestion.QuestionType type;
        try { type = com.swp391.g1.model.AssessmentQuestion.QuestionType.valueOf(typeStr); }
        catch (Exception e) { type = com.swp391.g1.model.AssessmentQuestion.QuestionType.MCQ; }

        assessmentService.updateQuestion(questionId, user, type, content, maxScore, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da cap nhat cau hoi thanh cong.");

        com.swp391.g1.model.AssessmentQuestion q = new com.swp391.g1.dao.impl.AssessmentQuestionDAOImpl().findById(questionId);
        int assessmentId = q != null ? q.getAssessmentId() : 0;
        response.sendRedirect(request.getContextPath() + "/assessment?action=questions&assessmentId=" + assessmentId);
    }

    private void handleDeleteQuestion(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int questionId = parseId(request.getParameter("questionId"));
        com.swp391.g1.model.AssessmentQuestion q = new com.swp391.g1.dao.impl.AssessmentQuestionDAOImpl().findById(questionId);
        int assessmentId = q != null ? q.getAssessmentId() : 0;
        assessmentService.deleteQuestion(questionId, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da xoa cau hoi thanh cong.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=questions&assessmentId=" + assessmentId);
    }

    private void handleAddOption(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int questionId = parseId(request.getParameter("questionId"));
        String optionText = request.getParameter("optionText");
        boolean correct = Boolean.parseBoolean(request.getParameter("isCorrect"));
        int displayOrder = parseId(request.getParameter("displayOrder"));

        assessmentService.addOption(questionId, user, optionText, correct, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da them phuong an thanh cong.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=options&questionId=" + questionId);
    }

    private void handleUpdateOption(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int optionId = parseId(request.getParameter("optionId"));
        String optionText = request.getParameter("optionText");
        boolean correct = Boolean.parseBoolean(request.getParameter("isCorrect"));
        int displayOrder = parseId(request.getParameter("displayOrder"));

        assessmentService.updateOption(optionId, user, optionText, correct, displayOrder);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da cap nhat phuong an thanh cong.");

        com.swp391.g1.model.AssessmentOption o = new com.swp391.g1.dao.impl.AssessmentOptionDAOImpl().findById(optionId);
        int questionId = o != null ? o.getQuestionId() : 0;
        response.sendRedirect(request.getContextPath() + "/assessment?action=options&questionId=" + questionId);
    }

    private void handleDeleteOption(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int optionId = parseId(request.getParameter("optionId"));
        com.swp391.g1.model.AssessmentOption o = new com.swp391.g1.dao.impl.AssessmentOptionDAOImpl().findById(optionId);
        int questionId = o != null ? o.getQuestionId() : 0;
        assessmentService.deleteOption(optionId, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da xoa phuong an thanh cong.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=options&questionId=" + questionId);
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int id = parseId(request.getParameter("id"));
        com.swp391.g1.model.Assessment a = assessmentService.getAssessmentById(id, user);
        if (a == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay danh gia.");
            return;
        }
        int activityId = a.getActivityId();
        boolean deleted = assessmentService.deleteAssessment(id, user);
        HttpSession session = request.getSession();
        if (deleted) {
            session.setAttribute("successMessage", "Da xoa danh gia thanh cong.");
        } else {
            session.setAttribute("errorMessage", "Khong the xoa danh gia. Vui long thu lai.");
        }
        response.sendRedirect(request.getContextPath() + "/assessment?action=manage&activityId=" + activityId);
    }
    private void handleStartAttempt(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int assessmentId = parseId(request.getParameter("assessmentId"));
        com.swp391.g1.model.Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong tim thay thong tin sinh vien.");
            return;
        }
        assessmentService.startAttempt(assessmentId, student.getId());
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da khoi tao bai lam.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=take&assessmentId=" + assessmentId);
    }

    private void handleSaveAnswers(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int attemptId = parseId(request.getParameter("attemptId"));

        com.swp391.g1.model.Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong tim thay thong tin sinh vien.");
            return;
        }
        int studentId = student.getId();

        Map<Integer, Object> answers = new HashMap<>();
        for (String key : request.getParameterMap().keySet()) {
            if (key.startsWith("q_")) {
                try {
                    int questionId = Integer.parseInt(key.substring(2));
                    answers.put(questionId, request.getParameter(key));
                } catch (NumberFormatException e) {
                    // skip invalid keys
                }
            }
        }

        assessmentService.saveAnswers(attemptId, studentId, answers);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da luu cau tra loi.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=take&assessmentId="
                + request.getParameter("assessmentId"));
    }

    private void handleSubmitAttempt(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int attemptId = parseId(request.getParameter("attemptId"));

        com.swp391.g1.model.Student student = getCurrentStudent(user);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong tim thay thong tin sinh vien.");
            return;
        }
        int studentId = student.getId();

        Map<Integer, Object> answers = new HashMap<>();
        for (String key : request.getParameterMap().keySet()) {
            if (key.startsWith("q_")) {
                try {
                    int questionId = Integer.parseInt(key.substring(2));
                    answers.put(questionId, request.getParameter(key));
                } catch (NumberFormatException e) {
                    // skip invalid keys
                }
            }
        }

        assessmentService.saveAnswers(attemptId, studentId, answers);

        com.swp391.g1.model.AssessmentAttempt attempt = assessmentService.submitAttempt(attemptId, studentId);
        int assessmentId = attempt != null ? attempt.getAssessmentId() : 0;

        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da nop bai thanh cong.");
        response.sendRedirect(request.getContextPath() + "/assessment?action=result&assessmentId=" + assessmentId);
    }

    private void handleGradeEssay(HttpServletRequest request, HttpServletResponse response,
            CurrentUser user) throws IOException {
        int answerId = parseId(request.getParameter("answerId"));
        String scoreStr = request.getParameter("score");
        BigDecimal score;
        try {
            score = new BigDecimal(scoreStr);
        } catch (NumberFormatException | NullPointerException e) {
            HttpSession session = request.getSession();
            session.setAttribute("errorMessage", "Diem khong hop le.");
            com.swp391.g1.model.AssessmentAnswer ans = new com.swp391.g1.dao.impl.AssessmentAnswerDAOImpl().findById(answerId);
            int attemptId2 = ans != null ? ans.getAttemptId() : 0;
            response.sendRedirect(request.getContextPath() + "/assessment?action=grade&attemptId=" + attemptId2);
            return;
        }
        String feedback = request.getParameter("feedback");
        assessmentService.gradeEssay(answerId, score, feedback, user);
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Da cham bai thanh cong.");

        com.swp391.g1.model.AssessmentAnswer ans = new com.swp391.g1.dao.impl.AssessmentAnswerDAOImpl().findById(answerId);
        int attemptId2 = ans != null ? ans.getAttemptId() : 0;
        response.sendRedirect(request.getContextPath() + "/assessment?action=grade&attemptId=" + attemptId2);
    }

    private com.swp391.g1.model.Student getCurrentStudent(CurrentUser user) {
        return new com.swp391.g1.service.impl.StudentServiceImpl()
                .getStudentByAccountId(user.getId());
    }

    private int parseId(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return 0; }
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
                && MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim(), DT_FORMAT);
        } catch (Exception e) {
            throw new IllegalArgumentException("Dinh dang thoi gian khong hop le: " + value, e);
        }
    }

    private double parseDouble(String value, double def) {
        if (value == null || value.trim().isEmpty()) return def;
        try { return Double.parseDouble(value.trim()); } catch (NumberFormatException e) { return def; }
    }

}