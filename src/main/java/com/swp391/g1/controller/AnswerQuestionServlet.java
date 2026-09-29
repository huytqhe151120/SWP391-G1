package com.swp391.g1.controller;

import com.swp391.g1.dao.AnswerDAO;
import com.swp391.g1.dao.QuestionDAO;
import com.swp391.g1.dao.StaffDAO;
import com.swp391.g1.model.Answer;
import com.swp391.g1.model.Question;
import com.swp391.g1.model.Staff;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * AnswerQuestionServlet - Row 17 in SonCN's assignment
 * Staff/Organizer views pending questions and posts answers.
 * Answers can be saved as DRAFT or immediately PUBLISHED to the public Q&A channel.
 *
 * URL patterns:
 *   GET  /qa-management          -> list all questions with status
 *   GET  /qa-management?action=view&id={qId} -> view question detail + existing answers
 *   POST /qa-management?action=answer        -> submit a new answer
 *   POST /qa-management?action=publish       -> publish (approve) a DRAFT answer
 *   POST /qa-management?action=delete        -> delete an answer
 */
@WebServlet("/qa-management")
public class AnswerQuestionServlet extends HttpServlet {

    // MOCK: In production, get this from HttpSession after login
    private static final int MOCK_STAFF_ID = 1;

    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AnswerDAO answerDAO = new AnswerDAO();
    private final StaffDAO staffDAO = new StaffDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("view".equals(action)) {
            // View a single question with all its answers
            String idParam = request.getParameter("id");
            if (idParam == null) {
                response.sendRedirect(request.getContextPath() + "/qa-management");
                return;
            }
            int questionId = Integer.parseInt(idParam);
            Question question = questionDAO.getById(questionId);
            List<Answer> answers = answerDAO.getByQuestionId(questionId);

            request.setAttribute("question", question);
            request.setAttribute("answers", answers);
            request.setAttribute("mockStaffId", MOCK_STAFF_ID);

            forward(request, response, "/WEB-INF/views/qa/answer-question.jsp");
        } else {
            // Default: list all questions
            String filter = request.getParameter("filter");
            List<Question> questions;
            if ("pending".equals(filter)) {
                questions = questionDAO.getPendingQuestions();
            } else {
                questions = questionDAO.getAllQuestions();
            }

            request.setAttribute("questions", questions);
            request.setAttribute("filter", filter == null ? "all" : filter);
            forward(request, response, "/WEB-INF/views/qa/qa-management.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        switch (action == null ? "" : action) {
            case "answer" -> handleAnswer(request, response);
            case "publish" -> handlePublish(request, response);
            case "delete" -> handleDelete(request, response);
            default -> response.sendRedirect(request.getContextPath() + "/qa-management");
        }
    }

    private void handleAnswer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int questionId = Integer.parseInt(request.getParameter("questionId"));
        String content = request.getParameter("content");
        String publishNow = request.getParameter("publishNow"); // "1" if publish immediately

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setStaffId(MOCK_STAFF_ID);
        answer.setContent(content);

        boolean saved = answerDAO.insert(answer);

        if (saved && "1".equals(publishNow)) {
            // Get the latest answer for this question by this staff and publish it
            List<Answer> answers = answerDAO.getByQuestionId(questionId);
            if (!answers.isEmpty()) {
                Answer latest = answers.get(answers.size() - 1);
                answerDAO.publish(latest.getId());
                questionDAO.updateStatus(questionId, "ANSWERED");
            }
        } else if (saved) {
            // Check if there are now published answers -> update question status
            List<Answer> published = answerDAO.getPublishedByQuestionId(questionId);
            if (!published.isEmpty()) {
                questionDAO.updateStatus(questionId, "ANSWERED");
            }
        }

        response.sendRedirect(request.getContextPath() + "/qa-management?action=view&id=" + questionId
                + "&msg=" + (saved ? "success" : "error"));
    }

    private void handlePublish(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int answerId = Integer.parseInt(request.getParameter("answerId"));
        int questionId = Integer.parseInt(request.getParameter("questionId"));

        boolean ok = answerDAO.publish(answerId);
        if (ok) {
            questionDAO.updateStatus(questionId, "ANSWERED");
        }

        response.sendRedirect(request.getContextPath() + "/qa-management?action=view&id=" + questionId
                + "&msg=" + (ok ? "published" : "error"));
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int answerId = Integer.parseInt(request.getParameter("answerId"));
        int questionId = Integer.parseInt(request.getParameter("questionId"));

        answerDAO.delete(answerId);

        // If no published answers remain, revert question status to PENDING
        List<Answer> published = answerDAO.getPublishedByQuestionId(questionId);
        if (published.isEmpty()) {
            questionDAO.updateStatus(questionId, "PENDING");
        }

        response.sendRedirect(request.getContextPath() + "/qa-management?action=view&id=" + questionId
                + "&msg=deleted");
    }

    private void forward(HttpServletRequest req, HttpServletResponse res, String path)
            throws ServletException, IOException {
        RequestDispatcher rd = req.getRequestDispatcher(path);
        rd.forward(req, res);
    }
}
