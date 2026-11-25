package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IPartnerCompanyDAO;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.PartnerCompany;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartnerCompanyDAOImpl implements IPartnerCompanyDAO {

    /**
     * Ánh xạ một dòng dữ liệu từ ResultSet sang thực thể {@link PartnerCompany}.
     *
     * @param rs con trỏ ResultSet đang đứng tại dòng dữ liệu hợp lệ
     * @return đối tượng {@link PartnerCompany} đã được điền đầy đủ thuộc tính
     */
    private PartnerCompany mapPartnerCompany(ResultSet rs) throws SQLException {
        PartnerCompany partnerCompany = new PartnerCompany();
        partnerCompany.setId(rs.getInt("id"));
        partnerCompany.setCode(rs.getString("code"));
        partnerCompany.setName(rs.getString("name"));
        partnerCompany.setWebsite(rs.getString("website"));
        partnerCompany.setEmail(rs.getString("email"));
        partnerCompany.setPhoneNumber(rs.getString("phone_number"));
        partnerCompany.setAddress(rs.getString("address"));
        partnerCompany.setDescription(rs.getString("description"));
        // Chuyển đổi an toàn cột status từ chuỗi sang enum CommonStatus (tránh lỗi khi null)
        String status = rs.getString("status");
        partnerCompany.setStatus(status == null ? null : Enum.CommonStatus.valueOf(status));
        return partnerCompany;
    }

    /**
     * Tìm danh sách công ty đối tác theo mã công ty ({@code code}).
     *
     * @param code mã công ty đối tác cần tìm
     * @return danh sách công ty đối tác khớp mã
     */
    @Override
    public List<PartnerCompany> findByCode(String code) {
        List<PartnerCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_company WHERE code = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,code);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt toàn bộ bản ghi trả về và ánh xạ sang thực thể PartnerCompany
                while (rs.next()) {
                    list.add(mapPartnerCompany(rs));
                }
            }
            // Ghi cảnh báo ra console nếu phát hiện nhiều hơn một công ty trùng mã code
            if(list.size() > 1){
                System.out.println("Warning: Multiple partner companies found with the same code: " + code);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime khi truy vấn theo mã thất bại
            throw new IllegalStateException("Failed to find partner companies by code.", e);
        }
        return list;
    }

    /**
     * Tìm danh sách công ty đối tác theo trạng thái chung (ACTIVE/INACTIVE).
     *
     * @param status trạng thái cần lọc
     * @return danh sách công ty đối tác có trạng thái tương ứng
     */
    @Override
    public List<PartnerCompany> findByCommonStatus(Enum.CommonStatus status) {
        List<PartnerCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_company WHERE status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả và ánh xạ sang danh sách công ty đối tác
                while (rs.next()) {
                    list.add(mapPartnerCompany(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn theo trạng thái thất bại
            throw new IllegalStateException("Failed to find partner companies by status.", e);
        }
        return list;
    }

    /**
     * Lấy toàn bộ danh sách công ty đối tác trong bảng {@code partner_company}.
     *
     * @return danh sách tất cả công ty đối tác
     */
    @Override
    public List<PartnerCompany> findAll() {
        List<PartnerCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_company";

        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt tất cả các dòng trong bảng và thêm vào danh sách kết quả
            while (rs.next()) {
                list.add(mapPartnerCompany(rs));
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi không thể lấy toàn bộ danh sách công ty đối tác
            throw new IllegalStateException("Failed to find all partner companies.", e);
        }
        return list;
    }

    /**
     * Tìm một công ty đối tác theo khóa chính {@code id}.
     *
     * @param id mã định danh công ty đối tác
     * @return thực thể {@link PartnerCompany} nếu tìm thấy, ngược lại null
     */
    @Override
    public PartnerCompany findById(Integer id) {
        PartnerCompany partnerCompany = null;
        String sql = "SELECT * FROM partner_company WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Ánh xạ dòng kết quả đầu tiên nếu tồn tại bản ghi khớp ID
                if (rs.next()) {
                    partnerCompany = mapPartnerCompany(rs);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tìm công ty đối tác theo ID thất bại
            throw new IllegalStateException("Failed to find partner company by ID.", e);
        }
        return partnerCompany;
    }

    /**
     * Chèn mới một công ty đối tác vào CSDL và trả về khóa chính tự tăng vừa sinh.
     *
     * @param partnerCompany thực thể công ty đối tác cần lưu
     * @return ID vừa được sinh ra, hoặc null nếu chèn không thành công
     */
    @Override
    public Integer insert(PartnerCompany partnerCompany) {
        String sql = "INSERT INTO partner_company "
                + "(code, name, website, email, phone_number, address, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, partnerCompany.getCode());
            ps.setString(2, partnerCompany.getName());
            ps.setString(3, partnerCompany.getWebsite());
            ps.setString(4, partnerCompany.getEmail());
            ps.setString(5, partnerCompany.getPhoneNumber());
            ps.setString(6, partnerCompany.getAddress());
            ps.setString(7, partnerCompany.getDescription());
            // Truyền tên trạng thái hoặc null nếu đối tượng chưa gán trạng thái
            ps.setString(8, partnerCompany.getStatus() == null ? null : partnerCompany.getStatus().name());

            // Nếu không có dòng nào được thêm mới thì trả về null
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Lấy khóa chính vừa tạo nếu có, ngược lại trả về null
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi thêm mới công ty đối tác thất bại
            throw new IllegalStateException("Failed to insert partner company.", e);
        }
    }

    /**
     * Cập nhật toàn bộ thông tin một công ty đối tác đã tồn tại theo ID.
     *
     * @param partnerCompany thực thể chứa thông tin cần cập nhật
     * @return true nếu có ít nhất một dòng được cập nhật, false nếu đầu vào không hợp lệ hoặc không tìm thấy bản ghi
     */
    @Override
    public boolean update(PartnerCompany partnerCompany) {
        // Từ chối cập nhật nếu đối tượng null hoặc ID không hợp lệ
        if (partnerCompany == null || partnerCompany.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE partner_company "
                + "SET code = ?, name = ?, website = ?, email = ?, phone_number = ?, address = ?, description = ?, status = ? "
                + "WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partnerCompany.getCode());
            ps.setString(2, partnerCompany.getName());
            ps.setString(3, partnerCompany.getWebsite());
            ps.setString(4, partnerCompany.getEmail());
            ps.setString(5, partnerCompany.getPhoneNumber());
            ps.setString(6, partnerCompany.getAddress());
            ps.setString(7, partnerCompany.getDescription());
            // Nếu trạng thái là null thì gán SQL NULL, ngược lại truyền tên enum tương ứng
            if (partnerCompany.getStatus() == null) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, partnerCompany.getStatus().name());
            }
            ps.setInt(9, partnerCompany.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi cập nhật công ty đối tác thất bại
            throw new IllegalStateException("Failed to update partner company.", e);
        }
    }

    /**
     * Xóa vĩnh viễn một công ty đối tác khỏi CSDL theo ID.
     *
     * @param id mã định danh công ty đối tác cần xóa
     * @return true nếu xóa thành công ít nhất một dòng, ngược lại false
     */
    @Override
    public boolean delete(Integer id) {
        // Trả về false ngay nếu ID là null
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM partner_company WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi xóa công ty đối tác thất bại (ví dụ vướng khóa ngoại)
            throw new IllegalStateException("Failed to delete partner company.", e);
        }
    }
}
