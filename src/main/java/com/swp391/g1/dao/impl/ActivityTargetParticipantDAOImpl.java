package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IActivityTargetParticipantDAO;
import com.swp391.g1.model.AcademicCohort;
import com.swp391.g1.model.AcademicProgram;
import com.swp391.g1.model.ActivityTargetParticipant;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.MainClass;
import com.swp391.g1.model.Major;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lớp DAO triển khai JDBC thao tác với cấu hình đối tượng tham gia hoạt động ngoại khóa
 * (bảng {@code activity_target_participant}), truy vấn danh mục học thuật và tự động ghi danh
 * sinh viên cho các hoạt động bắt buộc.
 */
public class ActivityTargetParticipantDAOImpl implements IActivityTargetParticipantDAO {

    // Kích thước mỗi lô (batch) khi tự động ghi danh hàng loạt sinh viên để tối ưu hiệu năng và bộ nhớ.
    private static final int BATCH_SIZE = 500;

    /**
     * Đọc cột kiểu số nguyên có thể mang giá trị NULL từ {@link ResultSet}.
     *
     * @param rs  tập kết quả JDBC đang trỏ tới dòng hiện tại
     * @param col tên cột cần đọc
     * @return giá trị {@link Integer} hoặc {@code null} nếu giá trị SQL là NULL
     * @throws SQLException nếu xảy ra lỗi đọc cột từ ResultSet
     */
    private Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int val = rs.getInt(col);
        // Nếu giá trị cột trong DB là NULL thì trả về null thay vì 0 mặc định của JDBC.
        return rs.wasNull() ? null : val;
    }

    /**
     * Gán giá trị số nguyên hoặc SQL NULL vào vị trí tham số trong {@link PreparedStatement}.
     *
     * @param ps    câu lệnh chuẩn bị JDBC
     * @param index vị trí tham số (bắt đầu từ 1)
     * @param value giá trị ID cần gán (null hoặc <= 0 sẽ được lưu thành NULL)
     * @throws SQLException nếu lỗi gán tham số JDBC
     */
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        // Quy ước null hoặc giá trị không dương được gán thành SQL NULL (wildcard không giới hạn).
        if (value == null || value <= 0) {
            ps.setNull(index, Types.INTEGER);
        } else {
            // Ngược lại gán trực tiếp giá trị số nguyên dương.
            ps.setInt(index, value);
        }
    }

    /**
     * Truy vấn danh sách các Khoa chuyên môn đang hoạt động (có loại "Khoa chuyên môn"
     * hoặc đang quản lý ít nhất một ngành học).
     *
     * @return danh sách {@link Department} sắp xếp theo ID tăng dần
     */
    @Override
    public List<Department> findActiveAcademicDepartments() {
        List<Department> list = new ArrayList<>();
        // Lọc các đơn vị ACTIVE là khoa chuyên môn hoặc có ngành học trực thuộc trong bảng major.
        String sql = """
                SELECT id, code, name, type, location, status
                FROM dbo.department
                WHERE status = 'ACTIVE'
                  AND (type = N'Khoa chuyên môn' OR EXISTS (
                       SELECT 1 FROM dbo.major m WHERE m.responsible_department_id = dbo.department.id
                  ))
                ORDER BY id ASC
                """;
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt từng dòng kết quả và ánh xạ sang model Department.
            while (rs.next()) {
                Department d = new Department();
                d.setId(rs.getInt("id"));
                d.setCode(rs.getString("code"));
                d.setName(rs.getString("name"));
                d.setType(rs.getString("type"));
                d.setLocation(rs.getString("location"));
                list.add(d);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime kèm thông điệp tiếng Việt.
            throw new IllegalStateException("Không thể tải danh sách Khoa.", e);
        }
        return list;
    }

    /**
     * Truy vấn danh sách các Niên khóa đang ở trạng thái hoạt động (ACTIVE).
     *
     * @return danh sách {@link AcademicCohort} sắp xếp theo ID tăng dần
     */
    @Override
    public List<AcademicCohort> findActiveCohorts() {
        List<AcademicCohort> list = new ArrayList<>();
        String sql = "SELECT id, code, name, description, status FROM dbo.academic_cohort WHERE status = 'ACTIVE' ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt từng dòng kết quả và ánh xạ sang model AcademicCohort.
            while (rs.next()) {
                AcademicCohort c = new AcademicCohort();
                c.setId(rs.getInt("id"));
                c.setCode(rs.getString("code"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                c.setStatus(rs.getString("status"));
                list.add(c);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime khi truy vấn Niên khóa thất bại.
            throw new IllegalStateException("Không thể tải danh sách Niên khóa.", e);
        }
        return list;
    }

    /**
     * Truy vấn danh sách các Ngành học đang hoạt động (ACTIVE).
     *
     * @return danh sách {@link Major} kèm ID Khoa quản lý, sắp xếp theo ID tăng dần
     */
    @Override
    public List<Major> findActiveMajors() {
        List<Major> list = new ArrayList<>();
        String sql = "SELECT id, responsible_department_id, code, name, status FROM dbo.major WHERE status = 'ACTIVE' ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt từng dòng kết quả và ánh xạ sang model Major.
            while (rs.next()) {
                Major m = new Major();
                m.setId(rs.getInt("id"));
                m.setResponsibleDepartmentId(getNullableInt(rs, "responsible_department_id"));
                m.setCode(rs.getString("code"));
                m.setName(rs.getString("name"));
                m.setStatus(rs.getString("status"));
                list.add(m);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime khi truy vấn Ngành thất bại.
            throw new IllegalStateException("Không thể tải danh sách Ngành.", e);
        }
        return list;
    }

    /**
     * Truy vấn danh sách các Chương trình đào tạo đang hoạt động (ACTIVE).
     *
     * @return danh sách {@link AcademicProgram} kèm ID Ngành quản lý, sắp xếp theo ID tăng dần
     */
    @Override
    public List<AcademicProgram> findActiveAcademicPrograms() {
        List<AcademicProgram> list = new ArrayList<>();
        String sql = "SELECT id, responsible_major_id, code, name, status FROM dbo.academic_program WHERE status = 'ACTIVE' ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt từng dòng kết quả và ánh xạ sang model AcademicProgram.
            while (rs.next()) {
                AcademicProgram p = new AcademicProgram();
                p.setId(rs.getInt("id"));
                p.setResponsibleMajorId(getNullableInt(rs, "responsible_major_id"));
                p.setCode(rs.getString("code"));
                p.setName(rs.getString("name"));
                p.setStatus(rs.getString("status"));
                list.add(p);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime khi truy vấn Chương trình đào tạo thất bại.
            throw new IllegalStateException("Không thể tải danh sách Chương trình đào tạo.", e);
        }
        return list;
    }

    /**
     * Truy vấn danh sách các Lớp sinh hoạt đang hoạt động (ACTIVE).
     *
     * @return danh sách {@link MainClass} sắp xếp theo mã lớp tăng dần
     */
    @Override
    public List<MainClass> findActiveMainClasses() {
        List<MainClass> list = new ArrayList<>();
        String sql = """
                SELECT id, mode_of_study_id, applicable_cohort_id, responsible_academic_program_id, code, status
                FROM dbo.main_class
                WHERE status = 'ACTIVE'
                ORDER BY code ASC
                """;
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Duyệt từng dòng kết quả và ánh xạ sang model MainClass.
            while (rs.next()) {
                MainClass mc = new MainClass();
                mc.setId(rs.getInt("id"));
                mc.setModeOfStudyId(getNullableInt(rs, "mode_of_study_id"));
                mc.setApplicableCohortId(getNullableInt(rs, "applicable_cohort_id"));
                mc.setResponsibleAcademicProgramId(getNullableInt(rs, "responsible_academic_program_id"));
                mc.setCode(rs.getString("code"));
                mc.setStatus(rs.getString("status"));
                list.add(mc);
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime khi truy vấn Lớp sinh hoạt thất bại.
            throw new IllegalStateException("Không thể tải danh sách Lớp sinh hoạt.", e);
        }
        return list;
    }

    /**
     * Lấy danh sách cấu hình đối tượng tham gia đã lưu của một hoạt động,
     * kèm thông tin mã và tên của Khoa, Niên khóa, Ngành, Chương trình đào tạo, Lớp sinh hoạt.
     *
     * @param activityId ID hoạt động cần tra cứu
     * @return danh sách {@link ActivityTargetParticipant} (rỗng nếu {@code activityId <= 0})
     */
    @Override
    public List<ActivityTargetParticipant> findByActivityId(int activityId) {
        List<ActivityTargetParticipant> list = new ArrayList<>();
        // Bỏ qua truy vấn nếu ID hoạt động không hợp lệ.
        if (activityId <= 0) {
            return list;
        }
        // LEFT JOIN với 5 bảng danh mục để lấy đầy đủ mã/tên hiển thị ngay cả khi một số cột lọc là NULL.
        String sql = """
                SELECT t.id, t.activity_id, t.department_id, t.cohort_id, t.major_id,
                       t.academic_program_id, t.main_class_id, t.created_at,
                       d.code AS department_code, d.name AS department_name,
                       c.code AS cohort_code, c.name AS cohort_name,
                       m.code AS major_code, m.name AS major_name,
                       p.code AS program_code, p.name AS program_name,
                       mc.code AS class_code
                FROM dbo.activity_target_participant t
                LEFT JOIN dbo.department d ON d.id = t.department_id
                LEFT JOIN dbo.academic_cohort c ON c.id = t.cohort_id
                LEFT JOIN dbo.major m ON m.id = t.major_id
                LEFT JOIN dbo.academic_program p ON p.id = t.academic_program_id
                LEFT JOIN dbo.main_class mc ON mc.id = t.main_class_id
                WHERE t.activity_id = ?
                ORDER BY t.id ASC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt từng dòng cấu hình đối tượng tham gia và ánh xạ cả ID lẫn thông tin mô tả từ các bảng JOIN.
                while (rs.next()) {
                    ActivityTargetParticipant t = new ActivityTargetParticipant();
                    t.setId(rs.getLong("id"));
                    t.setActivityId(rs.getInt("activity_id"));
                    t.setDepartmentId(getNullableInt(rs, "department_id"));
                    t.setCohortId(getNullableInt(rs, "cohort_id"));
                    t.setMajorId(getNullableInt(rs, "major_id"));
                    t.setAcademicProgramId(getNullableInt(rs, "academic_program_id"));
                    t.setMainClassId(getNullableInt(rs, "main_class_id"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    // Chỉ chuyển sang LocalDateTime khi cột created_at không null.
                    if (ts != null) {
                        t.setCreatedAt(ts.toLocalDateTime());
                    }
                    t.setDepartmentCode(rs.getString("department_code"));
                    t.setDepartmentName(rs.getString("department_name"));
                    t.setCohortCode(rs.getString("cohort_code"));
                    t.setCohortName(rs.getString("cohort_name"));
                    t.setMajorCode(rs.getString("major_code"));
                    t.setMajorName(rs.getString("major_name"));
                    t.setAcademicProgramCode(rs.getString("program_code"));
                    t.setAcademicProgramName(rs.getString("program_name"));
                    t.setMainClassCode(rs.getString("class_code"));
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Không thể tải cấu hình đối tượng tham gia của hoạt động.", e);
        }
        return list;
    }

    /**
     * Thay thế toàn bộ cấu hình đối tượng tham gia của một hoạt động trong một transaction duy nhất
     * (xóa toàn bộ bản ghi cũ rồi batch insert danh sách mới).
     *
     * @param activityId ID hoạt động cần cập nhật
     * @param targets    danh sách dòng đối tượng tham gia mới (nếu null/rỗng sẽ mặc định lưu 1 dòng toàn trường)
     */
    @Override
    public void replaceTargetsForActivity(int activityId, List<ActivityTargetParticipant> targets) {
        // Không thao tác DB nếu ID hoạt động không hợp lệ.
        if (activityId <= 0) {
            return;
        }
        String deleteSql = "DELETE FROM dbo.activity_target_participant WHERE activity_id = ?";
        String insertSql = """
                INSERT INTO dbo.activity_target_participant
                    (activity_id, department_id, cohort_id, major_id, academic_program_id, main_class_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, GETDATE())
                """;
        // Nếu danh sách truyền vào rỗng hoặc null thì chèn 1 dòng wildcard (toàn bộ cột null = áp dụng toàn trường).
        List<ActivityTargetParticipant> rows = (targets == null || targets.isEmpty())
                ? List.of(new ActivityTargetParticipant(null, null, null, null, null))
                : targets;

        // Mở transaction để đảm bảo thao tác xóa cũ và chèn mới diễn ra nguyên tử (atomic).
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(deleteSql);
                 PreparedStatement ins = conn.prepareStatement(insertSql)) {
                del.setInt(1, activityId);
                del.executeUpdate();

                // Duyệt từng dòng cấu hình đối tượng mới và gom vào batch để chèn một lần.
                for (ActivityTargetParticipant row : rows) {
                    ins.setInt(1, activityId);
                    setNullableInt(ins, 2, row.getDepartmentId());
                    setNullableInt(ins, 3, row.getCohortId());
                    setNullableInt(ins, 4, row.getMajorId());
                    setNullableInt(ins, 5, row.getAcademicProgramId());
                    setNullableInt(ins, 6, row.getMainClassId());
                    ins.addBatch();
                }
                ins.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                // Hoàn tác toàn bộ thay đổi nếu bước xóa hoặc chèn bị lỗi.
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime kèm ID hoạt động.
            throw new IllegalStateException("Không thể lưu cấu hình đối tượng tham gia cho hoạt động " + activityId, e);
        }
    }

    /**
     * Kiểm tra một sinh viên có đủ điều kiện tham gia hoạt động dựa trên cấu hình đối tượng tham gia hay không.
     * Sinh viên đủ điều kiện nếu hoạt động chưa cấu hình giới hạn nào hoặc khớp ít nhất một dòng đối tượng
     * (cột NULL được xem là khớp mọi giá trị).
     *
     * @param activityId ID hoạt động
     * @param studentId  ID sinh viên
     * @return {@code true} nếu sinh viên hợp lệ, ngược lại {@code false}
     */
    @Override
    public boolean isStudentEligible(int activityId, int studentId) {
        // Trả về false ngay nếu ID hoạt động hoặc ID sinh viên không hợp lệ.
        if (activityId <= 0 || studentId <= 0) {
            return false;
        }
        // Truy vấn CASE: trả về 1 nếu hoạt động không có dòng giới hạn nào hoặc sinh viên khớp ít nhất 1 dòng target theo cả 5 tiêu chí.
        String sql = """
                SELECT CASE
                    WHEN NOT EXISTS (
                        SELECT 1 FROM dbo.activity_target_participant t0 WHERE t0.activity_id = ?
                    ) THEN 1
                    WHEN EXISTS (
                        SELECT 1
                        FROM dbo.activity_target_participant t
                        JOIN dbo.student s ON s.id = ?
                        LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
                        LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
                        LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
                        WHERE t.activity_id = ?
                          AND (t.department_id IS NULL OR t.department_id = m.responsible_department_id)
                          AND (t.cohort_id IS NULL OR t.cohort_id = mc.applicable_cohort_id)
                          AND (t.major_id IS NULL OR t.major_id = p.responsible_major_id)
                          AND (t.academic_program_id IS NULL OR t.academic_program_id = mc.responsible_academic_program_id)
                          AND (t.main_class_id IS NULL OR t.main_class_id = s.main_class_id)
                    ) THEN 1
                    ELSE 0
                END AS IsEligible
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, studentId);
            ps.setInt(3, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("IsEligible") == 1;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Không thể kiểm tra điều kiện đối tượng tham gia của sinh viên.", e);
        }
    }

    /**
     * Tìm tập hợp ID của tất cả các hoạt động đã được duyệt (APPROVED) mà sinh viên đủ điều kiện tham gia.
     *
     * @param studentId ID sinh viên
     * @return tập hợp ID hoạt động phù hợp với hồ sơ học thuật của sinh viên
     */
    @Override
    public Set<Integer> findEligibleActivityIdsForStudent(int studentId) {
        Set<Integer> ids = new HashSet<>();
        // Trả về tập rỗng nếu ID sinh viên không hợp lệ.
        if (studentId <= 0) {
            return ids;
        }
        // Lọc các hoạt động APPROVED không giới hạn đối tượng hoặc có ít nhất một dòng target khớp với lớp/CTĐT/ngành/khoa/niên khóa của sinh viên.
        String sql = """
                SELECT a.id
                FROM dbo.extracurricular_activity a
                JOIN dbo.student s ON s.id = ?
                LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
                LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
                LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
                WHERE a.approval_status = 'APPROVED'
                  AND (
                      NOT EXISTS (
                          SELECT 1 FROM dbo.activity_target_participant t0 WHERE t0.activity_id = a.id
                      )
                      OR EXISTS (
                          SELECT 1
                          FROM dbo.activity_target_participant t
                          WHERE t.activity_id = a.id
                            AND (t.department_id IS NULL OR t.department_id = m.responsible_department_id)
                            AND (t.cohort_id IS NULL OR t.cohort_id = mc.applicable_cohort_id)
                            AND (t.major_id IS NULL OR t.major_id = p.responsible_major_id)
                            AND (t.academic_program_id IS NULL OR t.academic_program_id = mc.responsible_academic_program_id)
                            AND (t.main_class_id IS NULL OR t.main_class_id = s.main_class_id)
                      )
                  )
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt tập kết quả để thu thập toàn bộ ID hoạt động hợp lệ.
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Không thể lọc danh sách hoạt động theo đối tượng sinh viên.", e);
        }
        return ids;
    }

    /**
     * Tự động ghi danh hàng loạt (theo batch) toàn bộ sinh viên đang hoạt động (ACTIVE) thuộc đối tượng
     * tham gia của một hoạt động bắt buộc (MANDATORY) đã được duyệt (APPROVED) mà chưa có trong danh sách tham gia.
     *
     * @param activityId ID hoạt động bắt buộc
     * @return tổng số sinh viên đã được ghi danh mới thành công
     */
    @Override
    public int enrollEligibleStudentsForMandatoryActivity(int activityId) {
        // Không thực hiện nếu ID hoạt động không hợp lệ.
        if (activityId <= 0) {
            return 0;
        }
        // Truy vấn tất cả sinh viên ACTIVE phù hợp đối tượng của hoạt động MANDATORY + APPROVED và chưa có bản ghi trong activity_participant.
        String selectSql = """
                SELECT s.id
                FROM dbo.student s
                LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
                LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id
                LEFT JOIN dbo.major m ON m.id = p.responsible_major_id
                WHERE s.status = 'ACTIVE'
                  AND EXISTS (
                      SELECT 1
                      FROM dbo.extracurricular_activity a
                      WHERE a.id = ?
                        AND a.approval_status = 'APPROVED'
                        AND a.nature = 'MANDATORY'
                  )
                  AND (
                      NOT EXISTS (
                          SELECT 1 FROM dbo.activity_target_participant t0 WHERE t0.activity_id = ?
                      )
                      OR EXISTS (
                          SELECT 1
                          FROM dbo.activity_target_participant t
                          WHERE t.activity_id = ?
                            AND (t.department_id IS NULL OR t.department_id = m.responsible_department_id)
                            AND (t.cohort_id IS NULL OR t.cohort_id = mc.applicable_cohort_id)
                            AND (t.major_id IS NULL OR t.major_id = p.responsible_major_id)
                            AND (t.academic_program_id IS NULL OR t.academic_program_id = mc.responsible_academic_program_id)
                            AND (t.main_class_id IS NULL OR t.main_class_id = s.main_class_id)
                      )
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM dbo.activity_participant ap
                      WHERE ap.activity_id = ? AND ap.student_id = s.id
                  )
                ORDER BY s.id ASC
                """;
        // Chèn bản ghi AUTO_ENROLLED có kiểm tra lại NOT EXISTS để tránh trùng lặp khi chạy đồng thời (idempotent).
        String insertSql = """
                INSERT INTO dbo.activity_participant (activity_id, student_id, registration_status, registration_by)
                SELECT ?, ?, 'AUTO_ENROLLED', NULL
                WHERE NOT EXISTS (
                    SELECT 1 FROM dbo.activity_participant ap
                    WHERE ap.activity_id = ? AND ap.student_id = ?
                )
                """;

        List<Integer> studentIds = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
            selectPs.setInt(1, activityId);
            selectPs.setInt(2, activityId);
            selectPs.setInt(3, activityId);
            selectPs.setInt(4, activityId);
            try (ResultSet rs = selectPs.executeQuery()) {
                // Đọc toàn bộ ID sinh viên cần ghi danh vào bộ nhớ trước khi mở transaction batch insert.
                while (rs.next()) {
                    studentIds.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL khi lấy danh sách sinh viên đủ điều kiện.
            throw new IllegalStateException("Không thể truy vấn danh sách sinh viên tự động ghi danh.", e);
        }

        // Kết thúc sớm nếu không có sinh viên nào cần ghi danh thêm.
        if (studentIds.isEmpty()) {
            return 0;
        }

        int totalInserted = 0;
        // Mở transaction và thực hiện chèn theo từng lô BATCH_SIZE để tránh giữ lock lớn và tối ưu hiệu năng.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                int countInBatch = 0;
                // Duyệt từng sinh viên đủ điều kiện để đưa vào batch insert.
                for (Integer studentId : studentIds) {
                    insertPs.setInt(1, activityId);
                    insertPs.setInt(2, studentId);
                    insertPs.setInt(3, activityId);
                    insertPs.setInt(4, studentId);
                    insertPs.addBatch();
                    countInBatch++;
                    // Khi đạt ngưỡng BATCH_SIZE thì thực thi batch, cộng dồn số dòng chèn thành công và commit lô đó.
                    if (countInBatch >= BATCH_SIZE) {
                        int[] results = insertPs.executeBatch();
                        // Cộng dồn số bản ghi thực sự được chèn trong lô hiện tại.
                        for (int r : results) {
                            // Chỉ cộng các phần tử > 0 (bỏ qua 0 nếu bị NOT EXISTS lọc hoặc mã trạng thái đặc biệt).
                            if (r > 0) totalInserted += r;
                        }
                        conn.commit();
                        countInBatch = 0;
                    }
                }
                // Thực thi và commit phần bản ghi lẻ còn lại trong batch cuối cùng (nếu có).
                if (countInBatch > 0) {
                    int[] results = insertPs.executeBatch();
                    // Cộng dồn số bản ghi chèn thành công ở lô cuối.
                    for (int r : results) {
                        // Chỉ tính các lệnh chèn có số dòng ảnh hưởng dương.
                        if (r > 0) totalInserted += r;
                    }
                    conn.commit();
                }
            } catch (SQLException e) {
                // Hoàn tác lô chưa commit nếu xảy ra lỗi trong quá trình batch insert.
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Không thể batch insert danh sách sinh viên tự động ghi danh.", e);
        }
        return totalInserted;
    }

    /**
     * Xóa các bản ghi sinh viên được tự động ghi danh ({@code AUTO_ENROLLED}) nhưng chưa từng điểm danh
     * và chưa có lịch sử điểm danh nào cho hoạt động (dùng khi hủy duyệt hoạt động bắt buộc).
     *
     * @param activityId ID hoạt động cần dọn dẹp danh sách tự động ghi danh
     */
    @Override
    public void removeUnattendedAutoEnrolledParticipants(int activityId) {
        // Bỏ qua nếu ID hoạt động không hợp lệ.
        if (activityId <= 0) {
            return;
        }
        // Chỉ xóa các dòng AUTO_ENROLLED còn trạng thái NOT_YET và không tồn tại bất kỳ lịch sử điểm danh nào để bảo toàn dữ liệu đã ghi nhận.
        String sql = """
                DELETE ap
                FROM dbo.activity_participant ap
                WHERE ap.activity_id = ?
                  AND ap.registration_status = 'AUTO_ENROLLED'
                  AND ap.attendance_status = 'NOT_YET'
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.activity_attendance_history h
                      WHERE h.activity_participant_id = ap.id
                  )
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Bọc lỗi SQL thành ngoại lệ runtime.
            throw new IllegalStateException("Không thể xóa danh sách ghi danh tự động khi hủy duyệt.", e);
        }
    }
}
