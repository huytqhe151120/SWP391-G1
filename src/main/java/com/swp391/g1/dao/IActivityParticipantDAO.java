package com.swp391.g1.dao;

import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.AttendanceHistoryDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
import com.swp391.g1.dto.response.ParticipantSessionAttendanceDTO;
import com.swp391.g1.model.Enum.ParticipantStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IActivityParticipantDAO {

    record AttendanceStatusChange(
            int participantId,
            Integer studentAccountId,
            String oldStatus,
            String newStatus) {
    }

    /**
     * Kết quả thêm hàng loạt sinh viên vào hoạt động:
     * {@code exceededCapacity = true} nếu vượt quá {@code maxParticipants} (transaction đã rollback),
     * ngược lại {@code addedStudentIds} chứa chính xác các {@code student_id} vừa được chèn mới.
     */
    record InsertParticipantsResult(
            boolean exceededCapacity,
            List<Integer> addedStudentIds) {
    }

    /**
     * Dữ liệu thô của một người tham gia được đọc từ DB (không chứa logic tính điểm trong SQL)
     * để tầng Service tính toán và lắp ráp {@link ActivityParticipantResponseDTO}.
     */
    record ParticipantRawData(
            int participantId,
            int studentId,
            String studentCode,
            String studentName,
            String mainClassCode,
            String academicProgramName,
            String majorName,
            String departmentName,
            ParticipantStatus legacyAttendanceStatus,
            String registrationStatus,
            String participationRole,
            Long registrationBy,
            LocalDateTime createdAt,
            BigDecimal participationEvalScore,
            String participationFeedback,
            String participationGradedBy,
            LocalDateTime participationGradedAt,
            BigDecimal summaryScore,
            int totalSessions,
            boolean summaryRequired,
            BigDecimal orgLevelCoefficient,
            boolean hasAttendanceHistory,
            Map<Integer, ParticipantStatus> sessionStatuses) {
    }

    List<ActivityParticipantResponseDTO> findByActivityId(int activityId);

    default List<ActivityParticipantResponseDTO> findByActivityId(int activityId, int sessionNumber) {
        return findByActivityId(activityId);
    }

    /**
     * Tải dữ liệu thô (chưa tính điểm) của toàn bộ người tham gia trong hoạt động để tầng Service xử lý.
     */
    default List<ParticipantRawData> findRawParticipantsByActivityId(int activityId) {
        return List.of();
    }

    default Set<Integer> findActivityIdsByStudentId(int studentId) {
        return Set.of();
    }

    /** Active students that are not yet participants of the activity. */
    List<ParticipantCandidateDTO> findCandidates(int activityId);

    int countByActivityId(int activityId);

    /** Distinct students in the attendance roster or holding an active registration seat. */
    int countOccupiedByActivityId(int activityId);

    /**
     * Adds active students that are not yet in the activity, in one transaction. Returns the number of
     * inserted rows, or -1 (nothing inserted) when the result would exceed maxParticipants (null = no limit).
     */
    int insertParticipants(int activityId, List<Integer> studentIds, Integer maxParticipants);

    default int insertParticipants(int activityId, List<Integer> studentIds, Integer maxParticipants, Integer registrationBy) {
        return insertParticipants(activityId, studentIds, maxParticipants);
    }

    /**
     * Thêm hàng loạt sinh viên và trả về chính xác danh sách {@code student_id} vừa được thêm
     * (tránh việc Service phải truy vấn lại toàn bộ danh sách người tham gia trước/sau khi chèn).
     */
    default InsertParticipantsResult insertParticipantsAndReturnAdded(
            int activityId, List<Integer> studentIds, Integer maxParticipants, Integer registrationBy) {
        int added = insertParticipants(activityId, studentIds, maxParticipants, registrationBy);
        if (added < 0) {
            return new InsertParticipantsResult(true, List.of());
        }
        return new InsertParticipantsResult(false, List.of());
    }

    /**
     * Deletes participants that are still NOT_YET and have no attendance history, so no attendance
     * data is lost. Other ids are skipped. Returns the number of deleted rows.
     */
    int deleteRemovable(int activityId, List<Integer> participantIds);

    /**
     * Xóa các người tham gia đủ điều kiện và trả về chính xác danh sách {@code student_id} đã bị xóa.
     */
    default List<Integer> deleteRemovableAndReturnStudentIds(int activityId, List<Integer> participantIds) {
        deleteRemovable(activityId, participantIds);
        return List.of();
    }

    /**
     * Updates attendance statuses of participants in one activity and records each actual change
     * in activity_attendance_history, all in one transaction. Returns the number of changed rows.
     */
    int updateStatuses(int activityId, Map<Integer, ParticipantStatus> statusByParticipantId, String changedBy);

    default int updateStatuses(int activityId, int sessionNumber,
            Map<Integer, ParticipantStatus> statusByParticipantId, String changedBy) {
        return updateStatuses(activityId, statusByParticipantId, changedBy);
    }

    default List<AttendanceStatusChange> updateStatusesAndReturnChanges(
            int activityId, int sessionNumber,
            Map<Integer, ParticipantStatus> statusByParticipantId, String changedBy) {
        updateStatuses(activityId, sessionNumber, statusByParticipantId, changedBy);
        return List.of();
    }

    default void syncActivityResults(int activityId) {
    }

    default List<ParticipantSessionAttendanceDTO> findSessionAttendances(int activityId, int participantId, int totalSessions) {
        return List.of();
    }

    default List<AttendanceHistoryDTO> findAttendanceHistory(int activityId, int participantId) {
        return List.of();
    }

    /**
     * Updates participation evaluation score (0-5) and feedback for one or many participants (batch grading),
     * records audit trail in activity_grade_history, and recalculates student_activity_result.
     */
    int updateParticipationGrades(int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, String gradedBy, Integer gradedById);

    /**
     * Cập nhật điểm đánh giá tham gia theo batch và trả về danh sách {@code student_id} thực sự đủ điều kiện
     * và đã được chấm điểm.
     */
    default List<Integer> updateParticipationGradesAndReturnStudentIds(
            int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, String gradedBy, Integer gradedById) {
        updateParticipationGrades(activityId, participantIds, evalScore, feedback, gradedBy, gradedById);
        return List.of();
    }
}
