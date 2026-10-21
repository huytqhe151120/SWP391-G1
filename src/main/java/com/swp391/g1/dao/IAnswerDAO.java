package com.swp391.g1.dao;

import java.util.List;

import com.swp391.g1.model.Answer;

public interface IAnswerDAO {
    List<Answer> getByQuestionId(int questionId);

    List<Answer> getPublishedByQuestionId(int questionId);

    boolean insert(Answer answer);

    /** Inserts and publishes one answer atomically, returning false on failure. */
    boolean insertPublished(Answer answer);

    boolean publish(int answerId);

    boolean delete(int answerId);

    Answer getById(int id);
}
