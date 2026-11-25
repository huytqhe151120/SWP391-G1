package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityOrganizationLevelDAO;
import com.swp391.g1.dao.IActivityParticipantDAO;
import com.swp391.g1.dao.IActivityStaffDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IRegistrationFormDAO;
import com.swp391.g1.dao.ISemesterDAO;
import com.swp391.g1.dao.IStudentDAO;
import com.swp391.g1.dao.impl.ActivityOrganizationLevelDAOImpl;
import com.swp391.g1.dao.impl.ActivityParticipantDAOImpl;
import com.swp391.g1.dao.impl.ActivityStaffDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.SemesterDAOImpl;
import com.swp391.g1.dao.impl.StudentDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.request.ExtracurricularActivityRequestDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.ActivityTargetParticipant;
import com.swp391.g1.model.Enum.ActivityNature;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.Semester;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityTargetParticipantService;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Lớp cài đặt nghiệp vụ quản lý hoạt động ngoại khóa, bao gồm:
 * phân quyền phạm vi hiển thị, CRUD, kiểm tra ràng buộc dữ liệu, quy trình phê duyệt
 * và đồng bộ trạng thái hoạt động theo thời gian.
 */
public class ExtracurricularActivityServiceImpl implements IExtracurricularActivityService {

    private final IExtracurricularActivityDAO activityDAO;
    private final IActivityStaffDAO activityStaffDAO;
    private final ISemesterDAO semesterDAO = new SemesterDAOImpl();
    private final IActivityOrganizationLevelDAO organizationLevelDAO = new ActivityOrganizationLevelDAOImpl();
    private final IActivityParticipantDAO participantDAO = new ActivityParticipantDAOImpl();
    private final IActivityTargetParticipantService targetParticipantService = new ActivityTargetParticipantServiceImpl();
    private final IStudentDAO studentDAO = new StudentDAOImpl();
    private final com.swp391.g1.service.INotificationService notificationService = new NotificationServiceImpl();

    private static final String LOCKED_MESSAGE =
            "Không thể sửa hoặc xóa hoạt động đang diễn ra hoặc đã hoàn thành.";

    /**
     * Kiểm tra hoạt động có đang bị khóa chỉnh sửa/xóa hay không
     * (áp dụng khi trạng thái là ONGOING hoặc COMPLETED).
     *
     * @param activity đối tượng hoạt động cần kiểm tra
     * @return {@code true} nếu hoạt động đang diễn ra hoặc đã hoàn thành; ngược lại {@code false}
     */
    private boolean isLocked(ExtracurricularActivity activity) {
        return activity != null && (activity.getActivityStatus() == ActivityStatus.ONGOING
                || activity.getActivityStatus() == ActivityStatus.COMPLETED);
    }

    /**
     * Khởi tạo service với các DAO mặc định phục vụ truy vấn hoạt động và nhân sự phụ trách.
     */
    public ExtracurricularActivityServiceImpl() {
        this.activityDAO = new ExtracurricularActivityDAOImpl();
        this.activityStaffDAO = new ActivityStaffDAOImpl();
    }

    /**
     * Lấy toàn bộ danh sách hoạt động ngoại khóa kèm đầy đủ thông tin chi tiết liên quan.
     *
     * @return danh sách {@link ExtracurricularActivityResponseDTO}
     */
    @Override
    public List<ExtracurricularActivityResponseDTO> getAllActivities() {
        return activityDAO.findAllWithDetails();
    }

    /**
     * Lấy danh sách hoạt động ngoại khóa mà người dùng hiện tại được phép nhìn thấy theo vai trò:
     * ADMIN thấy tất cả, STUDENT chỉ thấy hoạt động đã duyệt và thuộc đối tượng mục tiêu,
     * các vai trò không quản lý chỉ thấy hoạt động đã duyệt, còn STAFF thấy trong phạm vi được phân công.
     *
     * @param user thông tin người dùng hiện tại
     * @return danh sách hoạt động được phép xem; danh sách rỗng nếu không hợp lệ
     */
    @Override
    public List<ExtracurricularActivityResponseDTO> getActivitiesVisibleTo(CurrentUser user) {
        // Chưa đăng nhập hoặc thiếu thông tin người dùng -> không trả về hoạt động nào
        if (user == null) {
            return List.of();
        }
        // Quản trị viên được xem toàn bộ hoạt động trong hệ thống
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return getAllActivities();
        }
        // Xử lý các tài khoản không phải STAFF (sinh viên, đối tác hoặc vai trò khác)
        if (!AccessPolicy.hasType(user, "STAFF")) {
            // Riêng sinh viên chỉ thấy hoạt động đã duyệt và khớp cấu hình đối tượng tham gia
            if (AccessPolicy.hasType(user, "STUDENT")) {
                Student student = studentDAO.findByAccountId(user.getId());
                // Nếu tài khoản chưa liên kết hồ sơ sinh viên thì không có hoạt động hợp lệ
                if (student == null) {
                    return List.of();
                }
                Set<Integer> eligibleIds = targetParticipantService.getEligibleActivityIdsForStudent(student.getId());
                // Lọc qua toàn bộ hoạt động để giữ lại các hoạt động APPROVED nằm trong tập ID sinh viên đủ điều kiện
                return getAllActivities().stream()
                        .filter(activity -> activity.getApprovalStatus() == ApprovalStatus.APPROVED
                                && eligibleIds.contains(activity.getId()))
                        .toList();
            }
            // Người dùng không có quyền quản lý hoạt động chỉ được xem các hoạt động đã duyệt (APPROVED)
            return getAllActivities().stream()
                    .filter(activity -> activity.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .toList();
        }
        // Kiểm tra tài khoản STAFF có phạm vi quản lý hợp lệ và ID dương hay không
        if (!AccessPolicy.canUseStaffActivityScope(user) || user.getId() <= 0) {
            return List.of();
        }
        return activityDAO.findAllWithDetailsForStaff(user.getId(), user.getRole());
    }

    /**
     * Lấy thông tin chi tiết một hoạt động theo ID, đồng thời nạp danh sách đối tượng mục tiêu.
     *
     * @param id mã định danh hoạt động
     * @return {@link ExtracurricularActivityResponseDTO} nếu tìm thấy; ngược lại {@code null}
     */
    @Override
    public ExtracurricularActivityResponseDTO getActivityById(int id) {
        // Bỏ qua truy vấn nếu ID không hợp lệ
        if (id <= 0) {
            return null;
        }
        // Duyệt danh sách hoạt động đầy đủ chi tiết để tìm bản ghi trùng khớp ID
        ExtracurricularActivityResponseDTO dto = activityDAO.findAllWithDetails().stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElse(null);
        // Nếu tìm thấy hoạt động thì nạp bổ sung cấu hình đối tượng tham gia mục tiêu
        if (dto != null) {
            dto.setTargetParticipants(targetParticipantService.getTargetsByActivityId(id));
        }
        return dto;
    }

    /**
     * Kiểm tra người dùng hiện tại có quyền truy cập/quản lý hoạt động có mã {@code id} hay không.
     *
     * @param id   mã định danh hoạt động
     * @param user thông tin người dùng hiện tại
     * @return {@code true} nếu được phép truy cập; ngược lại {@code false}
     */
    @Override
    public boolean canAccessActivity(int id, CurrentUser user) {
        // Không cho phép truy cập nếu ID không hợp lệ hoặc chưa có người dùng
        if (id <= 0 || user == null) {
            return false;
        }
        // ADMIN được truy cập mọi hoạt động miễn là bản ghi tồn tại trong DB
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return activityDAO.findById(id) != null;
        }
        return AccessPolicy.canUseStaffActivityScope(user)
                && user.getId() > 0
                && activityDAO.isInStaffActivityScope(id, user.getId(), user.getRole());
    }

    /**
     * Tạo mới một hoạt động ngoại khóa ở trạng thái mặc định DRAFT/UPCOMING,
     * lưu cấu hình đối tượng tham gia và đồng bộ cờ công khai.
     *
     * @param dto dữ liệu biểu mẫu tạo hoạt động
     * @return {@code true} nếu tạo thành công; {@code false} nếu chèn DB không trả về ID
     * @throws Exception nếu dữ liệu đầu vào không hợp lệ hoặc lỗi hệ thống
     */
    @Override
    public boolean createActivity(ExtracurricularActivityRequestDTO dto) throws Exception {
        // Bước 1: Validate dữ liệu đầu vào và xây dựng danh sách dòng đối tượng tham gia
        validateRequestDTO(dto, false);
        List<ActivityTargetParticipant> targetRows = targetParticipantService.buildTargetRows(
                dto.getTargetScope(),
                dto.getTargetDepartmentIds(),
                dto.getTargetCohortIds(),
                dto.getTargetMajorIds(),
                dto.getTargetProgramIds(),
                dto.getTargetClassIds()
        );

        // Bước 2: Chuyển RequestDTO thành Entity
        ExtracurricularActivity entity = mapToEntity(dto);

        // Bước 3: Thiết lập trạng thái mặc định
        entity.setApprovalStatus(ApprovalStatus.DRAFT);
        entity.setActivityStatus(ActivityStatus.UPCOMING);

        // Bước 4: Thêm hoạt động vào database
        Integer generatedId = activityDAO.insert(entity);

        // Bước 5: Kiểm tra thêm thành công; dừng ngay nếu không nhận được ID sinh tự động
        if (generatedId == null || generatedId <= 0) {
            return false;
        }

        // Bước 6: Lưu cấu hình đối tượng tham gia và cập nhật trạng thái công khai
        targetParticipantService.saveTargetsForActivity(generatedId, targetRows);
        activityDAO.refreshPublic(generatedId);

        return true;
    }

    /**
     * Cập nhật thông tin hoạt động ngoại khóa đang ở trạng thái UPCOMING.
     * Nếu hoạt động đã duyệt (APPROVED) và có thay đổi nội dung hoặc đối tượng mục tiêu,
     * trạng thái duyệt sẽ được hạ về DRAFT, xóa danh sách tự động ghi danh chưa điểm danh
     * và gửi thông báo cho người tham gia khi thời gian hoặc địa điểm thay đổi.
     *
     * @param dto dữ liệu biểu mẫu cập nhật hoạt động
     * @return {@code true} nếu cập nhật thành công hoặc không có thay đổi cần lưu
     * @throws Exception nếu vi phạm điều kiện nghiệp vụ hoặc dữ liệu không hợp lệ
     */
    @Override
    public boolean updateActivity(ExtracurricularActivityRequestDTO dto) throws Exception {

        // Bước 1: Validate dữ liệu đầu vào (giữ nguyên phần validate).
        validateRequestDTO(dto, true);
        List<ActivityTargetParticipant> targetRows = targetParticipantService.buildTargetRows(
                dto.getTargetScope(),
                dto.getTargetDepartmentIds(),
                dto.getTargetCohortIds(),
                dto.getTargetMajorIds(),
                dto.getTargetProgramIds(),
                dto.getTargetClassIds()
        );

        // Bước 2: Chuyển RequestDTO thành Entity.
        ExtracurricularActivity entity = mapToEntity(dto);
        entity.setId(Integer.parseInt(dto.getId().trim()));

        // Bước 3: Lấy hoạt động hiện tại từ database.
        ExtracurricularActivity existing = activityDAO.findById(entity.getId());
        // Chặn chỉnh sửa nếu hoạt động đã bắt đầu diễn ra hoặc đã hoàn thành
        if (isLocked(existing)) {
            throw new IllegalArgumentException(LOCKED_MESSAGE);
        }
        // Báo lỗi nếu bản ghi hoạt động không còn tồn tại trong hệ thống
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động cần cập nhật.");
        }
        // Bảo toàn thông tin đối tác hiện có vì phần đối tác được quản lý ở luồng riêng
        if (existing != null) {
            entity.setPartnerCompanyId(existing.getPartnerCompanyId());
            entity.setPartnerStaffId(existing.getPartnerStaffId());
        }

        // Bước 4: Chỉ cho phép sửa khi hoạt động vẫn UPCOMING và chưa đến thời điểm bắt đầu.
        requireUpcomingActivity(
                entity.getId(),
                "Không thể sửa hoạt động khi hoạt động không còn ở trạng thái UPCOMING."
        );

        // Bước 5: Giữ nguyên thông tin đối tác vì thông tin này được quản lý riêng.
        entity.setPartnerCompanyId(existing.getPartnerCompanyId());
        entity.setPartnerStaffId(existing.getPartnerStaffId());

        // Kiểm tra ràng buộc chỉ tiêu: không cho giảm maxParticipants thấp hơn số chỗ đã được giữ
        if (!Objects.equals(existing.getMaxParticipants(), entity.getMaxParticipants())
                && entity.getMaxParticipants() != null
                && entity.getMaxParticipants() < activityDAO.countReservedApplications(entity.getId())) {
            throw new IllegalArgumentException(
                    "Không thể giảm số lượng tối đa thấp hơn số đơn đang chờ xử lý và đã duyệt đang giữ chỗ.");
        }

        // Khóa việc thay đổi tính chất hoạt động và yêu cầu form đăng ký khi đã phát sinh đơn đăng ký hiệu lực
        int activeApplicationCount = activityDAO.countActiveApplications(entity.getId());
        // Chỉ kiểm tra ràng buộc tính chất/form khi đã có ít nhất 1 sinh viên đăng ký
        if (activeApplicationCount > 0) {
            // Không cho đổi tính chất hoạt động (ví dụ REQUIRE_REGISTRATION -> MANDATORY) khi đã có đơn
            if (!Objects.equals(existing.getNature(), entity.getNature())) {
                throw new IllegalArgumentException(
                        "Không thể thay đổi tính chất hoạt động khi đã có sinh viên đăng ký.");
            }
            // Không cho bật/tắt yêu cầu biểu mẫu đăng ký khi đã có sinh viên nộp đơn
            if (existing.isRegistrationFormRequired() != entity.isRegistrationFormRequired()) {
                throw new IllegalArgumentException(
                        "Không thể thay đổi yêu cầu form đăng ký khi đã có sinh viên đăng ký.");
            }
        }

        // So sánh cấu hình đối tượng tham gia cũ và mới để phát hiện thay đổi
        List<ActivityTargetParticipant> existingTargets =
                targetParticipantService.getTargetsByActivityId(entity.getId());
        boolean targetsChanged = !sameTargets(existingTargets, targetRows);

        // Bước 6: Nếu nội dung và đối tượng mục tiêu đều không thay đổi thì không cần cập nhật lại DB (tránh mất trạng thái duyệt).
        if (!hasContentChanged(existing, entity) && !targetsChanged) {

            // Đồng bộ lại trạng thái công khai
            activityDAO.refreshPublic(entity.getId());

            return true;
        }

        // Bước 7: Kiểm tra hoạt động đã được duyệt trước khi sửa hay chưa.
        boolean wasApproved =
                existing.getApprovalStatus() == ApprovalStatus.APPROVED;

        // Bước 8: Cập nhật có điều kiện trong database (kiểm tra đồng thời trạng thái duyệt và thời gian bắt đầu).
        // Nếu đã được duyệt thì hạ approvalStatus về DRAFT để bắt buộc gửi duyệt lại; ngược lại truyền null để giữ nguyên.
        boolean updated = activityDAO.updateUpcoming(
                entity,
                existing.getApprovalStatus(),
                wasApproved ? ApprovalStatus.DRAFT : null,
                DateTimeUtil.nowUtc()
        );

        // Bước 9: Nếu cập nhật thất bại (do tranh chấp đồng thời hoặc vi phạm điều kiện), phân loại nguyên nhân và báo lỗi.
        if (!updated) {
            // Kiểm tra lại trường hợp số đơn giữ chỗ tăng lên đồng thời khiến maxParticipants không còn đủ
            if (!Objects.equals(existing.getMaxParticipants(), entity.getMaxParticipants())
                    && entity.getMaxParticipants() != null
                    && entity.getMaxParticipants() < activityDAO.countReservedApplications(entity.getId())) {
                throw new IllegalArgumentException(
                        "Không thể giảm số lượng tối đa thấp hơn số đơn đang chờ xử lý và đã duyệt đang giữ chỗ.");
            }

            throw new IllegalArgumentException(
                    "Không thể sửa hoạt động vì hoạt động đã bắt đầu "
                            + "hoặc trạng thái duyệt đã thay đổi. Vui lòng tải lại trang."
            );
        }

        // Bước 10: Lưu lại cấu hình đối tượng tham gia, dọn người tự động ghi danh (nếu bị hạ từ APPROVED) và đồng bộ kết quả
        targetParticipantService.saveTargetsForActivity(entity.getId(), targetRows);
        // Nếu hoạt động trước đó đã duyệt thì gỡ các sinh viên tự động ghi danh chưa từng điểm danh
        if (wasApproved) {
            targetParticipantService.removeUnattendedAutoEnrolledParticipants(entity.getId());
        }
        activityDAO.refreshPublic(entity.getId());
        participantDAO.syncActivityResults(entity.getId());

        // Gửi thông báo cho người tham gia khi thời gian bắt đầu, thời gian kết thúc hoặc địa điểm thay đổi
        if (!sameMinute(existing.getStartTime(), entity.getStartTime())
                || !sameMinute(existing.getEndTime(), entity.getEndTime())
                || !sameText(existing.getAddress(), entity.getAddress())) {
            notificationService.notifyActivityParticipants(entity.getId(), "ACTIVITY_SCHEDULE_CHANGED",
                    "Hoạt động thay đổi thời gian/địa điểm",
                    "Hoạt động \"" + entity.getName() + "\" vừa được cập nhật thời gian hoặc địa điểm. "
                            + "Vui lòng xem lại chi tiết.",
                    "/activities?action=detail&id=" + entity.getId());
        }

        return true;
    }

    /**
     * So sánh hai danh sách đối tượng mục tiêu có tương đương nhau về mặt tập hợp hay không.
     * Danh sách rỗng hoặc {@code null} được quy ước tương đương với cấu hình toàn trường (mọi khóa đều null).
     *
     * @param a danh sách đối tượng mục tiêu cũ
     * @param b danh sách đối tượng mục tiêu mới
     * @return {@code true} nếu hai tập đối tượng giống nhau; ngược lại {@code false}
     */
    private boolean sameTargets(List<ActivityTargetParticipant> a, List<ActivityTargetParticipant> b) {
        // Chuẩn hóa danh sách A: rỗng/null tương đương 1 dòng toàn trường, ngược lại đưa vào HashSet để bỏ qua thứ tự
        Set<ActivityTargetParticipant> setA = a == null || a.isEmpty()
                ? Set.of(new ActivityTargetParticipant(null, null, null, null, null))
                : new HashSet<>(a);
        // Chuẩn hóa danh sách B tương tự như A trước khi so sánh bằng nhau
        Set<ActivityTargetParticipant> setB = b == null || b.isEmpty()
                ? Set.of(new ActivityTargetParticipant(null, null, null, null, null))
                : new HashSet<>(b);
        return setA.equals(setB);
    }

    /**
     * Kiểm tra xem các trường nội dung nghiệp vụ giữa hoạt động cũ và mới có khác biệt hay không.
     *
     * @param a bản ghi hoạt động hiện tại trong DB
     * @param b bản ghi hoạt động sau khi map từ form cập nhật
     * @return {@code true} nếu có ít nhất một trường nội dung thay đổi; ngược lại {@code false}
     */
    private boolean hasContentChanged(ExtracurricularActivity a, ExtracurricularActivity b) {
        return a.getSemesterId() != b.getSemesterId()
                || a.getActivityTypeId() != b.getActivityTypeId()
                || a.getOrganizationLevelId() != b.getOrganizationLevelId()
                || a.getResponsibleDepartmentId() != b.getResponsibleDepartmentId()
                || a.getResponsibleStaffId() != b.getResponsibleStaffId()
                || a.isRegistrationFormRequired() != b.isRegistrationFormRequired()
                || !sameText(a.getCode(), b.getCode())
                || !sameText(a.getName(), b.getName())
                || !sameText(a.getAddress(), b.getAddress())
                || !sameText(a.getDescription(), b.getDescription())
                || a.getNature() != b.getNature()
                || a.isSummaryRequired() != b.isSummaryRequired()
                || !Objects.equals(a.getMaxParticipants(), b.getMaxParticipants())
                || a.getAttendanceSessionCount() != b.getAttendanceSessionCount()
                || !sameMinute(a.getStartTime(), b.getStartTime())
                || !sameMinute(a.getEndTime(), b.getEndTime());
    }

    /**
     * So sánh hai chuỗi văn bản sau khi đã loại bỏ khoảng trắng đầu/cuối và xử lý an toàn với {@code null}.
     *
     * @param x chuỗi thứ nhất
     * @param y chuỗi thứ hai
     * @return {@code true} nếu hai chuỗi giống nhau sau khi trim
     */
    private boolean sameText(String x, String y) {
        // Chỉ trim khi chuỗi khác null để tránh NullPointerException
        return Objects.equals(x == null ? null : x.trim(), y == null ? null : y.trim());
    }

    /**
     * So sánh hai mốc thời gian đến độ chính xác cấp phút (bỏ qua phần giây/nano giây).
     *
     * @param x mốc thời gian thứ nhất
     * @param y mốc thời gian thứ hai
     * @return {@code true} nếu hai mốc thời gian trùng nhau đến từng phút
     */
    private boolean sameMinute(LocalDateTime x, LocalDateTime y) {
        // Cắt phần giây/nano giây nếu mốc thời gian khác null trước khi so sánh
        return Objects.equals(
                x == null ? null : x.truncatedTo(ChronoUnit.MINUTES),
                y == null ? null : y.truncatedTo(ChronoUnit.MINUTES));
    }

    /**
     * Xóa một hoạt động ngoại khóa nếu hoạt động chưa diễn ra và chưa hoàn thành.
     * Trước khi xóa sẽ gửi thông báo hủy hoạt động đến các người tham gia liên quan.
     *
     * @param id mã định danh hoạt động cần xóa
     * @return {@code true} nếu xóa thành công; {@code false} nếu {@code id <= 0}
     * @throws IllegalArgumentException nếu hoạt động đang diễn ra hoặc đã hoàn thành nên không thể xóa
     */
    @Override
    public boolean deleteActivity(int id) {
        // Không xử lý nếu ID không hợp lệ
        if (id <= 0) {
            return false;
        }

        // Kiểm tra trước điều kiện cho phép xóa để gửi thông báo trước khi dữ liệu tham gia bị xóa theo cascade
        ExtracurricularActivity existing = activityDAO.findById(id);
        boolean mayDelete = existing != null
                && existing.getActivityStatus() != ActivityStatus.ONGOING
                && existing.getActivityStatus() != ActivityStatus.COMPLETED;
        // Gửi thông báo hủy hoạt động đến người tham gia khi hoạt động đủ điều kiện xóa
        if (mayDelete) {
            notificationService.notifyActivityParticipants(id, "ACTIVITY_DELETED",
                    "Hoạt động đã bị hủy",
                    "Hoạt động \"" + existing.getName() + "\" đã bị xóa.", "/activities");
        }
        boolean deleted = activityDAO.deleteIfNotOngoingOrCompleted(id);

        // Nếu câu lệnh xóa có điều kiện không ảnh hưởng dòng nào -> hoạt động đang diễn ra/đã hoàn thành
        if (!deleted) {
            throw new IllegalArgumentException(
                    "Không thể xóa hoạt động đang diễn ra hoặc đã hoàn thành."
            );
        }

        return true;
    }

    /**
     * Kiểm tra toàn bộ ràng buộc hợp lệ của dữ liệu biểu mẫu tạo mới hoặc cập nhật hoạt động:
     * trường bắt buộc, trùng mã hoạt động, quan hệ phòng ban - nhân sự phụ trách, tính chất,
     * số lượng tối đa, số buổi điểm danh và khoảng thời gian nằm trong học kỳ.
     *
     * @param dto      dữ liệu đầu vào từ form
     * @param isUpdate {@code true} nếu đang cập nhật (bỏ qua chính bản ghi hiện tại khi kiểm tra trùng mã);
     *                 {@code false} nếu đang tạo mới
     * @throws Exception nếu bất kỳ quy tắc kiểm tra nào không thỏa mãn
     */
    private void validateRequestDTO(ExtracurricularActivityRequestDTO dto, boolean isUpdate) throws Exception {
        // Từ chối nếu đối tượng request DTO bị null
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu không hợp lệ: ExtracurricularActivityRequestDTO rỗng.");
        }

        // Khi cập nhật bắt buộc phải có ID hoạt động hợp lệ
        if (isUpdate && (dto.getId() == null || dto.getId().trim().isEmpty())) {
            throw new IllegalArgumentException("ID hoạt động không hợp lệ khi cập nhật.");
        }

        // Validate các trường bắt buộc (Not Null)
        // Kiểm tra mã hoạt động không được để trống
        if (isEmpty(dto.getCode())) {
            throw new IllegalArgumentException("Mã hoạt động không được để trống.");
        }
        String code = dto.getCode().trim();
        // Nếu đang cập nhật thì loại trừ chính ID đang sửa khỏi bước kiểm tra trùng mã
        int excludedActivityId = isUpdate ? Integer.parseInt(dto.getId()) : 0;
        // Duyệt các hoạt động có cùng mã để xem có bản ghi nào khác ID hiện tại bị trùng không
        boolean duplicateCode = activityDAO.findByCode(code).stream()
                .anyMatch(activity -> activity.getId() != excludedActivityId);
        // Báo lỗi nếu mã hoạt động đã được sử dụng bởi hoạt động khác
        if (duplicateCode) {
            throw new IllegalArgumentException("Đã tồn tại hoạt động ngoại khóa với mã này.");
        }
        // Kiểm tra tên hoạt động bắt buộc nhập
        if (isEmpty(dto.getName())) {
            throw new IllegalArgumentException("Tên hoạt động không được để trống.");
        }
        // Kiểm tra học kỳ bắt buộc chọn
        if (isEmpty(dto.getSemesterId())) {
            throw new IllegalArgumentException("Vui lòng chọn học kỳ.");
        }
        // Kiểm tra loại hoạt động bắt buộc chọn
        if (isEmpty(dto.getActivityTypeId())) {
            throw new IllegalArgumentException("Vui lòng chọn loại hoạt động.");
        }
        // Kiểm tra cấp tổ chức bắt buộc chọn và phải tồn tại trong DB
        if (isEmpty(dto.getOrganizationLevelId())) {
            throw new IllegalArgumentException("Vui lòng chọn cấp tổ chức.");
        }
        int organizationLevelId = parseRequiredId(
                dto.getOrganizationLevelId(), "Cấp tổ chức không hợp lệ.");
        // Báo lỗi nếu cấp tổ chức không tồn tại trong hệ thống
        if (organizationLevelDAO.findById(organizationLevelId) == null) {
            throw new IllegalArgumentException("Cấp tổ chức không tồn tại.");
        }
        // Kiểm tra phòng ban phụ trách bắt buộc chọn
        if (isEmpty(dto.getResponsibleDepartmentId())) {
            throw new IllegalArgumentException("Vui lòng chọn phòng ban phụ trách.");
        }
        // Kiểm tra nhân viên phụ trách bắt buộc chọn
        if (isEmpty(dto.getResponsibleStaffId())) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên phụ trách.");
        }
        int responsibleDepartmentId = parseRequiredId(
                dto.getResponsibleDepartmentId(), "Đơn vị phụ trách không hợp lệ.");
        int responsibleStaffId = parseRequiredId(
                dto.getResponsibleStaffId(), "Nhân viên phụ trách không hợp lệ.");
        // Đảm bảo nhân viên phụ trách thực sự trực thuộc phòng ban phụ trách đã chọn
        if (!activityStaffDAO.isStaffInDepartment(responsibleStaffId, responsibleDepartmentId)) {
            throw new IllegalArgumentException("Nhân viên phụ trách phải thuộc đơn vị phụ trách đã chọn.");
        }

        // Kiểm tra tính chất hoạt động không rỗng và khớp giá trị enum ActivityNature
        if (isEmpty(dto.getNature())) {
            throw new IllegalArgumentException("Vui lòng chọn tính chất hoạt động.");
        }
        try {
            ActivityNature.valueOf(dto.getNature().trim());
        } catch (IllegalArgumentException e) {
            // Chuyển ngoại lệ parse enum thành thông báo tiếng Việt thân thiện
            throw new IllegalArgumentException("Tính chất hoạt động không hợp lệ.");
        }

        // Chỉ kiểm tra số lượng tham gia tối đa khi người dùng có nhập (để trống nghĩa là không giới hạn)
        if (!isEmpty(dto.getMaxParticipants())) {
            try {
                // Yêu cầu chỉ tiêu tối đa phải là số nguyên dương
                if (Integer.parseInt(dto.getMaxParticipants().trim()) <= 0) {
                    throw new IllegalArgumentException("Số lượng tham gia tối đa phải lớn hơn 0.");
                }
            } catch (NumberFormatException e) {
                // Chuỗi nhập vào không phải số nguyên hợp lệ
                throw new IllegalArgumentException("Số lượng tham gia tối đa không hợp lệ.");
            }
        }

        // Kiểm tra số buổi điểm danh bắt buộc nhập và nằm trong khoảng [1, 50]
        if (isEmpty(dto.getAttendanceSessionCount())) {
            throw new IllegalArgumentException("Vui lòng nhập số buổi điểm danh.");
        }
        try {
            int sessionCount = Integer.parseInt(dto.getAttendanceSessionCount().trim());
            // Giới hạn nghiệp vụ số buổi điểm danh tối thiểu 1 và tối đa 50 buổi
            if (sessionCount < 1 || sessionCount > 50) {
                throw new IllegalArgumentException("Số buổi điểm danh phải từ 1 đến 50 buổi.");
            }
        } catch (NumberFormatException e) {
            // Chuỗi số buổi điểm danh không parse được sang số nguyên
            throw new IllegalArgumentException("Số buổi điểm danh không hợp lệ.");
        }

        // Validate khoảng thời gian: bắt buộc nhập, StartTime phải ở tương lai, trước EndTime và nằm trong học kỳ
        if (isEmpty(dto.getStartTime())) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được để trống.");
        }
        // Kiểm tra thời gian kết thúc bắt buộc nhập
        if (isEmpty(dto.getEndTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc không được để trống.");
        }
        try {
            LocalDateTime start = LocalDateTime.parse(dto.getStartTime().trim());
            LocalDateTime end = LocalDateTime.parse(dto.getEndTime().trim());

            // Không cho phép tạo/cập nhật hoạt động có thời gian bắt đầu ở quá khứ hoặc hiện tại
            if (!start.isAfter(DateTimeUtil.now())) {
                throw new IllegalArgumentException(
                        "Thời gian bắt đầu hoạt động phải ở tương lai.");
            }
            // Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc.");
            }
            Semester semester = semesterDAO.findById(parseRequiredId(dto.getSemesterId(), "Học kỳ không hợp lệ."));
            // Đảm bảo học kỳ đã chọn thực sự tồn tại trong DB
            if (semester == null) {
                throw new IllegalArgumentException("Học kỳ không tồn tại.");
            }
            // Thời gian bắt đầu hoạt động không được sớm hơn thời gian bắt đầu của học kỳ
            if (semester.getTimeStart() != null && start.isBefore(semester.getTimeStart())) {
                throw new IllegalArgumentException(
                        "Thời gian bắt đầu hoạt động phải sau thời gian bắt đầu của học kỳ.");
            }
            // Thời gian bắt đầu hoạt động không được muộn hơn thời gian kết thúc của học kỳ
            if (semester.getTimeEnd() != null && start.isAfter(semester.getTimeEnd())) {
                throw new IllegalArgumentException(
                        "Thời gian bắt đầu hoạt động phải trước thời gian kết thúc của học kỳ.");
            }
        } catch (DateTimeParseException e) {
            // Chuỗi thời gian không đúng định dạng ISO LocalDateTime
            throw new IllegalArgumentException("Định dạng ngày/giờ không hợp lệ.");
        }
    }

    /**
     * Chuyển chuỗi ID bắt buộc sang số nguyên dương; ném {@link IllegalArgumentException} nếu không hợp lệ.
     *
     * @param value        chuỗi ID cần chuyển đổi
     * @param errorMessage thông báo lỗi nghiệp vụ nếu chuỗi không phải số nguyên dương
     * @return giá trị ID nguyên dương
     */
    private int parseRequiredId(String value, String errorMessage) {
        try {
            int id = Integer.parseInt(value.trim());
            // Chỉ chấp nhận giá trị ID lớn hơn 0
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Bỏ qua ngoại lệ parse để ném thống nhất thành IllegalArgumentException bên dưới
        }
        throw new IllegalArgumentException(errorMessage);
    }

    /**
     * Chuyển đổi dữ liệu từ {@link ExtracurricularActivityRequestDTO} sang entity {@link ExtracurricularActivity}
     * kèm chuẩn hóa các cờ mặc định (số buổi điểm danh, yêu cầu biểu mẫu, trạng thái công khai).
     *
     * @param dto đối tượng DTO đã được validate
     * @return entity {@link ExtracurricularActivity} sẵn sàng lưu xuống DB
     */
    private ExtracurricularActivity mapToEntity(ExtracurricularActivityRequestDTO dto) {
        ExtracurricularActivity entity = new ExtracurricularActivity();

        entity.setCode(dto.getCode().trim());
        entity.setName(dto.getName().trim());
        // Loại bỏ khoảng trắng thừa cho địa chỉ và mô tả nếu có giá trị; giữ null nếu không nhập
        entity.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        entity.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        // Chuyển các khóa ngoại từ String sang int
        entity.setSemesterId(Integer.parseInt(dto.getSemesterId()));
        entity.setActivityTypeId(Integer.parseInt(dto.getActivityTypeId()));
        entity.setOrganizationLevelId(Integer.parseInt(dto.getOrganizationLevelId().trim()));
        entity.setResponsibleDepartmentId(Integer.parseInt(dto.getResponsibleDepartmentId()));
        entity.setResponsibleStaffId(Integer.parseInt(dto.getResponsibleStaffId()));

        entity.setNature(ActivityNature.valueOf(dto.getNature().trim()));
        entity.setSummaryRequired("true".equalsIgnoreCase(dto.getSummaryRequired()));
        // Để null nếu không giới hạn số lượng tham gia; ngược lại chuyển sang Integer
        entity.setMaxParticipants(isEmpty(dto.getMaxParticipants()) ? null : Integer.valueOf(dto.getMaxParticipants().trim()));
        // Mặc định 1 buổi điểm danh nếu dữ liệu rỗng; ngược lại lấy giá trị đã nhập
        entity.setAttendanceSessionCount(
                isEmpty(dto.getAttendanceSessionCount()) ? 1 : Integer.parseInt(dto.getAttendanceSessionCount().trim()));

        // Chuyển đổi chuỗi thời gian từ <input type="datetime-local"> (định dạng yyyy-MM-ddTHH:mm) sang LocalDateTime
        if (!isEmpty(dto.getStartTime())) {
            entity.setStartTime(LocalDateTime.parse(dto.getStartTime().trim()));
        }
        // Chuyển đổi thời gian kết thúc nếu có nhập
        if (!isEmpty(dto.getEndTime())) {
            entity.setEndTime(LocalDateTime.parse(dto.getEndTime().trim()));
        }
        // Ép cờ yêu cầu biểu mẫu đăng ký về false nếu tính chất hoạt động không phải REQUIRE_REGISTRATION
        boolean requiresForm = "true".equalsIgnoreCase(dto.getRegistrationFormRequired());
        if (entity.getNature() != ActivityNature.REQUIRE_REGISTRATION) {
            requiresForm = false;
        }
        entity.setRegistrationFormRequired(requiresForm);
        // Mặc định công khai (true) khi tham số isPublic không được truyền lên
        entity.setPublic(dto.getIsPublic() == null || dto.getIsPublic().isEmpty() ? true : "true".equalsIgnoreCase(dto.getIsPublic()));

        return entity;
    }

    /**
     * Kiểm tra chuỗi có bị {@code null} hoặc chỉ chứa khoảng trắng hay không.
     *
     * @param str chuỗi cần kiểm tra
     * @return {@code true} nếu chuỗi null hoặc rỗng sau khi trim
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Gửi hoạt động từ trạng thái DRAFT hoặc REJECTED sang PENDING để chờ phê duyệt
     * và gửi thông báo yêu cầu duyệt cho quản trị viên.
     *
     * @param id mã định danh hoạt động
     * @return {@code true} nếu gửi duyệt thành công; {@code false} nếu không tìm thấy hoạt động
     */
    @Override
    public boolean submitForApproval(int id) {
        ExtracurricularActivity submitted = changeApprovalStatus(id, ApprovalStatus.PENDING,
                "Only draft or rejected activities can be submitted for approval.",
                ApprovalStatus.DRAFT, ApprovalStatus.REJECTED);
        // Chỉ gửi thông báo yêu cầu phê duyệt khi chuyển trạng thái thành công
        if (submitted != null) {
            notificationService.notifyApprovalRequest(id, submitted.getName());
        }
        return submitted != null;
    }

    /**
     * Phê duyệt hoạt động đang ở trạng thái PENDING sang APPROVED.
     * Sau khi duyệt thành công sẽ tự động đảm bảo form đăng ký nhanh (nếu cần), làm mới cờ công khai,
     * kích hoạt tự động ghi danh cho hoạt động bắt buộc (MANDATORY) và gửi các thông báo liên quan.
     *
     * @param id mã định danh hoạt động
     * @return {@code true} nếu phê duyệt thành công; {@code false} nếu không tìm thấy hoạt động
     */
    @Override
    public boolean approveActivity(int id) {
        ExtracurricularActivity approved = changeApprovalStatus(id, ApprovalStatus.APPROVED,
                "Only activities pending approval can be approved.",
                ApprovalStatus.PENDING);
        // Thực hiện các tác vụ hậu phê duyệt khi trạng thái đã chuyển sang APPROVED thành công
        if (approved != null) {
            // Tự động tạo/mở form đăng ký nhanh (nếu hoạt động không dùng form tùy chỉnh) và ghi nhận chuyển đổi trạng thái
            IRegistrationFormDAO.FormStatusTransition quickTransition =
                    new com.swp391.g1.dao.impl.RegistrationFormDAOImpl().ensureQuickFormAndGetTransition(id);
            activityDAO.refreshPublic(id);
            // Nếu hoạt động mang tính chất bắt buộc thì đưa vào hàng đợi tự động ghi danh sinh viên đủ điều kiện
            if (approved.getNature() == ActivityNature.MANDATORY) {
                targetParticipantService.enqueueMandatoryAutoEnrollment(id);
            }

            // Đọc lại bản ghi mới nhất sau khi refreshPublic để có cờ isPublic chính xác khi gửi thông báo
            ExtracurricularActivity latest = activityDAO.findById(id);
            // Fallback về đối tượng approved trước đó nếu truy vấn lại trả về null
            ExtracurricularActivity current = latest != null ? latest : approved;
            notificationService.notifyActivityApproved(id, current.getName());
            notificationService.notifyStudentsActivityApproved(
                    id, current.getName(), current.getNature(), current.isPublic());
            // Nếu form đăng ký nhanh vừa chuyển trạng thái (ví dụ sang OPENING) thì gửi thông báo cho sinh viên
            if (quickTransition != null) {
                boolean canOpenDetail = current.getApprovalStatus() == ApprovalStatus.APPROVED && current.isPublic();
                notificationService.notifyRegistrationFormStatusChanged(
                        id,
                        quickTransition.formId(),
                        current.getName(),
                        quickTransition.newStatus(),
                        canOpenDetail);
            }
        }
        return approved != null;
    }

    /**
     * Từ chối hoạt động đang ở trạng thái PENDING sang REJECTED và gửi thông báo từ chối.
     *
     * @param id mã định danh hoạt động
     * @return {@code true} nếu từ chối thành công; {@code false} nếu không tìm thấy hoạt động
     */
    @Override
    public boolean rejectActivity(int id) {
        ExtracurricularActivity rejected = changeApprovalStatus(id, ApprovalStatus.REJECTED,
                "Only activities pending approval can be rejected.",
                ApprovalStatus.PENDING);
        // Gửi thông báo khi hoạt động đã chuyển sang trạng thái bị từ chối
        if (rejected != null) {
            notificationService.notifyActivityRejected(id, rejected.getName());
        }
        return rejected != null;
    }

    /**
     * Hủy quyết định phê duyệt của một hoạt động đang ở trạng thái APPROVED và UPCOMING,
     * đưa hoạt động về lại PENDING, gỡ các sinh viên tự động ghi danh chưa điểm danh và gửi thông báo.
     *
     * @param id mã định danh hoạt động
     * @return {@code true} nếu hủy duyệt thành công; {@code false} nếu không tìm thấy hoạt động hoặc ID không hợp lệ
     * @throws IllegalArgumentException nếu hoạt động không ở trạng thái APPROVED/UPCOMING hoặc đã bắt đầu
     */
    @Override
    public boolean revokeApproval(int id) {
        // Bỏ qua nếu ID không hợp lệ
        if (id <= 0) {
            return false;
        }
        ExtracurricularActivity activity = activityDAO.findById(id);
        // Không tìm thấy bản ghi hoạt động
        if (activity == null) {
            return false;
        }
        // Chỉ cho phép hủy duyệt khi hoạt động đang ở trạng thái APPROVED
        if (activity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Chỉ có thể hủy duyệt hoạt động đã được duyệt.");
        }
        // Không cho hủy duyệt nếu hoạt động đã diễn ra, kết thúc hoặc bị hủy
        if (activity.getActivityStatus() != ActivityStatus.UPCOMING) {
            throw new IllegalArgumentException(
                    "Không thể hủy duyệt vì hoạt động đã diễn ra, đã kết thúc hoặc đã bị hủy.");
        }

        // Cập nhật có điều kiện trong DB để đảm bảo nguyên tử khi kiểm tra đồng thời thời gian bắt đầu
        boolean updated = activityDAO.updateApprovalStatusIfUpcoming(
                id,
                ApprovalStatus.PENDING,
                List.of(ApprovalStatus.APPROVED),
                DateTimeUtil.nowUtc()
        );

        // Nếu không cập nhật được dòng nào -> hoạt động đã bắt đầu hoặc trạng thái đã thay đổi đồng thời
        if (!updated) {
            throw new IllegalArgumentException(
                    "Không thể hủy duyệt: hoạt động đã bắt đầu "
                            + "hoặc trạng thái duyệt đã thay đổi."
            );
        }

        targetParticipantService.removeUnattendedAutoEnrolledParticipants(id);
        notificationService.notifyApprovalRevoked(id, activity.getName());
        return true;
    }

    /**
     * Hủy quyết định từ chối của hoạt động đang ở trạng thái REJECTED để đưa về PENDING
     * và gửi thông báo tương ứng.
     *
     * @param id mã định danh hoạt động
     * @return {@code true} nếu hủy từ chối thành công; {@code false} nếu không tìm thấy hoạt động
     */
    @Override
    public boolean revokeRejection(int id) {
        ExtracurricularActivity revoked = changeApprovalStatus(id, ApprovalStatus.PENDING,
                "Chỉ có thể hủy từ chối hoạt động đang ở trạng thái bị từ chối.",
                ApprovalStatus.REJECTED);
        // Gửi thông báo khi hủy từ chối thành công
        if (revoked != null) {
            notificationService.notifyRejectionRevoked(id, revoked.getName());
        }
        return revoked != null;
    }

    /**
     * Hàm dùng chung để chuyển trạng thái phê duyệt của hoạt động một cách an toàn khi hoạt động vẫn UPCOMING.
     * Kiểm tra trạng thái nguồn hợp lệ trước khi thực thi câu lệnh cập nhật có điều kiện trong DB.
     *
     * @param id           mã định danh hoạt động
     * @param target       trạng thái phê duyệt đích cần chuyển tới
     * @param errorMessage thông báo lỗi nếu trạng thái hiện tại không nằm trong {@code allowedFrom}
     * @param allowedFrom  danh sách trạng thái phê duyệt nguồn được phép chuyển đổi
     * @return đối tượng {@link ExtracurricularActivity} trước khi cập nhật nếu thành công; {@code null} nếu không tìm thấy
     */
    private ExtracurricularActivity changeApprovalStatus(
            int id,
            ApprovalStatus target,
            String errorMessage,
            ApprovalStatus... allowedFrom) {

        // Trả về null ngay nếu ID không hợp lệ
        if (id <= 0) {
            return null;
        }

        // Bước 1: Lấy hoạt động từ database.
        ExtracurricularActivity activity = activityDAO.findById(id);

        // Không tìm thấy hoạt động tương ứng trong DB
        if (activity == null) {
            return null;
        }

        // Bước 2: Kiểm tra trạng thái duyệt hiện tại có nằm trong danh sách trạng thái nguồn cho phép không.
        if (!Arrays.asList(allowedFrom).contains(activity.getApprovalStatus())) {
            throw new IllegalArgumentException(errorMessage);
        }

        // Bước 3: Chỉ cho phép đổi trạng thái duyệt khi hoạt động UPCOMING.
        if (activity.getActivityStatus() != ActivityStatus.UPCOMING) {
            throw new IllegalArgumentException(
                    "Không thể thay đổi trạng thái duyệt vì hoạt động "
                            + "không còn ở trạng thái UPCOMING.");
        }

        // Bước 4: Cập nhật có điều kiện trực tiếp trong database (kiểm tra trạng thái nguồn và mốc thời gian UTC).
        boolean updated = activityDAO.updateApprovalStatusIfUpcoming(
                id,
                target,
                Arrays.asList(allowedFrom),
                DateTimeUtil.nowUtc()
        );

        // Bước 5: Nếu database không cập nhật được do thay đổi đồng thời hoặc đã quá giờ bắt đầu, báo lỗi.
        if (!updated) {
            throw new IllegalArgumentException(
                    "Không thể thay đổi trạng thái duyệt: hoạt động đã bắt đầu "
                            + "hoặc trạng thái đã thay đổi. Vui lòng tải lại trang."
            );
        }

        return activity;
    }

    /**
     * Đồng bộ trạng thái hoạt động (UPCOMING -> ONGOING -> COMPLETED) trong database theo thời gian UTC hiện tại.
     */
    @Override
    public void synchronizeActivityStatuses() {

        LocalDateTime now = DateTimeUtil.nowUtc();

        // Gọi DAO cập nhật trạng thái trong database.
        activityDAO.synchronizeActivityStatuses(now);
    }

    /**
     * Xác nhận hoạt động tồn tại, vẫn đang ở trạng thái UPCOMING và chưa chạm tới thời điểm bắt đầu.
     * Ném {@link IllegalArgumentException} để chặn mọi thao tác chỉnh sửa khi điều kiện không còn thỏa mãn.
     *
     * @param id           mã định danh hoạt động cần kiểm tra
     * @param errorMessage thông báo lỗi nếu hoạt động không còn ở trạng thái sắp diễn ra
     * @return đối tượng {@link ExtracurricularActivity} hợp lệ
     */
    private ExtracurricularActivity requireUpcomingActivity(
            int id, String errorMessage) {

        // Kiểm tra ID hợp lệ trước khi truy vấn
        if (id <= 0) {
            throw new IllegalArgumentException("ID hoạt động không hợp lệ.");
        }

        ExtracurricularActivity activity = activityDAO.findById(id);

        // Chặn thao tác nếu hoạt động không tồn tại
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }

        // Chặn thao tác nếu trạng thái trong DB không còn là UPCOMING
        if (activity.getActivityStatus() != ActivityStatus.UPCOMING) {
            throw new IllegalArgumentException(errorMessage);
        }

        // Chặn thao tác nếu thiếu mốc bắt đầu hoặc thời gian UTC hiện tại đã đến/vượt quá thời điểm bắt đầu
        if (activity.getStartTime() == null
                || !DateTimeUtil.nowUtc().isBefore(activity.getStartTime())) {
            throw new IllegalArgumentException(errorMessage);
        }

        return activity;
    }
}
