package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IPartnerCompanyDAO;
import com.swp391.g1.dao.impl.PartnerCompanyDAOImpl;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.PartnerCompany;
import com.swp391.g1.service.IPartnerCompanyService;

import java.util.List;
import java.util.stream.Collectors;

public class PartnerCompanyServiceImpl implements IPartnerCompanyService {

    private final IPartnerCompanyDAO partnerCompanyDAO;

    /**
     * Khởi tạo service quản lý công ty đối tác với DAO mặc định.
     */
    public PartnerCompanyServiceImpl() {
        this.partnerCompanyDAO = new PartnerCompanyDAOImpl();
    }

    /**
     * Lấy danh sách tất cả công ty đối tác trong hệ thống.
     *
     * @return danh sách thực thể {@link PartnerCompany}
     */
    @Override
    public List<PartnerCompany> getAllPartnerCompanies() {
        return partnerCompanyDAO.findAll();
    }

    /**
     * Lấy danh sách các công ty đối tác đang ở trạng thái hoạt động (ACTIVE).
     *
     * @return danh sách công ty đối tác ACTIVE
     */
    @Override
    public List<PartnerCompany> getActivePartnerCompanies() {
        // Duyệt toàn bộ danh sách công ty bằng stream và lọc những công ty có trạng thái ACTIVE
        return partnerCompanyDAO.findAll().stream()
                .filter(company -> company.getStatus() == CommonStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    /**
     * Tìm thông tin công ty đối tác theo ID.
     *
     * @param id mã định danh công ty đối tác
     * @return thực thể công ty đối tác nếu tìm thấy, hoặc null nếu ID không hợp lệ/không tồn tại
     */
    @Override
    public PartnerCompany getPartnerCompanyById(int id) {
        // Trả về null ngay nếu ID không hợp lệ
        if (id <= 0) {
            return null;
        }
        return partnerCompanyDAO.findById(id);
    }

    /**
     * Kiểm tra hợp lệ dữ liệu, đặt trạng thái mặc định ACTIVE và lưu công ty đối tác mới vào CSDL.
     *
     * @param partnerCompany đối tượng công ty đối tác cần tạo mới
     * @return true nếu thêm mới thành công và sinh được ID dương
     */
    @Override
    public boolean createPartnerCompany(PartnerCompany partnerCompany) throws Exception {
        // 1. Validate nghiệp vụ
        validatePartnerCompany(partnerCompany, false);

        // 2. Mặc định trạng thái ACTIVE khi tạo mới
        partnerCompany.setStatus(CommonStatus.ACTIVE);

        // 3. Gọi DAO lưu vào CSDL
        Integer generatedId = partnerCompanyDAO.insert(partnerCompany);
        return generatedId != null && generatedId > 0;
    }

    /**
     * Kiểm tra hợp lệ dữ liệu (bao gồm ID) và cập nhật thông tin công ty đối tác trong CSDL.
     *
     * @param partnerCompany đối tượng công ty đối tác cần cập nhật
     * @return true nếu cập nhật thành công
     */
    @Override
    public boolean updatePartnerCompany(PartnerCompany partnerCompany) throws Exception {
        // 1. Validate nghiệp vụ (bao gồm kiểm tra ID)
        validatePartnerCompany(partnerCompany, true);

        // 2. Gọi DAO cập nhật
        return partnerCompanyDAO.update(partnerCompany);
    }

    /**
     * Xóa công ty đối tác theo ID.
     *
     * @param id mã định danh công ty đối tác cần xóa
     * @return true nếu xóa thành công, false nếu ID không hợp lệ hoặc xóa thất bại
     */
    @Override
    public boolean deletePartnerCompany(int id) {
        // Từ chối xóa nếu ID không hợp lệ
        if (id <= 0) {
            return false;
        }
        return partnerCompanyDAO.delete(id);
    }

    // =========================================================================
    // HELPER METHOD: VALIDATION NGHIỆP VỤ
    // =========================================================================

    /**
     * Xác thực các ràng buộc nghiệp vụ của công ty đối tác (mã, tên bắt buộc; định dạng email và số điện thoại nếu có nhập).
     *
     * @param entity   thực thể công ty đối tác cần kiểm tra
     * @param isUpdate true nếu đang kiểm tra cho thao tác cập nhật (yêu cầu ID dương)
     */
    private void validatePartnerCompany(PartnerCompany entity, boolean isUpdate) throws Exception {
        // Kiểm tra đối tượng đầu vào không được null
        if (entity == null) {
            throw new IllegalArgumentException("Dữ liệu công ty đối tác không hợp lệ.");
        }

        // Khi cập nhật bắt buộc phải có ID dương hợp lệ
        if (isUpdate && entity.getId() <= 0) {
            throw new IllegalArgumentException("ID công ty đối tác không hợp lệ khi cập nhật.");
        }

        // Mã công ty là trường bắt buộc
        if (isEmpty(entity.getCode())) {
            throw new IllegalArgumentException("Mã công ty đối tác không được để trống.");
        }

        // Tên công ty là trường bắt buộc
        if (isEmpty(entity.getName())) {
            throw new IllegalArgumentException("Tên công ty đối tác không được để trống.");
        }

        // Validate định dạng Email (nếu người dùng có nhập)
        if (!isEmpty(entity.getEmail()) && !entity.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Địa chỉ email công ty không đúng định dạng.");
        }

        // Validate Số điện thoại (chỉ chứa 9 - 11 chữ số)
        if (!isEmpty(entity.getPhoneNumber()) && !entity.getPhoneNumber().matches("^\\d{9,11}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải chứa từ 9 đến 11 chữ số).");
        }
    }

    /**
     * Kiểm tra chuỗi có null hoặc chỉ gồm khoảng trắng hay không.
     *
     * @param str chuỗi cần kiểm tra
     * @return true nếu chuỗi null hoặc rỗng sau khi trim
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}