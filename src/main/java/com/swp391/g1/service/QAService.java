package com.swp391.g1.service;

import com.swp391.g1.dao.QuestionDAO;
import com.swp391.g1.model.Question;

import java.util.List;

public class QAService {
    private final QuestionDAO questionDAO = new QuestionDAO();

    public List<Question> getPublishedQuestions() {
        return questionDAO.getAnsweredQuestions();
    }

    public boolean submitQuestion(String title, String content, boolean isAnonymous, int studentId) {
        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            return false;
        }
        Question q = new Question(title.trim(), content.trim(), isAnonymous, studentId);
        return questionDAO.insertQuestion(q);
    }

    public List<Question> getPendingQuestionsForStaff() {
        return questionDAO.getPendingQuestions();
    }
}