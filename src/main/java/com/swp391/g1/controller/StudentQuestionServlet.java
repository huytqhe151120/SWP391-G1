package com.swp391.g1.controller;

import com.swp391.g1.dao.AnswerDAO;
import com.swp391.g1.dao.QuestionDAO;
import com.swp391.g1.dao.StudentDAO;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Answer;
import com.swp391.g1.model.Question;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
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

    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AnswerDAO answerDAO = new AnswerDAO();
    private final StudentDAO studentDAO = new StudentDAO();

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
        if (title.isEmpty() || content.isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập tiêu đề và nội dung câu hỏi.");
        } else if (title.length() > MAX_TITLE_LENGTH || content.length() > MAX_CONTENT_LENGTH) {
            request.setAttribute("error", "Tiêu đề tối đa 200 ký tự và nội dung tối đa 4.000 ký tự.");
        } else {
            Question question = new Question();
            question.setTitle(title);
            question.setContent(content);
            question.setAnonymous("on".equals(request.getParameter("anonymous")));
            question.setStudentId(student.getId());
            if (questionDAO.insert(question)) {
                response.sendRedirect(request.getContextPath() + "/student-qa?msg=created");
                return;
            }
            request.setAttribute("error", "Không thể gửi câu hỏi lúc này. Vui lòng thử lại.");
        }

        request.setAttribute("submittedTitle", title);
        request.setAttribute("submittedContent", content);
        request.setAttribute("submittedAnonymous", "on".equals(request.getParameter("anonymous")));
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
        Student student = studentDAO.getByAccountId(user.getId());
        if (student == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Không tìm thấy hồ sơ sinh viên của tài khoản.");
        }
        return student;
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, Student student)
            throws ServletException, IOException {
        List<Question> questions = questionDAO.getVisibleQuestionsForStudent(student.getId());
        Map<Integer, List<Answer>> answersByQuestion = new LinkedHashMap<>();
        for (Question question : questions) {
            if ("ANSWERED".equals(question.getStatus())) {
                answersByQuestion.put(question.getId(), answerDAO.getPublishedByQuestionId(question.getId()));
            }
        }
        request.setAttribute("student", student);
        request.setAttribute("questions", questions);
        request.setAttribute("answersByQuestion", answersByQuestion);
        request.getRequestDispatcher("/WEB-INF/views/qa/student-qa.jsp").forward(request, response);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
