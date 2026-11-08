package com.swp391.g1.service;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Assessment;
import com.swp391.g1.model.AssessmentAttempt;
import com.swp391.g1.model.AssessmentAnswer;
import com.swp391.g1.model.AssessmentQuestion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IAssessmentService {

    /* ---------- Assessment (staff/manager) ---------- */

    Assessment createAssessment(int activityId, CurrentUser user, String title,
            String description, LocalDateTime openTime, LocalDateTime closeTime);

    boolean updateAssessment(int id, CurrentUser user, String title, String description,
            LocalDateTime openTime, LocalDateTime closeTime);

    boolean publishAssessment(int id, boolean publish, CurrentUser user);

    boolean deleteAssessment(int id, CurrentUser user);

    Assessment getAssessmentById(int id, CurrentUser user);

    /** Load assessment only after verifying the attempt belongs to the student. Returns null if attempt not found or not owned by student. */
    Assessment getAssessmentForStudentAttempt(int assessmentId, int studentId);

    Assessment getAssessmentByActivityId(int activityId, CurrentUser user);

    /* ---------- Question (staff/manager) ---------- */

    AssessmentQuestion addQuestion(int assessmentId, CurrentUser user,
            AssessmentQuestion.QuestionType type, String content, double maxScore, int displayOrder);

    boolean updateQuestion(int questionId, CurrentUser user,
            AssessmentQuestion.QuestionType type, String content, double maxScore, int displayOrder);

    boolean deleteQuestion(int questionId, CurrentUser user);

    List<AssessmentQuestion> listQuestions(int assessmentId, CurrentUser user);

    /* ---------- Option (staff/manager) ---------- */

    com.swp391.g1.model.AssessmentOption addOption(int questionId, CurrentUser user,
            String optionText, boolean correct, int displayOrder);

    boolean updateOption(int optionId, CurrentUser user,
            String optionText, boolean correct, int displayOrder);

    boolean deleteOption(int optionId, CurrentUser user);

    List<com.swp391.g1.model.AssessmentOption> listOptions(int questionId, CurrentUser user);

    /* ---------- Student flow ---------- */

    boolean canStudentTake(int assessmentId, int studentId);

    AssessmentAttempt startAttempt(int assessmentId, int studentId);

    /* answerItem: questionId -> selectedOptionId (MCQ) or answerText (Essay) */
    void saveAnswers(int attemptId, int studentId,
            java.util.Map<Integer, java.lang.Object> answers);

    AssessmentAttempt submitAttempt(int attemptId, int studentId);

    AssessmentAttempt getMyAttempt(int assessmentId, int studentId);

    List<AssessmentAnswer> getAttemptAnswers(int attemptId, int studentId);

    /* ---------- Staff grading ---------- */

    boolean gradeEssay(int answerId, BigDecimal score, String feedback, CurrentUser user);

    List<AssessmentAttempt> listSubmissionsForGrading(int activityId, CurrentUser user);

    AssessmentAttempt getAttemptForGrading(int attemptId, CurrentUser user);
}
