package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.IRegistrationFormDAO;
import com.swp391.g1.model.RegistrationApplication;
import com.swp391.g1.model.RegistrationForm;
import com.swp391.g1.model.RegistrationFormField;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RegistrationFormDAOImpl implements IRegistrationFormDAO {
    private static final String SELECT = "SELECT f.*, a.name AS activity_name, " +
            "(SELECT COUNT(*) FROM dbo.activity_registration r WHERE r.form_id=f.id) AS registration_count " +
            "FROM dbo.activity_registration_form f JOIN dbo.extracurricular_activity a ON a.id=f.activity_id ";

    private RegistrationForm map(ResultSet rs) throws SQLException {
        RegistrationForm f = new RegistrationForm();
        f.setId(rs.getInt("id"));
        f.setActivityId(rs.getInt("activity_id"));
        f.setActivityName(rs.getString("activity_name"));
        f.setTitle(rs.getString("title"));
        f.setDescription(rs.getString("description"));
        Timestamp open = rs.getTimestamp("open_at");
        if (open != null) f.setOpenAt(open.toLocalDateTime());
        Timestamp close = rs.getTimestamp("close_at");
        if (close != null) f.setCloseAt(close.toLocalDateTime());
        f.setStatus(rs.getString("status"));
        f.setRegistrationCount(rs.getInt("registration_count"));
        return f;
    }

    @Override
    public List<RegistrationForm> findAll() {
        return query(SELECT + " ORDER BY f.created_at DESC", false);
    }

    @Override
    public List<RegistrationForm> findOpenForms() {
        return query(SELECT + " WHERE f.status='OPENING' AND (f.open_at IS NULL OR f.open_at <= SYSDATETIME()) " +
                "AND (f.close_at IS NULL OR f.close_at > SYSDATETIME()) AND a.approval_status='APPROVED' "
                + "AND a.activity_status='UPCOMING' AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME()) "
                + "ORDER BY f.created_at DESC", false);
    }

    @Override
    public List<RegistrationForm> findOpenFormsForStudent(int studentId) {
        String sql = SELECT + " WHERE f.status='OPENING' AND (f.open_at IS NULL OR f.open_at <= SYSDATETIME()) "
                + "AND (f.close_at IS NULL OR f.close_at > SYSDATETIME()) AND a.approval_status='APPROVED' "
                + "AND a.activity_status='UPCOMING' AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME()) "
                + "AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration r WHERE r.form_id=f.id AND r.student_id=?) "
                + "ORDER BY f.created_at DESC";
        List<RegistrationForm> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) list.add(map(r));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách form đăng ký.", e);
        }
        return list;
    }

    private List<RegistrationForm> query(String sql, boolean ignored) {
        List<RegistrationForm> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) list.add(map(r));
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách form đăng ký.", e);
        }
        return list;
    }

    @Override
    public RegistrationForm findById(int id) {
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(SELECT + " WHERE f.id=?")) {
            p.setInt(1, id);
            RegistrationForm form;
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                form = map(r);
            }
            form.setFields(findFields(c, id));
            return form;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải form đăng ký.", e);
        }
    }

    @Override
    public RegistrationForm findOpenFormById(int id, int studentId) {
        String sql = SELECT + " WHERE f.id=? AND f.status='OPENING' "
                + "AND (f.open_at IS NULL OR f.open_at<=SYSDATETIME()) "
                + "AND (f.close_at IS NULL OR f.close_at>SYSDATETIME()) AND a.approval_status='APPROVED' "
                + "AND a.activity_status='UPCOMING' AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME()) "
                + "AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration r WHERE r.form_id=f.id AND r.student_id=?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.setInt(2, studentId);
            RegistrationForm form;
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                form = map(r);
            }
            form.setFields(findFields(c, id));
            return form;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải form đăng ký.", e);
        }
    }

    @Override
    public boolean isActivityApproved(int activityId) {
        String sql = "SELECT 1 FROM dbo.extracurricular_activity WHERE id=? AND approval_status='APPROVED'";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra trạng thái duyệt của hoạt động.", e);
        }
    }

    @Override
    public boolean hasFormForActivity(int activityId, int exceptFormId) {
        String sql = "SELECT 1 FROM dbo.activity_registration_form WHERE activity_id=? AND id<>?";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            p.setInt(2, exceptFormId);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra form đã tạo cho hoạt động.", e);
        }
    }

    @Override
    public void synchronizeFormStatuses() {
        String closeExpiredSql = "UPDATE dbo.activity_registration_form SET status='CLOSED',updated_at=SYSDATETIME() "
                + "WHERE (status='OPENING' AND ((close_at IS NOT NULL AND close_at<=SYSDATETIME()) "
                + "OR NOT EXISTS(SELECT 1 FROM dbo.extracurricular_activity a "
                + "WHERE a.id=activity_registration_form.activity_id AND a.approval_status='APPROVED' "
                + "AND a.activity_status='UPCOMING' "
                + "AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME())))) "
                + "OR (status='DRAFT' AND close_at IS NOT NULL AND close_at<=SYSDATETIME())";
        String openScheduledSql = "UPDATE f SET status='OPENING',updated_at=SYSDATETIME() "
                + "FROM dbo.activity_registration_form f "
                + "JOIN dbo.extracurricular_activity a ON a.id=f.activity_id "
                + "WHERE f.status='DRAFT' AND a.approval_status='APPROVED' AND a.activity_status='UPCOMING' "
                + "AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME()) "
                + "AND f.open_at IS NOT NULL AND f.open_at<=SYSDATETIME() "
                + "AND (f.close_at IS NULL OR f.close_at>SYSDATETIME())";
        try (Connection c = DBContext.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(closeExpiredSql);
            s.executeUpdate(openScheduledSql);
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể đồng bộ trạng thái form đăng ký theo thời gian.", e);
        }
    }

    @Override
    public List<RegistrationApplication> findApplications(int formId) {
        String sql = "SELECT r.id,r.form_id,r.student_id,"
                + "COALESCE(submitted.student_code,s.code) AS student_code,"
                + "COALESCE(submitted.student_name,s.name) AS student_name,"
                + "COALESCE(submitted.class_code,mc.code) AS class_code,"
                + "COALESCE(submitted.email,s.email) AS email,r.note,r.status,r.submitted_at "
                + "FROM dbo.activity_registration r JOIN dbo.student s ON s.id=r.student_id "
                + "LEFT JOIN dbo.main_class mc ON mc.id=s.main_class_id "
                + "LEFT JOIN (SELECT registration_id,"
                + "MAX(CASE WHEN field_key='application_student_code' THEN answer_value END) AS student_code,"
                + "MAX(CASE WHEN field_key='application_student_name' THEN answer_value END) AS student_name,"
                + "MAX(CASE WHEN field_key='application_class_code' THEN answer_value END) AS class_code,"
                + "MAX(CASE WHEN field_key='application_email' THEN answer_value END) AS email "
                + "FROM dbo.activity_registration_answer GROUP BY registration_id) submitted "
                + "ON submitted.registration_id=r.id WHERE r.form_id=? "
                + "ORDER BY r.submitted_at DESC,r.id DESC";
        List<RegistrationApplication> applications = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, formId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) applications.add(mapApplication(r));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải danh sách đơn đăng ký.", e);
        }
        return applications;
    }

    @Override
    public RegistrationApplication findApplicationById(int registrationId) {
        String sql = "SELECT r.id,r.form_id,r.student_id,"
                + "COALESCE(submitted.student_code,s.code) AS student_code,"
                + "COALESCE(submitted.student_name,s.name) AS student_name,"
                + "COALESCE(submitted.class_code,mc.code) AS class_code,"
                + "COALESCE(submitted.email,s.email) AS email,r.note,r.status,r.submitted_at "
                + "FROM dbo.activity_registration r JOIN dbo.student s ON s.id=r.student_id "
                + "LEFT JOIN dbo.main_class mc ON mc.id=s.main_class_id "
                + "LEFT JOIN (SELECT registration_id,"
                + "MAX(CASE WHEN field_key='application_student_code' THEN answer_value END) AS student_code,"
                + "MAX(CASE WHEN field_key='application_student_name' THEN answer_value END) AS student_name,"
                + "MAX(CASE WHEN field_key='application_class_code' THEN answer_value END) AS class_code,"
                + "MAX(CASE WHEN field_key='application_email' THEN answer_value END) AS email "
                + "FROM dbo.activity_registration_answer GROUP BY registration_id) submitted "
                + "ON submitted.registration_id=r.id WHERE r.id=?";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, registrationId);
            RegistrationApplication application;
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                application = mapApplication(r);
            }
            application.setAnswers(findAnswers(c, registrationId));
            return application;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải chi tiết đơn đăng ký.", e);
        }
    }

    private static RegistrationApplication mapApplication(ResultSet r) throws SQLException {
        RegistrationApplication application = new RegistrationApplication();
        application.setId(r.getInt("id"));
        application.setFormId(r.getInt("form_id"));
        application.setStudentId(r.getInt("student_id"));
        application.setStudentCode(r.getString("student_code"));
        application.setStudentName(r.getString("student_name"));
        application.setClassCode(r.getString("class_code"));
        application.setEmail(r.getString("email"));
        application.setNote(r.getString("note"));
        application.setStatus(r.getString("status"));
        Timestamp submittedAt = r.getTimestamp("submitted_at");
        if (submittedAt != null) application.setSubmittedAt(submittedAt.toLocalDateTime());
        return application;
    }

    private static List<RegistrationFormField> findAnswers(Connection c, int registrationId) throws SQLException {
        List<RegistrationFormField> answers = new ArrayList<>();
        String sql = "SELECT field_key,field_label,answer_value FROM dbo.activity_registration_answer "
                + "WHERE registration_id=? ORDER BY id";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, registrationId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    RegistrationFormField answer = new RegistrationFormField();
                    answer.setFieldKey(r.getString("field_key"));
                    answer.setLabel(r.getString("field_label"));
                    answer.setValue(r.getString("answer_value"));
                    answers.add(answer);
                }
            }
        }
        return answers;
    }

    private List<RegistrationFormField> findFields(Connection c, int formId) throws SQLException {
        List<RegistrationFormField> fields = new ArrayList<>();
        String sql = "SELECT id, form_id, field_key, label, field_type, is_required, options_text, save_to_profile, sort_order "
                + "FROM dbo.activity_registration_form_field WHERE form_id=? ORDER BY sort_order, id";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, formId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    RegistrationFormField field = new RegistrationFormField();
                    field.setId(r.getInt("id"));
                    field.setFormId(r.getInt("form_id"));
                    field.setFieldKey(r.getString("field_key"));
                    field.setLabel(r.getString("label"));
                    field.setFieldType(r.getString("field_type"));
                    field.setRequired(r.getBoolean("is_required"));
                    field.setOptions(r.getString("options_text"));
                    field.setSaveToProfile(r.getBoolean("save_to_profile"));
                    field.setSortOrder(r.getInt("sort_order"));
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    @Override
    public boolean insert(RegistrationForm f) {
        String sql = "INSERT INTO dbo.activity_registration_form(activity_id,title,description,open_at,close_at,status,created_at) "
                + "OUTPUT INSERTED.id SELECT ?,?,?,?,?,?,SYSDATETIME() "
                + "WHERE EXISTS(SELECT 1 FROM dbo.extracurricular_activity "
                + "WHERE id=? AND approval_status='APPROVED') "
                + "AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration_form existing "
                + "WHERE existing.activity_id=? ) "
                + "AND (CAST(? AS datetime2) IS NULL OR CAST(? AS datetime2)>=SYSDATETIME()) "
                + "AND (?<>'OPENING' OR CAST(? AS datetime2) IS NULL "
                + "OR CAST(? AS datetime2)>SYSDATETIME())";
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int formId;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    bindForm(p, f);
                    p.setInt(7, f.getActivityId());
                    p.setInt(8, f.getActivityId());
                    p.setTimestamp(9, f.getOpenAt() == null ? null : Timestamp.valueOf(f.getOpenAt()));
                    p.setTimestamp(10, f.getOpenAt() == null ? null : Timestamp.valueOf(f.getOpenAt()));
                    p.setString(11, f.getStatus());
                    p.setTimestamp(12, f.getCloseAt() == null ? null : Timestamp.valueOf(f.getCloseAt()));
                    p.setTimestamp(13, f.getCloseAt() == null ? null : Timestamp.valueOf(f.getCloseAt()));
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) {
                            c.rollback();
                            return false;
                        }
                        formId = r.getInt(1);
                    }
                }
                saveFields(c, formId, f.getFields());
                c.commit();
                f.setId(formId);
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            if (isUniqueConstraintViolation(e)) return false;
            throw new IllegalStateException("Không thể tạo form đăng ký.", e);
        }
    }

    @Override
    public boolean update(RegistrationForm f) {
        String sql = "UPDATE dbo.activity_registration_form SET activity_id=?,title=?,description=?,open_at=?,close_at=?,status=?,updated_at=SYSDATETIME() "
                + "WHERE id=? AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration r WHERE r.form_id=?) "
                + "AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration_form existing "
                + "WHERE existing.activity_id=? AND existing.id<>?) "
                + "AND EXISTS(SELECT 1 FROM dbo.extracurricular_activity WHERE id=? AND approval_status='APPROVED') "
                + "AND (CAST(? AS datetime2) IS NULL OR CAST(? AS datetime2)>=SYSDATETIME()) "
                + "AND (?<>'OPENING' OR CAST(? AS datetime2) IS NULL "
                + "OR CAST(? AS datetime2)>SYSDATETIME())";
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    bindForm(p, f);
                    p.setInt(7, f.getId());
                    p.setInt(8, f.getId());
                    p.setInt(9, f.getActivityId());
                    p.setInt(10, f.getId());
                    p.setInt(11, f.getActivityId());
                    p.setTimestamp(12, f.getOpenAt() == null ? null : Timestamp.valueOf(f.getOpenAt()));
                    p.setTimestamp(13, f.getOpenAt() == null ? null : Timestamp.valueOf(f.getOpenAt()));
                    p.setString(14, f.getStatus());
                    p.setTimestamp(15, f.getCloseAt() == null ? null : Timestamp.valueOf(f.getCloseAt()));
                    p.setTimestamp(16, f.getCloseAt() == null ? null : Timestamp.valueOf(f.getCloseAt()));
                    changed = p.executeUpdate();
                }
                if (changed == 0) {
                    c.rollback();
                    return false;
                }
                try (PreparedStatement p = c.prepareStatement(
                        "DELETE FROM dbo.activity_registration_form_field WHERE form_id=?")) {
                    p.setInt(1, f.getId());
                    p.executeUpdate();
                }
                saveFields(c, f.getId(), f.getFields());
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            if (isUniqueConstraintViolation(e)) return false;
            throw new IllegalStateException("Không thể cập nhật form đăng ký.", e);
        }
    }

    private static boolean isUniqueConstraintViolation(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            if (current.getErrorCode() == 2601 || current.getErrorCode() == 2627) return true;
        }
        return false;
    }

    private static void bindForm(PreparedStatement p, RegistrationForm f) throws SQLException {
        p.setInt(1, f.getActivityId());
        p.setString(2, f.getTitle());
        p.setString(3, f.getDescription());
        p.setTimestamp(4, f.getOpenAt() == null ? null : Timestamp.valueOf(f.getOpenAt()));
        p.setTimestamp(5, f.getCloseAt() == null ? null : Timestamp.valueOf(f.getCloseAt()));
        p.setString(6, f.getStatus());
    }

    private static void saveFields(Connection c, int formId, List<RegistrationFormField> fields) throws SQLException {
        if (fields == null || fields.isEmpty()) return;
        String sql = "INSERT INTO dbo.activity_registration_form_field"
                + "(form_id,field_key,label,field_type,is_required,options_text,save_to_profile,sort_order) "
                + "VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (RegistrationFormField field : fields) {
                p.setInt(1, formId);
                p.setString(2, field.getFieldKey());
                p.setString(3, field.getLabel());
                p.setString(4, field.getFieldType());
                p.setBoolean(5, field.isRequired());
                p.setString(6, field.getOptions());
                p.setBoolean(7, field.isSaveToProfile());
                p.setInt(8, field.getSortOrder());
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    @Override
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE dbo.activity_registration_form SET status=?,updated_at=SYSDATETIME() "
                + "WHERE id=? AND (?<>'OPENING' OR ("
                + "EXISTS(SELECT 1 FROM dbo.extracurricular_activity a "
                + "WHERE a.id=activity_registration_form.activity_id AND a.approval_status='APPROVED' "
                + "AND a.activity_status='UPCOMING' "
                + "AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME())) "
                + "AND (open_at IS NULL OR open_at>SYSDATETIME() OR status='CLOSED') "
                + "AND (close_at IS NULL OR close_at>SYSDATETIME())))";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, status);
            p.setInt(2, id);
            p.setString(3, status);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật trạng thái form đăng ký.", e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM dbo.activity_registration_form WHERE id=? AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration r WHERE r.form_id=?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.setInt(2, id);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể xóa form đăng ký.", e);
        }
    }

    @Override
    public boolean hasStudentRegistered(int formId, int studentId) {
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement("SELECT 1 FROM dbo.activity_registration WHERE form_id=? AND student_id=?")) {
            p.setInt(1, formId);
            p.setInt(2, studentId);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra đăng ký.", e);
        }
    }

    @Override
    public boolean reviewApplication(int registrationId, String expectedStatus, String newStatus) {
        String sql = "UPDATE dbo.activity_registration SET status=? WHERE id=? AND status=?";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, newStatus);
            p.setInt(2, registrationId);
            p.setString(3, expectedStatus);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật trạng thái đơn đăng ký.", e);
        }
    }

    @Override
    public boolean registerStudent(int formId, int studentId, String note) {
        String sql = "INSERT INTO dbo.activity_registration(form_id,student_id,note,status,submitted_at) SELECT f.id,?,?, 'SUBMITTED',SYSDATETIME() FROM dbo.activity_registration_form f JOIN dbo.extracurricular_activity a ON a.id=f.activity_id WHERE f.id=? AND f.status='OPENING' AND a.approval_status='APPROVED' AND a.activity_status='UPCOMING' AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME()) AND (f.open_at IS NULL OR f.open_at<=SYSDATETIME()) AND (f.close_at IS NULL OR f.close_at>SYSDATETIME()) AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration x WHERE x.form_id=f.id AND x.student_id=?)";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            p.setString(2, note);
            p.setInt(3, formId);
            p.setInt(4, studentId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể gửi đăng ký. Vui lòng kiểm tra form còn mở và bạn chưa đăng ký.", e);
        }
    }

    @Override
    public boolean registerStudentWithAnswers(int formId, int studentId, String note,
            List<RegistrationFormField> answers) {
        String sql = "INSERT INTO dbo.activity_registration(form_id,student_id,note,status,submitted_at) "
                + "OUTPUT INSERTED.id SELECT f.id,?,?, 'SUBMITTED',SYSDATETIME() FROM dbo.activity_registration_form f "
                + "JOIN dbo.extracurricular_activity a ON a.id=f.activity_id "
                + "WHERE f.id=? AND f.status='OPENING' AND a.approval_status='APPROVED' "
                + "AND a.activity_status='UPCOMING' AND (a.start_time IS NULL OR a.start_time>SYSUTCDATETIME()) "
                + "AND (f.open_at IS NULL OR f.open_at<=SYSDATETIME()) "
                + "AND (f.close_at IS NULL OR f.close_at>SYSDATETIME()) "
                + "AND NOT EXISTS(SELECT 1 FROM dbo.activity_registration x WHERE x.form_id=f.id AND x.student_id=?)";
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int registrationId;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setInt(1, studentId);
                    p.setString(2, note);
                    p.setInt(3, formId);
                    p.setInt(4, studentId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) {
                            c.rollback();
                            return false;
                        }
                        registrationId = r.getInt(1);
                    }
                }
                if (answers != null && !answers.isEmpty()) {
                    String answerSql = "INSERT INTO dbo.activity_registration_answer"
                            + "(registration_id,field_key,field_label,answer_value) VALUES(?,?,?,?)";
                    try (PreparedStatement p = c.prepareStatement(answerSql)) {
                        for (RegistrationFormField answer : answers) {
                            p.setInt(1, registrationId);
                            p.setString(2, answer.getFieldKey());
                            p.setString(3, answer.getLabel());
                            p.setString(4, answer.getValue());
                            p.addBatch();
                        }
                        p.executeBatch();
                    }
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể gửi đăng ký cùng câu trả lời.", e);
        }
    }

    @Override
    public Map<String, String> findSavedProfile(int studentId) {
        Map<String, String> profile = new LinkedHashMap<>();
        String sql = "WITH latest_answers AS ("
                + "SELECT answer.field_key, answer.answer_value, "
                + "ROW_NUMBER() OVER(PARTITION BY answer.field_key ORDER BY registration.submitted_at DESC, answer.id DESC) AS row_num "
                + "FROM dbo.activity_registration_answer answer "
                + "JOIN dbo.activity_registration registration ON registration.id=answer.registration_id "
                + "JOIN dbo.activity_registration_form_field field ON field.form_id=registration.form_id "
                + "AND field.field_key=answer.field_key "
                + "WHERE registration.student_id=? AND field.save_to_profile=1 AND answer.answer_value IS NOT NULL "
                + "AND answer.answer_value<>'') "
                + "SELECT field_key,answer_value FROM latest_answers WHERE row_num=1";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) profile.put(r.getString("field_key"), r.getString("answer_value"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải thông tin hồ sơ đã lưu.", e);
        }
        return profile;
    }
}
