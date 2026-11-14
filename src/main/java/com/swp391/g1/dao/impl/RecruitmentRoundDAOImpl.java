package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.dao.IRecruitmentRoundDAO;
import com.swp391.g1.model.RecruitmentPosition;
import com.swp391.g1.model.RecruitmentQuestion;
import com.swp391.g1.model.RecruitmentRound;
import com.swp391.g1.model.RecruitmentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecruitmentRoundDAOImpl implements IRecruitmentRoundDAO {

    private static final String ELIGIBLE_ACTIVITY =
            "a.approval_status='APPROVED' AND a.activity_status='UPCOMING' "
                    + "AND a.start_time IS NOT NULL AND a.start_time>SYSUTCDATETIME()";

    private static final String SELECT_ROUND = "SELECT r.*, a.name AS activity_name, "
            + "(SELECT COUNT(*) FROM dbo.organizer_application x WHERE x.round_id=r.id AND x.status<>'WITHDRAWN') AS application_count "
            + "FROM dbo.organizer_recruitment_round r JOIN dbo.extracurricular_activity a ON a.id=r.activity_id ";

    public RecruitmentRoundDAOImpl() {
        RecruitmentSchema.ensure();
    }

    @Override
    public RecruitmentRound findById(int id) {
        return findOne(SELECT_ROUND + "WHERE r.id=?", id);
    }

    @Override
    public RecruitmentRound findByActivityId(int activityId) {
        return findOne(SELECT_ROUND + "WHERE r.activity_id=?", activityId);
    }

    private RecruitmentRound findOne(String sql, int param) {
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, param);
            RecruitmentRound round;
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                round = mapRound(rs);
            }
            round.setPositions(loadPositions(c, round.getId()));
            round.setQuestions(loadQuestions(c, round.getId()));
            return round;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải đợt tuyển ban tổ chức.", e);
        }
    }

    @Override
    public List<RecruitmentRound> findAll() {
        List<RecruitmentRound> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement p = c.prepareStatement(SELECT_ROUND + "ORDER BY r.created_at DESC, r.id DESC");
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                list.add(mapRound(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải danh sách đợt tuyển.", e);
        }
    }

    @Override
    public boolean hasApplications(int roundId) {
        try (Connection c = DBContext.getConnection()) {
            return hasApplications(c, roundId);
        } catch (SQLException e) {
            throw new DataAccessException("Không thể kiểm tra hồ sơ của đợt tuyển.", e);
        }
    }

    @Override
    public List<RecruitmentRound> findEligibleActivities(Integer includeActivityId) {
        String sql = "SELECT a.id, a.name FROM dbo.extracurricular_activity a WHERE ((" + ELIGIBLE_ACTIVITY
                + " AND NOT EXISTS (SELECT 1 FROM dbo.organizer_recruitment_round r WHERE r.activity_id=a.id))"
                + " OR a.id=?) ORDER BY a.name";
        List<RecruitmentRound> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, includeActivityId == null ? -1 : includeActivityId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    RecruitmentRound round = new RecruitmentRound();
                    round.setActivityId(rs.getInt("id"));
                    round.setActivityName(rs.getString("name"));
                    list.add(round);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải danh sách hoạt động.", e);
        }
    }

    @Override
    public LocalDateTime findParticipationOpenAt(int activityId) {
        String sql = "SELECT open_at FROM dbo.activity_registration_form WHERE activity_id=?";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? toLocal(rs.getTimestamp(1)) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải đợt đăng ký tham gia.", e);
        }
    }

    @Override
    public List<RecruitmentRound> findOpenRounds() {
        String sql = SELECT_ROUND + "WHERE r.status='OPEN' AND (r.open_at IS NULL OR r.open_at<=SYSDATETIME()) "
                + "AND (r.close_at IS NULL OR r.close_at>SYSDATETIME()) AND " +                 ELIGIBLE_ACTIVITY
                + " ORDER BY r.close_at, r.id";
        List<RecruitmentRound> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                list.add(mapRound(rs));
            }
            for (RecruitmentRound round : list) {
                round.setPositions(loadPositions(c, round.getId()));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tải các đợt đang tuyển.", e);
        }
    }

    @Override
    public int insert(RecruitmentRound round) {
        String sql = "INSERT INTO dbo.organizer_recruitment_round"
                + "(activity_id,title,description,open_at,close_at,status,interview_required,created_by) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, round.getActivityId());
                p.setString(2, round.getTitle());
                p.setString(3, round.getDescription());
                setTimestamp(p, 4, round.getOpenAt());
                setTimestamp(p, 5, round.getCloseAt());
                p.setString(6, round.getStatus() == null ? RecruitmentStatus.ROUND_DRAFT : round.getStatus());
                p.setBoolean(7, round.isInterviewRequired());
                if (round.getCreatedBy() == null) {
                    p.setNull(8, java.sql.Types.INTEGER);
                } else {
                    p.setInt(8, round.getCreatedBy());
                }
                p.executeUpdate();
                int id;
                try (ResultSet keys = p.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Không lấy được id đợt tuyển.");
                    }
                    id = keys.getInt(1);
                }
                insertPositions(c, id, round.getPositions());
                insertQuestions(c, id, round.getQuestions());
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw wrap("Không thể tạo đợt tuyển.", e);
        }
    }

    @Override
    public boolean update(RecruitmentRound round, boolean replaceStructure) {
        String sql = "UPDATE dbo.organizer_recruitment_round SET title=?,description=?,open_at=?,close_at=?,"
                + "interview_required=?,updated_at=SYSDATETIME() WHERE id=?";
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, round.getTitle());
                p.setString(2, round.getDescription());
                setTimestamp(p, 3, round.getOpenAt());
                setTimestamp(p, 4, round.getCloseAt());
                p.setBoolean(5, round.isInterviewRequired());
                p.setInt(6, round.getId());
                if (p.executeUpdate() == 0) {
                    c.rollback();
                    return false;
                }
                if (replaceStructure) {
                    if (hasApplications(c, round.getId())) {
                        c.rollback();
                        return false;
                    }
                    execute(c, "DELETE FROM dbo.organizer_recruitment_position WHERE round_id=?", round.getId());
                    execute(c, "DELETE FROM dbo.organizer_recruitment_question WHERE round_id=?", round.getId());
                    insertPositions(c, round.getId(), round.getPositions());
                    insertQuestions(c, round.getId(), round.getQuestions());
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw wrap("Không thể cập nhật đợt tuyển.", e);
        }
    }

    @Override
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE dbo.organizer_recruitment_round SET status=?,updated_at=SYSDATETIME() WHERE id=?";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, status);
            p.setInt(2, id);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw wrap("Không thể đổi trạng thái đợt tuyển.", e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (countActive(c, id) > 0) {
                    c.rollback();
                    return false;
                }
                execute(c, "DELETE FROM dbo.organizer_application WHERE round_id=?", id);
                boolean deleted = execute(c, "DELETE FROM dbo.organizer_recruitment_round WHERE id=?", id) > 0;
                c.commit();
                return deleted;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw wrap("Không thể xóa đợt tuyển.", e);
        }
    }

    @Override
    public int countActiveApplications(int roundId) {
        try (Connection c = DBContext.getConnection()) {
            return countActive(c, roundId);
        } catch (SQLException e) {
            throw new DataAccessException("Không thể đếm hồ sơ của đợt tuyển.", e);
        }
    }

    @Override
    public boolean isActivityEligible(int activityId) {
        String sql = "SELECT 1 FROM dbo.extracurricular_activity a WHERE a.id=? AND " + ELIGIBLE_ACTIVITY;
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, activityId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể kiểm tra hoạt động.", e);
        }
    }

    @Override
    public int synchronizeStatuses() {
        String sql = "UPDATE r SET status='CLOSED',updated_at=SYSDATETIME() FROM dbo.organizer_recruitment_round r "
                + "JOIN dbo.extracurricular_activity a ON a.id=r.activity_id "
                + "WHERE r.status='OPEN' AND ((r.close_at IS NOT NULL AND r.close_at<=SYSDATETIME()) "
                + "OR a.activity_status<>'UPCOMING' OR a.start_time IS NULL OR a.start_time<=SYSUTCDATETIME())";
        try (Connection c = DBContext.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            return p.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Không thể đồng bộ trạng thái đợt tuyển.", e);
        }
    }

    private static int countActive(Connection c, int roundId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.organizer_application WHERE round_id=? AND status<>'WITHDRAWN'";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, roundId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static boolean hasApplications(Connection c, int roundId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM dbo.organizer_application WHERE round_id=?")) {
            p.setInt(1, roundId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int execute(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            return p.executeUpdate();
        }
    }

    private static void insertPositions(Connection c, int roundId, List<RecruitmentPosition> positions) throws SQLException {
        if (positions == null) {
            return;
        }
        String sql = "INSERT INTO dbo.organizer_recruitment_position(round_id,name,quota,sort_order) VALUES (?,?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            int order = 0;
            for (RecruitmentPosition pos : positions) {
                p.setInt(1, roundId);
                p.setString(2, pos.getName());
                p.setInt(3, pos.getQuota());
                p.setInt(4, order++);
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private static void insertQuestions(Connection c, int roundId, List<RecruitmentQuestion> questions) throws SQLException {
        if (questions == null) {
            return;
        }
        String sql = "INSERT INTO dbo.organizer_recruitment_question"
                + "(round_id,question_text,question_type,required,options,sort_order) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            int order = 0;
            for (RecruitmentQuestion q : questions) {
                p.setInt(1, roundId);
                p.setString(2, q.getQuestionText());
                p.setString(3, q.getQuestionType());
                p.setBoolean(4, q.isRequired());
                p.setString(5, q.getOptions());
                p.setInt(6, order++);
                p.addBatch();
            }
            p.executeBatch();
        }
    }

    private static List<RecruitmentPosition> loadPositions(Connection c, int roundId) throws SQLException {
        String sql = "SELECT p.id,p.round_id,p.name,p.quota,p.sort_order, "
                + "(SELECT COUNT(*) FROM dbo.organizer_application x WHERE x.accepted_position_id=p.id AND x.status='ACCEPTED') AS accepted_count "
                + "FROM dbo.organizer_recruitment_position p WHERE p.round_id=? ORDER BY p.sort_order,p.id";
        List<RecruitmentPosition> list = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, roundId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    RecruitmentPosition pos = new RecruitmentPosition();
                    pos.setId(rs.getInt("id"));
                    pos.setRoundId(rs.getInt("round_id"));
                    pos.setName(rs.getString("name"));
                    pos.setQuota(rs.getInt("quota"));
                    pos.setSortOrder(rs.getInt("sort_order"));
                    pos.setAcceptedCount(rs.getInt("accepted_count"));
                    list.add(pos);
                }
            }
        }
        return list;
    }

    private static List<RecruitmentQuestion> loadQuestions(Connection c, int roundId) throws SQLException {
        String sql = "SELECT id,round_id,question_text,question_type,required,options,sort_order "
                + "FROM dbo.organizer_recruitment_question WHERE round_id=? ORDER BY sort_order,id";
        List<RecruitmentQuestion> list = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, roundId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    RecruitmentQuestion q = new RecruitmentQuestion();
                    q.setId(rs.getInt("id"));
                    q.setRoundId(rs.getInt("round_id"));
                    q.setQuestionText(rs.getString("question_text"));
                    q.setQuestionType(rs.getString("question_type"));
                    q.setRequired(rs.getBoolean("required"));
                    q.setOptions(rs.getString("options"));
                    q.setSortOrder(rs.getInt("sort_order"));
                    list.add(q);
                }
            }
        }
        return list;
    }

    private static RecruitmentRound mapRound(ResultSet rs) throws SQLException {
        RecruitmentRound r = new RecruitmentRound();
        r.setId(rs.getInt("id"));
        r.setActivityId(rs.getInt("activity_id"));
        r.setActivityName(rs.getString("activity_name"));
        r.setTitle(rs.getString("title"));
        r.setDescription(rs.getString("description"));
        r.setOpenAt(toLocal(rs.getTimestamp("open_at")));
        r.setCloseAt(toLocal(rs.getTimestamp("close_at")));
        r.setStatus(rs.getString("status"));
        r.setInterviewRequired(rs.getBoolean("interview_required"));
        int createdBy = rs.getInt("created_by");
        r.setCreatedBy(rs.wasNull() ? null : createdBy);
        r.setCreatedAt(toLocal(rs.getTimestamp("created_at")));
        r.setUpdatedAt(toLocal(rs.getTimestamp("updated_at")));
        r.setApplicationCount(rs.getInt("application_count"));
        return r;
    }

    private static LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static void setTimestamp(PreparedStatement p, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            p.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            p.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static DataAccessException wrap(String message, SQLException e) {
        boolean duplicate = e.getErrorCode() == 2627 || e.getErrorCode() == 2601;
        return new DataAccessException(message, e, duplicate);
    }
}
