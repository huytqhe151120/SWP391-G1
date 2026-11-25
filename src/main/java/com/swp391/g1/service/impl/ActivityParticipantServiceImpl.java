package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityParticipantDAO;
import com.swp391.g1.dao.impl.ActivityParticipantDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.AttendanceHistoryDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
import com.swp391.g1.dto.response.ParticipantSessionAttendanceDTO;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ParticipantStatus;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityParticipantService;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.util.ActivityScoreCalculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service quản lý danh sách người tham gia, điểm danh theo buổi và chấm điểm tham gia hoạt động.
 *
 * <p>Chịu trách nhiệm kiểm tra quy tắc nghiệp vụ, tính toán điểm/trạng thái từ dữ liệu thô qua
 * {@link ActivityScoreCalculator} và điều phối thông báo mà không cần truy vấn dư thừa toàn bộ hoạt động.</p>
 */
public class ActivityParticipantServiceImpl implements IActivityParticipantService {

    private static final BigDecimal MIN_EVAL_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_EVAL_SCORE = new BigDecimal("5.00");

    private final IActivityParticipantDAO participantDAO;
    private final IExtracurricularActivityService activityService;
    private final INotificationService notificationService;

    /**
     * Khóa đại diện cho một cặp chuyển đổi trạng thái điểm danh (trạng thái cũ -> trạng thái mới),
     * dùng để gom nhóm danh sách tài khoản sinh viên khi gửi thông báo hàng loạt.
     */
    private record StatusTransitionKey(String oldStatus, String newStatus) {
    }

    /**
     * Tạo đường dẫn tương đối tới trang chi tiết của hoạt động để đính kèm trong thông báo gửi sinh viên.
     */
    private static String activityUrl(int activityId) {
        return "/activities?action=detail&id=" + activityId;
    }

    /**
     * Khởi tạo service với các triển khai mặc định của DAO người tham gia, service hoạt động và service thông báo.
     */
    public ActivityParticipantServiceImpl() {
        this(new ActivityParticipantDAOImpl(),
                new ExtracurricularActivityServiceImpl(),
                new NotificationServiceImpl());
    }

    /**
     * Constructor hỗ trợ Dependency Injection (phục vụ Unit Test hoặc cấu hình tùy chỉnh).
     *
     * @param participantDAO      DAO thao tác dữ liệu người tham gia và điểm danh
     * @param activityService     Service tra cứu thông tin hoạt động ngoại khóa
     * @param notificationService Service gửi thông báo cho sinh viên
     */
    public ActivityParticipantServiceImpl(
            IActivityParticipantDAO participantDAO,
            IExtracurricularActivityService activityService,
            INotificationService notificationService) {
        this.participantDAO = participantDAO;
        this.activityService = activityService;
        this.notificationService = notificationService;
    }

    /**
     * Lấy danh sách người tham gia của hoạt động với trạng thái điểm danh mặc định ở buổi 1.
     */
    @Override
    public List<ActivityParticipantResponseDTO> getParticipants(int activityId) {
        return getParticipants(activityId, 1);
    }

    /**
     * Tải dữ liệu thô của người tham gia từ DAO rồi tính toàn bộ điểm thành phần, hệ số,
     * trạng thái tổng/theo buổi và quyền xóa tại tầng Service bằng {@link ActivityScoreCalculator}.
     */
    @Override
    public List<ActivityParticipantResponseDTO> getParticipants(int activityId, int sessionNumber) {
        // Trả về danh sách rỗng nếu mã hoạt động không hợp lệ.
        if (activityId <= 0) {
            return List.of();
        }
        int safeSession = Math.max(1, sessionNumber);
        List<IActivityParticipantDAO.ParticipantRawData> rawRows =
                participantDAO.findRawParticipantsByActivityId(activityId);
        List<ActivityParticipantResponseDTO> result = new ArrayList<>(rawRows.size());
        // Duyệt từng bản ghi dữ liệu thô để tính toán điểm/trạng thái và ánh xạ sang DTO phản hồi.
        for (IActivityParticipantDAO.ParticipantRawData raw : rawRows) {
            result.add(buildParticipantResponseDTO(raw, safeSession));
        }
        return result;
    }

    /**
     * Lắp ráp {@link ActivityParticipantResponseDTO} từ dữ liệu thô dựa trên {@link ActivityScoreCalculator}.
     */
    private ActivityParticipantResponseDTO buildParticipantResponseDTO(
            IActivityParticipantDAO.ParticipantRawData raw, int sessionNumber) {
        // Chuẩn hóa tổng số buổi (tối thiểu 1) và giới hạn buổi đang xem trong khoảng [1..totalSessions].
        int totalSessions = Math.max(1, raw.totalSessions());
        int clampedSession = Math.min(Math.max(1, sessionNumber), totalSessions);

        // Tính toàn bộ bảng phân rã điểm (điểm chuyên cần, điểm tham gia, điểm trung bình, hệ số, điểm cuối) và trạng thái tổng.
        ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                raw.sessionStatuses(),
                totalSessions,
                raw.legacyAttendanceStatus(),
                raw.participationEvalScore(),
                raw.summaryScore(),
                raw.summaryRequired(),
                raw.orgLevelCoefficient(),
                raw.registrationStatus(),
                raw.participationRole()
        );

        // Xác định trạng thái điểm danh của riêng buổi đang xem (có fallback về trạng thái cũ nếu chưa có dữ liệu buổi).
        ParticipantStatus sessionStatus = ActivityScoreCalculator.resolveSessionStatus(
                raw.sessionStatuses(), clampedSession, raw.legacyAttendanceStatus());

        ActivityParticipantResponseDTO dto = new ActivityParticipantResponseDTO();
        dto.setParticipantId(raw.participantId());
        dto.setStudentId(raw.studentId());
        dto.setStudentCode(raw.studentCode());
        dto.setStudentName(raw.studentName());
        dto.setMainClassCode(raw.mainClassCode());
        dto.setAcademicProgramName(raw.academicProgramName());
        dto.setMajorName(raw.majorName());
        dto.setDepartmentName(raw.departmentName());

        dto.setAttendanceStatus(breakdown.overallStatus());
        dto.setTotalSessions(totalSessions);
        dto.setSessionNumber(clampedSession);
        dto.setSessionAttendanceStatus(sessionStatus);
        dto.setSessionAttendanceScore(ActivityScoreCalculator.computeSessionScore(sessionStatus));
        dto.setAttendedSessionCount(breakdown.attendedSessionCount());
        dto.setAbsentSessionCount(breakdown.absentSessionCount());
        dto.setMarkedSessionCount(breakdown.markedSessionCount());

        dto.setRegistrationStatus(raw.registrationStatus());
        dto.setParticipationRole(raw.participationRole());
        dto.setRegistrationBy(raw.registrationBy());
        dto.setCreatedAt(raw.createdAt());
        // Xác định bản ghi có được phép xóa khỏi danh sách hay không (chưa từng điểm danh và không có lịch sử).
        dto.setRemovable(ActivityScoreCalculator.isRemovable(
                raw.participationRole(),
                breakdown.overallStatus(),
                breakdown.markedSessionCount(),
                raw.hasAttendanceHistory()));

        dto.setParticipationEvalScore(raw.participationEvalScore());
        dto.setParticipationFeedback(raw.participationFeedback());
        dto.setParticipationGradedBy(raw.participationGradedBy());
        dto.setParticipationGradedAt(raw.participationGradedAt());
        dto.setSummaryScore(raw.summaryScore());

        dto.setAttendanceScore(breakdown.attendanceScore());
        dto.setParticipationScore(breakdown.participationScore());
        dto.setAverageScore(breakdown.averageScore());
        dto.setOrgLevelCoefficient(breakdown.orgLevelCoefficient());
        dto.setEarlyRegCoefficient(breakdown.earlyRegCoefficient());
        dto.setFinalPoint(breakdown.finalPoint());
        return dto;
    }

    /**
     * Lấy tập hợp ID các hoạt động ngoại khóa mà sinh viên đã nằm trong danh sách tham gia.
     */
    @Override
    public Set<Integer> getActivityIdsByStudentId(int studentId) {
        // Trả về tập rỗng nếu studentId không hợp lệ.
        if (studentId <= 0) {
            return Set.of();
        }
        return participantDAO.findActivityIdsByStudentId(studentId);
    }

    /**
     * Kiểm tra hoạt động có đang ở trạng thái cho phép điểm danh và chấm điểm tham gia
     * (ONGOING hoặc COMPLETED) hay không.
     */
    @Override
    public boolean canTakeAttendance(ExtracurricularActivityResponseDTO activity) {
        return activity != null
                && (activity.getActivityStatus() == ActivityStatus.ONGOING
                        || activity.getActivityStatus() == ActivityStatus.COMPLETED);
    }

    /**
     * Lưu trạng thái điểm danh mặc định cho buổi 1 của hoạt động.
     */
    @Override
    public int saveAttendance(int activityId, Map<Integer, String> statusByParticipantId, CurrentUser user) {
        return saveAttendance(activityId, 1, statusByParticipantId, user);
    }

    /**
     * Kiểm tra điều kiện nghiệp vụ, lưu trạng thái điểm danh cho một buổi cụ thể của hoạt động
     * và gửi thông báo nhóm theo từng cặp chuyển đổi trạng thái cho các sinh viên bị thay đổi.
     *
     * @return số lượng sinh viên có trạng thái điểm danh thực sự thay đổi
     */
    @Override
    public int saveAttendance(int activityId, int sessionNumber, Map<Integer, String> statusByParticipantId, CurrentUser user) {
        // Kiểm tra tính hợp lệ của activityId, người thao tác và quyền điểm danh.
        if (activityId <= 0 || user == null || !AccessPolicy.canTakeAttendance(user)) {
            throw new IllegalArgumentException("Thông tin điểm danh không hợp lệ.");
        }
        ExtracurricularActivityResponseDTO activity = activityService.getActivityById(activityId);
        // Từ chối nếu hoạt động không tồn tại trong cơ sở dữ liệu.
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        // Chỉ cho phép điểm danh khi hoạt động đang diễn ra hoặc đã hoàn thành.
        if (!canTakeAttendance(activity)) {
            throw new IllegalArgumentException(
                    "Chỉ được điểm danh khi hoạt động đang diễn ra hoặc đã hoàn thành.");
        }
        int totalSessions = Math.max(1, activity.getAttendanceSessionCount());
        // Kiểm tra số buổi điểm danh nằm trong phạm vi [1..totalSessions] của hoạt động.
        if (sessionNumber < 1 || sessionNumber > totalSessions) {
            throw new IllegalArgumentException(
                    "Buổi điểm danh không hợp lệ. Hoạt động này có " + totalSessions + " buổi điểm danh.");
        }
        Map<Integer, ParticipantStatus> statuses = new LinkedHashMap<>();
        // Lọc và chuyển đổi map trạng thái chuỗi gửi lên sang enum ParticipantStatus hợp lệ.
        if (statusByParticipantId != null) {
            // Duyệt từng cặp (participantId -> tên trạng thái) trong đầu vào.
            for (Map.Entry<Integer, String> entry : statusByParticipantId.entrySet()) {
                ParticipantStatus status = parseStatus(entry.getValue());
                // Chỉ giữ lại các bản ghi có participantId dương và giá trị trạng thái khớp enum.
                if (entry.getKey() != null && entry.getKey() > 0 && status != null) {
                    statuses.put(entry.getKey(), status);
                }
            }
        }
        // Không có trạng thái hợp lệ nào để cập nhật -> kết thúc sớm và trả về 0.
        if (statuses.isEmpty()) {
            return 0;
        }
        // Lưu cập nhật xuống DB và nhận lại danh sách các bản ghi thực sự có thay đổi trạng thái.
        String actorId = String.valueOf(user.getId());
        List<IActivityParticipantDAO.AttendanceStatusChange> changes =
                participantDAO.updateStatusesAndReturnChanges(activityId, sessionNumber, statuses, actorId);
        // Nếu có thay đổi và người thao tác là cán bộ/quản trị (ADMIN, STAFF, PARTNER_STAFF), gửi thông báo cho sinh viên.
        if (!changes.isEmpty()
                && (AccessPolicy.hasType(user, "ADMIN")
                        || AccessPolicy.hasType(user, "STAFF")
                        || AccessPolicy.hasType(user, "PARTNER_STAFF"))) {
            Map<StatusTransitionKey, List<Integer>> accountIdsByTransition = new LinkedHashMap<>();
            // Gom danh sách mã tài khoản sinh viên theo từng cặp chuyển đổi (trạng thái cũ -> mới) để gửi thông báo gộp.
            for (IActivityParticipantDAO.AttendanceStatusChange change : changes) {
                // Chỉ đưa vào nhóm gửi thông báo những sinh viên có mã tài khoản hợp lệ.
                if (change.studentAccountId() != null && change.studentAccountId() > 0) {
                    StatusTransitionKey key = new StatusTransitionKey(change.oldStatus(), change.newStatus());
                    accountIdsByTransition.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(change.studentAccountId());
                }
            }
            // Duyệt từng nhóm chuyển đổi trạng thái và gọi service thông báo cho các tài khoản tương ứng.
            for (Map.Entry<StatusTransitionKey, List<Integer>> entry : accountIdsByTransition.entrySet()) {
                notificationService.notifyAttendanceStatusChanged(
                        activityId,
                        activity.getName(),
                        sessionNumber,
                        totalSessions,
                        entry.getKey().oldStatus(),
                        entry.getKey().newStatus(),
                        entry.getValue());
            }
        }
        return changes.size();
    }

    /**
     * Kiểm tra điều kiện hợp lệ, chấm điểm đánh giá tham gia (0 - 5 điểm) kèm nhận xét cho danh sách
     * sinh viên có mặt và gửi thông báo kết quả tới từng sinh viên đã được chấm điểm.
     *
     * @return số lượng sinh viên được cập nhật điểm đánh giá thành công
     */
    @Override
    public int saveParticipationGrades(int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, CurrentUser user) {
        // Kiểm tra tính hợp lệ của activityId, người chấm và quyền điểm danh/chấm điểm.
        if (activityId <= 0 || user == null || !AccessPolicy.canTakeAttendance(user)) {
            throw new IllegalArgumentException("Thông tin chấm điểm tham gia không hợp lệ.");
        }
        ExtracurricularActivityResponseDTO activity = activityService.getActivityById(activityId);
        // Đảm bảo hoạt động tồn tại trong hệ thống.
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        // Chỉ cho phép chấm điểm tham gia khi hoạt động đang diễn ra hoặc đã hoàn thành.
        if (!canTakeAttendance(activity)) {
            throw new IllegalArgumentException(
                    "Chỉ được chấm điểm tham gia khi hoạt động đang diễn ra hoặc đã hoàn thành.");
        }
        List<Integer> ids = distinctPositive(participantIds);
        // Yêu cầu phải có ít nhất một participantId hợp lệ được chọn.
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sinh viên để chấm điểm.");
        }
        // Ràng buộc điểm đánh giá phải nằm trong thang điểm cho phép [0.00 .. 5.00].
        if (evalScore == null
                || evalScore.compareTo(MIN_EVAL_SCORE) < 0
                || evalScore.compareTo(MAX_EVAL_SCORE) > 0) {
            throw new IllegalArgumentException("Điểm đánh giá tham gia phải nằm trong khoảng từ 0 đến 5 điểm.");
        }
        // Giới hạn độ dài nội dung nhận xét tối đa 1000 ký tự (sau khi cắt khoảng trắng đầu/cuối).
        if (feedback != null && feedback.trim().length() > 1000) {
            throw new IllegalArgumentException("Nhận xét / Feedback không được vượt quá 1000 ký tự.");
        }
        // Cập nhật điểm đánh giá xuống DB (chỉ áp dụng cho sinh viên có mặt) và nhận danh sách studentId đã được chấm.
        String actorLabel = buildActorLabel(user);
        List<Integer> gradedStudentIds = participantDAO.updateParticipationGradesAndReturnStudentIds(
                activityId, ids, evalScore, feedback, actorLabel, user.getId());
        // Nếu có ít nhất một sinh viên được chấm điểm, tạo nội dung và gửi thông báo tới từng sinh viên.
        if (!gradedStudentIds.isEmpty()) {
            String message = "Bạn được đánh giá " + evalScore.stripTrailingZeros().toPlainString()
                    + "/5 điểm cho hoạt động \"" + activity.getName() + "\".";
            String url = activityUrl(activityId);
            // Duyệt danh sách mã sinh viên vừa được chấm điểm để gửi thông báo cá nhân.
            for (Integer studentId : gradedStudentIds) {
                // Chỉ gửi thông báo với mã sinh viên dương hợp lệ.
                if (studentId != null && studentId > 0) {
                    notificationService.notifyStudent(studentId, "ACTIVITY_PARTICIPATION_GRADED",
                            "Điểm đánh giá tham gia", message, url);
                }
            }
        }
        return gradedStudentIds.size();
    }

    /**
     * Tạo nhãn hiển thị tên người thực hiện thao tác lưu vào lịch sử chấm điểm (dạng "Tên (Vai trò)").
     */
    private String buildActorLabel(CurrentUser user) {
        // Fallback về "Hệ thống" khi không có thông tin người dùng.
        if (user == null) {
            return "Hệ thống";
        }
        String name = user.getDisplayName();
        String role = user.getRoleLabel();
        // Ưu tiên tên hiển thị kèm vai trò trong ngoặc nếu có; nếu không có vai trò thì chỉ lấy tên.
        if (name != null && !name.isBlank()) {
            return (role != null && !role.isBlank())
                    ? name.trim() + " (" + role.trim() + ")"
                    : name.trim();
        }
        // Khi không có tên hiển thị, fallback sang username hoặc "Hệ thống".
        return user.getUsername() == null ? "Hệ thống" : user.getUsername();
    }

    /**
     * Kiểm tra hoạt động có cho phép thêm/xóa sinh viên trong danh sách tham gia hay không
     * (chỉ chặn khi hoạt động đã bị hủy).
     */
    @Override
    public boolean canManageParticipants(ExtracurricularActivityResponseDTO activity) {
        return activity != null && activity.getActivityStatus() != ActivityStatus.CANCELLED;
    }

    /**
     * Lấy danh sách sinh viên ứng viên đủ điều kiện và chưa có mặt trong danh sách tham gia của hoạt động.
     */
    @Override
    public List<ParticipantCandidateDTO> getCandidates(int activityId) {
        // Trả về danh sách rỗng nếu mã hoạt động không hợp lệ.
        if (activityId <= 0) {
            return List.of();
        }
        return participantDAO.findCandidates(activityId);
    }

    /**
     * Thêm danh sách sinh viên vào hoạt động với giá trị người đăng ký mặc định (registrationBy = 1).
     */
    @Override
    public int addStudents(int activityId, List<Integer> studentIds) {
        return addStudents(activityId, studentIds, 1);
    }

    /**
     * Thêm danh sách sinh viên vào hoạt động có kiểm tra sức chứa tối đa (maxParticipants),
     * đồng thời gửi thông báo tới các sinh viên thực sự mới được thêm.
     *
     * @return số lượng sinh viên mới được thêm thành công
     */
    @Override
    public int addStudents(int activityId, List<Integer> studentIds, Integer registrationBy) {
        ExtracurricularActivityResponseDTO activity = requireManageableActivity(activityId);
        List<Integer> ids = distinctPositive(studentIds);
        // Báo lỗi nếu danh sách chọn rỗng hoặc không chứa studentId dương hợp lệ nào.
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sinh viên.");
        }
        // Chèn sinh viên xuống DB kèm kiểm tra giới hạn sức chứa tối đa trong transaction ở DAO.
        Integer max = activity.getMaxParticipants();
        IActivityParticipantDAO.InsertParticipantsResult result =
                participantDAO.insertParticipantsAndReturnAdded(activityId, ids, max, registrationBy);
        // Nếu số lượng thêm vượt quá sức chứa còn lại, tính số chỗ trống hiện tại và ném lỗi cụ thể.
        if (result.exceededCapacity()) {
            int remaining = Math.max(0, max - participantDAO.countOccupiedByActivityId(activityId));
            throw new IllegalArgumentException("Hoạt động chỉ còn " + remaining
                    + " chỗ trống, không thể thêm " + ids.size() + " sinh viên.");
        }
        List<Integer> addedStudentIds = result.addedStudentIds();
        // Gửi thông báo cho tất cả sinh viên thực sự mới được thêm vào hoạt động.
        if (!addedStudentIds.isEmpty()) {
            String message = "Bạn đã được thêm vào danh sách tham gia hoạt động \"" + activity.getName() + "\".";
            String url = activityUrl(activityId);
            // Duyệt từng sinh viên mới thêm để phát thông báo cá nhân.
            for (Integer studentId : addedStudentIds) {
                notificationService.notifyStudent(studentId, "ACTIVITY_PARTICIPANT_ADDED",
                        "Bạn được thêm vào hoạt động", message, url);
            }
        }
        return addedStudentIds.size();
    }

    /**
     * Thêm hàng loạt sinh viên theo nhóm Khoa / Ngành / CTĐT / Lớp với giá trị registrationBy mặc định là 1.
     */
    @Override
    public int addStudentsByGroup(int activityId, int departmentId, int majorId, int programId, int classId) {
        return addStudentsByGroup(activityId, departmentId, majorId, programId, classId, 1);
    }

    /**
     * Lọc danh sách ứng viên theo các tiêu chí nhóm (Khoa, Ngành, CTĐT, Lớp) rồi thêm hàng loạt vào hoạt động.
     *
     * @return số lượng sinh viên mới được thêm thành công
     */
    @Override
    public int addStudentsByGroup(int activityId, int departmentId, int majorId, int programId, int classId, Integer registrationBy) {
        // Yêu cầu người dùng chọn ít nhất một tiêu chí lọc nhóm trước khi thêm hàng loạt.
        if (departmentId <= 0 && majorId <= 0 && programId <= 0 && classId <= 0) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ít nhất một tiêu chí: khoa, ngành, chương trình đào tạo hoặc lớp.");
        }
        requireManageableActivity(activityId);
        List<Integer> studentIds = new ArrayList<>();
        // Duyệt danh sách ứng viên chưa tham gia hoạt động để lọc những sinh viên thuộc nhóm đã chọn.
        for (ParticipantCandidateDTO candidate : participantDAO.findCandidates(activityId)) {
            // Chỉ giữ lại ứng viên khớp đồng thời tất cả tiêu chí lọc có ID > 0.
            if ((departmentId <= 0 || candidate.getDepartmentId() == departmentId)
                    && (majorId <= 0 || candidate.getMajorId() == majorId)
                    && (programId <= 0 || candidate.getAcademicProgramId() == programId)
                    && (classId <= 0 || candidate.getMainClassId() == classId)) {
                studentIds.add(candidate.getStudentId());
            }
        }
        // Không có ứng viên nào khớp tiêu chí nhóm -> trả về 0 mà không cần gọi addStudents.
        if (studentIds.isEmpty()) {
            return 0;
        }
        return addStudents(activityId, studentIds, registrationBy);
    }

    /**
     * Xóa các sinh viên đủ điều kiện (chưa được điểm danh) khỏi danh sách tham gia hoạt động
     * và gửi thông báo cho từng sinh viên đã bị xóa.
     *
     * @return số lượng sinh viên thực sự bị xóa khỏi danh sách
     */
    @Override
    public int removeParticipants(int activityId, List<Integer> participantIds) {
        ExtracurricularActivityResponseDTO activity = requireManageableActivity(activityId);
        List<Integer> ids = distinctPositive(participantIds);
        // Yêu cầu phải chọn ít nhất một participantId hợp lệ để thực hiện xóa.
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sinh viên.");
        }
        List<Integer> removedStudentIds = participantDAO.deleteRemovableAndReturnStudentIds(activityId, ids);
        // Nếu có sinh viên bị xóa thành công, gửi thông báo cho từng sinh viên bị xóa.
        if (!removedStudentIds.isEmpty()) {
            String message = "Bạn đã bị xóa khỏi danh sách tham gia hoạt động \"" + activity.getName() + "\".";
            // Duyệt danh sách mã sinh viên đã bị xóa để gửi thông báo cá nhân.
            for (Integer studentId : removedStudentIds) {
                // Chỉ gửi thông báo với mã sinh viên dương hợp lệ.
                if (studentId != null && studentId > 0) {
                    notificationService.notifyStudent(studentId, "ACTIVITY_PARTICIPANT_REMOVED",
                            "Bạn bị xóa khỏi hoạt động", message, "/activities");
                }
            }
        }
        return removedStudentIds.size();
    }

    /**
     * Lấy danh sách trạng thái điểm danh theo từng buổi (từ buổi 1 đến totalSessions) của một sinh viên.
     */
    @Override
    public List<ParticipantSessionAttendanceDTO> getSessionAttendances(int activityId, int participantId, int totalSessions) {
        // Trả về danh sách rỗng nếu mã hoạt động hoặc mã người tham gia không hợp lệ.
        if (activityId <= 0 || participantId <= 0) {
            return List.of();
        }
        return participantDAO.findSessionAttendances(activityId, participantId, Math.max(1, totalSessions));
    }

    /**
     * Lấy lịch sử thay đổi trạng thái điểm danh của một sinh viên trong hoạt động.
     */
    @Override
    public List<AttendanceHistoryDTO> getAttendanceHistory(int activityId, int participantId) {
        // Trả về danh sách rỗng nếu mã hoạt động hoặc mã người tham gia không hợp lệ.
        if (activityId <= 0 || participantId <= 0) {
            return List.of();
        }
        return participantDAO.findAttendanceHistory(activityId, participantId);
    }

    /**
     * Đồng bộ lại kết quả điểm rèn luyện / tổng hợp của toàn bộ sinh viên tham gia hoạt động xuống cơ sở dữ liệu.
     */
    @Override
    public void syncActivityResults(int activityId) {
        // Chỉ thực hiện đồng bộ khi mã hoạt động hợp lệ.
        if (activityId > 0) {
            participantDAO.syncActivityResults(activityId);
        }
    }

    /**
     * Xác thực hoạt động tồn tại và chưa bị hủy trước khi cho phép thêm/xóa sinh viên trong danh sách tham gia.
     */
    private ExtracurricularActivityResponseDTO requireManageableActivity(int activityId) {
        // Từ chối nếu mã hoạt động không phải số dương.
        if (activityId <= 0) {
            throw new IllegalArgumentException("Thông tin hoạt động không hợp lệ.");
        }
        ExtracurricularActivityResponseDTO activity = activityService.getActivityById(activityId);
        // Từ chối nếu không tìm thấy hoạt động tương ứng trong DB.
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        // Chặn thay đổi danh sách sinh viên nếu hoạt động đã bị hủy.
        if (!canManageParticipants(activity)) {
            throw new IllegalArgumentException("Không thể thay đổi danh sách sinh viên của hoạt động đã hủy.");
        }
        return activity;
    }

    /**
     * Lọc danh sách ID đầu vào để giữ lại các số nguyên dương không trùng lặp (giữ nguyên thứ tự ban đầu).
     */
    private List<Integer> distinctPositive(List<Integer> ids) {
        Set<Integer> result = new LinkedHashSet<>();
        // Chỉ duyệt khi danh sách đầu vào khác null.
        if (ids != null) {
            // Duyệt từng ID trong danh sách đầu vào để loại bỏ giá trị null, <= 0 và phần tử trùng lặp.
            for (Integer id : ids) {
                // Chỉ thêm vào tập kết quả các ID dương hợp lệ.
                if (id != null && id > 0) {
                    result.add(id);
                }
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Chuyển chuỗi tên trạng thái điểm danh sang enum {@link ParticipantStatus}; trả về null nếu chuỗi không hợp lệ.
     */
    private ParticipantStatus parseStatus(String value) {
        // Trả về null ngay nếu giá trị đầu vào là null.
        if (value == null) {
            return null;
        }
        try {
            return ParticipantStatus.valueOf(value.trim());
        // Chuỗi không khớp bất kỳ hằng số enum ParticipantStatus nào -> trả về null để bỏ qua bản ghi này.
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
