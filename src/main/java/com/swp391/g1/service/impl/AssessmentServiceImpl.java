package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IAssessmentAnswerDAO;
import com.swp391.g1.dao.IAssessmentAttemptDAO;
import com.swp391.g1.dao.IAssessmentQuestionDAO;
import com.swp391.g1.dao.IAssessmentOptionDAO;
import com.swp391.g1.dao.IAssessmentDAO;
import com.swp391.g1.dao.IActivityStaffDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IStudentDAO;
import com.swp391.g1.dao.impl.AssessmentAnswerDAOImpl;
import com.swp391.g1.dao.impl.AssessmentAttemptDAOImpl;
import com.swp391.g1.dao.impl.AssessmentQuestionDAOImpl;
import com.swp391.g1.dao.impl.AssessmentOptionDAOImpl;
import com.swp391.g1.dao.impl.AssessmentDAOImpl;
import com.swp391.g1.dao.impl.ActivityStaffDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.StudentDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Assessment;
import com.swp391.g1.model.AssessmentAttempt;
import com.swp391.g1.model.AssessmentAnswer;
import com.swp391.g1.model.AssessmentOption;
import com.swp391.g1.model.AssessmentQuestion;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.AssessmentServiceException;
import com.swp391.g1.service.IAssessmentService;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.util.DateTimeUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssessmentServiceImpl implements IAssessmentService {

    private final IAssessmentDAO assessmentDAO = new AssessmentDAOImpl();
    private final IAssessmentQuestionDAO questionDAO = new AssessmentQuestionDAOImpl();
    private final IAssessmentOptionDAO optionDAO = new AssessmentOptionDAOImpl();
    private final IAssessmentAttemptDAO attemptDAO = new AssessmentAttemptDAOImpl();
    private final IAssessmentAnswerDAO answerDAO = new AssessmentAnswerDAOImpl();
    private final IExtracurricularActivityDAO activityDAO = new ExtracurricularActivityDAOImpl();
    private final IStudentDAO studentDAO = new StudentDAOImpl();
    private final IActivityStaffDAO activityStaffDAO = new ActivityStaffDAOImpl();
    private final INotificationService notificationService = new NotificationServiceImpl();

    private void requireManageActivity(int activityId, CurrentUser user) {
        if (user == null) {
            throw new AssessmentServiceException("Bạn chưa đăng nhập.");
        }
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return;
        }
        if (!AccessPolicy.canManageActivities(user)) {
            throw new AssessmentServiceException("Chỉ STAFF hoặc ADMIN mới được quản lý đánh giá hoạt động.");
        }
        if (!activityDAO.isInStaffActivityScope(activityId, user.getId(), user.getRole())) {
            throw new AssessmentServiceException("Bạn không có quyền quản lý hoạt động này.");
        }
    }

    private ExtracurricularActivity requireActivity(int activityId) {
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        if (activity == null) {
            throw new AssessmentServiceException("Không tìm thấy hoạt động.");
        }
        return activity;
    }

    @Override
    public Assessment createAssessment(int activityId, CurrentUser user, String title,
            String description, LocalDateTime openTime, LocalDateTime closeTime) {
        requireManageActivity(activityId, user);
        requireActivity(activityId);

        if (assessmentDAO.findByActivityId(activityId) != null) {
            throw new AssessmentServiceException("Hoạt động này đã có bài đánh giá. Mỗi hoạt động chỉ có tối đa một bài đánh giá.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new AssessmentServiceException("Tiêu đề bài đánh giá không được để trống.");
        }

        LocalDateTime now = DateTimeUtil.now();
        if (openTime != null && !openTime.isAfter(now.minusSeconds(60))) {
            throw new AssessmentServiceException("Thời gian mở bài phải ở tương lai.");
        }
        if (closeTime != null && openTime != null && !closeTime.isAfter(openTime)) {
            throw new AssessmentServiceException("Thời gian đóng bài phải sau thời gian mở bài.");
        }

        Assessment assessment = new Assessment();
        assessment.setActivityId(activityId);
        assessment.setTitle(title.trim());
        assessment.setDescription(description == null ? null : description.trim());
        assessment.setOpenTime(openTime);
        assessment.setCloseTime(closeTime);
        assessment.setPublished(false);

        Integer id = assessmentDAO.insert(assessment);
        if (id == null) {
            throw new AssessmentServiceException("Không thể tạo bài đánh giá.");
        }
        assessment.setId(id);
        return assessment;
    }

    @Override
    public boolean updateAssessment(int id, CurrentUser user, String title, String description,
            LocalDateTime openTime, LocalDateTime closeTime) {
        Assessment assessment = assessmentDAO.findById(id);
        if (assessment == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        requireManageActivity(assessment.getActivityId(), user);

        if (title == null || title.trim().isEmpty()) {
            throw new AssessmentServiceException("Tiêu đề bài đánh giá không được để trống.");
        }
        if (openTime != null && closeTime != null && !closeTime.isAfter(openTime)) {
            throw new AssessmentServiceException("Thời gian đóng bài phải sau thời gian mở bài.");
        }

        assessment.setTitle(title.trim());
        assessment.setDescription(description == null ? null : description.trim());
        assessment.setOpenTime(openTime);
        assessment.setCloseTime(closeTime);
        return assessmentDAO.update(assessment);
    }

    @Override
    public boolean publishAssessment(int id, boolean publish, CurrentUser user) {
        Assessment assessment = assessmentDAO.findById(id);
        if (assessment == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        requireManageActivity(assessment.getActivityId(), user);

        List<AssessmentQuestion> questions = questionDAO.findByAssessmentId(id);
        if (questions.isEmpty()) {
            throw new AssessmentServiceException("Không thể công bố bài đánh giá chưa có câu hỏi.");
        }
        for (AssessmentQuestion q : questions) {
            if (q.getContent() == null || q.getContent().trim().isEmpty()) {
                throw new AssessmentServiceException("Có câu hỏi thiếu nội dung.");
            }
            if (q.getMaxScore() <= 0) {
                throw new AssessmentServiceException("Điểm tối đa của câu hỏi phải lớn hơn 0.");
            }
            if (q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                List<AssessmentOption> options = optionDAO.findByQuestionId(q.getId());
                if (options.size() < 2) {
                    throw new AssessmentServiceException("Câu hỏi trắc nghiệm (MCQ) cần ít nhất 2 phương án.");
                }
                long correctCount = options.stream().filter(AssessmentOption::isCorrect).count();
                if (correctCount != 1) {
                    throw new AssessmentServiceException("Câu hỏi trắc nghiệm (MCQ) phải có chính xác 1 đáp án đúng.");
                }
            }
        }
        boolean updated = assessmentDAO.publish(id, publish);
        if (updated && publish) {
            notificationService.notifyActivityParticipants(assessment.getActivityId(), "ASSESSMENT_PUBLISHED",
                    "Bài đánh giá mới",
                    "Bài đánh giá \"" + assessment.getTitle() + "\" đã được công bố.",
                    "/assessment?action=take&assessmentId=" + id);
        }
        return updated;
    }

    @Override
    public boolean deleteAssessment(int id, CurrentUser user) {
        Assessment assessment = assessmentDAO.findById(id);
        if (assessment == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        requireManageActivity(assessment.getActivityId(), user);
        List<AssessmentAttempt> attempts = attemptDAO.findByAssessmentId(id);
        if (!attempts.isEmpty()) {
            throw new AssessmentServiceException("Không thể xóa bài đánh giá đã có sinh viên làm bài.");
        }
        for (AssessmentQuestion q : questionDAO.findByAssessmentId(id)) {
            optionDAO.deleteByQuestionId(q.getId());
        }
        questionDAO.deleteByAssessmentId(id);
        return assessmentDAO.delete(id);
    }

    @Override
    public Assessment getAssessmentById(int id, CurrentUser user) {
        Assessment assessment = assessmentDAO.findById(id);
        if (assessment == null) {
            return null;
        }
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return assessment;
        }
        if (AccessPolicy.canManageActivities(user)
                && activityDAO.isInStaffActivityScope(assessment.getActivityId(), user.getId(), user.getRole())) {
            return assessment;
        }
        return null;
    }

    @Override
    public Assessment getAssessmentForStudentAttempt(int assessmentId, int studentId) {
        AssessmentAttempt attempt = attemptDAO.findByAssessmentAndStudent(assessmentId, studentId);
        if (attempt == null) {
            return null;
        }
        return assessmentDAO.findById(assessmentId);
    }

    @Override
    public Assessment getAssessmentByActivityId(int activityId, CurrentUser user) {
        Assessment assessment = assessmentDAO.findByActivityId(activityId);
        if (assessment == null) {
            return null;
        }
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return assessment;
        }
        if (AccessPolicy.canManageActivities(user)
                && activityDAO.isInStaffActivityScope(activityId, user.getId(), user.getRole())) {
            return assessment;
        }
        return null;
    }

    @Override
    public Assessment getAssessmentByActivityIdForStudent(int activityId) {
        // Read-only: no attempt creation, no scope check. Student eligibility is verified separately via canStudentTake().
        return assessmentDAO.findByActivityId(activityId);
    }

    @Override
    public List<com.swp391.g1.dto.AssessmentListItem> listAssessmentsForStaff(CurrentUser user) {
        if (user == null || !AccessPolicy.canManageActivities(user)) {
            throw new AssessmentServiceException("Chỉ STAFF hoặc ADMIN mới được xem danh sách đánh giá.");
        }
        return assessmentDAO.listForStaff();
    }

    @Override
    public List<com.swp391.g1.dto.AssessmentListItem> listAssessmentsForStudent(int studentId) {
        return assessmentDAO.listForStudent(studentId);
    }

    @Override
    public AssessmentQuestion addQuestion(int assessmentId, CurrentUser user,
            AssessmentQuestion.QuestionType type, String content, double maxScore, int displayOrder) {
        Assessment assessment = assessmentDAO.findById(assessmentId);
        if (assessment == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        requireManageActivity(assessment.getActivityId(), user);

        List<AssessmentAttempt> attempts = attemptDAO.findByAssessmentId(assessmentId);
        if (!attempts.isEmpty()) {
            throw new AssessmentServiceException("Không thể thay đổi cấu trúc câu hỏi sau khi đã có bài nộp.");
        }
        if (type == null) {
            throw new AssessmentServiceException("Loại câu hỏi không hợp lệ.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new AssessmentServiceException("Nội dung câu hỏi không được để trống.");
        }
        if (maxScore <= 0) {
            throw new AssessmentServiceException("Điểm tối đa phải lớn hơn 0.");
        }

        AssessmentQuestion question = new AssessmentQuestion();
        question.setAssessmentId(assessmentId);
        question.setQuestionType(type);
        question.setContent(content.trim());
        question.setMaxScore(maxScore);
        question.setDisplayOrder(displayOrder);
        Integer id = questionDAO.insert(question);
        if (id == null) {
            throw new AssessmentServiceException("Không thể thêm câu hỏi.");
        }
        question.setId(id);
        return question;
    }

    @Override
    public boolean updateQuestion(int questionId, CurrentUser user,
            AssessmentQuestion.QuestionType type, String content, double maxScore, int displayOrder) {
        AssessmentQuestion question = questionDAO.findById(questionId);
        if (question == null) {
            throw new AssessmentServiceException("Không tìm thấy câu hỏi.");
        }
        Assessment assessment = assessmentDAO.findById(question.getAssessmentId());
        requireManageActivity(assessment.getActivityId(), user);

        List<AssessmentAttempt> attempts = attemptDAO.findByAssessmentId(assessment.getId());
        if (!attempts.isEmpty()) {
            throw new AssessmentServiceException("Không thể thay đổi cấu trúc câu hỏi sau khi đã có bài nộp.");
        }
        if (type == null) {
            throw new AssessmentServiceException("Loại câu hỏi không hợp lệ.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new AssessmentServiceException("Nội dung câu hỏi không được để trống.");
        }
        if (maxScore <= 0) {
            throw new AssessmentServiceException("Điểm tối đa phải lớn hơn 0.");
        }

        question.setQuestionType(type);
        question.setContent(content.trim());
        question.setMaxScore(maxScore);
        question.setDisplayOrder(displayOrder);
        return questionDAO.update(question);
    }

    @Override
    public boolean deleteQuestion(int questionId, CurrentUser user) {
        AssessmentQuestion question = questionDAO.findById(questionId);
        if (question == null) {
            throw new AssessmentServiceException("Không tìm thấy câu hỏi.");
        }
        Assessment assessment = assessmentDAO.findById(question.getAssessmentId());
        requireManageActivity(assessment.getActivityId(), user);

        List<AssessmentAttempt> attempts = attemptDAO.findByAssessmentId(assessment.getId());
        if (!attempts.isEmpty()) {
            throw new AssessmentServiceException("Không thể xóa câu hỏi sau khi đã có bài nộp.");
        }
        optionDAO.deleteByQuestionId(questionId);
        return questionDAO.delete(questionId);
    }

    @Override
    public List<AssessmentQuestion> listQuestions(int assessmentId, CurrentUser user) {
        Assessment assessment = assessmentDAO.findById(assessmentId);
        if (assessment == null) {
            return List.of();
        }
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return questionDAO.findByAssessmentId(assessmentId);
        }
        if (AccessPolicy.canManageActivities(user)
                && activityDAO.isInStaffActivityScope(assessment.getActivityId(), user.getId(), user.getRole())) {
            return questionDAO.findByAssessmentId(assessmentId);
        }
        return List.of();
    }

    @Override
    public AssessmentOption addOption(int questionId, CurrentUser user,
            String optionText, boolean correct, int displayOrder) {
        AssessmentQuestion question = questionDAO.findById(questionId);
        if (question == null) {
            throw new AssessmentServiceException("Không tìm thấy câu hỏi.");
        }
        Assessment assessment = assessmentDAO.findById(question.getAssessmentId());
        requireManageActivity(assessment.getActivityId(), user);

        if (optionText == null || optionText.trim().isEmpty()) {
            throw new AssessmentServiceException("Nội dung phương án không được để trống.");
        }

        AssessmentOption option = new AssessmentOption();
        option.setQuestionId(questionId);
        option.setOptionText(optionText.trim());
        option.setCorrect(correct);
        option.setDisplayOrder(displayOrder);
        Integer id = optionDAO.insert(option);
        if (id == null) {
            throw new AssessmentServiceException("Không thể thêm phương án.");
        }
        option.setId(id);
        return option;
    }

    @Override
    public boolean updateOption(int optionId, CurrentUser user,
            String optionText, boolean correct, int displayOrder) {
        AssessmentOption option = optionDAO.findById(optionId);
        if (option == null) {
            throw new AssessmentServiceException("Không tìm thấy phương án.");
        }
        AssessmentQuestion question = questionDAO.findById(option.getQuestionId());
        if (question == null) {
            throw new AssessmentServiceException("Không tìm thấy câu hỏi.");
        }
        Assessment assessment = assessmentDAO.findById(question.getAssessmentId());
        requireManageActivity(assessment.getActivityId(), user);

        List<AssessmentAttempt> attempts = attemptDAO.findByAssessmentId(assessment.getId());
        if (!attempts.isEmpty()) {
            throw new AssessmentServiceException("Không thể thay đổi phương án sau khi đã có bài nộp.");
        }
        if (optionText == null || optionText.trim().isEmpty()) {
            throw new AssessmentServiceException("Nội dung phương án không được để trống.");
        }

        option.setOptionText(optionText.trim());
        option.setCorrect(correct);
        option.setDisplayOrder(displayOrder);
        return optionDAO.update(option);
    }

    @Override
    public boolean deleteOption(int optionId, CurrentUser user) {
        AssessmentOption option = optionDAO.findById(optionId);
        if (option == null) {
            throw new AssessmentServiceException("Không tìm thấy phương án.");
        }
        AssessmentQuestion question = questionDAO.findById(option.getQuestionId());
        if (question == null) {
            throw new AssessmentServiceException("Không tìm thấy câu hỏi.");
        }
        Assessment assessment = assessmentDAO.findById(question.getAssessmentId());
        requireManageActivity(assessment.getActivityId(), user);

        List<AssessmentAttempt> attempts = attemptDAO.findByAssessmentId(assessment.getId());
        if (!attempts.isEmpty()) {
            throw new AssessmentServiceException("Không thể xóa phương án sau khi đã có bài nộp.");
        }
        return optionDAO.delete(optionId);
    }

    @Override
    public List<AssessmentOption> listOptions(int questionId, CurrentUser user) {
        AssessmentQuestion question = questionDAO.findById(questionId);
        if (question == null) {
            return List.of();
        }
        Assessment assessment = assessmentDAO.findById(question.getAssessmentId());
        if (assessment == null) {
            return List.of();
        }
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return optionDAO.findByQuestionId(questionId);
        }
        if (AccessPolicy.canManageActivities(user)
                && activityDAO.isInStaffActivityScope(assessment.getActivityId(), user.getId(), user.getRole())) {
            return optionDAO.findByQuestionId(questionId);
        }
        return List.of();
    }

    /* ========================================================================
       Student flow
       ======================================================================== */

    @Override
    public boolean canStudentTake(int assessmentId, int studentId) {
        Assessment assessment = assessmentDAO.findById(assessmentId);
        if (assessment == null) {
            return false;
        }
        if (!assessment.isPublished()) {
            return false;
        }
        ExtracurricularActivity activity = activityDAO.findById(assessment.getActivityId());
        if (activity == null || activity.getActivityStatus() == Enum.ActivityStatus.CANCELLED) {
            return false;
        }
        LocalDateTime now = DateTimeUtil.now();
        if (assessment.getOpenTime() != null && now.isBefore(assessment.getOpenTime())) {
            return false;
        }
        if (activity.getEndTime() != null && now.isBefore(activity.getEndTime())) {
            return false;
        }
        if (assessment.getCloseTime() != null && now.isAfter(assessment.getCloseTime())) {
            return false;
        }
        if (!studentIsParticipant(assessment.getActivityId(), studentId)) {
            return false;
        }
        AssessmentAttempt existing = attemptDAO.findByAssessmentAndStudent(assessmentId, studentId);
        if (existing != null && (AssessmentAttempt.Status.SUBMITTED == existing.getStatus()
                || AssessmentAttempt.Status.COMPLETED == existing.getStatus())) {
            return false;
        }
        return true;
    }

    private boolean studentIsParticipant(int activityId, int studentId) {
        String sql = "SELECT 1 FROM activity_participant WHERE activity_id = ? AND student_id = ? AND attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')";
        try (java.sql.Connection conn = com.swp391.g1.common.DBContext.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, studentId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to verify assessment participation.", e);
        }
    }

    @Override
    public AssessmentAttempt startAttempt(int assessmentId, int studentId) {
        Assessment assessment = assessmentDAO.findById(assessmentId);
        if (assessment == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        if (!canStudentTake(assessmentId, studentId)) {
            throw new AssessmentServiceException("Bạn không đủ điều kiện làm bài đánh giá này.");
        }
        AssessmentAttempt existing = attemptDAO.findByAssessmentAndStudent(assessmentId, studentId);
        if (existing != null) {
            if (existing.getStatus() == AssessmentAttempt.Status.IN_PROGRESS) {
                return existing;
            }
            throw new AssessmentServiceException("Bạn đã nộp bài cho bài đánh giá này rồi.");
        }
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentId(assessmentId);
        attempt.setStudentId(studentId);
        Integer id = attemptDAO.insert(attempt);
        if (id == null) {
            throw new AssessmentServiceException("Không thể khởi tạo bài làm.");
        }
        attempt.setId(id);
        attempt.setStatus(AssessmentAttempt.Status.IN_PROGRESS);
        return attempt;
    }

    @Override
    public void saveAnswers(int attemptId, int studentId,
            java.util.Map<Integer, java.lang.Object> answers) {
        AssessmentAttempt attempt = attemptDAO.findById(attemptId);
        if (attempt == null) {
            throw new AssessmentServiceException("Không tìm thấy bài làm.");
        }
        if (attempt.getStudentId() != studentId) {
            throw new AssessmentServiceException("Bài làm không thuộc về bạn.");
        }
        if (attempt.getStatus() != AssessmentAttempt.Status.IN_PROGRESS) {
            throw new AssessmentServiceException("Không thể lưu câu trả lời sau khi đã nộp bài.");
        }

        Assessment assessment = assessmentDAO.findById(attempt.getAssessmentId());
        List<AssessmentQuestion> questions = questionDAO.findByAssessmentId(assessment.getId());
        Map<Integer, AssessmentQuestion> questionMap = new HashMap<>();
        for (AssessmentQuestion q : questions) {
            questionMap.put(q.getId(), q);
        }

        Connection conn = null;
        try {
            conn = com.swp391.g1.common.DBContext.getConnection();
            conn.setAutoCommit(false);

            // Lock attempt row to prevent concurrent submitAttempt
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, status FROM assessment_attempt WITH (UPDLOCK, HOLDLOCK) WHERE id = ?")) {
                ps.setInt(1, attemptId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new AssessmentServiceException("Không tìm thấy bài làm.");
                    }
                    String status = rs.getString("status");
                    if (!AssessmentAttempt.Status.IN_PROGRESS.name().equals(status)) {
                        conn.rollback();
                        throw new AssessmentServiceException("Không thể lưu câu trả lời sau khi đã nộp bài.");
                    }
                }
            }

            answerDAO.deleteByAttemptId(conn, attemptId);

            for (Map.Entry<Integer, java.lang.Object> entry : answers.entrySet()) {
                int questionId = entry.getKey();
                AssessmentQuestion question = questionMap.get(questionId);
                if (question == null) {
                    throw new AssessmentServiceException("Câu hỏi không thuộc bài đánh giá này.");
                }
                AssessmentAnswer answer = new AssessmentAnswer();
                answer.setAttemptId(attemptId);
                answer.setQuestionId(questionId);
                if (question.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                    Integer optionId = null;
                    if (entry.getValue() instanceof Number) {
                        optionId = ((Number) entry.getValue()).intValue();
                    } else if (entry.getValue() != null) {
                        try {
                            optionId = Integer.parseInt(entry.getValue().toString());
                        } catch (NumberFormatException ignored) {
                            optionId = null;
                        }
                    }
                    if (optionId != null) {
                        AssessmentOption option = optionDAO.findById(optionId);
                        if (option == null || option.getQuestionId() != questionId) {
                            throw new AssessmentServiceException("Phương án không thuộc câu hỏi này.");
                        }
                        answer.setSelectedOptionId(optionId);
                    }
                } else {
                    String text = entry.getValue() == null ? null : entry.getValue().toString();
                    if (text != null && text.length() > 10000) {
                        throw new AssessmentServiceException("Câu trả lời tự luận vượt quá độ dài cho phép (10.000 ký tự).");
                    }
                    answer.setAnswerText(text);
                }
                answer.setGradingStatus(
                        question.getQuestionType() == AssessmentQuestion.QuestionType.MCQ
                                ? AssessmentAnswer.GradingStatus.GRADED
                                : AssessmentAnswer.GradingStatus.PENDING_GRADING);

                // Insert using the shared Connection (within transaction)
                answerDAO.insert(conn, answer);
            }

            conn.commit();
        } catch (AssessmentServiceException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (java.sql.SQLException rollbackEx) {
                    throw new AssessmentServiceException("Transaction rollback failed: " + e.getMessage()
                            + ". Rollback also failed: " + rollbackEx.getMessage(), e);
                }
            }
            throw e;
        } catch (java.sql.SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (java.sql.SQLException rollbackEx) {
                    throw new IllegalStateException("Database operation failed and rollback also failed: "
                            + rollbackEx.getMessage(), e);
                }
            }
            throw new IllegalStateException("Failed to save answers.", e);
        } catch (RuntimeException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (java.sql.SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (java.sql.SQLException e) {
                    java.util.logging.Logger.getLogger(AssessmentServiceImpl.class.getName())
                            .log(java.util.logging.Level.WARNING, "Failed to close database connection.", e);
                }
            }
        }
    }

    @Override
    public AssessmentAttempt submitAttempt(int attemptId, int studentId) {
        AssessmentAttempt attempt = attemptDAO.findById(attemptId);
        if (attempt == null) {
            throw new AssessmentServiceException("Không tìm thấy bài làm.");
        }
        if (attempt.getStudentId() != studentId) {
            throw new AssessmentServiceException("Bài làm không thuộc về bạn.");
        }
        if (attempt.getStatus() != AssessmentAttempt.Status.IN_PROGRESS) {
            throw new AssessmentServiceException("Bài làm đã được nộp trước đó.");
        }

        Assessment assessment = assessmentDAO.findById(attempt.getAssessmentId());
        List<AssessmentQuestion> questions = questionDAO.findByAssessmentId(assessment.getId());
        List<AssessmentAnswer> answers = answerDAO.findByAttemptId(attemptId);

        double mcqTotal = 0;
        boolean hasEssay = false;

        for (AssessmentQuestion q : questions) {
            AssessmentAnswer ans = findAnswer(answers, q.getId());
            if (q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                if (ans != null && ans.getSelectedOptionId() != null) {
                    AssessmentOption option = optionDAO.findById(ans.getSelectedOptionId());
                    if (option != null && option.isCorrect()) {
                        mcqTotal += q.getMaxScore();
                    }
                }
            } else {
                hasEssay = true;
            }
        }

        LocalDateTime now = DateTimeUtil.now();
        AssessmentAttempt.Status newStatus;
        Double finalScore = null;

        if (hasEssay) {
            newStatus = AssessmentAttempt.Status.SUBMITTED;
        } else {
            newStatus = AssessmentAttempt.Status.COMPLETED;
            finalScore = mcqTotal;
        }

        Connection conn = null;
        try {
            conn = com.swp391.g1.common.DBContext.getConnection();
            conn.setAutoCommit(false);

            boolean updated = attemptDAO.updateStatusAndScore(conn, attemptId,
                    AssessmentAttempt.Status.IN_PROGRESS, newStatus,
                    finalScore == null ? null : BigDecimal.valueOf(finalScore), now);
            if (!updated) {
                conn.rollback();
                throw new AssessmentServiceException("Bài làm đã được nộp trước đó.");
            }

            conn.commit();
        } catch (java.sql.SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) {
                    throw new IllegalStateException("Database operation failed and rollback also failed: " + re.getMessage(), e);
                }
            }
            throw new IllegalStateException("Failed to submit attempt.", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {
                    java.util.logging.Logger.getLogger(AssessmentServiceImpl.class.getName())
                            .log(java.util.logging.Level.WARNING, "Failed to close database connection.", e);
                }
            }
        }

        attempt.setStatus(newStatus);
        attempt.setSubmittedAt(now);
        attempt.setFinalScore(finalScore);
        return attempt;
    }

    private AssessmentAnswer findAnswer(List<AssessmentAnswer> answers, int questionId) {
        for (AssessmentAnswer a : answers) {
            if (a.getQuestionId() == questionId) {
                return a;
            }
        }
        return null;
    }

    @Override
    public AssessmentAttempt getMyAttempt(int assessmentId, int studentId) {
        return attemptDAO.findByAssessmentAndStudent(assessmentId, studentId);
    }

    @Override
    public List<AssessmentAnswer> getAttemptAnswers(int attemptId, int studentId) {
        AssessmentAttempt attempt = attemptDAO.findById(attemptId);
        if (attempt == null || attempt.getStudentId() != studentId) {
            return List.of();
        }
        return answerDAO.findByAttemptId(attemptId);
    }

    @Override
    public boolean gradeEssay(int answerId, BigDecimal score, String feedback, CurrentUser user) {
        if (user == null) {
            throw new AssessmentServiceException("Bạn chưa đăng nhập.");
        }
        AssessmentAnswer answer = answerDAO.findById(answerId);
        if (answer == null) {
            throw new AssessmentServiceException("Không tìm thấy câu trả lời.");
        }
        AssessmentQuestion question = questionDAO.findById(answer.getQuestionId());
        if (question == null || question.getQuestionType() != AssessmentQuestion.QuestionType.ESSAY) {
            throw new AssessmentServiceException("Câu trả lời không phải là câu tự luận.");
        }
        Assessment assessment = assessmentDAO.findById(question.getAssessmentId());
        if (assessment == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        if (!AccessPolicy.hasType(user, "ADMIN")
                && (!AccessPolicy.canManageActivities(user)
                || !activityDAO.isInStaffActivityScope(assessment.getActivityId(), user.getId(), user.getRole()))) {
            throw new AssessmentServiceException("Bạn không có quyền chấm bài đánh giá này.");
        }
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(BigDecimal.valueOf(question.getMaxScore())) > 0) {
            throw new AssessmentServiceException("Điểm phải nằm trong khoảng từ 0 đến điểm tối đa của câu hỏi.");
        }

        AssessmentAttempt attempt = attemptDAO.findById(answer.getAttemptId());
        if (attempt == null) {
            throw new AssessmentServiceException("Không tìm thấy bài làm.");
        }

        Connection conn = null;
        boolean attemptCompleted = false;
        try {
            conn = com.swp391.g1.common.DBContext.getConnection();
            conn.setAutoCommit(false);

            // Lock attempt row to prevent concurrent grading of same attempt
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, status FROM assessment_attempt WITH (UPDLOCK, HOLDLOCK) WHERE id = ?")) {
                ps.setInt(1, attempt.getId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new AssessmentServiceException("Không tìm thấy bài làm.");
                    }
                    String status = rs.getString("status");
                    if (!AssessmentAttempt.Status.SUBMITTED.name().equals(status)) {
                        conn.rollback();
                        throw new AssessmentServiceException("Bài làm đã hoàn tất chấm điểm hoặc chưa được nộp.");
                    }
                }
            }

            boolean graded = answerDAO.updateGrade(conn, answerId, score, feedback,
                    AssessmentAnswer.GradingStatus.GRADED);
            if (!graded) {
                conn.rollback();
                throw new AssessmentServiceException("Không thể lưu điểm chấm bài.");
            }

            List<AssessmentQuestion> allQuestions = questionDAO.findByAssessmentId(conn, assessment.getId());
            List<AssessmentAnswer> allAnswers = answerDAO.findByAttemptId(conn, attempt.getId());
            boolean allEssayGraded = true;
            double totalScore = 0;

            for (AssessmentQuestion q : allQuestions) {
                AssessmentAnswer ans = findAnswer(allAnswers, q.getId());
                if (q.getQuestionType() == AssessmentQuestion.QuestionType.MCQ) {
                    if (ans != null && ans.getSelectedOptionId() != null) {
                        AssessmentOption option = optionDAO.findById(conn, ans.getSelectedOptionId());
                        if (option != null && option.isCorrect()) {
                            totalScore += q.getMaxScore();
                        }
                    }
                } else {
                    if (ans == null || ans.getGradingStatus() == AssessmentAnswer.GradingStatus.PENDING_GRADING) {
                        allEssayGraded = false;
                    } else if (ans.getAwardedScore() != null) {
                        totalScore += ans.getAwardedScore();
                    }
                }
            }

            if (allEssayGraded) {
                boolean updated = attemptDAO.updateStatusAndScore(conn, attempt.getId(),
                        AssessmentAttempt.Status.SUBMITTED, AssessmentAttempt.Status.COMPLETED,
                        BigDecimal.valueOf(totalScore), null);
                if (!updated) {
                    conn.rollback();
                    throw new AssessmentServiceException("Bài làm đã được cập nhật bởi người dùng khác.");
                }
                attempt.setStatus(AssessmentAttempt.Status.COMPLETED);
                attempt.setFinalScore(totalScore);
                attemptCompleted = true;
            }

            conn.commit();
        } catch (AssessmentServiceException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    throw new AssessmentServiceException("Transaction rollback failed: " + e.getMessage()
                            + ". Rollback also failed: " + rollbackEx.getMessage(), e);
                }
            }
            throw e;
        } catch (java.sql.SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    throw new IllegalStateException("Database operation failed and rollback also failed: "
                            + rollbackEx.getMessage(), e);
                }
            }
            throw new IllegalStateException("Failed to grade essay.", e);
        } catch (RuntimeException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {
                    java.util.logging.Logger.getLogger(AssessmentServiceImpl.class.getName())
                            .log(java.util.logging.Level.WARNING, "Failed to close database connection.", e);
                }
            }
        }

        if (attemptCompleted) {
            notificationService.notifyStudent(attempt.getStudentId(), "ASSESSMENT_GRADED",
                    "Bài đánh giá đã được chấm",
                    "Bài đánh giá \"" + assessment.getTitle() + "\" của bạn đã được chấm xong.",
                    "/assessment?action=result&assessmentId=" + assessment.getId());
        }
        return true;
    }

    @Override
    public List<AssessmentAttempt> listSubmissionsForGrading(int activityId, CurrentUser user) {
        if (user == null) {
            throw new AssessmentServiceException("Bạn chưa đăng nhập.");
        }
        if (AccessPolicy.hasType(user, "ADMIN")) {
            // ADMIN bypasses activity scope
        } else if (!AccessPolicy.canManageActivities(user)
                || !activityDAO.isInStaffActivityScope(activityId, user.getId(), user.getRole())) {
            throw new AssessmentServiceException("Bạn không có quyền xem bài nộp của hoạt động này.");
        }
        Assessment assessment = assessmentDAO.findByActivityId(activityId);
        if (assessment == null) {
            return List.of();
        }
        return attemptDAO.findByAssessmentId(assessment.getId());
    }

    @Override
    public AssessmentAttempt getAttemptForGrading(int attemptId, CurrentUser user) {
        if (user == null) {
            throw new AssessmentServiceException("Bạn chưa đăng nhập.");
        }
        AssessmentAttempt attempt = attemptDAO.findById(attemptId);
        if (attempt == null) {
            throw new AssessmentServiceException("Không tìm thấy bài làm.");
        }
        Assessment assessment = assessmentDAO.findById(attempt.getAssessmentId());
        if (assessment == null) {
            throw new AssessmentServiceException("Không tìm thấy bài đánh giá.");
        }
        if (!AccessPolicy.hasType(user, "ADMIN")
                && (!AccessPolicy.canManageActivities(user)
                || !activityDAO.isInStaffActivityScope(assessment.getActivityId(), user.getId(), user.getRole()))) {
            throw new AssessmentServiceException("Bạn không có quyền xem bài làm này.");
        }
        return attempt;
    }
}
