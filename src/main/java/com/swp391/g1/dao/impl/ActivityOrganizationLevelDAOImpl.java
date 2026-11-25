package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityOrganizationLevelDAO;
import com.swp391.g1.model.ActivityOrganizationLevel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai DAO bằng JDBC cho bảng {@code dbo.activity_organization_level}.
 * Cung cấp các thao tác CRUD và tìm kiếm cấp tổ chức hoạt động.
 */
public class ActivityOrganizationLevelDAOImpl implements IActivityOrganizationLevelDAO {

    /**
     * Ánh xạ dòng hiện tại của {@link ResultSet} sang đối tượng {@link ActivityOrganizationLevel}.
     *
     * @param rs ResultSet đang trỏ tới dòng dữ liệu cần ánh xạ
     * @return Đối tượng {@link ActivityOrganizationLevel} sau khi ánh xạ
     * @throws SQLException Nếu có lỗi khi đọc dữ liệu từ ResultSet
     */
    private ActivityOrganizationLevel mapOrganizationLevel(ResultSet rs) throws SQLException {
        ActivityOrganizationLevel level = new ActivityOrganizationLevel();
        level.setId(rs.getInt("id"));
        level.setCode(rs.getString("code"));
        level.setName(rs.getString("name"));
        level.setCoefficient(rs.getBigDecimal("coefficient"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        // Chỉ chuyển đổi created_at sang LocalDateTime khi giá trị trong DB khác null
        if (createdAt != null) {
            level.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        // Chỉ chuyển đổi updated_at sang LocalDateTime khi giá trị trong DB khác null
        if (updatedAt != null) {
            level.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return level;
    }

    /**
     * Lấy toàn bộ danh sách cấp tổ chức hoạt động trong cơ sở dữ liệu, sắp xếp tăng dần theo ID.
     *
     * @return Danh sách tất cả {@link ActivityOrganizationLevel} (rỗng nếu không có bản ghi)
     * @throws IllegalStateException Nếu xảy ra lỗi SQL trong quá trình truy vấn
     */
    @Override
    public List<ActivityOrganizationLevel> findAll() {
        List<ActivityOrganizationLevel> list = new ArrayList<>();
        String sql = "SELECT id, code, name, coefficient, created_at, updated_at "
                + "FROM dbo.activity_organization_level ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt từng dòng kết quả và ánh xạ sang model ActivityOrganizationLevel
            while (rs.next()) {
                list.add(mapOrganizationLevel(rs));
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành IllegalStateException để tầng trên xử lý thống nhất
            throw new IllegalStateException("Không thể tải danh sách cấp tổ chức hoạt động.", e);
        }
        return list;
    }

    /**
     * Tìm kiếm một cấp tổ chức hoạt động theo ID duy nhất.
     *
     * @param id ID của cấp tổ chức cần tìm
     * @return Đối tượng {@link ActivityOrganizationLevel} khớp ID, hoặc {@code null} nếu ID không hợp lệ hoặc không tìm thấy
     * @throws IllegalStateException Nếu xảy ra lỗi SQL trong quá trình truy vấn
     */
    @Override
    public ActivityOrganizationLevel findById(Integer id) {
        // Trả về null ngay nếu ID bị null hoặc không dương
        if (id == null || id <= 0) {
            return null;
        }
        String sql = "SELECT id, code, name, coefficient, created_at, updated_at "
                + "FROM dbo.activity_organization_level WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Ánh xạ bản ghi đầu tiên nếu tồn tại, ngược lại trả về null
                return rs.next() ? mapOrganizationLevel(rs) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn theo ID
            throw new IllegalStateException("Không thể tìm cấp tổ chức hoạt động theo ID.", e);
        }
    }

    /**
     * Tìm danh sách cấp tổ chức hoạt động khớp với mã cấp tổ chức cho trước.
     *
     * @param code Mã cấp tổ chức cần tìm kiếm
     * @return Danh sách các bản ghi {@link ActivityOrganizationLevel} khớp mã (rỗng nếu không có)
     * @throws IllegalStateException Nếu xảy ra lỗi SQL trong quá trình truy vấn
     */
    @Override
    public List<ActivityOrganizationLevel> findByCode(String code) {
        List<ActivityOrganizationLevel> list = new ArrayList<>();
        String sql = "SELECT id, code, name, coefficient, created_at, updated_at "
                + "FROM dbo.activity_organization_level WHERE code = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt toàn bộ bản ghi khớp mã và thêm vào danh sách kết quả
                while (rs.next()) {
                    list.add(mapOrganizationLevel(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn theo mã
            throw new IllegalStateException("Không thể tìm cấp tổ chức hoạt động theo mã.", e);
        }
        return list;
    }

    /**
     * Thêm mới một cấp tổ chức hoạt động vào cơ sở dữ liệu, làm tròn hệ số nhân về 2 chữ số thập phân (HALF_UP).
     *
     * @param level Đối tượng {@link ActivityOrganizationLevel} chứa dữ liệu cần chèn
     * @return ID tự sinh của bản ghi mới, hoặc {@code null} nếu chèn thất bại
     * @throws IllegalStateException Nếu xảy ra lỗi SQL trong quá trình thêm mới
     */
    @Override
    public Integer insert(ActivityOrganizationLevel level) {
        String sql = "INSERT INTO dbo.activity_organization_level (code, name, coefficient) VALUES (?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, level.getCode());
            ps.setString(2, level.getName());
            // Nếu hệ số khác null thì làm tròn 2 chữ số thập phân HALF_UP trước khi lưu vào DB
            ps.setBigDecimal(3, level.getCoefficient() == null
                    ? null
                    : level.getCoefficient().setScale(2, java.math.RoundingMode.HALF_UP));
            // Nếu không có dòng nào được chèn thì trả về null
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Đọc khóa chính tự tăng đầu tiên nếu driver trả về
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi thêm mới bản ghi
            throw new IllegalStateException("Không thể thêm mới cấp tổ chức hoạt động.", e);
        }
    }

    /**
     * Cập nhật mã, tên, hệ số nhân (làm tròn 2 chữ số thập phân) và thời gian {@code updated_at} (UTC hiện tại)
     * cho một cấp tổ chức hoạt động.
     *
     * @param level Đối tượng {@link ActivityOrganizationLevel} có ID hợp lệ và dữ liệu mới
     * @return {@code true} nếu có ít nhất một dòng được cập nhật; {@code false} nếu dữ liệu không hợp lệ hoặc không tìm thấy dòng khớp
     * @throws IllegalStateException Nếu xảy ra lỗi SQL trong quá trình cập nhật
     */
    @Override
    public boolean update(ActivityOrganizationLevel level) {
        // Từ chối cập nhật nếu đối tượng null hoặc ID không dương
        if (level == null || level.getId() <= 0) {
            return false;
        }
        String sql = "UPDATE dbo.activity_organization_level "
                + "SET code = ?, name = ?, coefficient = ?, updated_at = SYSUTCDATETIME() "
                + "WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, level.getCode());
            ps.setString(2, level.getName());
            // Làm tròn hệ số về 2 chữ số thập phân nếu không null
            ps.setBigDecimal(3, level.getCoefficient() == null
                    ? null
                    : level.getCoefficient().setScale(2, java.math.RoundingMode.HALF_UP));
            ps.setInt(4, level.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi cập nhật bản ghi
            throw new IllegalStateException("Không thể cập nhật cấp tổ chức hoạt động.", e);
        }
    }

    /**
     * Xóa một cấp tổ chức hoạt động khỏi cơ sở dữ liệu theo ID.
     *
     * @param id ID của cấp tổ chức cần xóa
     * @return {@code true} nếu xóa được bản ghi; {@code false} nếu ID không hợp lệ hoặc không tìm thấy bản ghi
     * @throws IllegalStateException Nếu xảy ra lỗi SQL trong quá trình xóa
     */
    @Override
    public boolean delete(Integer id) {
        // Bỏ qua thao tác xóa nếu ID null hoặc không dương
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "DELETE FROM dbo.activity_organization_level WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi xóa bản ghi (ví dụ đang bị khóa ngoại tham chiếu)
            throw new IllegalStateException("Không thể xóa cấp tổ chức hoạt động.", e);
        }
    }
}
