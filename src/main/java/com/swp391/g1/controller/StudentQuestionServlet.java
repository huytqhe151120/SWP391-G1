package com.swp391.g1.controller;

import com.swp391.g1.dao.IAnswerDAO;
import com.swp391.g1.dao.IQuestionDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.impl.AnswerDAOImpl;
import com.swp391.g1.dao.impl.QuestionDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Answer;
import com.swp391.g1.model.Question;
import com.swp391.g1.model.Student;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.Enum;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/student-qa")
public class StudentQuestionServlet extends HttpServlet {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 4000;

    private final IQuestionDAO questionDAO = new QuestionDAOImpl();
    private final IAnswerDAO answerDAO = new AnswerDAOImpl();
    private final IStudentService studentService = new StudentServiceImpl();
    private final IExtracurricularActivityDAO activityDAO = new ExtracurricularActivityDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Student student = resolveStudent(request, response);
        if (student == null) return;

        showPage(request, response, student);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Student student = resolveStudent(request, response);
        if (student == null) return;

        String title = normalize(request.getParameter("title"));
        String content = normalize(request.getParameter("content"));
        Integer activityId = parsePositiveInt(request.getParameter("activityId"));
        ExtracurricularActivity activity = activityId == null ? null : activityDAO.findById(activityId);
        if (activity == null || activity.getApprovalStatus() != Enum.ApprovalStatus.APPROVED) {
            request.setAttribute("error", "Vui lòng chọn một hoạt động đã được phê duyệt.");
        } else if (title.isEmpty() || content.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập tiêu đề và nội dung câu hỏi.");
        } else if (title.length() > MAX_TITLE_LENGTH || content.length() > MAX_CONTENT_LENGTH) {
            request.setAttribute("error", "Tiêu đề tối đa 200 ký tự và nội dung tối đa 4.000 ký tự.");
        } else {
            Question question = new Question();
            question.setTitle(title);
            question.setContent(content);
            question.setAnonymous("on".equals(request.getParameter("anonymous")));
            question.setStudentId(student.getId());
            question.setActivityId(activityId);
            if (questionDAO.insert(question)) {
                response.sendRedirect(request.getContextPath() + ("activity".equals(request.getParameter("returnTo"))
                        ? "/activities?action=detail&id=" + activityId + "&msg=question-created#activity-qa"
                        : "/student-qa?activityId=" + activityId + "&msg=created"));
                return;
            }
            request.setAttribute("error", "Không thể gửi câu hỏi lúc này. Vui lòng thử lại.");
        }

        request.setAttribute("submittedTitle", title);
        request.setAttribute("submittedContent", content);
        request.setAttribute("submittedAnonymous", "on".equals(request.getParameter("anonymous")));
        request.setAttribute("submittedActivityId", activityId);
        showPage(request, response, student);
    }

    private Student resolveStudent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        if (!AccessPolicy.canUseStudentQuestions(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ sinh viên được truy cập trang Hỏi & Đáp này.");
            return null;
        }
        Student student = studentService.getStudentByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Không tìm thấy hồ sơ sinh viên của tài khoản.");
        }
        return student;
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, Student student)
            throws ServletException, IOException {
        Integer activityId = parsePositiveInt(request.getParameter("activityId"));
        if (request.getParameter("activityId") != null && activityId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (activityId != null) {
            ExtracurricularActivity selected = activityDAO.findById(activityId);
            if (selected == null || selected.getApprovalStatus() != Enum.ApprovalStatus.APPROVED) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("selectedActivity", selected);
            request.setAttribute("submittedActivityId", activityId);
        }
        List<Question> questions = activityId == null
                ? questionDAO.getVisibleQuestionsForStudent(student.getId())
                : questionDAO.getVisibleQuestionsForStudent(student.getId(), activityId);
        Map<Integer, List<Answer>> answersByQuestion = new LinkedHashMap<>();
        for (Question question : questions) {
            if ("ANSWERED".equals(question.getStatus())) {
                answersByQuestion.put(question.getId(), answerDAO.getPublishedByQuestionId(question.getId()));
            }
        }
        request.setAttribute("student", student);
        request.setAttribute("questions", questions);
        request.setAttribute("answersByQuestion", answersByQuestion);
        request.setAttribute("activities", activityDAO.findByApprovalStatus(Enum.ApprovalStatus.APPROVED));
        request.getRequestDispatcher("/WEB-INF/views/qa/student-qa.jsp").forward(request, response);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
