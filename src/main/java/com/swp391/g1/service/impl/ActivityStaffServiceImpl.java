package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityDepartmentDAO;
import com.swp391.g1.dao.IActivityStaffDAO;
import com.swp391.g1.dao.IDepartmentDAO;
import com.swp391.g1.dao.IExtracurricularActivityDAO;
import com.swp391.g1.dao.IStaffDAO;
import com.swp391.g1.dao.impl.ActivityDepartmentDAOImpl;
import com.swp391.g1.dao.impl.ActivityStaffDAOImpl;
import com.swp391.g1.dao.impl.DepartmentDAOImpl;
import com.swp391.g1.dao.impl.ExtracurricularActivityDAOImpl;
import com.swp391.g1.dao.impl.StaffDAOImpl;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityDepartmentResponseDTO;
import com.swp391.g1.dto.response.ActivityStaffResponseDTO;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.model.Enum.ActivityStatus;
import com.swp391.g1.model.Staff;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityStaffService;
import com.swp391.g1.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lớp triển khai dịch vụ quản lý phân công nhân sự nội bộ và đơn vị đồng phụ trách cho hoạt động ngoại khóa.
 * Đảm bảo các quy tắc phân quyền, điều kiện thời gian (trước khi hoạt động bắt đầu) và phạm vi đơn vị hợp lệ.
 */
public class ActivityStaffServiceImpl implements IActivityStaffService {

    private final IActivityStaffDAO activityStaffDAO;
    private final IExtracurricularActivityDAO activityDAO;
    private final IStaffDAO staffDAO;
    private final IActivityDepartmentDAO activityDepartmentDAO;
    private final IDepartmentDAO departmentDAO;

    /**
     * Khởi tạo dịch vụ cùng các DAO phụ trợ liên quan tới nhân sự, hoạt động và đơn vị.
     */
    public ActivityStaffServiceImpl() {
        this.activityStaffDAO = new ActivityStaffDAOImpl();
        this.activityDAO = new ExtracurricularActivityDAOImpl();
        this.staffDAO = new StaffDAOImpl();
        this.activityDepartmentDAO = new ActivityDepartmentDAOImpl();
        this.departmentDAO = new DepartmentDAOImpl();
    }

    /**
     * Lấy danh sách nhân sự đã được phân công vào một hoạt động cụ thể.
     *
     * @param activityId ID hoạt động cần tra cứu
     * @return Danh sách DTO nhân sự đã phân công (rỗng nếu ID không hợp lệ)
     */
    @Override
    public List<ActivityStaffResponseDTO> getStaffsByActivityId(int activityId) {
        // Trả về danh sách rỗng nếu ID hoạt động không dương
        if (activityId <= 0) {
            return List.of();
        }
        return activityStaffDAO.findStaffsByActivityId(activityId);
    }

    /**
     * Lấy danh sách nhân sự đủ điều kiện để phân công thêm vào hoạt động.
     * Chỉ lấy nhân sự thuộc đơn vị phụ trách chính hoặc đơn vị đồng phụ trách đang hoạt động,
     * đồng thời loại bỏ nhân sự phụ trách chính và các nhân sự đã được phân công trước đó.
     *
     * @param activityId ID hoạt động cần tìm nhân sự khả dụng
     * @return Danh sách nhân sự có thể thêm vào hoạt động
     */
    @Override
    public List<Staff> getAvailableStaffsForActivity(int activityId) {
        // Trả về rỗng nếu ID hoạt động không hợp lệ
        if (activityId <= 0) {
            return List.of();
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        // Trả về rỗng nếu không tìm thấy hoạt động trong DB
        if (activity == null) {
            return List.of();
        }
        // Gom nhân sự từ tất cả đơn vị đang hoạt động của hoạt động, dùng LinkedHashMap để giữ thứ tự và khử trùng theo staffId
        Map<Integer, Staff> deptStaffs = new LinkedHashMap<>();
        // Duyệt từng ID đơn vị phụ trách / đồng phụ trách đang ACTIVE của hoạt động
        for (int deptId : getActiveActivityDepartmentIds(activity)) {
            // Duyệt từng nhân sự thuộc đơn vị đó và đưa vào map nếu chưa tồn tại
            for (Staff s : staffDAO.findByDepartmentId(deptId)) {
                deptStaffs.putIfAbsent(s.getId(), s);
            }
        }
        List<ActivityStaffResponseDTO> assigned = activityStaffDAO.findStaffsByActivityId(activityId);
        // Thu thập tập ID các nhân sự đã được phân công để loại trừ
        Set<Integer> assignedStaffIds = assigned.stream()
                .map(ActivityStaffResponseDTO::getStaffId)
                .collect(Collectors.toSet());

        // Lọc ra các nhân sự chưa phải người phụ trách chính và chưa có trong danh sách đã phân công
        return deptStaffs.values().stream()
                .filter(s -> s.getId() != activity.getResponsibleStaffId())
                .filter(s -> !assignedStaffIds.contains(s.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy bản đồ ánh xạ từ {@code staffId} sang chuỗi tên các đơn vị liên quan đến hoạt động.
     *
     * @param activityId ID hoạt động
     * @return Map {@code staffId -> "CODE - Name, ..."} (rỗng nếu ID không hợp lệ)
     */
    @Override
    public Map<Integer, String> getDepartmentLabelsByStaff(int activityId) {
        // Chỉ truy vấn DAO khi ID hoạt động dương, ngược lại trả về Map rỗng
        return activityId <= 0 ? Map.of() : activityStaffDAO.findDepartmentLabelsByStaff(activityId);
    }

    /**
     * Kiểm tra người dùng hiện tại có quyền quản lý phân công nhân sự của hoạt động hay không
     * (không xét yếu tố thời gian diễn ra hoạt động).
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại trong phiên đăng nhập
     * @return {@code true} nếu có quyền theo chính sách chung hoặc là nhân sự phụ trách chính của hoạt động
     */
    @Override
    public boolean canManageStaffAssignments(int activityId, CurrentUser user) {
        // Không cấp quyền khi ID hoạt động không hợp lệ hoặc chưa đăng nhập
        if (activityId <= 0 || user == null) {
            return false;
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        // Không cấp quyền nếu hoạt động không tồn tại
        if (activity == null) {
            return false;
        }
        // Cấp quyền nếu vai trò người dùng được phép phân công theo AccessPolicy hoặc khớp trực tiếp ID nhân sự phụ trách chính
        if (AccessPolicy.canAssignStaffToActivity(user)
                || user.getId() == activity.getResponsibleStaffId()) {
            return true;
        }
        // Fallback: tra cứu bản ghi Staff theo accountId (user.getId()) để so sánh với responsibleStaffId
        Staff currentStaff = staffDAO.findByAccountId(user.getId());
        return currentStaff != null
                && currentStaff.getId() == activity.getResponsibleStaffId();
    }

    /**
     * Kiểm tra người dùng hiện tại có được phép thêm/gỡ phân công nhân sự ngay tại thời điểm này không.
     * Yêu cầu đồng thời có quyền quản lý và hoạt động chưa bắt đầu.
     *
     * @param activityId ID hoạt động
     * @param user       Người dùng hiện tại
     * @return {@code true} nếu được phép thao tác phân công
     */
    @Override
    public boolean canAssignStaff(int activityId, CurrentUser user) {
        return canManageStaffAssignments(activityId, user)
                && isActivityBeforeStart(activityDAO.findById(activityId));
    }

    /**
     * Phân công một nhân sự vào hoạt động sau khi kiểm tra đầy đủ điều kiện thời gian, phân quyền,
     * trạng thái nhân sự, thuộc đơn vị liên quan và chưa bị trùng phân công.
     *
     * @param activityId ID hoạt động
     * @param staffId    ID nhân sự cần phân công
     * @param user       Người dùng thực hiện thao tác
     * @return {@code true} nếu phân công thành công
     * @throws Exception Nếu vi phạm bất kỳ quy tắc nghiệp vụ hoặc phân quyền nào
     */
    @Override
    public boolean assignStaff(int activityId, int staffId, CurrentUser user) throws Exception {
        // Kiểm tra tính hợp lệ của ID hoạt động và ID nhân sự
        if (activityId <= 0 || staffId <= 0) {
            throw new IllegalArgumentException("Thông tin phân công không hợp lệ.");
        }
        // Bắt buộc hoạt động vẫn ở trạng thái UPCOMING và chưa tới giờ bắt đầu
        requireActivityBeforeStart(activityId);
        // Kiểm tra quyền phân công của người dùng hiện tại
        if (!canAssignStaff(activityId, user)) {
            throw new SecurityException("Bạn không có quyền phân công nhân sự vào hoạt động này.");
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        // Đảm bảo hoạt động tồn tại trong hệ thống
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        Staff staff = staffDAO.findById(staffId);
        // Đảm bảo nhân sự được chọn tồn tại
        if (staff == null) {
            throw new IllegalArgumentException("Không tìm thấy nhân sự được chọn.");
        }
        // Không cho phép thêm nhân sự phụ trách chính vào danh sách nhân sự hỗ trợ
        if (staffId == activity.getResponsibleStaffId()) {
            throw new IllegalArgumentException("Nhân sự phụ trách chính không được thêm vào danh sách nhân sự của hoạt động.");
        }
        // Chỉ cho phép phân công nhân sự đang hoạt động (ACTIVE)
        if (!"ACTIVE".equals(staff.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể phân công nhân sự đang hoạt động (ACTIVE).");
        }
        // Nhân sự phải thuộc đơn vị phụ trách chính hoặc đơn vị đồng phụ trách đang ACTIVE của hoạt động
        if (!activityStaffDAO.isStaffInActivityDepartments(staffId, activityId)) {
            throw new IllegalArgumentException("Nhân sự được phân công phải thuộc đơn vị phụ trách hoặc đơn vị đồng phụ trách (đang hoạt động) của hoạt động.");
        }
        // Tránh phân công trùng lặp cùng một nhân sự vào cùng hoạt động
        if (activityStaffDAO.exists(activityId, staffId)) {
            throw new IllegalArgumentException("Nhân sự này đã được phân công vào hoạt động.");
        }
        return activityStaffDAO.insert(activityId, staffId);
    }

    /**
     * Gỡ phân công một nhân sự khỏi hoạt động trước khi hoạt động bắt đầu.
     *
     * @param activityId ID hoạt động
     * @param staffId    ID nhân sự cần gỡ
     * @param user       Người dùng thực hiện thao tác
     * @return {@code true} nếu gỡ thành công, {@code false} nếu tham số không hợp lệ hoặc không có quyền
     */
    @Override
    public boolean removeStaff(int activityId, int staffId, CurrentUser user) {
        // Từ chối khi ID hoạt động hoặc ID nhân sự không hợp lệ
        if (activityId <= 0 || staffId <= 0) {
            return false;
        }
        // Chỉ cho phép gỡ nhân sự trước khi hoạt động bắt đầu
        requireActivityBeforeStart(activityId);
        // Trả về false nếu người dùng không có quyền phân công
        if (!canAssignStaff(activityId, user)) {
            return false;
        }
        return activityStaffDAO.delete(activityId, staffId);
    }

    /**
     * Kiểm tra hoạt động đang ở trạng thái {@link ActivityStatus#UPCOMING} và thời điểm hiện tại (giờ Việt Nam)
     * vẫn nằm trước thời gian bắt đầu của hoạt động.
     *
     * @param activity Đối tượng hoạt động cần kiểm tra
     * @return {@code true} nếu hoạt động chưa bắt đầu
     */
    private boolean isActivityBeforeStart(ExtracurricularActivity activity) {
        return activity != null
                && activity.getActivityStatus() == ActivityStatus.UPCOMING
                && activity.getStartTime() != null
                && DateTimeUtil.now().isBefore(activity.getStartTime());
    }

    /**
     * Ràng buộc hoạt động phải chưa bắt đầu; nếu không sẽ ném {@link IllegalArgumentException}.
     *
     * @param activityId ID hoạt động cần kiểm tra
     */
    private void requireActivityBeforeStart(int activityId) {
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        // Chặn mọi thao tác thay đổi nhân sự khi hoạt động đã bắt đầu hoặc không còn UPCOMING
        if (!isActivityBeforeStart(activity)) {
            throw new IllegalArgumentException(
                    "Chỉ được thay đổi phân công nhân sự trước khi hoạt động bắt đầu.");
        }
    }

    /**
     * Lấy danh sách các đơn vị đồng phụ trách đã gán cho một hoạt động.
     *
     * @param activityId ID hoạt động
     * @return Danh sách DTO đơn vị đồng phụ trách (rỗng nếu ID không hợp lệ)
     */
    @Override
    public List<ActivityDepartmentResponseDTO> getDepartmentsByActivityId(int activityId) {
        // Trả về rỗng nếu ID hoạt động không dương
        if (activityId <= 0) {
            return List.of();
        }
        return activityDepartmentDAO.findByActivityId(activityId);
    }

    /**
     * Lấy danh sách các đơn vị đang hoạt động (ACTIVE) có thể thêm làm đơn vị đồng phụ trách cho hoạt động.
     * Loại trừ đơn vị phụ trách chính và các đơn vị đã được thêm làm đồng phụ trách.
     *
     * @param activityId ID hoạt động
     * @return Danh sách đơn vị khả dụng
     */
    @Override
    public List<Department> getAvailableDepartmentsForActivity(int activityId) {
        // Chỉ tra cứu hoạt động nếu activityId hợp lệ, ngược lại gán null
        ExtracurricularActivity activity = activityId <= 0 ? null : activityDAO.findById(activityId);
        // Trả về danh sách rỗng nếu hoạt động không tồn tại
        if (activity == null) {
            return List.of();
        }
        // Thu thập tập ID các đơn vị đã là đồng phụ trách của hoạt động
        Set<Integer> usedIds = activityDepartmentDAO.findByActivityId(activityId).stream()
                .map(ActivityDepartmentResponseDTO::getDepartmentId)
                .collect(Collectors.toSet());
        // Loại trừ thêm cả đơn vị phụ trách chính
        usedIds.add(activity.getResponsibleDepartmentId());
        // Lọc toàn bộ đơn vị trong hệ thống: chỉ giữ đơn vị đang ACTIVE và chưa có trong tập usedIds
        return departmentDAO.findAll().stream()
                .filter(d -> d.getStatus() == CommonStatus.ACTIVE)
                .filter(d -> !usedIds.contains(d.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Thêm một đơn vị đồng phụ trách vào hoạt động sau khi kiểm tra phân quyền, trạng thái đơn vị và tránh trùng lặp.
     *
     * @param activityId   ID hoạt động
     * @param departmentId ID đơn vị cần thêm
     * @param user         Người dùng thực hiện thao tác
     * @return {@code true} nếu thêm thành công
     * @throws Exception Nếu vi phạm quyền hoặc ràng buộc dữ liệu
     */
    @Override
    public boolean addDepartment(int activityId, int departmentId, CurrentUser user) throws Exception {
        // Kiểm tra ID hoạt động và ID đơn vị phải dương
        if (activityId <= 0 || departmentId <= 0) {
            throw new IllegalArgumentException("Thông tin đơn vị đồng phụ trách không hợp lệ.");
        }
        // Kiểm tra quyền quản lý và điều kiện hoạt động chưa bắt đầu
        if (!canAssignStaff(activityId, user)) {
            throw new SecurityException("Bạn không có quyền quản lý đơn vị đồng phụ trách của hoạt động này.");
        }
        ExtracurricularActivity activity = activityDAO.findById(activityId);
        // Đảm bảo hoạt động tồn tại
        if (activity == null) {
            throw new IllegalArgumentException("Không tìm thấy hoạt động.");
        }
        Department department = departmentDAO.findById(departmentId);
        // Đảm bảo đơn vị tồn tại
        if (department == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn vị được chọn.");
        }
        // Chỉ chấp nhận đơn vị đang ở trạng thái ACTIVE
        if (department.getStatus() != CommonStatus.ACTIVE) {
            throw new IllegalArgumentException("Chỉ có thể thêm đơn vị đang hoạt động (ACTIVE).");
        }
        // Không thêm đơn vị phụ trách chính vào danh sách đồng phụ trách
        if (departmentId == activity.getResponsibleDepartmentId()) {
            throw new IllegalArgumentException("Đơn vị phụ trách chính không cần thêm vào danh sách đồng phụ trách.");
        }
        // Kiểm tra tránh thêm trùng đơn vị đồng phụ trách đã tồn tại
        if (activityDepartmentDAO.exists(activityId, departmentId)) {
            throw new IllegalArgumentException("Đơn vị này đã là đơn vị đồng phụ trách của hoạt động.");
        }
        return activityDepartmentDAO.insert(activityId, departmentId);
    }

    /**
     * Gỡ một đơn vị đồng phụ trách khỏi hoạt động (đồng thời tầng DAO sẽ dọn các nhân sự không còn thuộc đơn vị hợp lệ).
     *
     * @param activityId   ID hoạt động
     * @param departmentId ID đơn vị cần gỡ
     * @param user         Người dùng thực hiện thao tác
     * @return {@code true} nếu gỡ thành công, {@code false} nếu tham số không hợp lệ hoặc không có quyền
     */
    @Override
    public boolean removeDepartment(int activityId, int departmentId, CurrentUser user) {
        // Từ chối khi ID không hợp lệ hoặc người dùng không có quyền chỉnh sửa phân công
        if (activityId <= 0 || departmentId <= 0 || !canAssignStaff(activityId, user)) {
            return false;
        }
        return activityDepartmentDAO.delete(activityId, departmentId);
    }

    /**
     * Tập hợp danh sách ID các đơn vị đang ACTIVE liên quan đến hoạt động (gồm đơn vị phụ trách chính và các đơn vị đồng phụ trách).
     *
     * @param activity Đối tượng hoạt động cần lấy danh sách đơn vị
     * @return Danh sách ID đơn vị không trùng lặp đang ở trạng thái ACTIVE
     */
    private List<Integer> getActiveActivityDepartmentIds(ExtracurricularActivity activity) {
        List<Integer> ids = new ArrayList<>();
        Department primary = departmentDAO.findById(activity.getResponsibleDepartmentId());
        // Ưu tiên thêm đơn vị phụ trách chính nếu tồn tại và đang ACTIVE
        if (primary != null && primary.getStatus() == CommonStatus.ACTIVE) {
            ids.add(primary.getId());
        }
        // Duyệt danh sách đơn vị đồng phụ trách của hoạt động
        for (ActivityDepartmentResponseDTO d : activityDepartmentDAO.findByActivityId(activity.getId())) {
            // Chỉ thêm các đơn vị đang ACTIVE và chưa có trong danh sách kết quả
            if ("ACTIVE".equals(d.getDepartmentStatus()) && !ids.contains(d.getDepartmentId())) {
                ids.add(d.getDepartmentId());
            }
        }
        return ids;
    }
}
