package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityOrganizationLevelDAO;
import com.swp391.g1.dao.impl.ActivityOrganizationLevelDAOImpl;
import com.swp391.g1.model.ActivityOrganizationLevel;
import com.swp391.g1.service.IActivityOrganizationLevelService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Lớp triển khai dịch vụ quản lý cấp tổ chức hoạt động (ActivityOrganizationLevel).
 * Xử lý kiểm tra hợp lệ dữ liệu đầu vào, chuẩn hóa mã/tên/hệ số nhân và gọi tầng DAO tương ứng.
 */
public class ActivityOrganizationLevelServiceImpl implements IActivityOrganizationLevelService {

    // Giới hạn dưới và giới hạn trên hợp lệ của hệ số nhân điểm theo cấp tổ chức
    private static final BigDecimal MIN_COEFFICIENT = new BigDecimal("0.01");
    private static final BigDecimal MAX_COEFFICIENT = new BigDecimal("10.00");

    private final IActivityOrganizationLevelDAO organizationLevelDAO;

    /**
     * Khởi tạo dịch vụ với triển khai mặc định của {@link IActivityOrganizationLevelDAO}.
     */
    public ActivityOrganizationLevelServiceImpl() {
        this.organizationLevelDAO = new ActivityOrganizationLevelDAOImpl();
    }

    /**
     * Lấy toàn bộ danh sách cấp tổ chức hoạt động trong hệ thống.
     *
     * @return Danh sách tất cả các cấp tổ chức hoạt động
     */
    @Override
    public List<ActivityOrganizationLevel> getAllOrganizationLevels() {
        return organizationLevelDAO.findAll();
    }

    /**
     * Lấy thông tin chi tiết một cấp tổ chức hoạt động theo ID.
     *
     * @param id ID của cấp tổ chức cần tìm
     * @return Đối tượng {@link ActivityOrganizationLevel} tương ứng, hoặc {@code null} nếu ID không hợp lệ hoặc không tồn tại
     */
    @Override
    public ActivityOrganizationLevel getOrganizationLevelById(int id) {
        // Từ chối truy vấn khi ID không dương
        if (id <= 0) {
            return null;
        }
        return organizationLevelDAO.findById(id);
    }

    /**
     * Kiểm tra hợp lệ và tạo mới một cấp tổ chức hoạt động.
     *
     * @param level Thông tin cấp tổ chức cần tạo
     * @return {@code true} nếu thêm mới thành công (nhận được ID sinh tự động dương), ngược lại {@code false}
     */
    @Override
    public boolean createOrganizationLevel(ActivityOrganizationLevel level) {
        validateOrganizationLevel(level, false);
        Integer id = organizationLevelDAO.insert(level);
        return id != null && id > 0;
    }

    /**
     * Kiểm tra hợp lệ và cập nhật thông tin một cấp tổ chức hoạt động đã tồn tại.
     *
     * @param level Thông tin cấp tổ chức cần cập nhật (bắt buộc có ID hợp lệ)
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}
     */
    @Override
    public boolean updateOrganizationLevel(ActivityOrganizationLevel level) {
        validateOrganizationLevel(level, true);
        return organizationLevelDAO.update(level);
    }

    /**
     * Xóa một cấp tổ chức hoạt động theo ID.
     *
     * @param id ID của cấp tổ chức cần xóa
     * @return {@code true} nếu xóa thành công, {@code false} nếu ID không hợp lệ hoặc không có bản ghi bị xóa
     */
    @Override
    public boolean deleteOrganizationLevel(int id) {
        // Bỏ qua thao tác xóa nếu ID không hợp lệ
        if (id <= 0) {
            return false;
        }
        return organizationLevelDAO.delete(id);
    }

    /**
     * Kiểm tra toàn bộ ràng buộc nghiệp vụ, chuẩn hóa dữ liệu và kiểm tra trùng mã cấp tổ chức.
     *
     * @param level    Đối tượng cấp tổ chức cần kiểm tra và chuẩn hóa
     * @param isUpdate {@code true} nếu đang ở luồng cập nhật (cần kiểm tra ID và bỏ qua chính bản ghi đó khi check trùng mã),
     *                 {@code false} nếu đang ở luồng tạo mới
     */
    private void validateOrganizationLevel(ActivityOrganizationLevel level, boolean isUpdate) {
        // Kiểm tra đối tượng đầu vào không được null
        if (level == null) {
            throw new IllegalArgumentException("Dữ liệu cấp tổ chức không hợp lệ.");
        }
        // Khi cập nhật, bắt buộc phải có ID dương hợp lệ
        if (isUpdate && level.getId() <= 0) {
            throw new IllegalArgumentException("ID cấp tổ chức không hợp lệ.");
        }
        // Kiểm tra mã cấp tổ chức không được rỗng hoặc chỉ chứa khoảng trắng
        if (level.getCode() == null || level.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã cấp tổ chức không được để trống.");
        }
        // Kiểm tra tên cấp tổ chức không được rỗng hoặc chỉ chứa khoảng trắng
        if (level.getName() == null || level.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên cấp tổ chức không được để trống.");
        }
        BigDecimal coeff = level.getCoefficient();
        // Hệ số nhân là trường bắt buộc
        if (coeff == null) {
            throw new IllegalArgumentException("Hệ số nhân không được để trống.");
        }
        // Làm tròn hệ số nhân về đúng 2 chữ số thập phân theo quy tắc HALF_UP trước khi so sánh biên
        coeff = coeff.setScale(2, RoundingMode.HALF_UP);
        // Đảm bảo hệ số nhân nằm trong đoạn [0.01, 10.00]
        if (coeff.compareTo(MIN_COEFFICIENT) < 0 || coeff.compareTo(MAX_COEFFICIENT) > 0) {
            throw new IllegalArgumentException("Hệ số nhân phải nằm trong khoảng từ 0.01 đến 10.00.");
        }
        // Gán lại giá trị đã chuẩn hóa: hệ số 2 chữ số thập phân, mã viết hoa cắt khoảng trắng, tên cắt khoảng trắng
        level.setCoefficient(coeff);
        String code = level.getCode().trim().toUpperCase();
        level.setCode(code);
        level.setName(level.getName().trim());
        // Nếu là cập nhật thì loại trừ ID của chính bản ghi đang sửa; nếu tạo mới thì không loại trừ bản ghi nào (0)
        int excludedId = isUpdate ? level.getId() : 0;
        // Duyệt các bản ghi có cùng mã trong DB để kiểm tra xem có bản ghi nào khác ID đang sửa hay không
        boolean duplicate = organizationLevelDAO.findByCode(code).stream()
                .anyMatch(existing -> existing.getId() != excludedId);
        // Ném ngoại lệ nếu phát hiện mã cấp tổ chức đã tồn tại ở bản ghi khác
        if (duplicate) {
            throw new IllegalArgumentException("Mã cấp tổ chức đã tồn tại.");
        }
    }
}
