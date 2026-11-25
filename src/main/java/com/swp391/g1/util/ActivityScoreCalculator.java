package com.swp391.g1.util;

import com.swp391.g1.model.Enum.ParticipantStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Lớp tập trung toàn bộ quy tắc nghiệp vụ về tính điểm, hệ số, suy luận trạng thái tổng
 * và điều kiện xóa người tham gia hoạt động (Single Source of Truth).
 *
 * <p>Tách biệt hoàn toàn khỏi JDBC/SQL để dùng chung giữa tầng Service (hiển thị, nghiệp vụ)
 * và tầng DAO (đồng bộ kết quả trong transaction), đồng thời dễ dàng kiểm thử bằng Unit Test.</p>
 */
public final class ActivityScoreCalculator {

    // Thang điểm chuyên cần quy đổi cho từng buổi điểm danh
    public static final BigDecimal SCORE_ATTENDED = new BigDecimal("5.00");
    public static final BigDecimal SCORE_LATE_OR_EARLY = new BigDecimal("3.00");
    public static final BigDecimal SCORE_ABSENT = new BigDecimal("-5.00");
    public static final BigDecimal ZERO_SCORE = new BigDecimal("0.00");

    // Các hệ số nhân áp dụng cho điểm trung bình của sinh viên có mặt
    public static final BigDecimal DEFAULT_COEFFICIENT = new BigDecimal("1.00");
    public static final BigDecimal EARLY_REG_COEFFICIENT = new BigDecimal("1.10");
    public static final BigDecimal ORGANIZER_COEFFICIENT = new BigDecimal("1.50");

    private static final BigDecimal TWO = new BigDecimal("2.0");

    /**
     * Ngăn khởi tạo thể hiện cho lớp tiện ích chỉ gồm các phương thức tĩnh.
     */
    private ActivityScoreCalculator() {
    }

    /**
     * Kết quả tổng hợp trạng thái và điểm chuyên cần theo các buổi của một người tham gia.
     */
    public record SessionStats(
            ParticipantStatus overallStatus,
            int attendedSessionCount,
            int absentSessionCount,
            int markedSessionCount,
            BigDecimal attendanceScore) {
    }

    /**
     * Bảng phân tích đầy đủ các điểm thành phần, hệ số và điểm cuối cùng của một người tham gia.
     */
    public record ScoreBreakdown(
            ParticipantStatus overallStatus,
            int attendedSessionCount,
            int absentSessionCount,
            int markedSessionCount,
            BigDecimal attendanceScore,
            BigDecimal effectiveEvalScore,
            BigDecimal participationScore,
            BigDecimal summaryScore,
            BigDecimal averageScore,
            BigDecimal orgLevelCoefficient,
            BigDecimal earlyRegCoefficient,
            BigDecimal organizerCoefficient,
            BigDecimal finalPoint) {
    }

    /**
     * Quy đổi trạng thái điểm danh của một buổi sang điểm số:
     * ATTENDED = 5.00, IN_LATE / OUT_EARLY = 3.00, ABSENT = -5.00, còn lại = 0.00.
     */
    public static BigDecimal computeSessionScore(ParticipantStatus status) {
        // Trạng thái null được coi như chưa điểm danh (0.00 điểm)
        if (status == null) {
            return ZERO_SCORE;
        }
        // Ánh xạ từng trạng thái điểm danh của buổi sang điểm số tương ứng
        return switch (status) {
            // Có mặt đầy đủ: +5.00 điểm
            case ATTENDED -> SCORE_ATTENDED;
            // Đi muộn hoặc về sớm: +3.00 điểm
            case IN_LATE, OUT_EARLY -> SCORE_LATE_OR_EARLY;
            // Vắng mặt: bị trừ -5.00 điểm
            case ABSENT -> SCORE_ABSENT;
            // Chưa điểm danh: 0.00 điểm
            case NOT_YET -> ZERO_SCORE;
        };
    }

    /**
     * Kiểm tra trạng thái có được tính là đã tham dự (có mặt) hay không.
     */
    public static boolean isPresentStatus(ParticipantStatus status) {
        // Có mặt đầy đủ, đi muộn hoặc về sớm đều được công nhận là đã tham dự hoạt động
        return status == ParticipantStatus.ATTENDED
                || status == ParticipantStatus.IN_LATE
                || status == ParticipantStatus.OUT_EARLY;
    }

    /**
     * Xác định trạng thái điểm danh của một buổi cụ thể; hỗ trợ fallback sang trạng thái cũ ở buổi 1
     * nếu bảng theo buổi chưa có bất kỳ bản ghi nào.
     */
    public static ParticipantStatus resolveSessionStatus(
            Map<Integer, ParticipantStatus> statusBySession,
            int sessionNumber,
            ParticipantStatus legacyFallbackStatus) {
        // Đảm bảo số thứ tự buổi tối thiểu là 1 và map trạng thái không bao giờ null
        int safeSession = Math.max(1, sessionNumber);
        Map<Integer, ParticipantStatus> safeMap = statusBySession == null ? Collections.emptyMap() : statusBySession;
        ParticipantStatus explicit = safeMap.get(safeSession);
        // Ưu tiên trạng thái đã lưu tường minh cho buổi đang xét trong bảng điểm danh theo buổi
        if (explicit != null) {
            return explicit;
        }
        // Fallback dữ liệu cũ: khi chưa có bản ghi nào theo buổi thì ánh xạ trạng thái cũ vào buổi 1
        if (safeSession == 1 && safeMap.isEmpty() && legacyFallbackStatus != null) {
            return legacyFallbackStatus;
        }
        return ParticipantStatus.NOT_YET;
    }

    /**
     * Suy ra trạng thái điểm danh tổng từ danh sách trạng thái các buổi theo thứ tự ưu tiên:
     * {@code ATTENDED > IN_LATE > OUT_EARLY > ABSENT > NOT_YET}.
     */
    public static ParticipantStatus resolveOverallStatus(Collection<ParticipantStatus> statuses) {
        // Không có trạng thái buổi nào -> mặc định chưa điểm danh (NOT_YET)
        if (statuses == null || statuses.isEmpty()) {
            return ParticipantStatus.NOT_YET;
        }
        ParticipantStatus best = ParticipantStatus.NOT_YET;
        int bestPriority = 0;
        // Duyệt tất cả trạng thái các buổi để chọn trạng thái có mức ưu tiên cao nhất
        for (ParticipantStatus status : statuses) {
            int priority = statusPriority(status);
            // Cập nhật trạng thái đại diện nếu tìm thấy trạng thái có độ ưu tiên cao hơn
            if (priority > bestPriority) {
                bestPriority = priority;
                best = status;
            }
        }
        return best;
    }

    /**
     * Trả về trọng số ưu tiên của từng trạng thái điểm danh để suy ra trạng thái tổng
     * ({@code ATTENDED=4 > IN_LATE=3 > OUT_EARLY=2 > ABSENT=1 > NOT_YET=0}).
     */
    private static int statusPriority(ParticipantStatus status) {
        // Giá trị null có mức ưu tiên thấp nhất (0)
        if (status == null) {
            return 0;
        }
        // Quy đổi mỗi trạng thái điểm danh sang thứ bậc ưu tiên từ 0 đến 4
        return switch (status) {
            // Ưu tiên cao nhất: có mặt đầy đủ
            case ATTENDED -> 4;
            // Ưu tiên thứ hai: đi muộn
            case IN_LATE -> 3;
            // Ưu tiên thứ ba: về sớm
            case OUT_EARLY -> 2;
            // Ưu tiên thứ tư: vắng mặt
            case ABSENT -> 1;
            // Ưu tiên thấp nhất: chưa điểm danh
            case NOT_YET -> 0;
        };
    }

    /**
     * Tổng hợp số buổi có mặt, vắng, đã điểm danh, trạng thái tổng và điểm chuyên cần trung bình
     * trong phạm vi {@code 1..totalSessions}.
     */
    public static SessionStats summarizeSessions(
            Map<Integer, ParticipantStatus> statusBySession,
            int totalSessions,
            ParticipantStatus legacyFallbackStatus) {
        // Đảm bảo tổng số buổi chia trung bình tối thiểu là 1 và map đầu vào an toàn không null
        int safeTotalSessions = Math.max(1, totalSessions);
        Map<Integer, ParticipantStatus> safeMap = statusBySession == null ? Collections.emptyMap() : statusBySession;

        int attended = 0;
        int absent = 0;
        int marked = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        ParticipantStatus bestStatus = ParticipantStatus.NOT_YET;
        int bestPriority = 0;

        boolean hasValidSessionRow = false;
        // Duyệt các bản ghi điểm danh theo buổi để đếm số buổi, cộng tổng điểm chuyên cần và chọn trạng thái tổng
        for (Map.Entry<Integer, ParticipantStatus> entry : safeMap.entrySet()) {
            Integer sessionNo = entry.getKey();
            ParticipantStatus status = entry.getValue();
            // Bỏ qua các bản ghi sai buổi (ngoài [1..safeTotalSessions]) hoặc trạng thái null
            if (sessionNo == null || sessionNo < 1 || sessionNo > safeTotalSessions || status == null) {
                continue;
            }
            hasValidSessionRow = true;
            // Buổi có mặt (ATTENDED/IN_LATE/OUT_EARLY) -> tăng cả số buổi có mặt và số buổi đã điểm danh
            if (isPresentStatus(status)) {
                attended++;
                marked++;
            // Buổi vắng mặt -> tăng số buổi vắng và số buổi đã điểm danh
            } else if (status == ParticipantStatus.ABSENT) {
                absent++;
                marked++;
            }
            // Cộng dồn điểm quy đổi của buổi vào tổng điểm chuyên cần
            totalScore = totalScore.add(computeSessionScore(status));
            int priority = statusPriority(status);
            // Cập nhật trạng thái tổng nếu buổi hiện tại có độ ưu tiên cao hơn
            if (priority > bestPriority) {
                bestPriority = priority;
                bestStatus = status;
            }
        }

        // Fallback cho dữ liệu cũ chưa có bản ghi trong bảng theo buổi (coi trạng thái cũ như 1 buổi đã điểm danh)
        if (!hasValidSessionRow
                && safeMap.isEmpty()
                && legacyFallbackStatus != null
                && legacyFallbackStatus != ParticipantStatus.NOT_YET) {
            bestStatus = legacyFallbackStatus;
            marked = 1;
            // Ghi nhận 1 buổi vắng nếu trạng thái cũ là ABSENT
            if (legacyFallbackStatus == ParticipantStatus.ABSENT) {
                absent = 1;
            // Ghi nhận 1 buổi có mặt nếu trạng thái cũ thuộc nhóm có mặt
            } else if (isPresentStatus(legacyFallbackStatus)) {
                attended = 1;
            }
            totalScore = computeSessionScore(legacyFallbackStatus);
        }

        // Điểm chuyên cần = (tổng điểm các buổi) / (tổng số buổi của hoạt động), làm tròn HALF_UP 2 chữ số thập phân
        BigDecimal avgAttendanceScore = totalScore
                .divide(BigDecimal.valueOf(safeTotalSessions), 2, RoundingMode.HALF_UP);

        return new SessionStats(bestStatus, attended, absent, marked, avgAttendanceScore);
    }

    /**
     * Xác định hệ số cấp tổ chức (mặc định 1.00 nếu null).
     */
    public static BigDecimal resolveOrgLevelCoefficient(BigDecimal orgLevelCoefficient) {
        // Nếu hoạt động chưa cấu hình hệ số cấp tổ chức thì dùng mặc định 1.00; ngược lại chuẩn hóa 2 chữ số thập phân
        return orgLevelCoefficient == null
                ? DEFAULT_COEFFICIENT
                : orgLevelCoefficient.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Xác định hệ số đăng ký sớm: REGISTERED = 1.10, còn lại = 1.00.
     */
    public static BigDecimal resolveEarlyRegCoefficient(String registrationStatus) {
        // Đăng ký trước hoạt động (REGISTERED) được hưởng hệ số 1.10; thêm thủ công/không đăng ký dùng 1.00
        return "REGISTERED".equals(registrationStatus) ? EARLY_REG_COEFFICIENT : DEFAULT_COEFFICIENT;
    }

    /**
     * Xác định hệ số vai trò ban tổ chức: ORGANIZER = 1.50, còn lại = 1.00.
     */
    public static BigDecimal resolveOrganizerCoefficient(String participationRole) {
        // Thành viên ban tổ chức (ORGANIZER) được hưởng hệ số 1.50; người tham gia thường dùng 1.00
        return "ORGANIZER".equals(participationRole) ? ORGANIZER_COEFFICIENT : DEFAULT_COEFFICIENT;
    }

    /**
     * Tính toàn bộ các điểm thành phần, điểm trung bình và điểm tổng kết cuối cùng ({@code finalPoint})
     * của một người tham gia từ dữ liệu thô.
     */
    public static ScoreBreakdown calculate(
            Map<Integer, ParticipantStatus> statusBySession,
            int totalSessions,
            ParticipantStatus legacyFallbackStatus,
            BigDecimal participationEvalScore,
            BigDecimal summaryScore,
            boolean summaryRequired,
            BigDecimal orgLevelCoefficient,
            String registrationStatus,
            String participationRole) {

        // Bước 1: Tổng hợp trạng thái và điểm chuyên cần từ dữ liệu điểm danh các buổi
        SessionStats stats = summarizeSessions(statusBySession, totalSessions, legacyFallbackStatus);
        ParticipantStatus overallStatus = stats.overallStatus();
        BigDecimal attendanceScore = stats.attendanceScore();

        // Bước 2: Xác định 3 hệ số nhân và chuẩn hóa điểm bài thu hoạch (nếu có) về 2 chữ số thập phân
        BigDecimal orgCoeff = resolveOrgLevelCoefficient(orgLevelCoefficient);
        BigDecimal regCoeff = resolveEarlyRegCoefficient(registrationStatus);
        BigDecimal organizerCoeff = resolveOrganizerCoefficient(participationRole);
        BigDecimal normalizedSummary = summaryScore == null
                ? null
                : summaryScore.setScale(2, RoundingMode.HALF_UP);

        BigDecimal effectiveEvalScore;
        BigDecimal participationScore;
        BigDecimal averageScore;
        BigDecimal finalPoint;

        // Bước 3: Tính điểm theo 3 nhóm trạng thái tổng (Có mặt / Vắng mặt / Chưa điểm danh)
        // Nhánh 1: Sinh viên có mặt ít nhất 1 buổi -> tính đủ điểm tham gia, điểm trung bình và nhân 3 hệ số
        if (isPresentStatus(overallStatus)) {
            // Chưa chấm điểm đánh giá tham gia thì quy về 0.00; đã chấm thì chuẩn hóa 2 chữ số thập phân
            effectiveEvalScore = participationEvalScore == null
                    ? ZERO_SCORE
                    : participationEvalScore.setScale(2, RoundingMode.HALF_UP);
            // Điểm tham gia = điểm chuyên cần + điểm đánh giá tham gia
            participationScore = attendanceScore.add(effectiveEvalScore).setScale(2, RoundingMode.HALF_UP);

            BigDecimal unroundedAvg;
            // Hoạt động có yêu cầu bài thu hoạch và sinh viên đã có điểm thu hoạch -> tính trung bình cộng 2 đầu điểm
            if (summaryRequired && normalizedSummary != null) {
                // Giữ 6 chữ số thập phân ở bước trung gian để tránh sai số làm tròn kép khi nhân tiếp với 3 hệ số
                unroundedAvg = participationScore.add(normalizedSummary).divide(TWO, 6, RoundingMode.HALF_UP);
                averageScore = unroundedAvg.setScale(2, RoundingMode.HALF_UP);
            // Không yêu cầu bài thu hoạch hoặc chưa có điểm thu hoạch -> điểm trung bình chính là điểm tham gia
            } else {
                unroundedAvg = participationScore;
                averageScore = participationScore;
            }
            // Điểm tổng kết cuối = điểm trung bình (chưa làm tròn sớm) * hệ số cấp tổ chức * hệ số đăng ký sớm * hệ số BTC
            finalPoint = unroundedAvg
                    .multiply(orgCoeff)
                    .multiply(regCoeff)
                    .multiply(organizerCoeff)
                    .setScale(2, RoundingMode.HALF_UP);
        // Nhánh 2: Vắng mặt toàn bộ các buổi đã điểm danh -> không tính điểm đánh giá, không nhân hệ số (giữ điểm âm chuyên cần)
        } else if (overallStatus == ParticipantStatus.ABSENT) {
            effectiveEvalScore = ZERO_SCORE;
            participationScore = attendanceScore;
            averageScore = attendanceScore;
            finalPoint = attendanceScore;
        // Nhánh 3: Chưa điểm danh buổi nào (NOT_YET) -> mọi điểm thành phần và điểm cuối cùng đều bằng 0.00
        } else {
            effectiveEvalScore = ZERO_SCORE;
            participationScore = ZERO_SCORE;
            averageScore = ZERO_SCORE;
            finalPoint = ZERO_SCORE;
        }

        return new ScoreBreakdown(
                overallStatus,
                stats.attendedSessionCount(),
                stats.absentSessionCount(),
                stats.markedSessionCount(),
                attendanceScore,
                effectiveEvalScore,
                participationScore,
                normalizedSummary,
                averageScore,
                orgCoeff,
                regCoeff,
                organizerCoeff,
                finalPoint
        );
    }

    /**
     * Xác định người tham gia có được phép xóa khỏi hoạt động hay không:
     * không phải ORGANIZER, chưa được điểm danh buổi nào (trạng thái tổng NOT_YET, marked = 0)
     * và chưa có bất kỳ bản ghi lịch sử điểm danh nào.
     */
    public static boolean isRemovable(
            String participationRole,
            ParticipantStatus overallStatus,
            int markedSessionCount,
            boolean hasAttendanceHistory) {
        // Chỉ cho phép xóa khi thỏa mãn đồng thời cả 4 điều kiện an toàn dữ liệu điểm danh và vai trò BTC
        return !"ORGANIZER".equals(participationRole)
                && overallStatus == ParticipantStatus.NOT_YET
                && markedSessionCount == 0
                && !hasAttendanceHistory;
    }
}
