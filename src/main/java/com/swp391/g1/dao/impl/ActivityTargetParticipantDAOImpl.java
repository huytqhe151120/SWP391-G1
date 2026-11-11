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

public class ActivityTargetParticipantDAOImpl implements IActivityTargetParticipantDAO {

    private static final int BATCH_SIZE = 500;

    private Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int val = rs.getInt(col);
        return rs.wasNull() ? null : val;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null || value <= 0) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    @Override
    public List<Department> findActiveAcademicDepartments() {
        List<Department> list = new ArrayList<>();
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
            throw new IllegalStateException("Không thể tải danh sách Khoa.", e);
        }
        return list;
    }

    @Override
    public List<AcademicCohort> findActiveCohorts() {
        List<AcademicCohort> list = new ArrayList<>();
        String sql = "SELECT id, code, name, description, status FROM dbo.academic_cohort WHERE status = 'ACTIVE' ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
            throw new IllegalStateException("Không thể tải danh sách Niên khóa.", e);
        }
        return list;
    }

    @Override
    public List<Major> findActiveMajors() {
        List<Major> list = new ArrayList<>();
        String sql = "SELECT id, responsible_department_id, code, name, status FROM dbo.major WHERE status = 'ACTIVE' ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
            throw new IllegalStateException("Không thể tải danh sách Ngành.", e);
        }
        return list;
    }

    @Override
    public List<AcademicProgram> findActiveAcademicPrograms() {
        List<AcademicProgram> list = new ArrayList<>();
        String sql = "SELECT id, responsible_major_id, code, name, status FROM dbo.academic_program WHERE status = 'ACTIVE' ORDER BY id ASC";
        try (Connection conn = DBContext.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
            throw new IllegalStateException("Không thể tải danh sách Chương trình đào tạo.", e);
        }
        return list;
    }

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
            throw new IllegalStateException("Không thể tải danh sách Lớp sinh hoạt.", e);
        }
        return list;
    }

    @Override
    public List<ActivityTargetParticipant> findByActivityId(int activityId) {
        List<ActivityTargetParticipant> list = new ArrayList<>();
        if (activityId <= 0) {
            return list;
        }
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
            throw new IllegalStateException("Không thể tải cấu hình đối tượng tham gia của hoạt động.", e);
        }
        return list;
    }

    @Override
    public void replaceTargetsForActivity(int activityId, List<ActivityTargetParticipant> targets) {
        if (activityId <= 0) {
            return;
        }
        String deleteSql = "DELETE FROM dbo.activity_target_participant WHERE activity_id = ?";
        String insertSql = """
                INSERT INTO dbo.activity_target_participant
                    (activity_id, department_id, cohort_id, major_id, academic_program_id, main_class_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, GETDATE())
                """;
        List<ActivityTargetParticipant> rows = (targets == null || targets.isEmpty())
                ? List.of(new ActivityTargetParticipant(null, null, null, null, null))
                : targets;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(deleteSql);
                 PreparedStatement ins = conn.prepareStatement(insertSql)) {
                del.setInt(1, activityId);
                del.executeUpdate();

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
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể lưu cấu hình đối tượng tham gia cho hoạt động " + activityId, e);
        }
    }

    @Override
    public boolean isStudentEligible(int activityId, int studentId) {
        if (activityId <= 0 || studentId <= 0) {
            return false;
        }
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
            throw new IllegalStateException("Không thể kiểm tra điều kiện đối tượng tham gia của sinh viên.", e);
        }
    }

    @Override
    public Set<Integer> findEligibleActivityIdsForStudent(int studentId) {
        Set<Integer> ids = new HashSet<>();
        if (studentId <= 0) {
            return ids;
        }
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
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể lọc danh sách hoạt động theo đối tượng sinh viên.", e);
        }
        return ids;
    }

    @Override
    public int enrollEligibleStudentsForMandatoryActivity(int activityId) {
        if (activityId <= 0) {
            return 0;
        }
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
                while (rs.next()) {
                    studentIds.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể truy vấn danh sách sinh viên tự động ghi danh.", e);
        }

        if (studentIds.isEmpty()) {
            return 0;
        }

        int totalInserted = 0;
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                int countInBatch = 0;
                for (Integer studentId : studentIds) {
                    insertPs.setInt(1, activityId);
                    insertPs.setInt(2, studentId);
                    insertPs.setInt(3, activityId);
                    insertPs.setInt(4, studentId);
                    insertPs.addBatch();
                    countInBatch++;
                    if (countInBatch >= BATCH_SIZE) {
                        int[] results = insertPs.executeBatch();
                        for (int r : results) {
                            if (r > 0) totalInserted += r;
                        }
                        conn.commit();
                        countInBatch = 0;
                    }
                }
                if (countInBatch > 0) {
                    int[] results = insertPs.executeBatch();
                    for (int r : results) {
                        if (r > 0) totalInserted += r;
                    }
                    conn.commit();
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể batch insert danh sách sinh viên tự động ghi danh.", e);
        }
        return totalInserted;
    }

    @Override
    public void removeUnattendedAutoEnrolledParticipants(int activityId) {
        if (activityId <= 0) {
            return;
        }
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
            throw new IllegalStateException("Không thể xóa danh sách ghi danh tự động khi hủy duyệt.", e);
        }
    }
}
