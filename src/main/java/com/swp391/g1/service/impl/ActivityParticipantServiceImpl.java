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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityParticipantServiceImpl implements IActivityParticipantService {

    private static final BigDecimal MIN_EVAL_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_EVAL_SCORE = new BigDecimal("5.00");

    private final IActivityParticipantDAO participantDAO;
    private final IExtracurricularActivityService activityService;

    public ActivityParticipantServiceImpl() {
        this.participantDAO = new ActivityParticipantDAOImpl();
        this.activityService = new ExtracurricularActivityServiceImpl();
    }

    @Override
    public List<ActivityParticipantResponseDTO> getParticipants(int activityId) {
        return getParticipants(activityId, 1);
    }

    @Override
    public List<ActivityParticipantResponseDTO> getParticipants(int activityId, int sessionNumber) {
        if (activityId <= 0) {
            return List.of();
        }
        return participantDAO.findByActivityId(activityId, Math.max(1, sessionNumber));
    }

    @Override
    public boolean canTakeAttendance(ExtracurricularActivityResponseDTO activity) {
        return activity != null
                && (activity.getActivityStatus() == ActivityStatus.ONGOING
                        || activity.getActivityStatus() == ActivityStatus.COMPLETED);
    }

    @Override
    public int saveAttendance(int activityId, Map<Integer, String> statusByParticipantId, CurrentUser user) {
        return saveAttendance(activityId, 1, statusByParticipantId, user);
    }

    @Override
    public int saveAttendance(int activityId, int sessionNumber, Map<Integer, String> statusByParticipantId, CurrentUser user) {
        if (activityId <= 0 || user == null || !AccessPolicy.canTakeAttendance(user)) {
            throw new IllegalArgumentException("Thông tin điểm danh không hợp lệ.");
        }
        ExtracurricularActivityResponseDTO activity = activityService.getActivityById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        if (!canTakeAttendance(activity)) {
            throw new IllegalArgumentException(
                    "Chỉ được điểm danh khi hoạt động đang diễn ra hoặc đã hoàn thành.");
        }
        int totalSessions = Math.max(1, activity.getAttendanceSessionCount());
        if (sessionNumber < 1 || sessionNumber > totalSessions) {
            throw new IllegalArgumentException(
                    "Buổi điểm danh không hợp lệ. Hoạt động này có " + totalSessions + " buổi điểm danh.");
        }
        Map<Integer, ParticipantStatus> statuses = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : statusByParticipantId.entrySet()) {
            ParticipantStatus status = parseStatus(entry.getValue());
            if (entry.getKey() != null && entry.getKey() > 0 && status != null) {
                statuses.put(entry.getKey(), status);
            }
        }
        if (statuses.isEmpty()) {
            return 0;
        }
        String actorId = String.valueOf(user.getId());
        return participantDAO.updateStatuses(activityId, sessionNumber, statuses, actorId);
    }

    @Override
    public int saveParticipationGrades(int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, CurrentUser user) {
        if (activityId <= 0 || user == null || !AccessPolicy.canTakeAttendance(user)) {
            throw new IllegalArgumentException("Thông tin chấm điểm tham gia không hợp lệ.");
        }
        ExtracurricularActivityResponseDTO activity = activityService.getActivityById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        if (!canTakeAttendance(activity)) {
            throw new IllegalArgumentException(
                    "Chỉ được chấm điểm tham gia khi hoạt động đang diễn ra hoặc đã hoàn thành.");
        }
        List<Integer> ids = distinctPositive(participantIds);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sinh viên để chấm điểm.");
        }
        if (evalScore == null
                || evalScore.compareTo(MIN_EVAL_SCORE) < 0
                || evalScore.compareTo(MAX_EVAL_SCORE) > 0) {
            throw new IllegalArgumentException("Điểm đánh giá tham gia phải nằm trong khoảng từ 0 đến 5 điểm.");
        }
        if (feedback != null && feedback.trim().length() > 1000) {
            throw new IllegalArgumentException("Nhận xét / Feedback không được vượt quá 1000 ký tự.");
        }
        String actorLabel = buildActorLabel(user);
        return participantDAO.updateParticipationGrades(
                activityId, ids, evalScore, feedback, actorLabel, user.getId());
    }

    private String buildActorLabel(CurrentUser user) {
        if (user == null) {
            return "Hệ thống";
        }
        String name = user.getDisplayName();
        String role = user.getRoleLabel();
        if (name != null && !name.isBlank()) {
            return (role != null && !role.isBlank())
                    ? name.trim() + " (" + role.trim() + ")"
                    : name.trim();
        }
        return user.getUsername() == null ? "Hệ thống" : user.getUsername();
    }

    @Override
    public boolean canManageParticipants(ExtracurricularActivityResponseDTO activity) {
        return activity != null && activity.getActivityStatus() != ActivityStatus.CANCELLED;
    }

    @Override
    public List<ParticipantCandidateDTO> getCandidates(int activityId) {
        if (activityId <= 0) {
            return List.of();
        }
        return participantDAO.findCandidates(activityId);
    }

    @Override
    public int addStudents(int activityId, List<Integer> studentIds) {
        return addStudents(activityId, studentIds, 1);
    }

    @Override
    public int addStudents(int activityId, List<Integer> studentIds, Integer registrationBy) {
        ExtracurricularActivityResponseDTO activity = requireManageableActivity(activityId);
        List<Integer> ids = distinctPositive(studentIds);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sinh viên.");
        }
        Integer max = activity.getMaxParticipants();
        int added = participantDAO.insertParticipants(activityId, ids, max, registrationBy);
        if (added < 0) {
            int remaining = Math.max(0, max - participantDAO.countOccupiedByActivityId(activityId));
            throw new IllegalArgumentException("Hoạt động chỉ còn " + remaining
                    + " chỗ trống, không thể thêm " + ids.size() + " sinh viên.");
        }
        return added;
    }

    @Override
    public int addStudentsByGroup(int activityId, int departmentId, int majorId, int programId, int classId) {
        return addStudentsByGroup(activityId, departmentId, majorId, programId, classId, 1);
    }

    @Override
    public int addStudentsByGroup(int activityId, int departmentId, int majorId, int programId, int classId, Integer registrationBy) {
        if (departmentId <= 0 && majorId <= 0 && programId <= 0 && classId <= 0) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ít nhất một tiêu chí: khoa, ngành, chương trình đào tạo hoặc lớp.");
        }
        requireManageableActivity(activityId);
        List<Integer> studentIds = new ArrayList<>();
        for (ParticipantCandidateDTO candidate : participantDAO.findCandidates(activityId)) {
            if ((departmentId <= 0 || candidate.getDepartmentId() == departmentId)
                    && (majorId <= 0 || candidate.getMajorId() == majorId)
                    && (programId <= 0 || candidate.getAcademicProgramId() == programId)
                    && (classId <= 0 || candidate.getMainClassId() == classId)) {
                studentIds.add(candidate.getStudentId());
            }
        }
        if (studentIds.isEmpty()) {
            return 0;
        }
        return addStudents(activityId, studentIds, registrationBy);
    }

    @Override
    public int removeParticipants(int activityId, List<Integer> participantIds) {
        requireManageableActivity(activityId);
        List<Integer> ids = distinctPositive(participantIds);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sinh viên.");
        }
        return participantDAO.deleteRemovable(activityId, ids);
    }

    @Override
    public List<ParticipantSessionAttendanceDTO> getSessionAttendances(int activityId, int participantId, int totalSessions) {
        if (activityId <= 0 || participantId <= 0) {
            return List.of();
        }
        return participantDAO.findSessionAttendances(activityId, participantId, Math.max(1, totalSessions));
    }

    @Override
    public List<AttendanceHistoryDTO> getAttendanceHistory(int activityId, int participantId) {
        if (activityId <= 0 || participantId <= 0) {
            return List.of();
        }
        return participantDAO.findAttendanceHistory(activityId, participantId);
    }

    private ExtracurricularActivityResponseDTO requireManageableActivity(int activityId) {
        if (activityId <= 0) {
            throw new IllegalArgumentException("Thông tin hoạt động không hợp lệ.");
        }
        ExtracurricularActivityResponseDTO activity = activityService.getActivityById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        if (!canManageParticipants(activity)) {
            throw new IllegalArgumentException("Không thể thay đổi danh sách sinh viên của hoạt động đã hủy.");
        }
        return activity;
    }

    private List<Integer> distinctPositive(List<Integer> ids) {
        Set<Integer> result = new LinkedHashSet<>();
        if (ids != null) {
            for (Integer id : ids) {
                if (id != null && id > 0) {
                    result.add(id);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private ParticipantStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ParticipantStatus.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
