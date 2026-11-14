package com.swp391.g1.dao;

import com.swp391.g1.model.RecruitmentRound;

import java.time.LocalDateTime;
import java.util.List;

public interface IRecruitmentRoundDAO {

    /** Tìm đợt theo id kèm vị trí (có số đã trúng tuyển) và câu hỏi; null nếu không có. */
    RecruitmentRound findById(int id);

    RecruitmentRound findByActivityId(int activityId);

    /** Danh sách cho staff: có tên hoạt động và số hồ sơ còn hiệu lực, chưa nạp vị trí/câu hỏi. */
    List<RecruitmentRound> findAll();

    /** Các đợt đang nhận hồ sơ, hoạt động đã duyệt và chưa diễn ra; kèm vị trí và số đã trúng tuyển. */
    List<RecruitmentRound> findOpenRounds();

    /** Thêm đợt cùng vị trí và câu hỏi trong một transaction; trả về id. Trùng hoạt động thì ném DataAccessException duplicateKey. */
    int insert(RecruitmentRound round);

    /** Cập nhật thông tin chung; chỉ thay vị trí và câu hỏi khi replaceStructure = true. */
    boolean update(RecruitmentRound round, boolean replaceStructure);

    boolean updateStatus(int id, String status);

    /** Xóa đợt; chỉ thành công khi không còn hồ sơ nào ngoài hồ sơ đã rút. */
    boolean delete(int id);

    /** Số hồ sơ chưa rút của đợt. */
    int countActiveApplications(int roundId);

    /** Hoạt động đã duyệt và chưa diễn ra. */
    boolean isActivityEligible(int activityId);

    /** Đợt đã có bất kỳ hồ sơ nào (kể cả đã rút): khóa việc sửa vị trí và câu hỏi. */
    boolean hasApplications(int roundId);

    /** Hoạt động đủ điều kiện và chưa có đợt tuyển (kèm hoạt động includeActivityId nếu có); chỉ điền activityId, activityName. */
    List<RecruitmentRound> findEligibleActivities(Integer includeActivityId);

    /** Giờ mở của đợt đăng ký tham gia cùng hoạt động; null nếu không có. */
    LocalDateTime findParticipationOpenAt(int activityId);

    /** Tự đóng đợt hết hạn hoặc khi hoạt động đã bắt đầu; trả về số đợt được đóng. */
    int synchronizeStatuses();
}
