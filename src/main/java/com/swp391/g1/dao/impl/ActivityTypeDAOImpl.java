package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityTypeDAO;
import com.swp391.g1.model.ActivityType;
import com.swp391.g1.model.Enum;

import java.sql.*;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai DAO bằng JDBC cho bảng {@code activity_type}.
 * Cung cấp các thao tác tìm kiếm và CRUD cho danh mục loại hoạt động ngoại khóa.
 */
public class ActivityTypeDAOImpl implements IActivityTypeDAO {

    /**
     * Ánh xạ dòng hiện tại của {@link ResultSet} sang đối tượng {@link ActivityType}.
     *
     * @param rs ResultSet đang trỏ tới bản ghi cần đọc
     * @return Đối tượng {@link ActivityType} tương ứng
     * @throws SQLException Nếu có lỗi khi đọc cột từ ResultSet
     */
    private ActivityType mapActivityType(ResultSet rs) throws SQLException {
        ActivityType activityType = new ActivityType();
        activityType.setId(rs.getInt("id"));
        activityType.setCode(rs.getString("code"));
        activityType.setName(rs.getString("name"));
        activityType.setDescription(rs.getString("description"));
        String status = rs.getString("status");
        // Chuyển chuỗi trạng thái sang enum CommonStatus nếu khác null, ngược lại gán null
        activityType.setStatus(status == null ? null : Enum.CommonStatus.valueOf(status));
        return activityType;
    }

    /**
     * Tìm kiếm các loại hoạt động theo mã loại hoạt động.
     *
     * @param code Mã loại hoạt động cần tìm
     * @return Danh sách {@link ActivityType} khớp mã (rỗng nếu không có)
     */
    @Override
    public List<ActivityType> findByCode(String code) {
        List<ActivityType> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_type WHERE code = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả khớp mã và ánh xạ vào danh sách trả về
                while (rs.next()) {
                    list.add(mapActivityType(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành IllegalStateException khi tìm theo mã
            throw new IllegalStateException("Failed to find activity types by code.", e);
        }
        return list;
    }

    /**
     * Tìm kiếm danh sách loại hoạt động theo trạng thái chung ({@link Enum.CommonStatus}).
     *
     * @param status Trạng thái cần lọc (ví dụ ACTIVE, INACTIVE)
     * @return Danh sách {@link ActivityType} có trạng thái tương ứng
     */
    @Override
    public List<ActivityType> findByCommonStatus(Enum.CommonStatus status) {
        List<ActivityType> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_type WHERE status = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng kết quả thỏa trạng thái và ánh xạ sang model
                while (rs.next()) {
                    list.add(mapActivityType(rs));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi truy vấn theo trạng thái
            throw new IllegalStateException("Failed to find activity types by status.", e);
        }
        return list;
    }

    /**
     * Lấy toàn bộ danh sách loại hoạt động trong bảng {@code activity_type}.
     *
     * @return Danh sách tất cả các {@link ActivityType}
     */
    @Override
    public List<ActivityType> findAll() {
        List<ActivityType> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_type";

        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt toàn bộ các dòng trong bảng và ánh xạ sang danh sách model
            while (rs.next()) {
                list.add(mapActivityType(rs));
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tải toàn bộ loại hoạt động
            throw new IllegalStateException("Failed to find all activity types.", e);
        }
        return list;
    }

    /**
     * Tìm kiếm một loại hoạt động theo ID khóa chính.
     *
     * @param id ID của loại hoạt động cần tìm
     * @return Đối tượng {@link ActivityType} nếu tìm thấy, ngược lại trả về {@code null}
     */
    @Override
    public ActivityType findById(Integer id) {
        String sql = "SELECT * FROM activity_type WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Ánh xạ bản ghi nếu tồn tại, ngược lại trả về null
                return rs.next() ? mapActivityType(rs) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi tìm theo ID
            throw new IllegalStateException("Failed to find activity type by ID.", e);
        }
    }

    /**
     * Thêm mới một bản ghi loại hoạt động vào bảng {@code activity_type}.
     *
     * @param activityType Thông tin loại hoạt động cần chèn
     * @return ID tự sinh của bản ghi mới, hoặc {@code null} nếu chèn không thành công
     */
    @Override
    public Integer insert(ActivityType activityType) {
        String sql = "INSERT INTO activity_type "
                + "(code, name, description, status) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, activityType.getCode());
            ps.setString(2, activityType.getName());
            ps.setString(3, activityType.getDescription());
            // Gán NULL kiểu VARCHAR nếu trạng thái chưa được thiết lập
            if (activityType.getStatus() == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                // Ngược lại lưu tên enum trạng thái dưới dạng chuỗi
                ps.setString(4, activityType.getStatus().name());
            }

            // Trả về null nếu câu lệnh INSERT không tác động dòng nào
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Lấy ID tự tăng vừa sinh ra nếu có
                return keys.next() ? keys.getInt(1) : null;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi chèn loại hoạt động mới
            throw new IllegalStateException("Failed to insert activity type.", e);
        }
    }

    /**
     * Cập nhật thông tin (mã, tên, mô tả, trạng thái) của một loại hoạt động theo ID.
     *
     * @param activityType Đối tượng loại hoạt động chứa ID hợp lệ và dữ liệu mới
     * @return {@code true} nếu cập nhật thành công ít nhất 1 dòng, {@code false} nếu dữ liệu không hợp lệ hoặc không tìm thấy ID
     */
    @Override
    public boolean update(ActivityType activityType) {
        // Bỏ qua cập nhật khi đối tượng null hoặc ID không dương
        if (activityType == null || activityType.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE activity_type "
                + "SET code = ?, name = ?,                 description = ?, status = ? "
                                + "WHERE id = ?";

                        try (Connection conn = DBContext.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, activityType.getCode());
                            ps.setString(2, activityType.getName());
                            ps.setString(3, activityType.getDescription());
                            // Nếu status là null thì truyền NULL kiểu VARCHAR xuống DB
                            if (activityType.getStatus() == null) {
                                ps.setNull(4, Types.VARCHAR);
                            } else {
                                // Ngược lại truyền tên enum status tương ứng
                                ps.setString(4, activityType.getStatus().name());
                            }
                            ps.setInt(5, activityType.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi cập nhật loại hoạt động
            throw new IllegalStateException("Failed to update activity type.", e);
        }
    }

    /**
     * Xóa một bản ghi loại hoạt động khỏi cơ sở dữ liệu theo ID.
     *
     * @param id ID loại hoạt động cần xóa
     * @return {@code true} nếu xóa được bản ghi, {@code false} nếu ID null hoặc không tồn tại
     */
    @Override
    public boolean delete(Integer id) {
        // Không thực thi câu lệnh DELETE nếu ID bị null
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM activity_type WHERE id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Bọc lỗi SQL khi xóa loại hoạt động
            throw new IllegalStateException("Failed to delete activity type.", e);
        }
    }
}
