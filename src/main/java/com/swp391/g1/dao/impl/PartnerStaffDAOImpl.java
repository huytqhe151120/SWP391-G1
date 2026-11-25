package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IPartnerStaffDAO;
import com.swp391.g1.dto.response.PartnerStaffResponseDTO;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.PartnerStaff;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartnerStaffDAOImpl implements IPartnerStaffDAO {

    /**
     * Ánh xạ một dòng dữ liệu từ ResultSet sang thực thể {@link PartnerStaff}.
     *
     * @param rs con trỏ ResultSet đang đứng tại dòng dữ liệu hợp lệ
     * @return đối tượng {@link PartnerStaff} đã được điền đầy đủ thuộc tính
     */
    private PartnerStaff mapPartnerStaff(ResultSet rs) throws SQLException {
        PartnerStaff partnerStaff = new PartnerStaff();
        partnerStaff.setId(rs.getInt("id"));
        partnerStaff.setCode(rs.getString("code"));
        partnerStaff.setName(rs.getString("name"));
        partnerStaff.setCompanyId(rs.getInt("company_id"));
        // Chuyển đổi an toàn ngày sinh từ java.sql.Date sang LocalDate
        Date dob = rs.getDate("dob");
        partnerStaff.setDob(dob == null ? null : dob.toLocalDate());
        // Kiểm tra null trước khi đọc giá trị boolean cho cột gender
        partnerStaff.setGender(rs.getObject("gender") == null ? null : rs.getBoolean("gender"));
        partnerStaff.setPosition(rs.getString("position"));
        // Chuyển đổi an toàn trạng thái sang enum CommonStatus
        String status = rs.getString("status");
        partnerStaff.setStatus(status == null ? null : Enum.CommonStatus.valueOf(status));
        // Đọc an toàn khóa ngoại account_id có thể null
        Object accountId = rs.getObject("account_id");
        partnerStaff.setAccountId(accountId == null ? null : ((Number) accountId).intValue());
        return partnerStaff;
    }

    /**
     * Tìm danh sách nhân viên đối tác theo mã nhân viên ({@code code}).
     *
     * @param code mã nhân viên đối tác cần tìm
     * @return danh sách nhân viên đối tác khớp mã
     */
    @Override
    public List<PartnerStaff> findByCode(String code) {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff WHERE code = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,code);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt tất cả bản ghi khớp mã và ánh xạ sang thực thể PartnerStaff
                while (rs.next()) {
                    list.add(mapPartnerStaff(rs));
                }
            }
            // Ghi cảnh báo ra console nếu phát hiện dữ liệu trùng mã nhân viên đối tác
            if(list.size() > 1){
                System.out.println("Warning: Multiple partner staff found with the same code: " + code);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn nhân viên đối tác theo mã thất bại
            throw new IllegalStateException("Failed to find partner staff by code.", e);
        }
        return list;
    }

    /**
     * Tìm danh sách nhân viên đối tác theo trạng thái chung (ACTIVE/INACTIVE).
     *
     * @param status trạng thái cần lọc
     * @return danh sách nhân viên đối tác có trạng thái tương ứng
     */
    @Override
    public List<PartnerStaff> findByCommonStatus(Enum.CommonStatus status) {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff WHERE status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt kết quả truy vấn và thêm từng nhân viên vào danh sách
                while (rs.next()) {
                    list.add(mapPartnerStaff(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi lọc nhân viên đối tác theo trạng thái thất bại
            throw new IllegalStateException("Failed to find partner staff by status.", e);
        }
        return list;
    }

    /**
     * Lấy danh sách nhân viên đối tác thuộc một công ty đối tác cụ thể.
     *
     * @param companyId mã định danh công ty đối tác
     * @return danh sách nhân viên đối tác của công ty
     */
    @Override
    public List<PartnerStaff> findByCompany(int companyId) {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff WHERE company_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt toàn bộ nhân viên thuộc công ty và ánh xạ sang thực thể
                while (rs.next()) {
                    list.add(mapPartnerStaff(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn nhân viên theo công ty thất bại
            throw new IllegalStateException("Failed to find partner staff by company.", e);
        }
        return list;
    }

    /**
     * Tìm hồ sơ nhân viên đối tác được liên kết với một tài khoản đăng nhập ({@code account_id}).
     *
     * @param accountId ID tài khoản đăng nhập
     * @return thực thể {@link PartnerStaff} nếu tìm thấy, ngược lại null
     */
    @Override
    public PartnerStaff findByAccountId(int accountId) {
        String sql = "SELECT * FROM partner_staff WHERE account_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                // Trả về bản ghi đã ánh xạ nếu tồn tại, ngược lại trả về null
                return rs.next() ? mapPartnerStaff(rs) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tìm nhân viên đối tác theo account_id thất bại
            throw new IllegalStateException("Failed to find partner staff by account.", e);
        }
    }

    /**
     * Lấy danh sách tất cả nhân viên đối tác kèm mã và tên công ty đối tác (LEFT JOIN {@code partner_company}),
     * sắp xếp mới nhất theo ID giảm dần.
     *
     * @return danh sách DTO chi tiết nhân viên đối tác
     */
    @Override
    public List<PartnerStaffResponseDTO> findAllWithDetails() {
        List<PartnerStaffResponseDTO> list = new ArrayList<>();

        // Câu SQL JOIN lấy dữ liệu nhân viên và thông tin công ty đối tác
        String sql = "SELECT ps.*, "
                + "pc.code AS company_code, "
                + "pc.name AS company_name "
                + "FROM partner_staff ps "
                + "LEFT JOIN partner_company pc ON ps.company_id = pc.id "
                + "ORDER BY ps.id DESC";

        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Duyệt từng dòng kết quả JOIN và ánh xạ sang PartnerStaffResponseDTO
            while (rs.next()) {
                PartnerStaffResponseDTO dto = new PartnerStaffResponseDTO();

                // 1. Mapping các thuộc tính cơ bản
                dto.setId(rs.getInt("id"));
                dto.setCode(rs.getString("code"));
                dto.setName(rs.getString("name"));
                dto.setCompanyId(rs.getInt("company_id"));

                // 2. Mapping java.sql.Date -> java.time.LocalDate (dob)
                Date dobSql = rs.getDate("dob");
                dto.setDob(dobSql != null ? dobSql.toLocalDate() : null);

                // 3. Mapping Boolean (gender - tránh lỗi NullPointerException nếu DB null)
                if (rs.getObject("gender") != null) {
                    dto.setGender(rs.getBoolean("gender"));
                } else {
                    // Gán null cho DTO nếu cột gender trong CSDL để trống
                    dto.setGender(null);
                }

                dto.setPosition(rs.getString("position"));
                dto.setStatus(rs.getString("status"));

                // 4. Mapping các trường JOIN từ partner_company (Sửa tên setter cho khớp DTO)
                dto.setCompanyCode(rs.getString("company_code"));
                dto.setCompanyName(rs.getString("company_name"));

                // 5. Mapping username (nếu trong DB có JOIN với bảng Account/User)
                try {
                    dto.setUsername(rs.getString("username"));
                } catch (SQLException e) {
                    // Bỏ qua nếu câu SQL chưa SELECT cột username
                    dto.setUsername(null);
                }

                list.add(dto);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn danh sách chi tiết nhân viên đối tác thất bại
            throw new IllegalStateException("Failed to find partner staff list with details.", e);
        }
        return list;
    }

    /**
     * Lấy toàn bộ danh sách thực thể nhân viên đối tác từ bảng {@code partner_staff}.
     *
     * @return danh sách tất cả nhân viên đối tác
     */
    @Override
    public List<PartnerStaff> findAll() {
        List<PartnerStaff> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_staff";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Duyệt tất cả các dòng trong bảng và ánh xạ sang thực thể PartnerStaff
            while (rs.next()) {
                list.add(mapPartnerStaff(rs));
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi lấy toàn bộ danh sách nhân viên đối tác thất bại
            throw new IllegalStateException("Failed to find all partner staff.", e);
        }
        return list;
    }

    /**
     * Tìm một nhân viên đối tác theo khóa chính {@code id}.
     *
     * @param id mã định danh nhân viên đối tác
     * @return thực thể {@link PartnerStaff} nếu tìm thấy, ngược lại null
     */
    @Override
    public PartnerStaff findById(Integer id) {
        PartnerStaff partnerStaff = null;
        String sql = "SELECT * FROM partner_staff WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Ánh xạ bản ghi đầu tiên nếu tìm thấy nhân viên khớp ID
                if (rs.next()) {
                    partnerStaff = mapPartnerStaff(rs);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tìm nhân viên đối tác theo ID thất bại
            throw new IllegalStateException("Failed to find partner staff by ID.", e);
        }
        return partnerStaff;
    }

    /**
     * Chèn mới một nhân viên đối tác vào CSDL và trả về khóa chính tự tăng vừa sinh.
     *
     * @param partnerStaff thực thể nhân viên đối tác cần lưu
     * @return ID tự tăng vừa tạo, hoặc null nếu chèn thất bại
     */
    @Override
    public Integer insert(PartnerStaff partnerStaff) {
        String sql = "INSERT INTO partner_staff "
                + "(company_id, code, name, dob, gender, position, status, account_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, partnerStaff.getCompanyId());
            ps.setString(2, partnerStaff.getCode());
            ps.setString(3, partnerStaff.getName());
            // Chuyển LocalDate sang java.sql.Date hoặc truyền null nếu chưa có ngày sinh
            ps.setDate(4, partnerStaff.getDob() == null ? null : Date.valueOf(partnerStaff.getDob()));
            // Gán SQL NULL (BIT) nếu giới tính không được cung cấp, ngược lại gán giá trị boolean
            if (partnerStaff.getGender() == null) {
                ps.setNull(5, Types.BIT);
            } else {
                ps.setBoolean(5, partnerStaff.getGender());
            }
            ps.setString(6, partnerStaff.getPosition());
            ps.setString(7, partnerStaff.getStatus().name());
            // Gán SQL NULL (INTEGER) nếu nhân viên chưa gắn tài khoản đăng nhập, ngược lại truyền accountId
            if (partnerStaff.getAccountId() == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, partnerStaff.getAccountId());
            }

            // Nếu không có dòng nào được thêm thì trả về null
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Lấy khóa chính vừa sinh ra nếu có, ngược lại trả về null
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi thêm mới nhân viên đối tác thất bại
            throw new IllegalStateException("Failed to insert partner company.", e);
        }
    }

    /**
     * Cập nhật thông tin hồ sơ của một nhân viên đối tác theo ID (không thay đổi {@code account_id}).
     *
     * @param entity thực thể chứa dữ liệu cần cập nhật
     * @return true nếu có ít nhất một bản ghi được cập nhật
     */
    @Override
    public boolean update(PartnerStaff entity) {
        String sql = "UPDATE partner_staff SET company_id = ?, code = ?, name = ?, "
                + "dob = ?, gender = ?, position = ?, status = ? WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getCompanyId());
            ps.setString(2, entity.getCode());
            ps.setString(3, entity.getName());
            // Chuyển đổi ngày sinh sang SQL Date hoặc null
            ps.setDate(4, entity.getDob() == null ? null : Date.valueOf(entity.getDob()));
            // Gán SQL NULL nếu giới tính trống, ngược lại gán giá trị boolean
            if (entity.getGender() == null) {
                ps.setNull(5, Types.BIT);
            } else {
                ps.setBoolean(5, entity.getGender());
            }
            ps.setString(6, entity.getPosition());
            ps.setString(7, entity.getStatus().name());
            ps.setInt(8, entity.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi cập nhật nhân viên đối tác thất bại
            throw new IllegalStateException("Failed to update partner staff.", e);
        }
    }

    /**
     * Xóa vĩnh viễn một nhân viên đối tác khỏi CSDL theo ID.
     *
     * @param id mã định danh nhân viên đối tác cần xóa
     * @return true nếu xóa thành công ít nhất một dòng
     */
    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM partner_staff WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi xóa nhân viên đối tác thất bại
            throw new IllegalStateException("Failed to delete partner staff.", e);
        }
    }
}
