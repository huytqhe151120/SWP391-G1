package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityParticipantDAO;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.AttendanceHistoryDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
import com.swp391.g1.dto.response.ParticipantSessionAttendanceDTO;
import com.swp391.g1.model.Enum.ParticipantStatus;
import com.swp391.g1.util.ActivityScoreCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAO triển khai truy xuất dữ liệu người tham gia hoạt động ngoại khóa.
 *
 * <p>Kiến trúc tối ưu:</p>
 * <ul>
 *     <li>SQL chỉ đảm nhiệm truy vấn gọn (join/filter), quản lý transaction, khóa sức chứa và ghi batch.</li>
 *     <li>Toàn bộ công thức tính điểm, hệ số, suy luận trạng thái tổng và điều kiện xóa được tập trung tại
 *         {@link ActivityScoreCalculator}.</li>
 *     <li>Các thao tác điểm danh, chấm điểm, thêm/xóa chỉ đồng bộ đúng các {@code participantIds} bị ảnh hưởng
 *         bằng batch read/write, loại bỏ hoàn toàn N+1 và khóa toàn bộ hoạt động.</li>
 * </ul>
 */
public class ActivityParticipantDAOImpl implements IActivityParticipantDAO {

    /** Kích thước chunk an toàn cho mệnh đề IN (...) để không vượt giới hạn 2100 tham số của SQL Server. */
    private static final int IN_CLAUSE_CHUNK_SIZE = 500;
    /** ID tài khoản mặc định dùng khi không phân tích được người thực hiện thao tác điểm danh. */
    private static final int DEFAULT_ACTOR_ID = 767;

    /** Câu lệnh MERGE (UPSERT) cập nhật hoặc thêm mới trạng thái điểm danh theo từng buổi cho người tham gia. */
    private static final String UPSERT_SESSION_SQL = """
            MERGE dbo.activity_participant_session_attendance AS target
            USING (SELECT ? AS activity_participant_id, ? AS session_number) AS src
            ON (target.activity_participant_id = src.activity_participant_id
                AND target.session_number = src.session_number)
            WHEN MATCHED THEN
                UPDATE SET attendance_status = ?, updated_by = ?, updated_at = SYSUTCDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (activity_participant_id, session_number, attendance_status, updated_by, updated_at)
                VALUES (src.activity_participant_id, src.session_number, ?, ?, SYSUTCDATETIME());
            """;

    /** Câu lệnh chèn bản ghi lịch sử mỗi khi trạng thái điểm danh của một buổi bị thay đổi. */
    private static final String INSERT_ATTENDANCE_HISTORY_SQL = """
            INSERT INTO dbo.activity_attendance_history
                (activity_participant_id, session_number, old_status, new_status, created_by)
            VALUES (?, ?, ?, ?, ?)
            """;

    /** Câu lệnh cập nhật trạng thái điểm danh tổng của người tham gia trong bảng activity_participant. */
    private static final String UPDATE_PARTICIPANT_STATUS_SQL = """
            UPDATE dbo.activity_participant
            SET attendance_status = ?
            WHERE id = ? AND activity_id = ?
            """;

    /** Câu lệnh MERGE (UPSERT) đồng bộ các điểm thành phần, hệ số và điểm tổng kết vào student_activity_result. */
    private static final String UPSERT_ACTIVITY_RESULT_SQL = """
            MERGE dbo.student_activity_result AS target
            USING (SELECT ? AS student_id, ? AS activity_id) AS src
            ON (target.student_id = src.student_id AND target.activity_id = src.activity_id)
            WHEN MATCHED THEN
                UPDATE SET
                    semester_id = ?,
                    participation_role = ?,
                    attendance_score = ?,
                    participation_eval_score = ?,
                    participation_score = ?,
                    summary_score = ?,
                    average_score = ?,
                    org_level_coefficient = ?,
                    early_reg_coefficient = ?,
                    organizer_coefficient = ?,
                    final_point = ?,
                    updated_at = SYSUTCDATETIME()
            WHEN NOT MATCHED THEN
                INSERT (
                    student_id, activity_id, semester_id, participation_role,
                    attendance_score, participation_eval_score, participation_score,
                    summary_score, average_score,
                    org_level_coefficient, early_reg_coefficient, organizer_coefficient,
                    final_point, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME());
            """;

    /** Siêu dữ liệu cấu hình tính điểm của một hoạt động. */
    private record ActivityMeta(
            int activityId,
            Integer semesterId,
            int totalSessions,
            boolean summaryRequired,
            BigDecimal orgLevelCoefficient,
            Integer responsibleDepartmentId) {
    }

    /** Bản ghi nội bộ phục vụ đồng bộ điểm cho một người tham gia cụ thể. */
    private record ParticipantSyncRow(
            int participantId,
            int studentId,
            Integer studentAccountId,
            ParticipantStatus currentOverallStatus,
            String registrationStatus,
            String participationRole,
            BigDecimal evalScore,
            BigDecimal summaryScore) {
    }

    /** Bản ghi đầu vào đã tính toán sẵn để ghi batch vào {@code student_activity_result}. */
    private record ResultUpsertRow(
            int studentId,
            int activityId,
            Integer semesterId,
            String participationRole,
            ActivityScoreCalculator.ScoreBreakdown breakdown) {
    }

    /** Bản ghi ghi batch điểm danh theo buổi. */
    private record SessionUpsertRow(
            int participantId,
            int sessionNumber,
            ParticipantStatus status,
            Integer actorId) {
    }

    /** Thông tin người thao tác đã giải mã (mã, tên, phòng ban / doanh nghiệp đối tác). */
    private record ActorInfo(String code, String name, String unit) {
    }

    /** Thông tin cơ bản của cán bộ trường để giải mã người thực hiện thao tác. */
    private record StaffBasic(int staffId, Integer accountId, String code, String name) {
    }

    /** Thông tin cơ bản của nhân sự doanh nghiệp đối tác để giải mã người thực hiện thao tác. */
    private record PartnerBasic(int accountId, String code, String name, String companyName) {
    }

    /** Bản ghi tóm tắt lần chỉnh sửa lịch sử điểm danh gần nhất của một buổi. */
    private record HistoryEntry(int sessionNumber, Integer actorId, LocalDateTime createdAt) {
    }

    /**
     * Khởi tạo DAO và đảm bảo schema cơ sở dữ liệu liên quan đến đăng ký/tuyển thành viên đã được tạo.
     */
    public ActivityParticipantDAOImpl() {
        RecruitmentSchema.ensure();
    }

    /**
     * Lấy danh sách người tham gia của một hoạt động, mặc định xem trạng thái điểm danh ở buổi 1.
     */
    @Override
    public List<ActivityParticipantResponseDTO> findByActivityId(int activityId) {
        return findByActivityId(activityId, 1);
    }

    /**
     * Lấy danh sách chi tiết người tham gia tại buổi {@code sessionNumber}.
     * Tải dữ liệu thô qua {@link #findRawParticipantsByActivityId(int)} rồi tính toàn bộ điểm/trạng thái
     * bằng {@link ActivityScoreCalculator}.
     */
    @Override
    public List<ActivityParticipantResponseDTO> findByActivityId(int activityId, int sessionNumber) {
        List<ParticipantRawData> rawList = findRawParticipantsByActivityId(activityId);
        List<ActivityParticipantResponseDTO> result = new ArrayList<>(rawList.size());
        // Duyệt từng người tham gia thô và chuyển đổi sang DTO đã tính đủ điểm thành phần, hệ số và trạng thái buổi
        for (ParticipantRawData raw : rawList) {
            result.add(mapToResponseDTO(raw, sessionNumber));
        }
        return result;
    }

    /**
     * Tải dữ liệu thô của toàn bộ người tham gia trong hoạt động bằng 3 truy vấn phẳng, thân thiện với index
     * (không chứa công thức tính điểm hay {@code OUTER APPLY} tính tổng trong SQL) để tầng Service xử lý.
     */
    @Override
    public List<ParticipantRawData> findRawParticipantsByActivityId(int activityId) {
        // Đảm bảo các bảng/cột điểm danh theo buổi và đăng ký đã tồn tại trước khi truy vấn
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        RecruitmentSchema.ensure();

        // Truy vấn 1: lấy thông tin người tham gia kèm hồ sơ học vụ sinh viên và cấu hình tính điểm của hoạt động
        String baseSql = """
                SELECT ap.id, ap.student_id, ap.attendance_status, ap.registration_status, ap.registration_by,
                       ap.created_at, ap.participation_role,
                       ap.participation_eval_score, ap.participation_feedback,
                       ap.participation_graded_by, ap.participation_graded_at,
                       ap.summary_score,
                       COALESCE(NULLIF(ea.attendance_session_count, 0), 1) AS total_sessions,
                       ea.is_summary_required,
                       ol.coefficient AS org_level_coeff,
                       s.code AS student_code, s.name AS student_name, mc.code AS class_code,
                       p.name AS program_name, m.name AS major_name, d.name AS department_name
                FROM dbo.activity_participant ap
                JOIN dbo.student s ON s.id = ap.student_id
                JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
                LEFT JOIN dbo.activity_organization_level ol ON ol.id = ea.organization_level_id
                LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
                LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
                LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
                LEFT JOIN dbo.department d ON d.id = m.responsible_department_id
                WHERE ap.activity_id = ?
                ORDER BY s.name ASC, s.code ASC
                """;

        try (Connection conn = DBContext.getConnection()) {
            List<BaseParticipantTemp> baseRows = new ArrayList<>();
            int totalSessions = 1;

            try (PreparedStatement ps = conn.prepareStatement(baseSql)) {
                ps.setInt(1, activityId);
                try (ResultSet rs = ps.executeQuery()) {
                    // Duyệt toàn bộ bản ghi người tham gia trả về từ truy vấn gốc và lưu tạm vào baseRows
                    while (rs.next()) {
                        int rowTotalSessions = Math.max(1, rs.getInt("total_sessions"));
                        totalSessions = rowTotalSessions;
                        long regByVal = rs.getLong("registration_by");
                        // Nếu cột registration_by là NULL trong DB thì giữ null thay vì giá trị mặc định 0L
                        Long regBy = rs.wasNull() ? null : regByVal;
                        Timestamp createdTs = rs.getTimestamp("created_at");
                        Timestamp gradedTs = rs.getTimestamp("participation_graded_at");

                        baseRows.add(new BaseParticipantTemp(
                                rs.getInt("id"),
                                rs.getInt("student_id"),
                                rs.getString("student_code"),
                                rs.getString("student_name"),
                                rs.getString("class_code"),
                                rs.getString("program_name"),
                                rs.getString("major_name"),
                                rs.getString("department_name"),
                                parseStatusOrNotYet(rs.getString("attendance_status")),
                                rs.getString("registration_status"),
                                rs.getString("participation_role"),
                                regBy,
                                // Chuyển đổi Timestamp sang LocalDateTime nếu có giá trị
                                createdTs == null ? null : createdTs.toLocalDateTime(),
                                rs.getBigDecimal("participation_eval_score"),
                                rs.getString("participation_feedback"),
                                rs.getString("participation_graded_by"),
                                gradedTs == null ? null : gradedTs.toLocalDateTime(),
                                rs.getBigDecimal("summary_score"),
                                rowTotalSessions,
                                rs.getBoolean("is_summary_required"),
                                rs.getBigDecimal("org_level_coeff")
                        ));
                    }
                }
            }

            // Nếu hoạt động chưa có người tham gia nào thì kết thúc sớm, bỏ qua 2 truy vấn bổ sung
            if (baseRows.isEmpty()) {
                return List.of();
            }

            // Truy vấn 2 & 3: tải trạng thái điểm danh chi tiết theo buổi và danh sách participant đã có lịch sử điểm danh
            Map<Integer, Map<Integer, ParticipantStatus>> sessionsByParticipant =
                    loadSessionStatusesForActivity(conn, activityId, totalSessions);
            Set<Integer> participantsWithHistory = loadParticipantsWithHistoryForActivity(conn, activityId);

            List<ParticipantRawData> result = new ArrayList<>(baseRows.size());
            // Ghép dữ liệu cơ bản với map điểm danh theo buổi và cờ lịch sử để tạo danh sách ParticipantRawData hoàn chỉnh
            for (BaseParticipantTemp row : baseRows) {
                Map<Integer, ParticipantStatus> sessionMap = sessionsByParticipant.getOrDefault(
                        row.participantId, Collections.emptyMap());
                result.add(new ParticipantRawData(
                        row.participantId,
                        row.studentId,
                        row.studentCode,
                        row.studentName,
                        row.classCode,
                        row.programName,
                        row.majorName,
                        row.departmentName,
                        row.legacyAttendanceStatus,
                        row.registrationStatus,
                        row.participationRole,
                        row.registrationBy,
                        row.createdAt,
                        row.evalScore,
                        row.feedback,
                        row.gradedBy,
                        row.gradedAt,
                        row.summaryScore,
                        row.totalSessions,
                        row.summaryRequired,
                        row.orgLevelCoeff,
                        participantsWithHistory.contains(row.participantId),
                        sessionMap
                ));
            }
            return result;
        } catch (SQLException e) {
            // Bọc lỗi SQL thành IllegalStateException để báo lỗi truy vấn cho tầng gọi
            throw new IllegalStateException("Failed to find participants for activity " + activityId, e);
        }
    }

    /** Bản ghi tạm giữ dữ liệu phẳng từ truy vấn chính trước khi ghép với dữ liệu buổi điểm danh và lịch sử. */
    private record BaseParticipantTemp(
            int participantId,
            int studentId,
            String studentCode,
            String studentName,
            String classCode,
            String programName,
            String majorName,
            String departmentName,
            ParticipantStatus legacyAttendanceStatus,
            String registrationStatus,
            String participationRole,
            Long registrationBy,
            LocalDateTime createdAt,
            BigDecimal evalScore,
            String feedback,
            String gradedBy,
            LocalDateTime gradedAt,
            BigDecimal summaryScore,
            int totalSessions,
            boolean summaryRequired,
            BigDecimal orgLevelCoeff) {
    }

    /**
     * Lắp ráp {@link ActivityParticipantResponseDTO} từ dữ liệu thô bằng {@link ActivityScoreCalculator}.
     */
    public static ActivityParticipantResponseDTO mapToResponseDTO(ParticipantRawData raw, int sessionNumber) {
        // Chuẩn hóa tổng số buổi >= 1 và giới hạn số buổi đang xem trong khoảng [1, totalSessions]
        int totalSessions = Math.max(1, raw.totalSessions());
        int clampedSession = Math.min(Math.max(1, sessionNumber), totalSessions);

        // Tính toàn bộ điểm thành phần, trạng thái tổng, hệ số và điểm cuối bằng công thức tập trung
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

        // Suy luận trạng thái điểm danh riêng của buổi đang xem (có fallback dữ liệu cũ cho buổi 1)
        ParticipantStatus sessionStatus = ActivityScoreCalculator.resolveSessionStatus(
                raw.sessionStatuses(), clampedSession, raw.legacyAttendanceStatus());

        // Gán thông tin định danh và học vụ sinh viên vào DTO phản hồi
        ActivityParticipantResponseDTO dto = new ActivityParticipantResponseDTO();
        dto.setParticipantId(raw.participantId());
        dto.setStudentId(raw.studentId());
        dto.setStudentCode(raw.studentCode());
        dto.setStudentName(raw.studentName());
        dto.setMainClassCode(raw.mainClassCode());
        dto.setAcademicProgramName(raw.academicProgramName());
        dto.setMajorName(raw.majorName());
        dto.setDepartmentName(raw.departmentName());

        // Gán thông tin điểm danh tổng, điểm danh buổi đang chọn và số buổi đã điểm danh
        dto.setAttendanceStatus(breakdown.overallStatus());
        dto.setTotalSessions(totalSessions);
        dto.setSessionNumber(clampedSession);
        dto.setSessionAttendanceStatus(sessionStatus);
        dto.setSessionAttendanceScore(ActivityScoreCalculator.computeSessionScore(sessionStatus));
        dto.setAttendedSessionCount(breakdown.attendedSessionCount());
        dto.setAbsentSessionCount(breakdown.absentSessionCount());
        dto.setMarkedSessionCount(breakdown.markedSessionCount());

        // Gán thông tin đăng ký, vai trò và xác định xem người tham gia có được phép xóa khỏi hoạt động hay không
        dto.setRegistrationStatus(raw.registrationStatus());
        dto.setParticipationRole(raw.participationRole());
        dto.setRegistrationBy(raw.registrationBy());
        dto.setCreatedAt(raw.createdAt());
        dto.setRemovable(ActivityScoreCalculator.isRemovable(
                raw.participationRole(),
                breakdown.overallStatus(),
                breakdown.markedSessionCount(),
                raw.hasAttendanceHistory()));

        // Gán dữ liệu chấm điểm đánh giá và điểm bài thu hoạch
        dto.setParticipationEvalScore(raw.participationEvalScore());
        dto.setParticipationFeedback(raw.participationFeedback());
        dto.setParticipationGradedBy(raw.participationGradedBy());
        dto.setParticipationGradedAt(raw.participationGradedAt());
        dto.setSummaryScore(raw.summaryScore());

        // Gán kết quả điểm đã tính toán và các hệ số quy đổi điểm rèn luyện
        dto.setAttendanceScore(breakdown.attendanceScore());
        dto.setParticipationScore(breakdown.participationScore());
        dto.setAverageScore(breakdown.averageScore());
        dto.setOrgLevelCoefficient(breakdown.orgLevelCoefficient());
        dto.setEarlyRegCoefficient(breakdown.earlyRegCoefficient());
        dto.setFinalPoint(breakdown.finalPoint());
        return dto;
    }

    /**
     * Tìm danh sách các sinh viên đang ở trạng thái ACTIVE và chưa tham gia hoạt động
     * để làm ứng viên thêm vào danh sách tham gia.
     */
    @Override
    public List<ParticipantCandidateDTO> findCandidates(int activityId) {
        List<ParticipantCandidateDTO> list = new ArrayList<>();
        // Truy vấn sinh viên ACTIVE kèm thông tin lớp/chương trình/ngành/khoa và loại trừ những sinh viên đã có trong hoạt động
        String sql = """
                SELECT s.id, s.code, s.name, mc.id AS class_id, mc.code AS class_code,
                       p.id AS program_id, p.name AS program_name, m.id AS major_id, m.name AS major_name,
                       d.id AS department_id, d.name AS department_name
                FROM dbo.student s
                LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
                LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
                LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
                LEFT JOIN dbo.department d ON d.id = m.responsible_department_id
                WHERE s.status = 'ACTIVE'
                  AND NOT EXISTS (SELECT 1 FROM dbo.activity_participant ap
                                  WHERE ap.activity_id = ? AND ap.student_id = s.id)
                ORDER BY s.code ASC
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng sinh viên ứng viên và ánh xạ sang ParticipantCandidateDTO
                while (rs.next()) {
                    ParticipantCandidateDTO dto = new ParticipantCandidateDTO();
                    dto.setStudentId(rs.getInt("id"));
                    dto.setStudentCode(rs.getString("code"));
                    dto.setStudentName(rs.getString("name"));
                    dto.setMainClassId(rs.getInt("class_id"));
                    dto.setMainClassCode(rs.getString("class_code"));
                    dto.setAcademicProgramId(rs.getInt("program_id"));
                    dto.setAcademicProgramName(rs.getString("program_name"));
                    dto.setMajorId(rs.getInt("major_id"));
                    dto.setMajorName(rs.getString("major_name"));
                    dto.setDepartmentId(rs.getInt("department_id"));
                    dto.setDepartmentName(rs.getString("department_name"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn danh sách ứng viên tham gia
            throw new IllegalStateException("Failed to find participant candidates for activity " + activityId, e);
        }
        return list;
    }

    /**
     * Đếm tổng số người tham gia hiện có trong bảng {@code activity_participant} của một hoạt động.
     */
    @Override
    public int countByActivityId(int activityId) {
        try (Connection conn = DBContext.getConnection()) {
            return count(conn, activityId);
        } catch (SQLException e) {
            // Bọc lỗi SQL khi đếm tổng số người tham gia
            throw new IllegalStateException("Failed to count participants for activity " + activityId, e);
        }
    }

    /**
     * Đếm tổng số chỗ (slot) đã bị chiếm của hoạt động, bao gồm không trùng lặp các sinh viên
     * có vai trò PARTICIPANT đã tham gia và các sinh viên có đơn đăng ký ở trạng thái SUBMITTED hoặc APPROVED.
     */
    @Override
    public int countOccupiedByActivityId(int activityId) {
        RecruitmentSchema.ensure();
        try (Connection conn = DBContext.getConnection()) {
            return countOccupied(conn, activityId);
        } catch (SQLException e) {
            // Bọc lỗi SQL khi đếm số chỗ đã bị chiếm của hoạt động
            throw new IllegalStateException("Failed to count occupied seats for activity " + activityId, e);
        }
    }

    /**
     * Thêm danh sách sinh viên vào hoạt động với người đăng ký mặc định (ID = 1).
     */
    @Override
    public int insertParticipants(int activityId, List<Integer> studentIds, Integer maxParticipants) {
        return insertParticipants(activityId, studentIds, maxParticipants, 1);
    }

    /**
     * Thêm hàng loạt sinh viên và trả về số sinh viên thực tế được thêm (hoặc -1 nếu vượt sức chứa).
     */
    @Override
    public int insertParticipants(int activityId, List<Integer> studentIds, Integer maxParticipants, Integer registrationBy) {
        InsertParticipantsResult result = insertParticipantsAndReturnAdded(
                activityId, studentIds, maxParticipants, registrationBy);
        // Quy ước trả về -1 nếu vượt sức chứa, ngược lại trả về số lượng sinh viên đã được thêm
        return result.exceededCapacity() ? -1 : result.addedStudentIds().size();
    }

    /**
     * Thêm hàng loạt sinh viên ACTIVE (chưa có trong hoạt động) trong một transaction có khóa hoạt động
     * ({@code UPDLOCK, ROWLOCK}), chỉ khởi tạo bản ghi {@code student_activity_result} cho đúng các sinh viên
     * vừa được thêm (không đồng bộ lại toàn bộ hoạt động) và trả về danh sách {@code student_id} đã thêm.
     */
    @Override
    public InsertParticipantsResult insertParticipantsAndReturnAdded(
            int activityId, List<Integer> studentIds, Integer maxParticipants, Integer registrationBy) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        RecruitmentSchema.ensure();
        // Nếu danh sách sinh viên truyền vào rỗng hoặc null thì không cần mở kết nối DB
        if (studentIds == null || studentIds.isEmpty()) {
            return new InsertParticipantsResult(false, List.of());
        }

        // Câu lệnh thêm mới người tham gia với trạng thái cán bộ đăng ký trực tiếp (STAFF_ENROLLED)
        String insertSql = """
                INSERT INTO dbo.activity_participant (activity_id, student_id, registration_status, registration_by)
                VALUES (?, ?, 'STAFF_ENROLLED', ?)
                """;

        try (Connection conn = DBContext.getConnection()) {
            // Mở transaction để đảm bảo kiểm tra sức chứa, chèn participant và khởi tạo kết quả điểm là nguyên tử
            conn.setAutoCommit(false);
            try {
                // Khóa bản ghi hoạt động (UPDLOCK, ROWLOCK) đồng thời lấy metadata cấu hình điểm trong 1 truy vấn
                ActivityMeta meta = loadActivityMeta(conn, activityId, true);
                // Nếu hoạt động không tồn tại trong DB thì rollback và trả về kết quả rỗng
                if (meta == null) {
                    conn.rollback();
                    return new InsertParticipantsResult(false, List.of());
                }

                // Lọc đúng các sinh viên ACTIVE chưa tồn tại trong hoạt động (truy vấn batch theo mệnh đề IN)
                List<Integer> eligibleStudentIds = findEligibleStudentIdsToInsert(conn, activityId, studentIds);
                // Nếu không còn sinh viên nào đủ điều kiện thêm thì commit giải phóng khóa và kết thúc sớm
                if (eligibleStudentIds.isEmpty()) {
                    conn.commit();
                    return new InsertParticipantsResult(false, List.of());
                }

                // Xác định ID người đăng ký hợp lệ; nếu không có hoặc <= 0 thì dùng giá trị mặc định 1L
                long byId = (registrationBy != null && registrationBy > 0) ? registrationBy.longValue() : 1L;
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    // Đưa từng sinh viên hợp lệ vào batch chèn vào bảng activity_participant
                    for (Integer studentId : eligibleStudentIds) {
                        insert.setInt(1, activityId);
                        insert.setInt(2, studentId);
                        insert.setLong(3, byId);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }

                // Kiểm tra sức chứa sau khi chèn để xử lý chính xác trường hợp sinh viên đã giữ chỗ qua đơn đăng ký
                if (maxParticipants != null && countOccupied(conn, activityId) > maxParticipants) {
                    // Nếu tổng số chỗ vượt giới hạn cho phép thì rollback toàn bộ batch và báo exceededCapacity = true
                    conn.rollback();
                    return new InsertParticipantsResult(true, List.of());
                }

                // Tính sẵn bộ điểm khởi tạo mặc định (NOT_YET, STAFF_ENROLLED, PARTICIPANT) để khởi tạo cho các sinh viên mới thêm
                ActivityScoreCalculator.ScoreBreakdown initialBreakdown = ActivityScoreCalculator.calculate(
                        Collections.emptyMap(),
                        meta.totalSessions(),
                        ParticipantStatus.NOT_YET,
                        null,
                        null,
                        meta.summaryRequired(),
                        meta.orgLevelCoefficient(),
                        "STAFF_ENROLLED",
                        "PARTICIPANT"
                );
                List<ResultUpsertRow> resultsToUpsert = new ArrayList<>(eligibleStudentIds.size());
                // Tạo danh sách dòng kết quả ban đầu cho từng sinh viên vừa thêm
                for (Integer studentId : eligibleStudentIds) {
                    resultsToUpsert.add(new ResultUpsertRow(
                            studentId, activityId, meta.semesterId(), "PARTICIPANT", initialBreakdown));
                }
                // Ghi batch khởi tạo vào bảng student_activity_result
                batchUpsertActivityResults(conn, resultsToUpsert);

                // Xác nhận hoàn tất transaction
                conn.commit();
                return new InsertParticipantsResult(false, eligibleStudentIds);
            } catch (SQLException e) {
                // Rollback an toàn khi có lỗi SQL trong quá trình chèn batch hoặc đồng bộ điểm
                rollbackQuietly(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL ra ngoài để tầng gọi xử lý
            throw new IllegalStateException("Failed to add participants to activity " + activityId, e);
        }
    }

    /**
     * Xóa hàng loạt người tham gia đủ điều kiện và trả về số bản ghi đã xóa.
     */
    @Override
    public int deleteRemovable(int activityId, List<Integer> participantIds) {
        return deleteRemovableAndReturnStudentIds(activityId, participantIds).size();
    }

    /**
     * Xóa hàng loạt người tham gia thỏa mãn điều kiện {@link ActivityScoreCalculator#isRemovable}
     * trong một transaction, chỉ xóa các bản ghi tương ứng trong {@code student_activity_result}
     * (không đồng bộ lại toàn bộ hoạt động) và trả về danh sách {@code student_id} đã bị xóa.
     */
    @Override
    public List<Integer> deleteRemovableAndReturnStudentIds(int activityId, List<Integer> participantIds) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        // Nếu danh sách ID cần xóa rỗng thì trả về danh sách rỗng ngay
        if (participantIds == null || participantIds.isEmpty()) {
            return List.of();
        }

        // Câu lệnh xóa có ràng buộc điều kiện kép tại DB (phòng race condition): không phải ORGANIZER, chưa điểm danh, chưa có buổi khác NOT_YET và chưa có lịch sử
        String deleteParticipantSql = """
                DELETE FROM dbo.activity_participant
                WHERE id = ? AND activity_id = ?
                  AND participation_role <> 'ORGANIZER'
                  AND attendance_status = 'NOT_YET'
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_participant_session_attendance s
                      WHERE s.activity_participant_id = dbo.activity_participant.id
                        AND s.attendance_status <> 'NOT_YET'
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_attendance_history h
                      WHERE h.activity_participant_id = dbo.activity_participant.id
                  )
                """;
        // Câu lệnh xóa bản ghi kết quả điểm tương ứng trong student_activity_result cho sinh viên vừa bị xóa
        String deleteResultSql = "DELETE FROM dbo.student_activity_result WHERE activity_id = ? AND student_id = ?";

        try (Connection conn = DBContext.getConnection()) {
            // Mở transaction để xóa người tham gia và bản ghi kết quả điểm đồng bộ
            conn.setAutoCommit(false);
            try {
                // Tải thông tin hiện tại của nhóm participantIds thuộc hoạt động
                Map<Integer, ParticipantSyncRow> targetRows = loadParticipantSyncRowsByIds(conn, activityId, participantIds);
                // Nếu không tìm thấy bản ghi nào khớp thì commit và trả về rỗng
                if (targetRows.isEmpty()) {
                    conn.commit();
                    return List.of();
                }

                // Tải trước các participant đã có buổi điểm danh hoặc có lịch sử để lọc đúng theo nghiệp vụ isRemovable
                List<Integer> existingIds = new ArrayList<>(targetRows.keySet());
                Set<Integer> markedIds = loadParticipantIdsWithMarkedSession(conn, existingIds);
                Set<Integer> historyIds = loadParticipantIdsWithHistory(conn, existingIds);

                List<ParticipantSyncRow> removableRows = new ArrayList<>();
                // Duyệt từng người tham gia mục tiêu để kiểm tra điều kiện được phép xóa
                for (ParticipantSyncRow row : targetRows.values()) {
                    // Nếu có bất kỳ buổi nào đã được điểm danh thì đánh dấu markedCount = 1, ngược lại 0
                    int markedCount = markedIds.contains(row.participantId()) ? 1 : 0;
                    boolean hasHistory = historyIds.contains(row.participantId());
                    // Chỉ giữ lại các bản ghi thỏa quy tắc isRemovable của ActivityScoreCalculator
                    if (ActivityScoreCalculator.isRemovable(
                            row.participationRole(), row.currentOverallStatus(), markedCount, hasHistory)) {
                        removableRows.add(row);
                    }
                }

                // Nếu không có người tham gia nào đủ điều kiện xóa thì kết thúc giao dịch sớm
                if (removableRows.isEmpty()) {
                    conn.commit();
                    return List.of();
                }

                List<Integer> deletedStudentIds = new ArrayList<>(removableRows.size());
                try (PreparedStatement deleteParticipant = conn.prepareStatement(deleteParticipantSql)) {
                    // Đưa từng người tham gia hợp lệ vào batch xóa khỏi activity_participant
                    for (ParticipantSyncRow row : removableRows) {
                        deleteParticipant.setInt(1, row.participantId());
                        deleteParticipant.setInt(2, activityId);
                        deleteParticipant.addBatch();
                    }
                    int[] deletedCounts = deleteParticipant.executeBatch();
                    // Đối chiếu kết quả executeBatch để ghi nhận đúng các studentId thực sự đã bị xóa tại DB
                    for (int i = 0; i < removableRows.size(); i++) {
                        // Chỉ ghi nhận khi câu DELETE tác động khác 0 dòng (bao gồm cả SUCCESS_NO_INFO)
                        if (deletedCounts[i] != 0) {
                            deletedStudentIds.add(removableRows.get(i).studentId());
                        }
                    }
                }

                // Nếu có ít nhất một sinh viên đã bị xóa khỏi activity_participant thì xóa tiếp bản ghi kết quả điểm tương ứng
                if (!deletedStudentIds.isEmpty()) {
                    try (PreparedStatement deleteResult = conn.prepareStatement(deleteResultSql)) {
                        // Đưa từng studentId đã xóa vào batch dọn bảng student_activity_result
                        for (Integer studentId : deletedStudentIds) {
                            deleteResult.setInt(1, activityId);
                            deleteResult.setInt(2, studentId);
                            deleteResult.addBatch();
                        }
                        deleteResult.executeBatch();
                    }
                }

                // Xác nhận transaction xóa hàng loạt
                conn.commit();
                return deletedStudentIds;
            } catch (SQLException e) {
                // Rollback an toàn khi có lỗi xảy ra trong transaction xóa
                rollbackQuietly(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi xóa người tham gia khỏi hoạt động
            throw new IllegalStateException("Failed to remove participants from activity " + activityId, e);
        }
    }

    /**
     * Cập nhật trạng thái điểm danh cho các người tham gia ở buổi 1 và trả về số lượng bản ghi thực sự thay đổi.
     */
    @Override
    public int updateStatuses(int activityId, Map<Integer, ParticipantStatus> statusByParticipantId, String changedBy) {
        return updateStatuses(activityId, 1, statusByParticipantId, changedBy);
    }

    /**
     * Cập nhật trạng thái điểm danh cho các người tham gia ở buổi {@code sessionNumber}
     * và trả về số lượng bản ghi thực sự thay đổi.
     */
    @Override
    public int updateStatuses(int activityId, int sessionNumber, Map<Integer, ParticipantStatus> statusByParticipantId,
            String changedBy) {
        return updateStatusesAndReturnChanges(activityId, sessionNumber, statusByParticipantId, changedBy).size();
    }

    /**
     * Cập nhật trạng thái điểm danh theo buổi bằng batch read/write trong một transaction:
     * <ul>
     *     <li>Tải thông tin và các buổi điểm danh của đúng nhóm {@code participantIds} truyền vào (không N+1).</li>
     *     <li>Tính trạng thái thay đổi, trạng thái tổng và điểm mới bằng {@link ActivityScoreCalculator}.</li>
     *     <li>Chỉ ghi batch và đồng bộ bảng {@code student_activity_result} cho đúng các bản ghi thực sự đổi.</li>
     * </ul>
     */
    @Override
    public List<AttendanceStatusChange> updateStatusesAndReturnChanges(
            int activityId, int sessionNumber, Map<Integer, ParticipantStatus> statusByParticipantId,
            String changedBy) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        // Nếu map cập nhật rỗng hoặc null thì không cần thực thi truy vấn nào
        if (statusByParticipantId == null || statusByParticipantId.isEmpty()) {
            return List.of();
        }
        // Chuẩn hóa số buổi >= 1 và giải mã ID người thao tác (fallback sang DEFAULT_ACTOR_ID nếu không hợp lệ)
        int safeSessionNumber = Math.max(1, sessionNumber);
        int actorId = parseActorId(changedBy);

        try (Connection conn = DBContext.getConnection()) {
            // Mở transaction để đồng bộ đồng thời bảng điểm danh buổi, lịch sử, trạng thái tổng và bảng điểm kết quả
            conn.setAutoCommit(false);
            try {
                // Tải metadata cấu hình tính điểm của hoạt động (không cần khóa hàng hoạt động)
                ActivityMeta meta = loadActivityMeta(conn, activityId, false);
                // Nếu hoạt động không tồn tại thì commit và trả về rỗng
                if (meta == null) {
                    conn.commit();
                    return List.of();
                }

                // Tải thông tin hiện tại của đúng tập participantIds cần cập nhật
                List<Integer> targetIds = new ArrayList<>(statusByParticipantId.keySet());
                Map<Integer, ParticipantSyncRow> rowsById = loadParticipantSyncRowsByIds(conn, activityId, targetIds);
                // Nếu không có participant nào hợp lệ thuộc hoạt động thì kết thúc sớm
                if (rowsById.isEmpty()) {
                    conn.commit();
                    return List.of();
                }

                // Tải toàn bộ trạng thái điểm danh các buổi hiện có của đúng nhóm participant này (tránh N+1)
                Map<Integer, Map<Integer, ParticipantStatus>> sessionsByParticipant =
                        loadSessionStatusesForParticipants(conn, rowsById.keySet(), meta.totalSessions());

                List<AttendanceStatusChange> changes = new ArrayList<>();
                List<SessionUpsertRow> sessionUpserts = new ArrayList<>();
                Map<Integer, ParticipantStatus> overallStatusUpdates = new LinkedHashMap<>();
                List<ResultUpsertRow> resultUpserts = new ArrayList<>();

                // Duyệt từng yêu cầu cập nhật trạng thái điểm danh để lọc ra các bản ghi thực sự thay đổi và tính lại điểm
                for (Map.Entry<Integer, ParticipantStatus> entry : statusByParticipantId.entrySet()) {
                    int participantId = entry.getKey();
                    ParticipantStatus newStatus = entry.getValue();
                    ParticipantSyncRow row = rowsById.get(participantId);
                    // Bỏ qua nếu participant không tồn tại trong hoạt động hoặc trạng thái mới bị null
                    if (row == null || newStatus == null) {
                        continue;
                    }

                    // Xác định trạng thái cũ tại buổi đang điểm danh (có xét fallback dữ liệu legacy cho buổi 1)
                    Map<Integer, ParticipantStatus> existingSessions = sessionsByParticipant.getOrDefault(
                            participantId, Collections.emptyMap());
                    ParticipantStatus oldStatus = ActivityScoreCalculator.resolveSessionStatus(
                            existingSessions, safeSessionNumber, row.currentOverallStatus());
                    // Nếu trạng thái mới trùng với trạng thái cũ thì bỏ qua, không ghi đè hay tạo lịch sử thừa
                    if (oldStatus == newStatus) {
                        continue;
                    }

                    // Sao chép map buổi hiện tại để mô phỏng trạng thái sau cập nhật và tính trạng thái tổng + điểm mới trong bộ nhớ
                    Map<Integer, ParticipantStatus> updatedSessions = new HashMap<>(existingSessions);
                    // Bảo toàn dữ liệu điểm danh cũ ở buổi 1 nếu chưa từng có dòng nào trong bảng theo buổi và đang sửa buổi khác 1
                    if (existingSessions.isEmpty()
                            && safeSessionNumber != 1
                            && row.currentOverallStatus() != ParticipantStatus.NOT_YET) {
                        updatedSessions.put(1, row.currentOverallStatus());
                        sessionUpserts.add(new SessionUpsertRow(participantId, 1, row.currentOverallStatus(), null));
                    }
                    updatedSessions.put(safeSessionNumber, newStatus);
                    sessionUpserts.add(new SessionUpsertRow(participantId, safeSessionNumber, newStatus, actorId));

                    // Tính lại toàn bộ bộ điểm và trạng thái tổng sau khi cập nhật buổi hiện tại
                    ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                            updatedSessions,
                            meta.totalSessions(),
                            row.currentOverallStatus(),
                            row.evalScore(),
                            row.summaryScore(),
                            meta.summaryRequired(),
                            meta.orgLevelCoefficient(),
                            row.registrationStatus(),
                            row.participationRole()
                    );

                    // Chỉ bổ sung lệnh UPDATE trạng thái tổng trên bảng activity_participant nếu trạng thái tổng có thay đổi
                    if (breakdown.overallStatus() != row.currentOverallStatus()) {
                        overallStatusUpdates.put(participantId, breakdown.overallStatus());
                    }
                    // Chuẩn bị bản ghi đồng bộ điểm kết quả và bản ghi thay đổi để ghi lịch sử + trả về cho tầng Service
                    resultUpserts.add(new ResultUpsertRow(
                            row.studentId(), activityId, meta.semesterId(), row.participationRole(), breakdown));
                    changes.add(new AttendanceStatusChange(
                            participantId, row.studentAccountId(), oldStatus.name(), newStatus.name()));
                }

                // Nếu không có người tham gia nào thực sự đổi trạng thái thì kết thúc giao dịch mà không ghi DB
                if (changes.isEmpty()) {
                    conn.commit();
                    return List.of();
                }

                // Thực thi tuần tự 4 thao tác ghi batch: điểm danh buổi, lịch sử điểm danh, trạng thái tổng và bảng kết quả điểm
                batchUpsertSessionAttendances(conn, sessionUpserts);
                batchInsertAttendanceHistory(conn, safeSessionNumber, changes, actorId);
                batchUpdateParticipantOverallStatus(conn, activityId, overallStatusUpdates);
                batchUpsertActivityResults(conn, resultUpserts);

                // Xác nhận transaction sau khi toàn bộ batch thành công
                conn.commit();
                return changes;
            } catch (SQLException e) {
                // Rollback an toàn nếu bất kỳ lệnh đọc/ghi nào trong transaction gặp lỗi
                rollbackQuietly(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi cập nhật trạng thái điểm danh
            throw new IllegalStateException("Failed to update attendance for activity " + activityId, e);
        }
    }

    /**
     * Cập nhật điểm đánh giá tham gia và trả về số lượng sinh viên thực tế được chấm.
     */
    @Override
    public int updateParticipationGrades(int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, String gradedBy, Integer gradedById) {
        return updateParticipationGradesAndReturnStudentIds(
                activityId, participantIds, evalScore, feedback, gradedBy, gradedById).size();
    }

    /**
     * Chấm điểm đánh giá tham gia theo batch trong một transaction:
     * chỉ tải và cập nhật đúng nhóm {@code participantIds} đủ điều kiện có mặt (không N+1),
     * ghi lịch sử vào {@code activity_grade_history}, tính lại và đồng bộ bảng {@code student_activity_result}
     * bằng {@link ActivityScoreCalculator} cho đúng nhóm này, rồi trả về danh sách {@code student_id} đã chấm.
     */
    @Override
    public List<Integer> updateParticipationGradesAndReturnStudentIds(
            int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, String gradedBy, Integer gradedById) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        // Nếu danh sách participant cần chấm rỗng hoặc null thì thoát sớm
        if (participantIds == null || participantIds.isEmpty()) {
            return List.of();
        }

        // Câu lệnh cập nhật điểm đánh giá, nhận xét và thông tin người/thời điểm chấm điểm
        String updateGradeSql = """
                UPDATE dbo.activity_participant
                SET participation_eval_score = ?,
                    participation_feedback = ?,
                    participation_graded_by = ?,
                    participation_graded_by_id = ?,
                    participation_graded_at = SYSUTCDATETIME()
                WHERE id = ? AND activity_id = ?
                """;
        // Câu lệnh lưu vết lịch sử thay đổi điểm đánh giá (loại PARTICIPATION_EVAL) vào bảng activity_grade_history
        String historySql = """
                INSERT INTO dbo.activity_grade_history
                    (activity_participant_id, grade_type, old_score, new_score, feedback, created_by, created_by_id)
                VALUES (?, 'PARTICIPATION_EVAL', ?, ?, ?, ?, ?)
                """;

        // Chuẩn hóa điểm đánh giá về 2 chữ số thập phân và chuẩn hóa chuỗi nhận xét (rỗng -> null)
        BigDecimal normalizedScore = evalScore == null ? null : evalScore.setScale(2, RoundingMode.HALF_UP);
        String trimmedFeedback = (feedback == null || feedback.trim().isEmpty()) ? null : feedback.trim();

        try (Connection conn = DBContext.getConnection()) {
            // Mở transaction để cập nhật điểm, ghi lịch sử chấm điểm và đồng bộ student_activity_result nguyên tử
            conn.setAutoCommit(false);
            try {
                // Tải metadata hoạt động để biết tổng số buổi, yêu cầu bài thu hoạch và hệ số cấp tổ chức
                ActivityMeta meta = loadActivityMeta(conn, activityId, false);
                // Nếu hoạt động không tồn tại thì commit và trả về danh sách rỗng
                if (meta == null) {
                    conn.commit();
                    return List.of();
                }

                // Tải thông tin hiện tại của nhóm participantIds trong hoạt động
                Map<Integer, ParticipantSyncRow> rowsById = loadParticipantSyncRowsByIds(conn, activityId, participantIds);
                // Nếu không có participant nào khớp thì kết thúc sớm
                if (rowsById.isEmpty()) {
                    conn.commit();
                    return List.of();
                }

                // Tải bản đồ điểm danh theo buổi của nhóm participant để xác định chính xác trạng thái tổng và tính điểm mới
                Map<Integer, Map<Integer, ParticipantStatus>> sessionsByParticipant =
                        loadSessionStatusesForParticipants(conn, rowsById.keySet(), meta.totalSessions());

                List<ParticipantSyncRow> gradedRows = new ArrayList<>();
                List<ResultUpsertRow> resultUpserts = new ArrayList<>();
                Map<Integer, ParticipantStatus> overallStatusUpdates = new LinkedHashMap<>();

                // Dùng seenIds để loại trùng participantId nếu danh sách đầu vào có phần tử lặp
                Set<Integer> seenIds = new HashSet<>();
                // Duyệt từng participantId đầu vào để lọc người đủ điều kiện có mặt và tính lại bảng điểm
                for (Integer pid : participantIds) {
                    // Bỏ qua ID không hợp lệ hoặc đã được xử lý trước đó
                    if (pid == null || pid <= 0 || !seenIds.add(pid)) {
                        continue;
                    }
                    ParticipantSyncRow row = rowsById.get(pid);
                    // Bỏ qua nếu participant không thuộc hoạt động hiện tại
                    if (row == null) {
                        continue;
                    }

                    // Tính lại điểm tổng hợp với điểm đánh giá mới normalizedScore
                    Map<Integer, ParticipantStatus> sessionMap = sessionsByParticipant.getOrDefault(
                            pid, Collections.emptyMap());
                    ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                            sessionMap,
                            meta.totalSessions(),
                            row.currentOverallStatus(),
                            normalizedScore,
                            row.summaryScore(),
                            meta.summaryRequired(),
                            meta.orgLevelCoefficient(),
                            row.registrationStatus(),
                            row.participationRole()
                    );

                    // Quy tắc nghiệp vụ: chỉ cho phép chấm điểm đối với sinh viên có trạng thái tổng là đã có mặt
                    if (!ActivityScoreCalculator.isPresentStatus(breakdown.overallStatus())) {
                        continue;
                    }

                    gradedRows.add(row);
                    // Đồng bộ lại cột attendance_status tổng nếu kết quả suy luận khác giá trị đang lưu
                    if (breakdown.overallStatus() != row.currentOverallStatus()) {
                        overallStatusUpdates.put(pid, breakdown.overallStatus());
                    }
                    resultUpserts.add(new ResultUpsertRow(
                            row.studentId(), activityId, meta.semesterId(), row.participationRole(), breakdown));
                }

                // Nếu không có sinh viên nào đủ điều kiện được chấm điểm thì commit và thoát sớm
                if (gradedRows.isEmpty()) {
                    conn.commit();
                    return List.of();
                }

                // Thực thi batch cập nhật điểm đánh giá trên activity_participant và chèn lịch sử chấm điểm
                try (PreparedStatement update = conn.prepareStatement(updateGradeSql);
                     PreparedStatement history = conn.prepareStatement(historySql)) {
                    // Chuẩn bị song song batch cập nhật điểm và batch lưu lịch sử cho từng sinh viên hợp lệ
                    for (ParticipantSyncRow row : gradedRows) {
                        setNullableBigDecimal(update, 1, normalizedScore);
                        setNullableString(update, 2, trimmedFeedback);
                        update.setString(3, gradedBy);
                        setNullableInt(update, 4, gradedById);
                        update.setInt(5, row.participantId());
                        update.setInt(6, activityId);
                        update.addBatch();

                        history.setInt(1, row.participantId());
                        setNullableBigDecimal(history, 2, row.evalScore());
                        setNullableBigDecimal(history, 3, normalizedScore);
                        setNullableString(history, 4, trimmedFeedback);
                        history.setString(5, gradedBy);
                        setNullableInt(history, 6, gradedById);
                        history.addBatch();
                    }
                    update.executeBatch();
                    history.executeBatch();
                }

                // Đồng bộ trạng thái tổng (nếu lệch) và ghi batch kết quả điểm mới vào student_activity_result
                batchUpdateParticipantOverallStatus(conn, activityId, overallStatusUpdates);
                batchUpsertActivityResults(conn, resultUpserts);

                // Xác nhận hoàn tất transaction chấm điểm
                conn.commit();
                List<Integer> gradedStudentIds = new ArrayList<>(gradedRows.size());
                // Thu thập danh sách studentId đã được chấm điểm thành công để trả về cho tầng gọi
                for (ParticipantSyncRow row : gradedRows) {
                    gradedStudentIds.add(row.studentId());
                }
                return gradedStudentIds;
            } catch (SQLException e) {
                // Rollback an toàn khi có lỗi SQL trong quá trình chấm điểm
                rollbackQuietly(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi cập nhật điểm đánh giá tham gia
            throw new IllegalStateException("Failed to update participation grades for activity " + activityId, e);
        }
    }

    /**
     * Lấy danh sách chi tiết trạng thái điểm danh cho từng buổi (từ buổi 1 đến {@code totalSessions})
     * của một người tham gia bằng truy vấn gọn, sau đó giải mã thông tin người cập nhật theo batch trong Java.
     */
    @Override
    public List<ParticipantSessionAttendanceDTO> findSessionAttendances(int activityId, int participantId, int totalSessions) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        // Đảm bảo tổng số buổi luôn tối thiểu là 1
        int safeTotalSessions = Math.max(1, totalSessions);

        // Truy vấn trạng thái điểm danh chi tiết của các buổi hợp lệ (<= safeTotalSessions)
        String sessionSql = """
                SELECT s.session_number, s.attendance_status, s.updated_by, s.updated_at
                FROM dbo.activity_participant_session_attendance s
                JOIN dbo.activity_participant ap ON ap.id = s.activity_participant_id
                WHERE ap.activity_id = ? AND s.activity_participant_id = ?
                  AND s.session_number <= ?
                ORDER BY s.session_number ASC
                """;
        // Truy vấn lịch sử điểm danh sắp xếp mới nhất trước để lấy người/thời điểm cập nhật gần nhất theo từng buổi
        String historySql = """
                SELECT COALESCE(h.session_number, 1) AS session_number, h.created_by, h.created_at
                FROM dbo.activity_attendance_history h
                JOIN dbo.activity_participant ap ON ap.id = h.activity_participant_id
                WHERE ap.activity_id = ? AND h.activity_participant_id = ?
                ORDER BY h.created_at DESC, h.id DESC
                """;
        // Truy vấn trạng thái tổng cũ dùng cho trường hợp fallback buổi 1 khi bảng theo buổi chưa có dữ liệu
        String legacyStatusSql = """
                SELECT ap.attendance_status
                FROM dbo.activity_participant ap
                WHERE ap.activity_id = ? AND ap.id = ?
                """;

        Map<Integer, ParticipantSessionAttendanceDTO> bySession = new HashMap<>();

        try (Connection conn = DBContext.getConnection()) {
            // 1. Lấy lịch sử mới nhất theo từng buổi (và lịch sử mới nhất tổng cho fallback)
            Map<Integer, HistoryEntry> latestHistBySession = new HashMap<>();
            HistoryEntry latestOverallHist = null;
            try (PreparedStatement ps = conn.prepareStatement(historySql)) {
                ps.setInt(1, activityId);
                ps.setInt(2, participantId);
                try (ResultSet rs = ps.executeQuery()) {
                    // Duyệt lịch sử theo thứ tự giảm dần thời gian; bản ghi đầu tiên của mỗi buổi là lần cập nhật mới nhất
                    while (rs.next()) {
                        int sessNo = Math.max(1, rs.getInt("session_number"));
                        Integer actorId = parseNullableActorId(rs.getObject("created_by"));
                        Timestamp ts = rs.getTimestamp("created_at");
                        HistoryEntry entry = new HistoryEntry(sessNo, actorId, ts == null ? null : ts.toLocalDateTime());
                        // Ghi nhận bản ghi đầu tiên làm lịch sử tổng mới nhất (phục vụ fallback dữ liệu cũ)
                        if (latestOverallHist == null) {
                            latestOverallHist = entry;
                        }
                        latestHistBySession.putIfAbsent(sessNo, entry);
                    }
                }
            }

            // 2. Lấy bản ghi điểm danh theo buổi
            Set<Integer> actorIds = new LinkedHashSet<>();
            try (PreparedStatement ps = conn.prepareStatement(sessionSql)) {
                ps.setInt(1, activityId);
                ps.setInt(2, participantId);
                ps.setInt(3, safeTotalSessions);
                try (ResultSet rs = ps.executeQuery()) {
                    // Duyệt từng buổi đã có bản ghi điểm danh và hợp nhất thông tin người/thời gian cập nhật với lịch sử
                    while (rs.next()) {
                        int sessNo = rs.getInt("session_number");
                        ParticipantStatus pst = parseStatusOrNotYet(rs.getString("attendance_status"));
                        Integer updatedBy = parseNullableActorId(rs.getObject("updated_by"));
                        Timestamp updatedTs = rs.getTimestamp("updated_at");
                        LocalDateTime updatedAt = updatedTs == null ? null : updatedTs.toLocalDateTime();

                        HistoryEntry hist = latestHistBySession.get(sessNo);
                        // Ưu tiên updated_by trên bảng session; nếu null thì lấy actorId từ lịch sử gần nhất của buổi đó
                        Integer effectiveActorId = updatedBy != null ? updatedBy : (hist != null ? hist.actorId() : null);
                        // Ưu tiên thời điểm ghi lịch sử gần nhất (chính xác theo lần đổi trạng thái); nếu không có thì dùng updated_at
                        LocalDateTime effectiveUpdatedAt = (hist != null && hist.createdAt() != null)
                                ? hist.createdAt()
                                : updatedAt;

                        ParticipantSessionAttendanceDTO dto = new ParticipantSessionAttendanceDTO();
                        dto.setSessionNumber(sessNo);
                        dto.setAttendanceStatus(pst);
                        dto.setSessionScore(ActivityScoreCalculator.computeSessionScore(pst));
                        dto.setUpdatedById(effectiveActorId);
                        dto.setUpdatedAt(effectiveUpdatedAt);
                        // Gom actorId khác null để giải mã thông tin cán bộ/đối tác theo batch ở bước 4
                        if (effectiveActorId != null) {
                            actorIds.add(effectiveActorId);
                        }
                        bySession.put(sessNo, dto);
                    }
                }
            }

            // 3. Fallback dữ liệu cũ cho buổi 1 nếu chưa có dòng nào trong bảng theo buổi
            if (bySession.isEmpty()) {
                try (PreparedStatement fb = conn.prepareStatement(legacyStatusSql)) {
                    fb.setInt(1, activityId);
                    fb.setInt(2, participantId);
                    try (ResultSet rs = fb.executeQuery()) {
                        // Kiểm tra bản ghi người tham gia tồn tại trong bảng activity_participant
                        if (rs.next()) {
                            ParticipantStatus pst = parseStatusOrNotYet(rs.getString("attendance_status"));
                            // Chỉ dựng bản ghi fallback buổi 1 nếu trạng thái tổng cũ đã từng được điểm danh (khác NOT_YET)
                            if (pst != ParticipantStatus.NOT_YET) {
                                ParticipantSessionAttendanceDTO dto = new ParticipantSessionAttendanceDTO();
                                dto.setSessionNumber(1);
                                dto.setAttendanceStatus(pst);
                                dto.setSessionScore(ActivityScoreCalculator.computeSessionScore(pst));
                                // Nếu có lịch sử điểm danh tổng gần nhất thì gắn người/thời gian cập nhật tương ứng
                                if (latestOverallHist != null) {
                                    dto.setUpdatedById(latestOverallHist.actorId());
                                    dto.setUpdatedAt(latestOverallHist.createdAt());
                                    // Đưa actorId từ lịch sử vào tập cần giải mã
                                    if (latestOverallHist.actorId() != null) {
                                        actorIds.add(latestOverallHist.actorId());
                                    }
                                }
                                bySession.put(1, dto);
                            }
                        }
                    }
                }
            }

            // 4. Giải mã thông tin người thao tác 1 lần cho toàn bộ tập actorIds duy nhất
            Map<Integer, ActorInfo> actorInfoMap = loadActorInfoMap(conn, activityId, actorIds);
            // Điền mã, tên và đơn vị của người cập nhật vào từng DTO buổi đã tải
            for (ParticipantSessionAttendanceDTO dto : bySession.values()) {
                // Chỉ tra cứu khi buổi có ghi nhận ID người cập nhật
                if (dto.getUpdatedById() != null) {
                    ActorInfo info = actorInfoMap.get(dto.getUpdatedById());
                    // Nếu giải mã thành công thì gán đầy đủ thông tin hiển thị người thao tác
                    if (info != null) {
                        dto.setUpdatedByCode(info.code());
                        dto.setUpdatedByName(info.name());
                        dto.setUpdatedByUnit(info.unit());
                        dto.setUpdatedBy(info.name());
                    }
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tải danh sách điểm danh chi tiết theo buổi
            throw new IllegalStateException("Failed to load session attendances for participant " + participantId, e);
        }

        List<ParticipantSessionAttendanceDTO> result = new ArrayList<>(safeTotalSessions);
        // Lấp đầy đủ danh sách từ buổi 1 đến safeTotalSessions (buổi chưa có bản ghi sẽ mang trạng thái NOT_YET và 0 điểm)
        for (int i = 1; i <= safeTotalSessions; i++) {
            ParticipantSessionAttendanceDTO existing = bySession.get(i);
            // Nếu buổi i đã có dữ liệu trong DB (hoặc fallback) thì dùng trực tiếp
            if (existing != null) {
                result.add(existing);
            } else {
                // Ngược lại khởi tạo DTO mặc định cho buổi chưa điểm danh
                ParticipantSessionAttendanceDTO emptyDto = new ParticipantSessionAttendanceDTO();
                emptyDto.setSessionNumber(i);
                emptyDto.setAttendanceStatus(ParticipantStatus.NOT_YET);
                emptyDto.setSessionScore(ActivityScoreCalculator.ZERO_SCORE);
                result.add(emptyDto);
            }
        }
        return result;
    }

    /**
     * Truy vấn toàn bộ lịch sử thay đổi trạng thái điểm danh của một người tham gia trong hoạt động
     * (sắp xếp theo thời gian mới nhất trước) và giải mã thông tin người thực hiện theo batch.
     */
    @Override
    public List<AttendanceHistoryDTO> findAttendanceHistory(int activityId, int participantId) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        // Truy vấn các bản ghi lịch sử điểm danh của participant thuộc hoạt động, sắp xếp mới nhất trước
        String sql = """
                SELECT h.id,
                       h.activity_participant_id,
                       COALESCE(h.session_number, 1) AS session_number,
                       h.old_status,
                       h.new_status,
                       h.created_by,
                       h.created_at
                FROM dbo.activity_attendance_history h
                JOIN dbo.activity_participant ap ON ap.id = h.activity_participant_id
                WHERE ap.activity_id = ? AND h.activity_participant_id = ?
                ORDER BY h.created_at DESC, h.id DESC
                """;

        List<AttendanceHistoryDTO> list = new ArrayList<>();
        List<String> rawCreatedByList = new ArrayList<>();
        Set<Integer> actorIds = new LinkedHashSet<>();

        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, activityId);
                ps.setInt(2, participantId);
                try (ResultSet rs = ps.executeQuery()) {
                    // Duyệt từng dòng lịch sử để tạo DTO và thu thập tập actorId duy nhất phục vụ giải mã batch
                    while (rs.next()) {
                        AttendanceHistoryDTO dto = new AttendanceHistoryDTO();
                        dto.setId(rs.getLong("id"));
                        dto.setParticipantId(rs.getInt("activity_participant_id"));
                        dto.setSessionNumber(rs.getInt("session_number"));
                        dto.setOldStatus(parseStatusOrNotYet(rs.getString("old_status")));
                        dto.setNewStatus(parseStatusOrNotYet(rs.getString("new_status")));
                        Object rawCreatedByObj = rs.getObject("created_by");
                        Integer actorId = parseNullableActorId(rawCreatedByObj);
                        dto.setCreatedById(actorId);
                        // Nếu created_by phân tích được sang ID dương thì đưa vào danh sách cần tra cứu
                        if (actorId != null) {
                            actorIds.add(actorId);
                        }
                        Timestamp ts = rs.getTimestamp("created_at");
                        dto.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                        list.add(dto);
                        // Lưu lại chuỗi gốc để làm giá trị fallback nếu không giải mã được tên người thao tác
                        rawCreatedByList.add(rawCreatedByObj == null ? null : String.valueOf(rawCreatedByObj));
                    }
                }
            }

            // Giải mã thông tin người thao tác một lần theo batch rồi gắn ngược lại cho từng bản ghi lịch sử
            Map<Integer, ActorInfo> actorInfoMap = loadActorInfoMap(conn, activityId, actorIds);
            // Duyệt từng DTO lịch sử để điền thông tin người thực hiện đã giải mã hoặc fallback chuỗi gốc
            for (int i = 0; i < list.size(); i++) {
                AttendanceHistoryDTO dto = list.get(i);
                ActorInfo info = dto.getCreatedById() == null ? null : actorInfoMap.get(dto.getCreatedById());
                // Nếu tìm thấy thông tin người thao tác thì điền mã/tên/đơn vị (ưu tiên tên, fallback chuỗi gốc)
                if (info != null) {
                    dto.setCreatedByCode(info.code());
                    dto.setCreatedByName(info.name());
                    dto.setCreatedByUnit(info.unit());
                    dto.setCreatedBy(info.name() != null ? info.name() : rawCreatedByList.get(i));
                } else {
                    // Nếu không tra cứu được (ví dụ dữ liệu lịch sử cũ lưu thẳng chuỗi tên) thì giữ nguyên giá trị gốc
                    dto.setCreatedBy(rawCreatedByList.get(i));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tải lịch sử thay đổi trạng thái điểm danh
            throw new IllegalStateException("Failed to load attendance history for participant " + participantId, e);
        }
        return list;
    }

    /**
     * Lấy tập hợp ID của tất cả các hoạt động mà một sinh viên đã được ghi nhận trong bảng {@code activity_participant}.
     */
    @Override
    public Set<Integer> findActivityIdsByStudentId(int studentId) {
        // Nếu studentId không hợp lệ (<= 0) thì trả về tập rỗng ngay
        if (studentId <= 0) {
            return Set.of();
        }
        Set<Integer> ids = new LinkedHashSet<>();
        String sql = "SELECT activity_id FROM dbo.activity_participant WHERE student_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt kết quả và gom toàn bộ activity_id mà sinh viên đã tham gia
                while (rs.next()) {
                    ids.add(rs.getInt("activity_id"));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn danh sách hoạt động theo studentId
            throw new IllegalStateException("Failed to find participated activity ids for student " + studentId, e);
        }
        return ids;
    }

    /**
     * Đồng bộ lại toàn bộ dữ liệu điểm danh và bảng kết quả điểm ({@code student_activity_result})
     * của một hoạt động (dùng khi cấu hình hoạt động như số buổi, cấp tổ chức, học kỳ thay đổi).
     * Toàn bộ công thức tính điểm được thực hiện bằng {@link ActivityScoreCalculator} và ghi batch.
     */
    @Override
    public void syncActivityResults(int activityId) {
        ExtracurricularActivityDAOImpl.ensureAttendanceSessionSchema();
        RecruitmentSchema.ensure();

        // Câu lệnh xóa các bản ghi điểm danh của những buổi vượt quá tổng số buổi mới (khi giảm số buổi hoạt động)
        String deleteExtraSessionsSql = """
                DELETE apsa
                FROM dbo.activity_participant_session_attendance apsa
                JOIN dbo.activity_participant ap ON ap.id = apsa.activity_participant_id
                WHERE ap.activity_id = ? AND apsa.session_number > ?
                """;
        // Câu lệnh dọn các bản ghi điểm kết quả mồ côi không còn tồn tại sinh viên tương ứng trong activity_participant
        String deleteOrphanResultsSql = """
                DELETE sar
                FROM dbo.student_activity_result sar
                WHERE sar.activity_id = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_participant ap
                      WHERE ap.activity_id = sar.activity_id AND ap.student_id = sar.student_id
                  )
                """;

        try (Connection conn = DBContext.getConnection()) {
            // Mở transaction để toàn bộ quá trình dọn dữ liệu thừa, migrate buổi 1 và đồng bộ bảng điểm nhất quán
            conn.setAutoCommit(false);
            try {
                // Tải metadata cấu hình mới nhất của hoạt động
                ActivityMeta meta = loadActivityMeta(conn, activityId, false);
                // Nếu hoạt động không tồn tại thì kết thúc sớm
                if (meta == null) {
                    conn.commit();
                    return;
                }

                // 1. Dọn các buổi vượt quá tổng số buổi mới và các bản ghi kết quả dư thừa
                try (PreparedStatement ps = conn.prepareStatement(deleteExtraSessionsSql)) {
                    ps.setInt(1, activityId);
                    ps.setInt(2, meta.totalSessions());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(deleteOrphanResultsSql)) {
                    ps.setInt(1, activityId);
                    ps.executeUpdate();
                }

                // 2. Tải dữ liệu thô của người tham gia và các buổi điểm danh
                List<ParticipantSyncRow> participants = loadAllParticipantSyncRows(conn, activityId);
                // Nếu hoạt động không còn người tham gia nào thì commit sau khi đã dọn dữ liệu mồ côi
                if (participants.isEmpty()) {
                    conn.commit();
                    return;
                }
                Map<Integer, Map<Integer, ParticipantStatus>> sessionsByParticipant =
                        loadSessionStatusesForActivity(conn, activityId, meta.totalSessions());

                // 3. Tính toán toàn bộ kết quả bằng ActivityScoreCalculator
                List<SessionUpsertRow> legacySessionMigrations = new ArrayList<>();
                Map<Integer, ParticipantStatus> overallStatusUpdates = new LinkedHashMap<>();
                List<ResultUpsertRow> resultUpserts = new ArrayList<>(participants.size());

                // Duyệt toàn bộ người tham gia trong hoạt động để migrate dữ liệu cũ (nếu có) và tính lại điểm đồng bộ
                for (ParticipantSyncRow row : participants) {
                    Map<Integer, ParticipantStatus> sessionMap = sessionsByParticipant.getOrDefault(
                            row.participantId(), Collections.emptyMap());

                    // Chuyển dữ liệu điểm danh cũ sang buổi 1 nếu chưa có bản ghi theo buổi nào mà trạng thái tổng khác NOT_YET
                    if (sessionMap.isEmpty() && row.currentOverallStatus() != ParticipantStatus.NOT_YET) {
                        legacySessionMigrations.add(
                                new SessionUpsertRow(row.participantId(), 1, row.currentOverallStatus(), null));
                    }

                    // Tính lại trạng thái tổng, các điểm thành phần, hệ số và điểm cuối theo cấu hình hoạt động hiện tại
                    ActivityScoreCalculator.ScoreBreakdown breakdown = ActivityScoreCalculator.calculate(
                            sessionMap,
                            meta.totalSessions(),
                            row.currentOverallStatus(),
                            row.evalScore(),
                            row.summaryScore(),
                            meta.summaryRequired(),
                            meta.orgLevelCoefficient(),
                            row.registrationStatus(),
                            row.participationRole()
                    );

                    // Nếu trạng thái tổng suy luận khác với giá trị hiện lưu thì ghi nhận để cập nhật batch
                    if (breakdown.overallStatus() != row.currentOverallStatus()) {
                        overallStatusUpdates.put(row.participantId(), breakdown.overallStatus());
                    }
                    resultUpserts.add(new ResultUpsertRow(
                            row.studentId(), activityId, meta.semesterId(), row.participationRole(), breakdown));
                }

                // 4. Ghi batch các thay đổi: migrate buổi 1, cập nhật trạng thái tổng và UPSERT toàn bộ kết quả điểm
                batchUpsertSessionAttendances(conn, legacySessionMigrations);
                batchUpdateParticipantOverallStatus(conn, activityId, overallStatusUpdates);
                batchUpsertActivityResults(conn, resultUpserts);

                // Xác nhận hoàn tất đồng bộ hoạt động
                conn.commit();
            } catch (SQLException e) {
                // Rollback an toàn khi xảy ra lỗi SQL trong quá trình đồng bộ
                rollbackQuietly(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi đồng bộ lại kết quả điểm của hoạt động
            throw new IllegalStateException("Failed to sync activity results for activity " + activityId, e);
        }
    }

    // ==================== Helper tải dữ liệu gọn & ghi batch ====================

    /**
     * Tải siêu dữ liệu cấu hình tính điểm của một hoạt động (học kỳ, số buổi, yêu cầu bài thu hoạch,
     * hệ số cấp tổ chức, phòng ban phụ trách).
     *
     * @param conn        kết nối JDBC đang mở trong transaction
     * @param activityId  ID hoạt động cần truy vấn
     * @param withRowLock {@code true} nếu cần gắn gợi ý khóa {@code WITH (UPDLOCK, ROWLOCK)} để chống tranh chấp sức chứa
     * @return đối tượng {@link ActivityMeta}, hoặc {@code null} nếu không tìm thấy hoạt động
     */
    private ActivityMeta loadActivityMeta(Connection conn, int activityId, boolean withRowLock) throws SQLException {
        // Chỉ khóa bản ghi hoạt động ở mức hàng khi thao tác yêu cầu kiểm soát đồng thời (như kiểm tra sức chứa lúc thêm)
        String lockHint = withRowLock ? " WITH (UPDLOCK, ROWLOCK)" : "";
        String sql = "SELECT ea.id, ea.semester_id, "
                + "COALESCE(NULLIF(ea.attendance_session_count, 0), 1) AS total_sessions, "
                + "ea.is_summary_required, ol.coefficient AS org_level_coeff, ea.responsible_department_id "
                + "FROM dbo.extracurricular_activity ea" + lockHint + " "
                + "LEFT JOIN dbo.activity_organization_level ol ON ol.id = ea.organization_level_id "
                + "WHERE ea.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Nếu không có bản ghi nào khớp activityId thì trả về null
                if (!rs.next()) {
                    return null;
                }
                Integer semesterId = (Integer) rs.getObject("semester_id");
                Integer responsibleDeptId = (Integer) rs.getObject("responsible_department_id");
                return new ActivityMeta(
                        rs.getInt("id"),
                        semesterId,
                        Math.max(1, rs.getInt("total_sessions")),
                        rs.getBoolean("is_summary_required"),
                        rs.getBigDecimal("org_level_coeff"),
                        responsibleDeptId
                );
            }
        }
    }

    /**
     * Tải toàn bộ danh sách {@link ParticipantSyncRow} thuộc một hoạt động để phục vụ đồng bộ tổng thể.
     *
     * @param conn       kết nối JDBC đang mở
     * @param activityId ID hoạt động
     * @return danh sách bản ghi người tham gia cần đồng bộ
     */
    private List<ParticipantSyncRow> loadAllParticipantSyncRows(Connection conn, int activityId) throws SQLException {
        String sql = """
                SELECT ap.id, ap.student_id, st.account_id, ap.attendance_status,
                       ap.registration_status, ap.participation_role,
                       ap.participation_eval_score, ap.summary_score
                FROM dbo.activity_participant ap
                JOIN dbo.student st ON st.id = ap.student_id
                WHERE ap.activity_id = ?
                """;
        List<ParticipantSyncRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt toàn bộ bản ghi người tham gia của hoạt động và đọc sang ParticipantSyncRow
                while (rs.next()) {
                    rows.add(readParticipantSyncRow(rs));
                }
            }
        }
        return rows;
    }

    /**
     * Tải theo từng chunk (tránh vượt giới hạn tham số mệnh đề {@code IN}) các {@link ParticipantSyncRow}
     * ứng với tập {@code participantIds} thuộc một hoạt động.
     *
     * @param conn           kết nối JDBC đang mở
     * @param activityId     ID hoạt động
     * @param participantIds tập ID người tham gia cần tải
     * @return map ánh xạ {@code participantId -> ParticipantSyncRow}
     */
    private Map<Integer, ParticipantSyncRow> loadParticipantSyncRowsByIds(
            Connection conn, int activityId, Collection<Integer> participantIds) throws SQLException {
        Map<Integer, ParticipantSyncRow> result = new LinkedHashMap<>();
        // Chia nhỏ tập participantIds thành từng chunk an toàn trước khi dựng câu SQL có mệnh đề IN (...)
        for (List<Integer> chunk : chunkPositiveIds(participantIds)) {
            String sql = """
                    SELECT ap.id, ap.student_id, st.account_id, ap.attendance_status,
                           ap.registration_status, ap.participation_role,
                           ap.participation_eval_score, ap.summary_score
                    FROM dbo.activity_participant ap
                    JOIN dbo.student st ON st.id = ap.student_id
                    WHERE ap.activity_id = ? AND ap.id IN (%s)
                    """.formatted(placeholders(chunk.size()));
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, activityId);
                bindIntList(ps, 2, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Đọc từng người tham gia khớp trong chunk hiện tại và đưa vào map kết quả
                    while (rs.next()) {
                        ParticipantSyncRow row = readParticipantSyncRow(rs);
                        result.put(row.participantId(), row);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Ánh xạ dòng hiện tại của {@link ResultSet} thành bản ghi nội bộ {@link ParticipantSyncRow}.
     *
     * @param rs tập kết quả đang trỏ tới một dòng hợp lệ
     * @return đối tượng {@link ParticipantSyncRow}
     */
    private ParticipantSyncRow readParticipantSyncRow(ResultSet rs) throws SQLException {
        return new ParticipantSyncRow(
                rs.getInt("id"),
                rs.getInt("student_id"),
                (Integer) rs.getObject("account_id"),
                parseStatusOrNotYet(rs.getString("attendance_status")),
                rs.getString("registration_status"),
                rs.getString("participation_role"),
                rs.getBigDecimal("participation_eval_score"),
                rs.getBigDecimal("summary_score")
        );
    }

    /**
     * Tải toàn bộ trạng thái điểm danh theo buổi (từ buổi 1 đến {@code totalSessions})
     * của tất cả người tham gia trong một hoạt động.
     *
     * @param conn          kết nối JDBC đang mở
     * @param activityId    ID hoạt động
     * @param totalSessions tổng số buổi hợp lệ của hoạt động
     * @return map hai tầng {@code participantId -> (sessionNumber -> ParticipantStatus)}
     */
    private Map<Integer, Map<Integer, ParticipantStatus>> loadSessionStatusesForActivity(
            Connection conn, int activityId, int totalSessions) throws SQLException {
        String sql = """
                SELECT s.activity_participant_id, s.session_number, s.attendance_status
                FROM dbo.activity_participant_session_attendance s
                JOIN dbo.activity_participant ap ON ap.id = s.activity_participant_id
                WHERE ap.activity_id = ? AND s.session_number <= ?
                """;
        Map<Integer, Map<Integer, ParticipantStatus>> result = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, Math.max(1, totalSessions));
            try (ResultSet rs = ps.executeQuery()) {
                // Gom trạng thái điểm danh từng buổi vào map lồng nhau theo participantId
                while (rs.next()) {
                    int pid = rs.getInt("activity_participant_id");
                    int sessNo = rs.getInt("session_number");
                    ParticipantStatus status = parseStatusOrNotYet(rs.getString("attendance_status"));
                    result.computeIfAbsent(pid, k -> new HashMap<>()).put(sessNo, status);
                }
            }
        }
        return result;
    }

    /**
     * Tải trạng thái điểm danh các buổi hợp lệ (<= {@code totalSessions}) cho riêng tập {@code participantIds}
     * bằng truy vấn chunk theo mệnh đề {@code IN}.
     *
     * @param conn           kết nối JDBC đang mở
     * @param participantIds tập ID người tham gia cần tải điểm danh buổi
     * @param totalSessions  tổng số buổi hợp lệ của hoạt động
     * @return map hai tầng {@code participantId -> (sessionNumber -> ParticipantStatus)}
     */
    private Map<Integer, Map<Integer, ParticipantStatus>> loadSessionStatusesForParticipants(
            Connection conn, Collection<Integer> participantIds, int totalSessions) throws SQLException {
        Map<Integer, Map<Integer, ParticipantStatus>> result = new HashMap<>();
        int safeTotalSessions = Math.max(1, totalSessions);
        // Duyệt từng chunk participantId để truy vấn điểm danh theo buổi không vượt giới hạn tham số SQL
        for (List<Integer> chunk : chunkPositiveIds(participantIds)) {
            String sql = """
                    SELECT s.activity_participant_id, s.session_number, s.attendance_status
                    FROM dbo.activity_participant_session_attendance s
                    WHERE s.activity_participant_id IN (%s) AND s.session_number <= ?
                    """.formatted(placeholders(chunk.size()));
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindIntList(ps, 1, chunk);
                ps.setInt(chunk.size() + 1, safeTotalSessions);
                try (ResultSet rs = ps.executeQuery()) {
                    // Đưa từng cặp (sessionNumber, status) vào map tương ứng của từng participantId
                    while (rs.next()) {
                        int pid = rs.getInt("activity_participant_id");
                        int sessNo = rs.getInt("session_number");
                        ParticipantStatus status = parseStatusOrNotYet(rs.getString("attendance_status"));
                        result.computeIfAbsent(pid, k -> new HashMap<>()).put(sessNo, status);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Lấy tập ID các người tham gia trong hoạt động đã có ít nhất một bản ghi trong bảng lịch sử điểm danh.
     *
     * @param conn       kết nối JDBC đang mở
     * @param activityId ID hoạt động
     * @return tập {@code participantId} đã có lịch sử điểm danh
     */
    private Set<Integer> loadParticipantsWithHistoryForActivity(Connection conn, int activityId) throws SQLException {
        String sql = """
                SELECT DISTINCT h.activity_participant_id
                FROM dbo.activity_attendance_history h
                JOIN dbo.activity_participant ap ON ap.id = h.activity_participant_id
                WHERE ap.activity_id = ?
                """;
        Set<Integer> result = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Thu thập toàn bộ participantId có xuất hiện trong bảng lịch sử điểm danh
                while (rs.next()) {
                    result.add(rs.getInt(1));
                }
            }
        }
        return result;
    }

    /**
     * Kiểm tra theo batch xem những {@code participantIds} nào đã có ít nhất một buổi điểm danh
     * mang trạng thái khác {@code NOT_YET} (dùng để kiểm tra điều kiện được phép xóa).
     *
     * @param conn           kết nối JDBC đang mở
     * @param participantIds tập ID người tham gia cần kiểm tra
     * @return tập {@code participantId} đã từng được điểm danh ít nhất 1 buổi
     */
    private Set<Integer> loadParticipantIdsWithMarkedSession(
            Connection conn, Collection<Integer> participantIds) throws SQLException {
        Set<Integer> result = new HashSet<>();
        // Chia nhỏ tập ID theo chunk để truy vấn mệnh đề IN an toàn
        for (List<Integer> chunk : chunkPositiveIds(participantIds)) {
            String sql = """
                    SELECT DISTINCT activity_participant_id
                    FROM dbo.activity_participant_session_attendance
                    WHERE activity_participant_id IN (%s)
                      AND attendance_status <> 'NOT_YET'
                    """.formatted(placeholders(chunk.size()));
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindIntList(ps, 1, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Ghi nhận các participantId đã có ít nhất một buổi điểm danh khác NOT_YET
                    while (rs.next()) {
                        result.add(rs.getInt(1));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Kiểm tra theo batch xem những {@code participantIds} nào đã có bản ghi lịch sử điểm danh
     * trong bảng {@code activity_attendance_history} (dùng để chặn xóa người đã phát sinh lịch sử).
     *
     * @param conn           kết nối JDBC đang mở
     * @param participantIds tập ID người tham gia cần kiểm tra
     * @return tập {@code participantId} đã phát sinh lịch sử điểm danh
     */
    private Set<Integer> loadParticipantIdsWithHistory(
            Connection conn, Collection<Integer> participantIds) throws SQLException {
        Set<Integer> result = new HashSet<>();
        // Chia nhỏ tập ID theo chunk để truy vấn mệnh đề IN an toàn
        for (List<Integer> chunk : chunkPositiveIds(participantIds)) {
            String sql = """
                    SELECT DISTINCT activity_participant_id
                    FROM dbo.activity_attendance_history
                    WHERE activity_participant_id IN (%s)
                    """.formatted(placeholders(chunk.size()));
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindIntList(ps, 1, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Ghi nhận các participantId đã tồn tại trong lịch sử điểm danh
                    while (rs.next()) {
                        result.add(rs.getInt(1));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Lọc theo batch các {@code studentIds} đủ điều kiện thêm mới vào hoạt động:
     * sinh viên đang ở trạng thái {@code ACTIVE} và chưa tồn tại trong {@code activity_participant} của hoạt động này.
     *
     * @param conn       kết nối JDBC đang mở trong transaction
     * @param activityId ID hoạt động
     * @param studentIds danh sách ID sinh viên yêu cầu thêm
     * @return danh sách ID sinh viên hợp lệ thực sự có thể chèn mới
     */
    private List<Integer> findEligibleStudentIdsToInsert(
            Connection conn, int activityId, List<Integer> studentIds) throws SQLException {
        List<Integer> eligible = new ArrayList<>();
        // Chia nhỏ danh sách studentIds theo chunk để lọc bằng truy vấn IN + NOT EXISTS
        for (List<Integer> chunk : chunkPositiveIds(studentIds)) {
            String sql = """
                    SELECT s.id
                    FROM dbo.student s
                    WHERE s.id IN (%s)
                      AND s.status = 'ACTIVE'
                      AND NOT EXISTS (
                          SELECT 1 FROM dbo.activity_participant ap
                          WHERE ap.activity_id = ? AND ap.student_id = s.id
                      )
                    """.formatted(placeholders(chunk.size()));
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindIntList(ps, 1, chunk);
                ps.setInt(chunk.size() + 1, activityId);
                try (ResultSet rs = ps.executeQuery()) {
                    // Thu thập các studentId đủ điều kiện chèn mới
                    while (rs.next()) {
                        eligible.add(rs.getInt(1));
                    }
                }
            }
        }
        return eligible;
    }

    /**
     * Thực thi batch {@code MERGE} (UPSERT) trạng thái điểm danh theo buổi vào bảng
     * {@code activity_participant_session_attendance}.
     *
     * @param conn kết nối JDBC đang mở trong transaction
     * @param rows danh sách bản ghi điểm danh theo buổi cần ghi
     */
    private void batchUpsertSessionAttendances(Connection conn, List<SessionUpsertRow> rows) throws SQLException {
        // Bỏ qua nếu không có dòng điểm danh buổi nào cần ghi
        if (rows.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SESSION_SQL)) {
            // Gắn tham số cho cả nhánh UPDATE (khi đã có buổi) và INSERT (khi chưa có buổi) vào batch
            for (SessionUpsertRow row : rows) {
                ps.setInt(1, row.participantId());
                ps.setInt(2, row.sessionNumber());
                ps.setString(3, row.status().name());
                setNullableInt(ps, 4, row.actorId());
                ps.setString(5, row.status().name());
                setNullableInt(ps, 6, row.actorId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Ghi hàng loạt bản ghi lịch sử thay đổi trạng thái điểm danh vào bảng {@code activity_attendance_history}.
     *
     * @param conn          kết nối JDBC đang mở trong transaction
     * @param sessionNumber số thứ tự buổi điểm danh bị thay đổi
     * @param changes       danh sách thay đổi trạng thái (trạng thái cũ -> trạng thái mới)
     * @param actorId       ID người thực hiện thay đổi
     */
    private void batchInsertAttendanceHistory(
            Connection conn, int sessionNumber, List<AttendanceStatusChange> changes, int actorId) throws SQLException {
        // Bỏ qua nếu không phát sinh thay đổi trạng thái nào
        if (changes.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ATTENDANCE_HISTORY_SQL)) {
            // Đưa từng thay đổi trạng thái vào batch lưu lịch sử
            for (AttendanceStatusChange change : changes) {
                ps.setInt(1, change.participantId());
                ps.setInt(2, sessionNumber);
                ps.setString(3, change.oldStatus());
                ps.setString(4, change.newStatus());
                ps.setInt(5, actorId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Cập nhật hàng loạt cột trạng thái điểm danh tổng ({@code attendance_status})
     * trên bảng {@code activity_participant} cho những người tham gia có trạng thái tổng thay đổi.
     *
     * @param conn       kết nối JDBC đang mở trong transaction
     * @param activityId ID hoạt động
     * @param updates    map {@code participantId -> trạng thái tổng mới}
     */
    private void batchUpdateParticipantOverallStatus(
            Connection conn, int activityId, Map<Integer, ParticipantStatus> updates) throws SQLException {
        // Bỏ qua nếu không có người tham gia nào thay đổi trạng thái tổng
        if (updates.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_PARTICIPANT_STATUS_SQL)) {
            // Đưa từng cặp (participantId, overallStatus mới) vào batch UPDATE
            for (Map.Entry<Integer, ParticipantStatus> entry : updates.entrySet()) {
                ps.setString(1, entry.getValue().name());
                ps.setInt(2, entry.getKey());
                ps.setInt(3, activityId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Thực thi batch {@code MERGE} (UPSERT) toàn bộ bảng kết quả điểm ({@code student_activity_result})
     * từ các đối tượng {@link ResultUpsertRow} đã được tính sẵn bởi {@link ActivityScoreCalculator}.
     *
     * @param conn kết nối JDBC đang mở trong transaction
     * @param rows danh sách kết quả điểm cần đồng bộ
     */
    private void batchUpsertActivityResults(Connection conn, List<ResultUpsertRow> rows) throws SQLException {
        // Bỏ qua nếu không có bản ghi kết quả điểm nào cần ghi
        if (rows.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_ACTIVITY_RESULT_SQL)) {
            // Gắn đầy đủ tham số khóa USING, nhánh UPDATE và nhánh INSERT cho từng sinh viên vào batch
            for (ResultUpsertRow row : rows) {
                ActivityScoreCalculator.ScoreBreakdown b = row.breakdown();
                // Mặc định vai trò là PARTICIPANT nếu giá trị vai trò đang bị null
                String role = row.participationRole() == null ? "PARTICIPANT" : row.participationRole();

                // Khóa đối chiếu USING (student_id, activity_id)
                ps.setInt(1, row.studentId());
                ps.setInt(2, row.activityId());

                // Nhánh WHEN MATCHED -> UPDATE các cột điểm, hệ số và học kỳ
                setNullableInt(ps, 3, row.semesterId());
                ps.setString(4, role);
                ps.setBigDecimal(5, b.attendanceScore());
                ps.setBigDecimal(6, b.effectiveEvalScore());
                ps.setBigDecimal(7, b.participationScore());
                setNullableBigDecimal(ps, 8, b.summaryScore());
                ps.setBigDecimal(9, b.averageScore());
                ps.setBigDecimal(10, b.orgLevelCoefficient());
                ps.setBigDecimal(11, b.earlyRegCoefficient());
                ps.setBigDecimal(12, b.organizerCoefficient());
                ps.setBigDecimal(13, b.finalPoint());

                // Nhánh WHEN NOT MATCHED -> INSERT bản ghi kết quả mới
                ps.setInt(14, row.studentId());
                ps.setInt(15, row.activityId());
                setNullableInt(ps, 16, row.semesterId());
                ps.setString(17, role);
                ps.setBigDecimal(18, b.attendanceScore());
                ps.setBigDecimal(19, b.effectiveEvalScore());
                ps.setBigDecimal(20, b.participationScore());
                setNullableBigDecimal(ps, 21, b.summaryScore());
                ps.setBigDecimal(22, b.averageScore());
                ps.setBigDecimal(23, b.orgLevelCoefficient());
                ps.setBigDecimal(24, b.earlyRegCoefficient());
                ps.setBigDecimal(25, b.organizerCoefficient());
                ps.setBigDecimal(26, b.finalPoint());

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Giải mã thông tin người thao tác (mã, tên, phòng ban / đơn vị đối tác) theo batch
     * cho tập {@code actorIds} duy nhất, thay thế cho các khối {@code OUTER APPLY} lồng nhau bị lặp trong SQL.
     */
    private Map<Integer, ActorInfo> loadActorInfoMap(
            Connection conn, int activityId, Set<Integer> actorIds) throws SQLException {
        // Nếu không có actorId nào cần giải mã thì trả về map rỗng ngay
        if (actorIds == null || actorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Lấy phòng ban phụ trách & phòng ban phối hợp của hoạt động để ưu tiên hiển thị đơn vị cán bộ
        Integer responsibleDeptId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT responsible_department_id FROM dbo.extracurricular_activity WHERE id = ?")) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Nếu hoạt động tồn tại thì đọc ID phòng ban phụ trách chính
                if (rs.next()) {
                    responsibleDeptId = (Integer) rs.getObject(1);
                }
            }
        }
        Set<Integer> collabDeptIds = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT department_id FROM dbo.activity_department WHERE activity_id = ?")) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Gom toàn bộ ID các phòng ban phối hợp của hoạt động
                while (rs.next()) {
                    collabDeptIds.add(rs.getInt(1));
                }
            }
        }

        // 2. Tải account, staff (theo account_id), partner_staff (theo account_id)
        Map<Integer, String> usernameByAccountId = new HashMap<>();
        Map<Integer, StaffBasic> staffByAccountId = new HashMap<>();
        Map<Integer, PartnerBasic> partnerByAccountId = new HashMap<>();

        // Chia tập actorIds theo chunk và tải song song thông tin từ 3 bảng account, staff, partner_staff
        for (List<Integer> chunk : chunkPositiveIds(actorIds)) {
            String ph = placeholders(chunk.size());
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, username FROM dbo.account WHERE id IN (" + ph + ")")) {
                bindIntList(ps, 1, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Lưu username tương ứng với từng account_id làm tên fallback cuối cùng
                    while (rs.next()) {
                        usernameByAccountId.put(rs.getInt("id"), rs.getString("username"));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, account_id, code, name FROM dbo.staff WHERE account_id IN (" + ph + ")")) {
                bindIntList(ps, 1, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Ánh xạ account_id -> cán bộ trường (giữ bản ghi đầu tiên nếu có nhiều dòng)
                    while (rs.next()) {
                        int accId = rs.getInt("account_id");
                        staffByAccountId.putIfAbsent(accId, new StaffBasic(
                                rs.getInt("id"), accId, rs.getString("code"), rs.getString("name")));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ps.account_id, ps.code, ps.name, pc.name AS company_name "
                            + "FROM dbo.partner_staff ps "
                            + "LEFT JOIN dbo.partner_company pc ON pc.id = ps.company_id "
                            + "WHERE ps.account_id IN (" + ph + ")")) {
                bindIntList(ps, 1, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Ánh xạ account_id -> nhân sự doanh nghiệp đối tác kèm tên công ty
                    while (rs.next()) {
                        int accId = rs.getInt("account_id");
                        partnerByAccountId.putIfAbsent(accId, new PartnerBasic(
                                accId, rs.getString("code"), rs.getString("name"), rs.getString("company_name")));
                    }
                }
            }
        }

        // 3. Fallback cho các ID cũ trỏ trực tiếp sang staff.id thay vì account.id
        List<Integer> unresolvedIds = new ArrayList<>();
        // Lọc ra các actorId chưa tìm thấy ở cả staff(account_id) lẫn partner_staff(account_id)
        for (Integer id : actorIds) {
            if (!staffByAccountId.containsKey(id) && !partnerByAccountId.containsKey(id)) {
                unresolvedIds.add(id);
            }
        }
        Map<Integer, StaffBasic> staffByStaffId = new HashMap<>();
        // Tra cứu fallback theo staff.id cho các ID chưa giải mã được
        for (List<Integer> chunk : chunkPositiveIds(unresolvedIds)) {
            String ph = placeholders(chunk.size());
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, account_id, code, name FROM dbo.staff WHERE id IN (" + ph + ")")) {
                bindIntList(ps, 1, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Lưu ánh xạ trực tiếp staff.id -> StaffBasic
                    while (rs.next()) {
                        int staffId = rs.getInt("id");
                        staffByStaffId.put(staffId, new StaffBasic(
                                staffId, (Integer) rs.getObject("account_id"),
                                rs.getString("code"), rs.getString("name")));
                    }
                }
            }
        }

        // 4. Tải phòng ban cho tất cả staffId liên quan và chọn theo độ ưu tiên trong Java
        Set<Integer> allStaffIds = new LinkedHashSet<>();
        // Gom staffId từ nhóm tìm thấy theo account_id
        for (StaffBasic s : staffByAccountId.values()) {
            allStaffIds.add(s.staffId());
        }
        // Gom thêm staffId từ nhóm tìm thấy qua fallback staff.id
        for (StaffBasic s : staffByStaffId.values()) {
            allStaffIds.add(s.staffId());
        }
        Map<Integer, String> bestDeptByStaffId = loadBestDepartmentByStaffIds(
                conn, allStaffIds, responsibleDeptId, collabDeptIds);

        // 5. Lắp ráp ActorInfo cho từng actorId theo thứ tự ưu tiên: staff(account) -> partner -> staff(id) -> username
        Map<Integer, ActorInfo> result = new HashMap<>(actorIds.size());
        // Duyệt từng actorId để kết hợp mã, tên hiển thị và tên đơn vị phù hợp nhất
        for (Integer id : actorIds) {
            StaffBasic st = staffByAccountId.get(id);
            PartnerBasic ps = partnerByAccountId.get(id);
            // Chỉ dùng fallback staffByStaffId khi cả staff(account) và partner(account) đều không khớp
            StaffBasic stById = (st == null && ps == null) ? staffByStaffId.get(id) : null;
            String username = usernameByAccountId.get(id);

            // Chọn mã định danh đầu tiên khác rỗng theo độ ưu tiên nguồn dữ liệu
            String code = firstNonNull(
                    st != null ? st.code() : null,
                    ps != null ? ps.code() : null,
                    stById != null ? stById.code() : null);
            // Chọn họ tên đầu tiên khác rỗng; fallback cuối cùng là username tài khoản
            String name = firstNonNull(
                    st != null ? st.name() : null,
                    ps != null ? ps.name() : null,
                    stById != null ? stById.name() : null,
                    username);
            // Chọn đơn vị công tác (phòng ban ưu tiên của cán bộ hoặc tên công ty đối tác)
            String unit = firstNonNull(
                    st != null ? bestDeptByStaffId.get(st.staffId()) : null,
                    ps != null ? ps.companyName() : null,
                    stById != null ? bestDeptByStaffId.get(stById.staffId()) : null);

            // Chỉ đưa vào map kết quả nếu giải mã được ít nhất một thông tin khác null
            if (code != null || name != null || unit != null) {
                result.put(id, new ActorInfo(code, name, unit));
            }
        }
        return result;
    }

    /**
     * Tải và chọn tên phòng ban hiển thị phù hợp nhất cho từng cán bộ trong {@code staffIds}
     * theo thứ tự ưu tiên: phòng ban phụ trách hoạt động (0) > phòng ban phối hợp (1) > phòng ban khác (2).
     *
     * @param conn              kết nối JDBC đang mở
     * @param staffIds          tập ID cán bộ cần tra cứu đơn vị
     * @param responsibleDeptId ID phòng ban phụ trách chính của hoạt động (có thể {@code null})
     * @param collabDeptIds     tập ID các phòng ban phối hợp của hoạt động
     * @return map {@code staffId -> tên phòng ban ưu tiên cao nhất}
     */
    private Map<Integer, String> loadBestDepartmentByStaffIds(
            Connection conn, Set<Integer> staffIds, Integer responsibleDeptId, Set<Integer> collabDeptIds)
            throws SQLException {
        // Trả về rỗng ngay nếu không có cán bộ nào cần tra cứu phòng ban
        if (staffIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> bestPriorityByStaff = new HashMap<>();
        Map<Integer, String> bestDeptNameByStaff = new HashMap<>();

        // Chia tập staffIds theo chunk để truy vấn phân công phòng ban an toàn với mệnh đề IN
        for (List<Integer> chunk : chunkPositiveIds(staffIds)) {
            String sql = """
                    SELECT sa.staff_id, sa.department_id, d.name AS department_name
                    FROM dbo.staff_assignment sa
                    JOIN dbo.department d ON d.id = sa.department_id
                    WHERE sa.staff_id IN (%s)
                    ORDER BY sa.id ASC
                    """.formatted(placeholders(chunk.size()));
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindIntList(ps, 1, chunk);
                try (ResultSet rs = ps.executeQuery()) {
                    // Duyệt từng bản ghi phân công phòng ban (đã sắp theo sa.id ASC) và giữ lại phòng ban ưu tiên cao nhất
                    while (rs.next()) {
                        int staffId = rs.getInt("staff_id");
                        int deptId = rs.getInt("department_id");
                        String deptName = rs.getString("department_name");

                        // Xác định mức ưu tiên hiển thị của phòng ban đối với hoạt động hiện tại
                        int priority;
                        if (responsibleDeptId != null && responsibleDeptId == deptId) {
                            // Ưu tiên 0 (cao nhất): phòng ban phụ trách chính hoạt động
                            priority = 0;
                        } else if (collabDeptIds.contains(deptId)) {
                            // Ưu tiên 1: phòng ban phối hợp tổ chức hoạt động
                            priority = 1;
                        } else {
                            // Ưu tiên 2: các phòng ban khác mà cán bộ trực thuộc
                            priority = 2;
                        }

                        Integer currentBest = bestPriorityByStaff.get(staffId);
                        // Chỉ thay thế khi chưa có phòng ban nào hoặc tìm thấy phòng ban có mức ưu tiên cao hơn (số nhỏ hơn)
                        if (currentBest == null || priority < currentBest) {
                            bestPriorityByStaff.put(staffId, priority);
                            bestDeptNameByStaff.put(staffId, deptName);
                        }
                    }
                }
            }
        }
        return bestDeptNameByStaff;
    }

    /**
     * Hàm hỗ trợ đếm tổng số bản ghi người tham gia của hoạt động trên một kết nối (transaction) đang mở.
     */
    private int count(Connection conn, int activityId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM dbo.activity_participant WHERE activity_id = ?")) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Hàm hỗ trợ đếm tổng số chỗ đã chiếm (PARTICIPANT + đơn SUBMITTED/APPROVED) trên kết nối đang mở
     * để kiểm tra sức chứa ngay trong transaction.
     */
    private int countOccupied(Connection conn, int activityId) throws SQLException {
        // Dùng UNION (loại trùng student_id) giữa người tham gia vai trò PARTICIPANT và các đơn đăng ký đang giữ chỗ
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT ap.student_id
                    FROM dbo.activity_participant ap
                    WHERE ap.activity_id = ? AND ap.participation_role = 'PARTICIPANT'
                    UNION
                    SELECT r.student_id
                    FROM dbo.activity_registration_form f
                    JOIN dbo.activity_registration r ON r.form_id = f.id
                    WHERE f.activity_id = ? AND r.status IN ('SUBMITTED', 'APPROVED')
                ) occupied
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ==================== Utility helpers ====================

    /**
     * Lọc lấy các ID nguyên dương duy nhất (giữ nguyên thứ tự xuất hiện) và chia nhỏ thành các danh sách con
     * kích thước tối đa {@link #IN_CLAUSE_CHUNK_SIZE} để tránh vượt giới hạn 2100 tham số của SQL Server.
     *
     * @param ids tập ID đầu vào (có thể {@code null}, chứa phần tử trùng hoặc {@code <= 0})
     * @return danh sách các chunk ID hợp lệ
     */
    private static List<List<Integer>> chunkPositiveIds(Collection<Integer> ids) {
        // Nếu tập đầu vào null hoặc rỗng thì không có chunk nào
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Integer> distinct = new LinkedHashSet<>();
        // Loại bỏ phần tử null, <= 0 và loại trùng bằng LinkedHashSet để bảo toàn thứ tự
        for (Integer id : ids) {
            if (id != null && id > 0) {
                distinct.add(id);
            }
        }
        // Nếu không còn ID dương hợp lệ nào thì trả về rỗng
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<Integer> all = new ArrayList<>(distinct);
        List<List<Integer>> chunks = new ArrayList<>();
        // Cắt danh sách thành từng đoạn có kích thước tối đa IN_CLAUSE_CHUNK_SIZE
        for (int i = 0; i < all.size(); i += IN_CLAUSE_CHUNK_SIZE) {
            chunks.add(all.subList(i, Math.min(i + IN_CLAUSE_CHUNK_SIZE, all.size())));
        }
        return chunks;
    }

    /**
     * Tạo chuỗi dấu hỏi chấm phân cách bằng dấu phẩy ({@code "?,?,..."}) tương ứng với số lượng tham số {@code count}
     * dùng cho mệnh đề SQL {@code IN (...)}.
     *
     * @param count số lượng phần tử trong mệnh đề IN
     * @return chuỗi placeholder JDBC
     */
    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    /**
     * Gắn lần lượt các giá trị số nguyên trong danh sách {@code values} vào {@link PreparedStatement}
     * bắt đầu từ chỉ số tham số {@code startIndex}.
     *
     * @param ps         câu lệnh chuẩn bị JDBC
     * @param startIndex vị trí tham số bắt đầu (1-based)
     * @param values     danh sách số nguyên cần gắn
     */
    private static void bindIntList(PreparedStatement ps, int startIndex, List<Integer> values) throws SQLException {
        // Duyệt danh sách giá trị và gán liên tiếp từ vị trí startIndex
        for (int i = 0; i < values.size(); i++) {
            ps.setInt(startIndex + i, values.get(i));
        }
    }

    /**
     * Gắn tham số kiểu {@link Integer} có thể {@code null} vào {@link PreparedStatement}.
     *
     * @param ps    câu lệnh chuẩn bị JDBC
     * @param index vị trí tham số (1-based)
     * @param value giá trị số nguyên hoặc {@code null}
     */
    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        // Nếu null thì gắn SQL NULL kiểu INTEGER, ngược lại gắn giá trị int
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    /**
     * Gắn tham số kiểu {@link BigDecimal} có thể {@code null} vào {@link PreparedStatement}.
     *
     * @param ps    câu lệnh chuẩn bị JDBC
     * @param index vị trí tham số (1-based)
     * @param value giá trị số thập phân hoặc {@code null}
     */
    private static void setNullableBigDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        // Nếu null thì gắn SQL NULL kiểu DECIMAL, ngược lại gắn giá trị BigDecimal
        if (value == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, value);
        }
    }

    /**
     * Gắn tham số kiểu {@link String} có thể {@code null} vào {@link PreparedStatement}.
     *
     * @param ps    câu lệnh chuẩn bị JDBC
     * @param index vị trí tham số (1-based)
     * @param value giá trị chuỗi hoặc {@code null}
     */
    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        // Nếu null thì gắn SQL NULL kiểu NVARCHAR, ngược lại gắn chuỗi
        if (value == null) {
            ps.setNull(index, Types.NVARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    /**
     * Chuyển đổi chuỗi trạng thái điểm danh sang enum {@link ParticipantStatus};
     * nếu chuỗi rỗng, {@code null} hoặc không khớp hằng số enum hợp lệ thì trả về {@link ParticipantStatus#NOT_YET}.
     *
     * @param status chuỗi trạng thái đọc từ cơ sở dữ liệu
     * @return trạng thái {@link ParticipantStatus} hợp lệ
     */
    private static ParticipantStatus parseStatusOrNotYet(String status) {
        // Nếu giá trị trống thì mặc định là chưa điểm danh (NOT_YET)
        if (status == null || status.isBlank()) {
            return ParticipantStatus.NOT_YET;
        }
        try {
            return ParticipantStatus.valueOf(status.trim());
        } catch (IllegalArgumentException e) {
            // Fallback an toàn sang NOT_YET nếu dữ liệu DB chứa mã trạng thái lạ hoặc không hợp lệ
            return ParticipantStatus.NOT_YET;
        }
    }

    /**
     * Chuyển chuỗi {@code changedBy} sang ID số nguyên dương của người thực hiện thao tác;
     * nếu chuỗi rỗng, không phải số hợp lệ hoặc {@code <= 0} thì trả về ID mặc định 767.
     */
    private static int parseActorId(String changedBy) {
        // Chỉ thử ép kiểu khi chuỗi changedBy có nội dung
        if (changedBy != null && !changedBy.isBlank()) {
            try {
                int parsed = Integer.parseInt(changedBy.trim());
                // Chỉ chấp nhận ID số nguyên dương hợp lệ
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Bỏ qua lỗi định dạng số để fallback về DEFAULT_ACTOR_ID bên dưới
            }
        }
        return DEFAULT_ACTOR_ID;
    }

    /**
     * Phân tích đối tượng {@code raw} đọc từ cột {@code updated_by}/{@code created_by} sang ID số nguyên dương;
     * trả về {@code null} nếu rỗng, không phải số hoặc {@code <= 0}.
     *
     * @param raw giá trị thô lấy từ {@link ResultSet#getObject(String)}
     * @return ID người thao tác dương hoặc {@code null}
     */
    private static Integer parseNullableActorId(Object raw) {
        // Nếu giá trị DB là NULL thì trả về null ngay
        if (raw == null) {
            return null;
        }
        // Nếu driver JDBC đã trả về kiểu Number thì lấy trực tiếp giá trị int (chỉ nhận > 0)
        if (raw instanceof Number number) {
            int val = number.intValue();
            return val > 0 ? val : null;
        }
        String text = String.valueOf(raw).trim();
        // Nếu chuyển sang chuỗi mà rỗng thì trả về null
        if (text.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(text);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            // Trả về null nếu dữ liệu lịch sử cũ lưu tên chuỗi thay vì ID số
            return null;
        }
    }

    /**
     * Trả về chuỗi đầu tiên khác {@code null} và không chỉ gồm khoảng trắng trong danh sách truyền vào.
     *
     * @param values các chuỗi ứng viên theo thứ tự ưu tiên giảm dần
     * @return chuỗi hợp lệ đầu tiên hoặc {@code null} nếu tất cả đều rỗng
     */
    private static String firstNonNull(String... values) {
        // Duyệt tuần tự theo thứ tự ưu tiên và dừng ngay khi gặp chuỗi có nội dung
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /**
     * Thực hiện rollback kết nối một cách an toàn trong khối {@code catch}; nếu bản thân lệnh rollback
     * cũng ném {@link SQLException} thì gắn nó làm suppressed exception vào lỗi chính {@code primary}.
     *
     * @param conn    kết nối JDBC cần rollback
     * @param primary ngoại lệ SQL chính gây ra việc hủy transaction
     */
    private static void rollbackQuietly(Connection conn, SQLException primary) {
        try {
            conn.rollback();
        } catch (SQLException rollbackException) {
            // Không che khuất nguyên nhân lỗi gốc: gắn lỗi rollback vào danh sách suppressed của primary
            primary.addSuppressed(rollbackException);
        }
    }
}
