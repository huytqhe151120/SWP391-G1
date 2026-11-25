package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityTypeDAO;
import com.swp391.g1.dao.impl.ActivityTypeDAOImpl;
import com.swp391.g1.model.ActivityType;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.service.IActivityTypeService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp triển khai dịch vụ quản lý danh mục loại hoạt động ngoại khóa ({@link ActivityType}).
 * Chịu trách nhiệm kiểm tra dữ liệu nghiệp vụ và điều phối thao tác CRUD xuống tầng DAO.
 */
public class ActivityTypeServiceImpl implements IActivityTypeService {

    private final IActivityTypeDAO activityTypeDAO;

    /**
     * Khởi tạo dịch vụ với triển khai mặc định của {@link IActivityTypeDAO}.
     */
    public ActivityTypeServiceImpl() {
        this.activityTypeDAO = new ActivityTypeDAOImpl();
    }

    /**
     * Lấy toàn bộ danh sách loại hoạt động (dùng cho màn hình quản trị).
     *
     * @return Danh sách tất cả các loại hoạt động
     */
    @Override
    public List<ActivityType> getAllActivityTypes() {
        return activityTypeDAO.findAll();
    }

    /**
     * Lấy danh sách các loại hoạt động đang ở trạng thái kích hoạt ({@link CommonStatus#ACTIVE}).
     *
     * @return Danh sách loại hoạt động khả dụng để gán cho hoạt động mới
     */
    @Override
    public List<ActivityType> getActiveActivityTypes() {
        // Duyệt toàn bộ danh sách loại hoạt động và chỉ giữ lại các bản ghi có trạng thái ACTIVE
        return activityTypeDAO.findAll().stream()
                .filter(type -> type.getStatus() == CommonStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin loại hoạt động theo ID.
     *
     * @param id ID loại hoạt động cần tìm
     * @return Đối tượng {@link ActivityType} tương ứng, hoặc {@code null} nếu ID không hợp lệ hoặc không tồn tại
     */
    @Override
    public ActivityType getActivityTypeById(int id) {
        // Không truy vấn cơ sở dữ liệu khi ID không dương
        if (id <= 0) {
            return null;
        }
        return activityTypeDAO.findById(id);
    }

    /**
     * Kiểm tra hợp lệ dữ liệu và tạo mới một loại hoạt động với trạng thái mặc định là {@link CommonStatus#ACTIVE}.
     *
     * @param activityType Đối tượng loại hoạt động cần tạo
     * @return {@code true} nếu tạo mới thành công, ngược lại {@code false}
     * @throws Exception Nếu dữ liệu đầu vào vi phạm quy tắc kiểm tra hợp lệ
     */
    @Override
    public boolean createActivityType(ActivityType activityType) throws Exception {
        // Kiểm tra ràng buộc nghiệp vụ cho loại hoạt động mới (không kiểm tra ID)
        validateActivityType(activityType, false);
        // Thiết lập trạng thái mặc định là ACTIVE khi tạo mới loại hoạt động
        activityType.setStatus(CommonStatus.ACTIVE);
        // Gọi DAO chèn bản ghi mới và kiểm tra ID tự sinh trả về
        Integer generatedId = activityTypeDAO.insert(activityType);
        return generatedId != null && generatedId > 0;
    }

    /**
     * Kiểm tra hợp lệ dữ liệu và cập nhật thông tin một loại hoạt động đã có.
     *
     * @param activityType Đối tượng loại hoạt động cần cập nhật (bắt buộc có ID hợp lệ)
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}
     * @throws Exception Nếu dữ liệu đầu vào vi phạm quy tắc kiểm tra hợp lệ
     */
    @Override
    public boolean updateActivityType(ActivityType activityType) throws Exception {
        // Kiểm tra ràng buộc nghiệp vụ khi cập nhật (bao gồm kiểm tra ID hợp lệ)
        validateActivityType(activityType, true);
        // Gọi DAO thực hiện cập nhật bản ghi trong cơ sở dữ liệu
        return activityTypeDAO.update(activityType);
    }

    /**
     * Xóa một loại hoạt động theo ID.
     *
     * @param id ID loại hoạt động cần xóa
     * @return {@code true} nếu xóa thành công, {@code false} nếu ID không hợp lệ hoặc không có bản ghi bị xóa
     */
    @Override
    public boolean deleteActivityType(int id) {
        // Từ chối thao tác xóa khi ID không dương
        if (id <= 0) {
            return false;
        }
        return activityTypeDAO.delete(id);
    }

    /**
     * Kiểm tra các quy tắc hợp lệ cơ bản cho đối tượng {@link ActivityType}.
     *
     * @param entity   Đối tượng loại hoạt động cần kiểm tra
     * @param isUpdate {@code true} nếu đang cập nhật (cần kiểm tra ID > 0), {@code false} nếu đang tạo mới
     * @throws Exception Nếu bất kỳ trường bắt buộc nào không hợp lệ
     */
    private void validateActivityType(ActivityType entity, boolean isUpdate) throws Exception {
        // Kiểm tra đối tượng không được null
        if (entity == null) {
            throw new IllegalArgumentException("Loại hoạt động không được để trống.");
        }

        // Ở luồng cập nhật bắt buộc phải có ID dương
        if (isUpdate && entity.getId() <= 0) {
            throw new IllegalArgumentException("ID loại hoạt động không hợp lệ khi cập nhật.");
        }

        // Mã loại hoạt động không được rỗng hoặc chỉ gồm khoảng trắng
        if (entity.getCode() == null || entity.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã loại hoạt động không được để trống.");
        }

        // Tên loại hoạt động không được rỗng hoặc chỉ gồm khoảng trắng
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên loại hoạt động không được để trống.");
        }
    }
}
