package com.swp391.g1.service.impl;

import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.dao.IRecruitmentApplicationDAO;
import com.swp391.g1.dao.IRecruitmentRoundDAO;
import com.swp391.g1.dao.impl.RecruitmentApplicationDAOImpl;
import com.swp391.g1.dao.impl.RecruitmentRoundDAOImpl;
import com.swp391.g1.model.RecruitmentAnswer;
import com.swp391.g1.model.RecruitmentApplication;
import com.swp391.g1.model.RecruitmentInterview;
import com.swp391.g1.model.RecruitmentPosition;
import com.swp391.g1.model.RecruitmentQuestion;
import com.swp391.g1.model.RecruitmentRound;
import com.swp391.g1.model.RecruitmentStatus;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.service.IRecruitmentService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecruitmentServiceImpl implements IRecruitmentService {

    private static final int MAX_QUOTA = 1000;
    private static final Logger LOGGER = Logger.getLogger(RecruitmentServiceImpl.class.getName());

    private final IRecruitmentRoundDAO roundDao;
    private final IRecruitmentApplicationDAO applicationDao;
    private final INotificationService notificationService;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public RecruitmentServiceImpl() {
        this(new RecruitmentRoundDAOImpl(), new RecruitmentApplicationDAOImpl(), new NotificationServiceImpl());
    }

    RecruitmentServiceImpl(IRecruitmentRoundDAO roundDao, IRecruitmentApplicationDAO applicationDao,
                           INotificationService notificationService) {
        this.roundDao = roundDao;
        this.applicationDao = applicationDao;
        this.notificationService = notificationService;
    }

    private void notifyStudent(RecruitmentApplication app, String type, String title, String content) {
        notificationService.notifyStudent(app.getStudentId(), type, title, content,
                "/organizer-recruitment?action=myApplications&id=" + app.getId());
    }

    @Override
    public List<RecruitmentRound> getRounds() {
        roundDao.synchronizeStatuses();
        return roundDao.findAll();
    }

    @Override
    public RecruitmentRound getRound(int id) {
        if (id <= 0) {
            return null;
        }
        roundDao.synchronizeStatuses();
        return roundDao.findById(id);
    }

    @Override
    public RecruitmentRound getRoundByActivity(int activityId) {
        if (activityId <= 0) {
            return null;
        }
        roundDao.synchronizeStatuses();
        return roundDao.findByActivityId(activityId);
    }

    @Override
    public List<RecruitmentRound> getSelectableActivities(Integer includeActivityId) {
        return roundDao.findEligibleActivities(includeActivityId);
    }

    @Override
    public LocalDateTime getParticipationOpenAt(int activityId) {
        return activityId > 0 ? roundDao.findParticipationOpenAt(activityId) : null;
    }

    @Override
    public boolean isStructureLocked(int roundId) {
        return roundId > 0 && roundDao.hasApplications(roundId);
    }

    @Override
    public String saveRound(RecruitmentRound round, int accountId, boolean openAfterSave) {
        if (round != null && round.getId() > 0) {
            RecruitmentRound existing = roundDao.findById(round.getId());
            if (existing != null && roundDao.hasApplications(round.getId())) {
                round.setPositions(existing.getPositions());
                round.setQuestions(existing.getQuestions());
            }
        }
        String error = validateRound(round);
        if (error != null) {
            return error;
        }
        try {
            if (round.getId() <= 0) {
                if (!roundDao.isActivityEligible(round.getActivityId())) {
                    return "Chỉ tạo đợt tuyển cho hoạt động đã duyệt và chưa diễn ra.";
                }
                if (roundDao.findByActivityId(round.getActivityId()) != null) {
                    return "Hoạt động này đã có đợt tuyển ban tổ chức.";
                }
                round.setStatus(RecruitmentStatus.ROUND_DRAFT);
                round.setCreatedBy(accountId);
                round.setId(roundDao.insert(round));
            } else {
                RecruitmentRound current = roundDao.findById(round.getId());
                if (current == null) {
                    return "Không tìm thấy đợt tuyển.";
                }
                if (!roundDao.isActivityEligible(current.getActivityId())) {
                    return "Hoạt động đã bắt đầu hoặc không còn hợp lệ nên không thể sửa đợt tuyển.";
                }
                boolean replaceStructure = !roundDao.hasApplications(round.getId());
                if (!roundDao.update(round, replaceStructure)) {
                    return "Không thể cập nhật đợt tuyển.";
                }
                round.setActivityId(current.getActivityId());
                round.setStatus(current.getStatus());
            }
            if (openAfterSave && !RecruitmentStatus.ROUND_OPEN.equals(round.getStatus())) {
                return openRound(round.getId());
            }
            return null;
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) {
                return "Hoạt động này đã có đợt tuyển ban tổ chức.";
            }
            return e.getMessage();
        }
    }

    private static String validateRound(RecruitmentRound round) {
        if (round == null) {
            return "Thông tin đợt tuyển không hợp lệ.";
        }
        round.setTitle(trim(round.getTitle()));
        round.setDescription(trim(round.getDescription()));
        if (round.getId() <= 0 && round.getActivityId() <= 0) {
            return "Vui lòng chọn hoạt động.";
        }
        if (round.getTitle().isEmpty()) {
            return "Vui lòng nhập tiêu đề đợt tuyển.";
        }
        if (round.getTitle().length() > 200) {
            return "Tiêu đề không được vượt quá 200 ký tự.";
        }
        if (round.getDescription().length() > 2000) {
            return "Mô tả không được vượt quá 2.000 ký tự.";
        }
        if (round.getOpenAt() != null && round.getCloseAt() != null && !round.getCloseAt().isAfter(round.getOpenAt())) {
            return "Thời gian đóng phải sau thời gian mở.";
        }
        List<RecruitmentPosition> positions = round.getPositions();
        if (positions.isEmpty()) {
            return "Vui lòng khai báo ít nhất một vị trí tuyển.";
        }
        if (positions.size() > RecruitmentStatus.MAX_POSITIONS) {
            return "Tối đa " + RecruitmentStatus.MAX_POSITIONS + " vị trí tuyển.";
        }
        Set<String> names = new HashSet<>();
        int order = 0;
        for (RecruitmentPosition position : positions) {
            position.setName(trim(position.getName()));
            if (position.getName().isEmpty() || position.getName().length() > 100) {
                return "Tên vị trí không được để trống và tối đa 100 ký tự.";
            }
            if (!names.add(position.getName().toLowerCase(Locale.ROOT))) {
                return "Tên vị trí \"" + position.getName() + "\" bị trùng.";
            }
            if (position.getQuota() < 1 || position.getQuota() > MAX_QUOTA) {
                return "Chỉ tiêu của mỗi vị trí phải từ 1 đến " + MAX_QUOTA + ".";
            }
            position.setSortOrder(order++);
        }
        List<RecruitmentQuestion> questions = round.getQuestions();
        if (questions.size() > RecruitmentStatus.MAX_QUESTIONS) {
            return "Tối đa " + RecruitmentStatus.MAX_QUESTIONS + " câu hỏi bổ sung.";
        }
        order = 0;
        for (RecruitmentQuestion question : questions) {
            question.setQuestionText(trim(question.getQuestionText()));
            if (question.getQuestionText().isEmpty() || question.getQuestionText().length() > 500) {
                return "Nội dung câu hỏi không được để trống và tối đa 500 ký tự.";
            }
            String type = question.getQuestionType();
            if (!RecruitmentStatus.QUESTION_TEXT.equals(type) && !RecruitmentStatus.QUESTION_DROPDOWN.equals(type)
                    && !RecruitmentStatus.QUESTION_MULTI_CHOICE.equals(type)) {
                return "Loại câu hỏi không hợp lệ.";
            }
            if (RecruitmentStatus.QUESTION_TEXT.equals(type)) {
                question.setOptions(null);
            } else {
                Set<String> options = new java.util.LinkedHashSet<>();
                for (String option : question.getOptionList()) {
                    String value = trim(option);
                    if (!value.isEmpty()) {
                        options.add(value);
                    }
                }
                if (options.size() < 2) {
                    return "Câu hỏi \"" + question.getQuestionText() + "\" cần ít nhất 2 lựa chọn khác nhau.";
                }
                question.setOptions(String.join("\n", options));
            }
            question.setSortOrder(order++);
        }
        return null;
    }

    @Override
    public String openRound(int id) {
        RecruitmentRound round = roundDao.findById(id);
        if (round == null) {
            return "Không tìm thấy đợt tuyển.";
        }
        if (RecruitmentStatus.ROUND_OPEN.equals(round.getStatus())) {
            return null;
        }
        if (!roundDao.isActivityEligible(round.getActivityId())) {
            return "Hoạt động đã bắt đầu hoặc chưa được duyệt nên không thể mở nhận hồ sơ.";
        }
        if (round.getOpenAt() == null || round.getCloseAt() == null) {
            return "Vui lòng đặt thời gian mở và đóng hồ sơ trước khi mở tuyển.";
        }
        if (round.getCloseAt() != null && !round.getCloseAt().isAfter(LocalDateTime.now())) {
            return "Thời gian đóng đã qua. Vui lòng sửa thời gian đóng trước khi mở lại.";
        }
        return roundDao.updateStatus(id, RecruitmentStatus.ROUND_OPEN) ? null : "Không thể mở đợt tuyển.";
    }

    @Override
    public String closeRound(int id) {
        RecruitmentRound round = roundDao.findById(id);
        if (round == null) {
            return "Không tìm thấy đợt tuyển.";
        }
        if (RecruitmentStatus.ROUND_CLOSED.equals(round.getStatus())) {
            return null;
        }
        return roundDao.updateStatus(id, RecruitmentStatus.ROUND_CLOSED) ? null : "Không thể đóng đợt tuyển.";
    }

    @Override
    public String deleteRound(int id) {
        if (roundDao.findById(id) == null) {
            return "Không tìm thấy đợt tuyển.";
        }
        return roundDao.delete(id) ? null
                : "Không thể xóa đợt tuyển đã có hồ sơ ứng tuyển. Hãy đóng đợt thay vì xóa.";
    }

    @Override
    public List<RecruitmentRound> getOpenRounds() {
        roundDao.synchronizeStatuses();
        return roundDao.findOpenRounds();
    }

    @Override
    public List<RecruitmentApplication> getStudentApplications(int studentId) {
        return studentId > 0 ? applicationDao.findByStudent(studentId) : new ArrayList<>();
    }

    @Override
    public RecruitmentApplication getStudentApplication(int applicationId, int studentId) {
        RecruitmentApplication app = applicationId > 0 ? applicationDao.findById(applicationId) : null;
        return app != null && app.getStudentId() == studentId ? app : null;
    }

    @Override
    public RecruitmentApplication getActiveApplication(int roundId, int studentId) {
        RecruitmentApplication app = applicationDao.findByRoundAndStudent(roundId, studentId);
        return app != null && !RecruitmentStatus.APP_WITHDRAWN.equals(app.getStatus()) ? app : null;
    }

    @Override
    public boolean hasActiveParticipation(int activityId, int studentId) {
        return applicationDao.hasActiveParticipation(activityId, studentId);
    }

    @Override
    public boolean hasAcceptedApplication(int activityId, int studentId) {
        return activityId > 0 && studentId > 0 && applicationDao.hasAcceptedApplication(activityId, studentId);
    }

    @Override
    public String apply(int roundId, int studentId, Integer choice1, Integer choice2,
                        Map<Integer, List<String>> answers, String note) {
        roundDao.synchronizeStatuses();
        RecruitmentRound round = roundId > 0 ? roundDao.findById(roundId) : null;
        if (round == null || studentId <= 0) {
            return "Không tìm thấy đợt tuyển.";
        }
        LocalDateTime now = LocalDateTime.now();
        boolean open = RecruitmentStatus.ROUND_OPEN.equals(round.getStatus())
                && (round.getOpenAt() == null || !round.getOpenAt().isAfter(now))
                && (round.getCloseAt() == null || round.getCloseAt().isAfter(now));
        if (!open) {
            return "Đợt tuyển chưa mở hoặc đã đóng.";
        }
        if (!roundDao.isActivityEligible(round.getActivityId())) {
            return "Hoạt động đã bắt đầu hoặc không còn hợp lệ.";
        }
        if (round.getPositions().stream().noneMatch(p -> p.getRemaining() > 0)) {
            return "Đợt tuyển đã hết chỉ tiêu ở tất cả vị trí.";
        }
        if (choice1 == null) {
            return "Vui lòng chọn nguyện vọng 1.";
        }
        if (choice2 != null && choice2.equals(choice1)) {
            return "Nguyện vọng 2 phải khác nguyện vọng 1.";
        }
        for (Integer choice : choice2 == null ? List.of(choice1) : List.of(choice1, choice2)) {
            RecruitmentPosition position = findPosition(round, choice);
            if (position == null) {
                return "Vị trí đã chọn không thuộc đợt tuyển này.";
            }
            if (position.getRemaining() <= 0) {
                return "Vị trí \"" + position.getName() + "\" đã hết chỉ tiêu.";
            }
        }
        String trimmedNote = trim(note);
        if (trimmedNote.length() > 1000) {
            return "Ghi chú không được vượt quá 1.000 ký tự.";
        }

        List<RecruitmentAnswer> collected = new ArrayList<>();
        for (RecruitmentQuestion question : round.getQuestions()) {
            List<String> values = new ArrayList<>();
            List<String> raw = answers == null ? null : answers.get(question.getId());
            if (raw != null) {
                for (String value : raw) {
                    if (value != null && !trim(value).isEmpty()) {
                        values.add(trim(value));
                    }
                }
            }
            if (values.isEmpty()) {
                if (question.isRequired()) {
                    return "Vui lòng trả lời câu hỏi: " + question.getQuestionText();
                }
                continue;
            }
            String type = question.getQuestionType();
            if (RecruitmentStatus.QUESTION_TEXT.equals(type)) {
                if (values.size() > 1 || values.get(0).length() > 4000) {
                    return "Câu trả lời không hợp lệ hoặc vượt quá 4.000 ký tự.";
                }
            } else {
                List<String> allowed = question.getOptionList();
                if (RecruitmentStatus.QUESTION_DROPDOWN.equals(type) && values.size() > 1) {
                    return "Câu hỏi \"" + question.getQuestionText() + "\" chỉ được chọn một đáp án.";
                }
                for (String value : values) {
                    if (!allowed.contains(value)) {
                        return "Đáp án của câu hỏi \"" + question.getQuestionText() + "\" không hợp lệ.";
                    }
                }
            }
            RecruitmentAnswer answer = new RecruitmentAnswer();
            answer.setQuestionId(question.getId());
            answer.setAnswerValue(String.join("; ", values));
            collected.add(answer);
        }

        RecruitmentApplication existing = applicationDao.findByRoundAndStudent(roundId, studentId);
        if (existing != null) {
            if (!RecruitmentStatus.APP_WITHDRAWN.equals(existing.getStatus())) {
                return "Bạn đã nộp hồ sơ cho đợt tuyển này.";
            }
        }

        RecruitmentApplication app = new RecruitmentApplication();
        app.setRoundId(roundId);
        app.setStudentId(studentId);
        app.setChoice1PositionId(choice1);
        app.setChoice2PositionId(choice2);
        app.setNote(trimmedNote.isEmpty() ? null : trimmedNote);
        app.setAnswers(collected);
        try {
            applicationDao.insert(app, round.getUpdatedAt());
            return null;
        } catch (DataAccessException e) {
            return e.isDuplicateKey() ? "Bạn đã nộp hồ sơ cho đợt tuyển này." : e.getMessage();
        }
    }

    private static RecruitmentPosition findPosition(RecruitmentRound round, Integer id) {
        for (RecruitmentPosition position : round.getPositions()) {
            if (id != null && position.getId() == id) {
                return position;
            }
        }
        return null;
    }

    @Override
    public String withdraw(int applicationId, int studentId, String reason) {
        RecruitmentApplication app = getStudentApplication(applicationId, studentId);
        if (app == null) {
            return "Không tìm thấy hồ sơ ứng tuyển.";
        }
        String trimmedReason = trim(reason);
        if (trimmedReason.length() > 500) {
            return "Lý do không được vượt quá 500 ký tự.";
        }
        String status = app.getStatus();
        if (RecruitmentStatus.APP_PENDING.equals(status)) {
            return applicationDao.withdraw(applicationId, List.of(RecruitmentStatus.APP_PENDING),
                    trimmedReason.isEmpty() ? null : trimmedReason)
                    ? null : "Hồ sơ đã được xử lý nên không thể tự rút. Vui lòng tải lại trang.";
        }
        if (RecruitmentStatus.APP_ACCEPTED.equals(status)) {
            LocalDateTime start = app.getActivityStart();
            if (start == null || !LocalDateTime.now().isBefore(
                    start.minusHours(RecruitmentStatus.WITHDRAW_ACCEPTED_HOURS_BEFORE_ACTIVITY))) {
                return "Đã quá mốc " + RecruitmentStatus.WITHDRAW_ACCEPTED_HOURS_BEFORE_ACTIVITY
                        + " giờ trước hoạt động. Vui lòng liên hệ staff để rút khỏi ban tổ chức.";
            }
            if (trimmedReason.isEmpty()) {
                return "Vui lòng nhập lý do rút khỏi ban tổ chức.";
            }
            if (!applicationDao.withdraw(applicationId, List.of(RecruitmentStatus.APP_ACCEPTED), trimmedReason)) {
                return "Không thể rút khỏi ban tổ chức. Vui lòng tải lại trang.";
            }
            notificationService.notifyActivityStaff(app.getActivityId(), "RECRUITMENT_ORGANIZER_WITHDRAWN",
                    "Thành viên ban tổ chức xin rút",
                    app.getStudentName() + " (" + app.getStudentCode() + ") đã rút khỏi ban tổ chức hoạt động \""
                            + app.getActivityName() + "\". Lý do: " + trimmedReason + ". Chỉ tiêu đã được trả lại.",
                    "/organizer-recruitment?action=applications&id=" + app.getRoundId() + "&appId=" + applicationId);
            return null;
        }
        if (RecruitmentStatus.APP_INTERVIEW_INVITED.equals(status) || RecruitmentStatus.APP_INTERVIEWED.equals(status)) {
            return "Hồ sơ đã được mời phỏng vấn. Vui lòng báo staff nếu bạn muốn rút.";
        }
        return "Hồ sơ này không thể rút.";
    }

    @Override
    public String confirmInterview(int applicationId, int studentId) {
        RecruitmentApplication app = getStudentApplication(applicationId, studentId);
        if (app == null || !RecruitmentStatus.APP_INTERVIEW_INVITED.equals(app.getStatus()) || app.getInterview() == null) {
            return "Không có lịch phỏng vấn cần xác nhận.";
        }
        if (!RecruitmentStatus.INTERVIEW_SCHEDULED.equals(app.getInterview().getStatus())) {
            return "Lịch phỏng vấn đã được xác nhận hoặc không còn hiệu lực.";
        }
        return applicationDao.confirmInterview(applicationId) ? null
                : "Đã quá hạn xác nhận. Vui lòng liên hệ staff để được hỗ trợ.";
    }

    @Override
    public List<RecruitmentApplication> getApplications(int roundId, String statusFilter, Integer positionId) {
        String status = statusFilter == null || statusFilter.isBlank() || "all".equals(statusFilter) ? null : statusFilter;
        return applicationDao.findByRound(roundId, status, positionId);
    }

    @Override
    public RecruitmentApplication getApplication(int applicationId) {
        return applicationId > 0 ? applicationDao.findById(applicationId) : null;
    }

    @Override
    public String saveInternalNote(int applicationId, String note) {
        String value = trim(note);
        if (value.length() > 1000) {
            return "Ghi chú nội bộ không được vượt quá 1000 ký tự.";
        }
        return applicationDao.updateInternalNote(applicationId, value.isEmpty() ? null : value)
                ? null : "Không tìm thấy hồ sơ.";
    }

    @Override
    public String invite(int applicationId, int accountId, RecruitmentInterview iv) {
        RecruitmentApplication app = applicationDao.findById(applicationId);
        if (app == null) {
            return "Không tìm thấy hồ sơ.";
        }
        RecruitmentRound round = roundDao.findById(app.getRoundId());
        if (round == null || !round.isInterviewRequired()) {
            return "Đợt tuyển này không có vòng phỏng vấn.";
        }
        if (!RecruitmentStatus.APP_PENDING.equals(app.getStatus())
                && !RecruitmentStatus.APP_INTERVIEW_INVITED.equals(app.getStatus())) {
            return "Chỉ mời hoặc đổi lịch cho hồ sơ đang Chờ xét hoặc Mời phỏng vấn.";
        }
        String error = validateInterview(iv, app);
        if (error != null) {
            return error;
        }
        iv.setApplicationId(applicationId);
        iv.setCreatedBy(accountId);
        iv.setLocation(trim(iv.getLocation()));
        iv.setNote(trim(iv.getNote()).isEmpty() ? null : trim(iv.getNote()));
        if (!applicationDao.saveInterview(iv)) {
            return "Trạng thái hồ sơ đã thay đổi. Vui lòng tải lại trang.";
        }
        boolean reschedule = RecruitmentStatus.APP_INTERVIEW_INVITED.equals(app.getStatus());
        notifyStudent(app, reschedule ? "RECRUITMENT_INTERVIEW_RESCHEDULED" : "RECRUITMENT_INTERVIEW_INVITED",
                reschedule ? "Lịch phỏng vấn ban tổ chức đã thay đổi" : "Mời phỏng vấn ban tổ chức",
                "Bạn được " + (reschedule ? "đổi lịch" : "mời") + " phỏng vấn ban tổ chức hoạt động \""
                        + app.getActivityName() + "\" lúc " + FORMAT.format(iv.getScheduledAt())
                        + ". Vui lòng xác nhận tham dự trước " + FORMAT.format(iv.getConfirmDeadline()) + ".");
        return null;
    }

    private static String validateInterview(RecruitmentInterview iv, RecruitmentApplication app) {
        LocalDateTime now = LocalDateTime.now();
        if (iv.getScheduledAt() == null || !iv.getScheduledAt().isAfter(now)) {
            return "Thời gian phỏng vấn phải ở tương lai.";
        }
        if (iv.getConfirmDeadline() == null || !iv.getConfirmDeadline().isAfter(now)
                || !iv.getConfirmDeadline().isBefore(iv.getScheduledAt())) {
            return "Hạn xác nhận phải ở tương lai và trước giờ phỏng vấn.";
        }
        if (app.getActivityStart() != null && !iv.getScheduledAt().isBefore(app.getActivityStart())) {
            return "Lịch phỏng vấn phải trước khi hoạt động bắt đầu.";
        }
        if (!RecruitmentStatus.FORMAT_ONLINE.equals(iv.getFormat())
                && !RecruitmentStatus.FORMAT_OFFLINE.equals(iv.getFormat())) {
            return "Hình thức phỏng vấn không hợp lệ.";
        }
        String location = trim(iv.getLocation());
        if (location.isEmpty() || location.length() > 300) {
            return "Địa điểm hoặc liên kết là bắt buộc và tối đa 300 ký tự.";
        }
        if (trim(iv.getNote()).length() > 500) {
            return "Ghi chú không được vượt quá 500 ký tự.";
        }
        return null;
    }

    @Override
    public String cancelInterview(int applicationId) {
        RecruitmentApplication app = applicationDao.findById(applicationId);
        if (app == null || !RecruitmentStatus.APP_INTERVIEW_INVITED.equals(app.getStatus())) {
            return "Hồ sơ không có lịch phỏng vấn để hủy.";
        }
        if (!applicationDao.cancelInterview(applicationId, RecruitmentStatus.APP_PENDING)) {
            return "Không thể hủy lịch. Vui lòng tải lại trang.";
        }
        notifyStudent(app, "RECRUITMENT_INTERVIEW_CANCELLED", "Lịch phỏng vấn ban tổ chức đã bị hủy",
                "Lịch phỏng vấn ban tổ chức hoạt động \"" + app.getActivityName()
                        + "\" đã bị hủy. Staff sẽ liên hệ lại với bạn nếu cần.");
        return null;
    }

    @Override
    public String completeInterview(int applicationId, String evaluation) {
        String value = trim(evaluation);
        if (value.length() > 1000) {
            return "Nhận xét không được vượt quá 1000 ký tự.";
        }
        RecruitmentApplication app = applicationDao.findById(applicationId);
        if (app == null || !RecruitmentStatus.APP_INTERVIEW_INVITED.equals(app.getStatus())) {
            return "Hồ sơ chưa được mời phỏng vấn.";
        }
        if (app.getInterview() == null || app.getInterview().getScheduledAt() == null
                || app.getInterview().getScheduledAt().isAfter(LocalDateTime.now())) {
            return "Chỉ có thể ghi nhận sau thời gian phỏng vấn.";
        }
        return applicationDao.completeInterview(applicationId, value.isEmpty() ? null : value)
                ? null : "Không thể ghi nhận buổi phỏng vấn. Vui lòng tải lại trang.";
    }

    @Override
    public String accept(int applicationId, int positionId) {
        RecruitmentApplication app = applicationDao.findById(applicationId);
        if (app == null) {
            return "Không tìm thấy hồ sơ.";
        }
        String error = decisionBlocked(app);
        if (error != null) {
            return error;
        }
        RecruitmentRound round = roundDao.findById(app.getRoundId());
        boolean interviewed = RecruitmentStatus.APP_INTERVIEWED.equals(app.getStatus());
        boolean direct = RecruitmentStatus.APP_PENDING.equals(app.getStatus())
                && round != null && !round.isInterviewRequired();
        if (!interviewed && !direct) {
            return "Hồ sơ cần phỏng vấn xong trước khi trúng tuyển.";
        }
        boolean belongs = round != null && round.getPositions().stream().anyMatch(p -> p.getId() == positionId);
        if (!belongs) {
            return "Vui lòng chọn ban được nhận.";
        }
        if (!applicationDao.accept(applicationId, List.of(app.getStatus()), positionId)) {
            return "Ban đã hết chỉ tiêu hoặc hồ sơ vừa được xử lý. Vui lòng tải lại trang.";
        }
        String positionName = round.getPositions().stream().filter(p -> p.getId() == positionId)
                .map(RecruitmentPosition::getName).findFirst().orElse("");
        notifyStudent(app, "RECRUITMENT_ACCEPTED", "Trúng tuyển ban tổ chức",
                "Chúc mừng! Bạn đã trúng tuyển ban tổ chức hoạt động \"" + app.getActivityName()
                        + "\", vị trí: " + positionName + ". Đăng ký tham gia (nếu có) của bạn ở hoạt động này đã được hủy.");
        return null;
    }

    @Override
    public String reject(int applicationId, String reason) {
        String value = trim(reason);
        if (value.length() > 500) {
            return "Lý do không được vượt quá 500 ký tự.";
        }
        RecruitmentApplication app = applicationDao.findById(applicationId);
        if (app == null) {
            return "Không tìm thấy hồ sơ.";
        }
        String error = decisionBlocked(app);
        if (error != null) {
            return error;
        }
        List<String> allowed = List.of(RecruitmentStatus.APP_PENDING, RecruitmentStatus.APP_INTERVIEW_INVITED,
                RecruitmentStatus.APP_INTERVIEWED);
        if (!allowed.contains(app.getStatus())) {
            return "Hồ sơ ở trạng thái này không thể loại.";
        }
        if (!applicationDao.reject(applicationId, List.of(app.getStatus()), value.isEmpty() ? null : value)) {
            return "Hồ sơ vừa được xử lý. Vui lòng tải lại trang.";
        }
        notifyStudent(app, "RECRUITMENT_REJECTED", "Kết quả tuyển ban tổ chức",
                "Rất tiếc, hồ sơ ứng tuyển ban tổ chức hoạt động \"" + app.getActivityName()
                        + "\" chưa đạt. Bạn vẫn có thể đăng ký tham gia nếu đợt đăng ký còn mở.");
        return null;
    }

    @Override
    public String revertDecision(int applicationId) {
        RecruitmentApplication app = applicationDao.findById(applicationId);
        if (app == null || (!RecruitmentStatus.APP_ACCEPTED.equals(app.getStatus())
                && !RecruitmentStatus.APP_REJECTED.equals(app.getStatus()))) {
            return "Hồ sơ chưa có quyết định để thu hồi.";
        }
        String error = decisionBlocked(app);
        if (error != null) {
            return error;
        }
        boolean interviewed = app.getInterview() != null
                && RecruitmentStatus.INTERVIEW_DONE.equals(app.getInterview().getStatus());
        String back = interviewed ? RecruitmentStatus.APP_INTERVIEWED : RecruitmentStatus.APP_PENDING;
        if (!applicationDao.revertDecision(applicationId, back)) {
            return "Không thể thu hồi vì hoạt động đã bắt đầu.";
        }
        notifyStudent(app, "RECRUITMENT_DECISION_REVOKED", "Kết quả tuyển ban tổ chức được thu hồi",
                "Quyết định " + (RecruitmentStatus.APP_ACCEPTED.equals(app.getStatus()) ? "trúng tuyển" : "không đạt")
                        + " ban tổ chức hoạt động \"" + app.getActivityName() + "\" đã được thu hồi. Hồ sơ đang được xét lại.");
        return null;
    }

    @Override
    public int sendInterviewReminders() {
        int sent = 0;
        for (RecruitmentInterview iv : applicationDao.findInterviewsNeedingReminder(
                RecruitmentStatus.REMINDER_HOURS_BEFORE_DEADLINE)) {
            try {
                RecruitmentApplication app = applicationDao.findById(iv.getApplicationId());
                if (app != null && applicationDao.sendInterviewReminderIfDue(iv,
                        RecruitmentStatus.REMINDER_HOURS_BEFORE_DEADLINE, "Nhắc xác nhận lịch phỏng vấn",
                        "Vui lòng xác nhận tham dự phỏng vấn ban tổ chức hoạt động \"" + app.getActivityName()
                                + "\" trước " + FORMAT.format(iv.getConfirmDeadline()) + ".",
                        "/organizer-recruitment?action=myApplications&id=" + app.getId())) {
                    sent++;
                }
            } catch (DataAccessException e) {
                LOGGER.log(Level.WARNING, "Không thể gửi thông báo nhắc phỏng vấn " + iv.getId(), e);
            }
        }
        return sent;
    }

    @Override
    public void synchronizeRoundStatuses() {
        roundDao.synchronizeStatuses();
    }

    @Override
    public String remindInterview(int applicationId) {
        RecruitmentApplication app = applicationDao.findById(applicationId);
        if (app == null || !RecruitmentStatus.APP_INTERVIEW_INVITED.equals(app.getStatus())
                || app.getInterview() == null
                || !RecruitmentStatus.INTERVIEW_SCHEDULED.equals(app.getInterview().getStatus())) {
            return "Hồ sơ không có lịch phỏng vấn đang chờ xác nhận.";
        }
        notifyStudent(app, "RECRUITMENT_INTERVIEW_REMINDER", "Nhắc xác nhận lịch phỏng vấn",
                "Staff nhắc bạn xác nhận tham dự phỏng vấn ban tổ chức hoạt động \"" + app.getActivityName()
                        + "\" lúc " + FORMAT.format(app.getInterview().getScheduledAt()) + ".");
        return null;
    }

    private static String decisionBlocked(RecruitmentApplication app) {
        if (app.getActivityStart() != null && !app.getActivityStart().isAfter(LocalDateTime.now())) {
            return "Hoạt động đã bắt đầu nên không thể thay đổi quyết định.";
        }
        return null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
