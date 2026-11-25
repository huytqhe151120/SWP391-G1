package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IPartnerStaffDAO;
import com.swp391.g1.dao.impl.PartnerStaffDAOImpl;
import com.swp391.g1.dto.request.PartnerStaffRequestDTO;
import com.swp391.g1.dto.response.PartnerStaffResponseDTO;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.PartnerStaff;
import com.swp391.g1.service.IPartnerStaffService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.stream.Collectors;

public class PartnerStaffServiceImpl implements IPartnerStaffService {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final IPartnerStaffDAO partnerStaffDAO;

    /**
     * Khởi tạo service quản lý nhân viên đối tác với DAO mặc định.
     */
    public PartnerStaffServiceImpl() {
        this.partnerStaffDAO = new PartnerStaffDAOImpl();
    }

    /**
     * Lấy danh sách tất cả nhân viên đối tác kèm thông tin công ty đối tác.
     *
     * @return danh sách DTO chi tiết nhân viên đối tác
     */
    @Override
    public List<PartnerStaffResponseDTO> getAllPartnerStaffs() {
        return partnerStaffDAO.findAllWithDetails();
    }

    /**
     * Lấy danh sách nhân viên đối tác thuộc một công ty cụ thể.
     *
     * @param companyId mã định danh công ty đối tác
     * @return danh sách nhân viên đối tác thuộc công ty, hoặc danh sách rỗng nếu companyId không hợp lệ
     */
    @Override
    public List<PartnerStaffResponseDTO> getStaffsByCompanyId(int companyId) {
        // Trả về danh sách rỗng nếu ID công ty không hợp lệ
        if (companyId <= 0) {
            return List.of();
        }
        // Duyệt danh sách nhân viên chi tiết bằng stream và lọc những bản ghi có companyId trùng khớp
        return partnerStaffDAO.findAllWithDetails().stream()
                .filter(staff -> staff.getCompanyId() == companyId)
                .collect(Collectors.toList());
    }

    /**
     * Tìm thông tin chi tiết một nhân viên đối tác theo ID.
     *
     * @param id mã định danh nhân viên đối tác
     * @return DTO chi tiết nhân viên nếu tìm thấy, ngược lại null
     */
    @Override
    public PartnerStaffResponseDTO getPartnerStaffById(int id) {
        // Trả về null ngay nếu ID không hợp lệ
        if (id <= 0) {
            return null;
        }
        // Duyệt danh sách nhân viên chi tiết bằng stream và lấy bản ghi đầu tiên khớp ID
        return partnerStaffDAO.findAllWithDetails().stream()
                .filter(staff -> staff.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Kiểm tra dữ liệu đầu vào, ánh xạ sang thực thể với trạng thái mặc định ACTIVE và lưu nhân viên đối tác mới.
     *
     * @param dto dữ liệu nhân viên đối tác từ biểu mẫu tạo mới
     * @return true nếu thêm mới thành công và nhận được ID dương
     */
    @Override
    public boolean createPartnerStaff(PartnerStaffRequestDTO dto) throws Exception {
        // 1. Validate dữ liệu đầu vào
        validateRequestDTO(dto, false);

        // 2. Map sang Entity
        PartnerStaff entity = mapToEntity(dto);
        entity.setStatus(CommonStatus.ACTIVE);

        // 3. Gọi DAO insert
        Integer generatedId = partnerStaffDAO.insert(entity);
        return generatedId != null && generatedId > 0;
    }

    /**
     * Kiểm tra dữ liệu đầu vào (gồm ID), ánh xạ sang thực thể cùng trạng thái và cập nhật nhân viên đối tác.
     *
     * @param dto dữ liệu nhân viên đối tác từ biểu mẫu cập nhật
     * @return true nếu cập nhật thành công
     */
    @Override
    public boolean updatePartnerStaff(PartnerStaffRequestDTO dto) throws Exception {
        // 1. Validate dữ liệu đầu vào (bao gồm kiểm tra ID)
        validateRequestDTO(dto, true);

        // 2. Map sang Entity
        PartnerStaff entity = mapToEntity(dto);
        entity.setId(dto.getId());
        // Chuyển đổi trạng thái nếu người dùng có truyền; ngược lại mặc định ACTIVE
        if (!isEmpty(dto.getStatus())) {
            try {
                entity.setStatus(CommonStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Fallback về ACTIVE nếu chuỗi trạng thái không khớp hằng enum
                entity.setStatus(CommonStatus.ACTIVE);
            }
        } else {
            // Mặc định giữ trạng thái ACTIVE khi biểu mẫu không gửi trường status
            entity.setStatus(CommonStatus.ACTIVE);
        }

        // 3. Gọi DAO update
        return partnerStaffDAO.update(entity);
    }

    /**
     * Xóa nhân viên đối tác theo ID.
     *
     * @param id mã định danh nhân viên đối tác cần xóa
     * @return true nếu xóa thành công, false nếu ID không hợp lệ hoặc xóa thất bại
     */
    @Override
    public boolean deletePartnerStaff(int id) {
        // Từ chối xóa nếu ID không hợp lệ
        if (id <= 0) {
            return false;
        }
        return partnerStaffDAO.delete(id);
    }

    // =========================================================================
    // HELPER METHODS: VALIDATION & MAPPING
    // =========================================================================

    /**
     * Xác thực toàn bộ ràng buộc đầu vào của biểu mẫu nhân viên đối tác (công ty, mã, tên, ngày sinh, giới tính, trạng thái).
     *
     * @param dto      dữ liệu biểu mẫu cần kiểm tra
     * @param isUpdate true nếu đang kiểm tra cho thao tác cập nhật (yêu cầu ID dương)
     */
    private void validateRequestDTO(PartnerStaffRequestDTO dto, boolean isUpdate) throws Exception {
        // Kiểm tra DTO không được null
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu gửi lên không hợp lệ.");
        }

        // Khi cập nhật bắt buộc phải có ID nhân viên dương
        if (isUpdate && dto.getId() <= 0) {
            throw new IllegalArgumentException("ID nhân viên không hợp lệ khi cập nhật.");
        }

        // Bắt buộc phải chọn công ty đối tác
        if (isEmpty(dto.getCompanyId())) {
            throw new IllegalArgumentException("Vui lòng chọn công ty đối tác.");
        }

        // Kiểm tra ID công ty đối tác là số nguyên dương hợp lệ
        try {
            int companyId = Integer.parseInt(dto.getCompanyId());
            // Từ chối ID công ty nhỏ hơn hoặc bằng 0
            if (companyId <= 0) {
                throw new IllegalArgumentException("Công ty đối tác không hợp lệ.");
            }
        } catch (NumberFormatException e) {
            // Báo lỗi nếu chuỗi companyId không phải định dạng số nguyên
            throw new IllegalArgumentException("ID công ty đối tác phải là số.");
        }

        // Mã nhân viên đối tác là trường bắt buộc
        if (isEmpty(dto.getCode())) {
            throw new IllegalArgumentException("Mã nhân viên đối tác không được để trống.");
        }

        // Tên nhân viên đối tác là trường bắt buộc
        if (isEmpty(dto.getName())) {
            throw new IllegalArgumentException("Tên nhân viên đối tác không được để trống.");
        }

        // Nếu có nhập ngày sinh thì kiểm tra tính hợp lệ của ngày theo định dạng hỗ trợ
        if (!isEmpty(dto.getDob())) {
            try {
                parseDob(dto.getDob());
            } catch (java.time.format.DateTimeParseException e) {
                // Chuyển lỗi phân tích ngày thành thông báo định dạng dd-mm-yyyy cho người dùng
                throw new IllegalArgumentException("Ngày sinh phải có định dạng dd-mm-yyyy.");
            }
        }

        // Nếu có chọn giới tính thì chỉ chấp nhận các giá trị nhị phân hợp lệ ("1", "0", "true", "false")
        if (!isEmpty(dto.getGender())
                && !dto.getGender().equals("1")
                && !dto.getGender().equals("0")
                && !dto.getGender().equalsIgnoreCase("true")
                && !dto.getGender().equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Giới tính không hợp lệ.");
        }

        // Nếu có truyền trạng thái thì chỉ chấp nhận ACTIVE hoặc INACTIVE
        if (!isEmpty(dto.getStatus())
                && !dto.getStatus().equalsIgnoreCase(CommonStatus.ACTIVE.name())
                && !dto.getStatus().equalsIgnoreCase(CommonStatus.INACTIVE.name())) {
            throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ.");
        }
    }

    /**
     * Chuyển đổi dữ liệu từ {@link PartnerStaffRequestDTO} sang thực thể {@link PartnerStaff}.
     *
     * @param dto dữ liệu đầu vào đã được xác thực
     * @return thực thể {@link PartnerStaff} đã chuẩn hóa các trường
     */
    private PartnerStaff mapToEntity(PartnerStaffRequestDTO dto) {
        PartnerStaff entity = new PartnerStaff();
        entity.setCompanyId(Integer.parseInt(dto.getCompanyId()));
        entity.setCode(dto.getCode().trim());
        entity.setName(dto.getName().trim());
        // Chuyển ngày sinh sang LocalDate nếu có nhập, ngược lại để null
        entity.setDob(isEmpty(dto.getDob()) ? null : parseDob(dto.getDob()));
        // Quy đổi giới tính sang Boolean (true cho "1"/"true", false cho "0"/"false", null nếu bỏ trống)
        entity.setGender(isEmpty(dto.getGender()) ? null
                : dto.getGender().equals("1") || dto.getGender().equalsIgnoreCase("true"));
        // Cắt khoảng trắng chức vụ hoặc gán null nếu rỗng
        entity.setPosition(isEmpty(dto.getPosition()) ? null : dto.getPosition().trim());
        return entity;
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

    /**
     * Phân tích chuỗi ngày sinh sang {@link LocalDate}, hỗ trợ định dạng hiển thị {@code dd-MM-yyyy} (nghiêm ngặt)
     * và fallback sang định dạng ISO-8601 {@code yyyy-MM-dd}.
     *
     * @param value chuỗi ngày sinh cần phân tích
     * @return đối tượng {@link LocalDate} tương ứng
     */
    private LocalDate parseDob(String value) {
        value = value.trim();
        // Nếu chuỗi có dạng dd-MM-yyyy thì phân tích theo định dạng hiển thị kiểm tra lịch nghiêm ngặt
        if (value.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return LocalDate.parse(value, DISPLAY_DATE_FORMAT);
        }
        // Mặc định phân tích theo chuẩn ISO yyyy-MM-dd (ví dụ từ input type="date")
        return LocalDate.parse(value);
    }
}