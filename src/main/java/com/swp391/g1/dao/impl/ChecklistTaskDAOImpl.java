package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.dao.IChecklistTaskDAO;
import com.swp391.g1.dto.response.ChecklistAssigneeResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssignmentIdsDTO;
import com.swp391.g1.dto.response.ChecklistTaskDetailResponseDTO;
import com.swp391.g1.dto.response.ChecklistTaskResponseDTO;
import com.swp391.g1.model.ChecklistTaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChecklistTaskDAOImpl implements IChecklistTaskDAO {

    private static final String TASK_COLUMNS =
            "SELECT DISTINCT t.id, t.code, t.name, t.description, t.deadline, t.status ";

    @Override
    public List<ChecklistTaskResponseDTO> findAll() {
        String sql = """
                SELECT t.id, t.code, t.name, t.description, t.deadline, t.status
                FROM checklist_task t
                ORDER BY CASE WHEN t.deadline IS NULL THEN 1 ELSE 0 END, t.deadline, t.code
                """;
        return findTasks(sql, null, "all accounts");
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> findActiveStaff() {
        String sql = "SELECT id, code, name FROM staff WHERE status = 'ACTIVE' ORDER BY name, code";
        return findAssignees(sql, false);
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> findActivePartnerStaff() {
        String sql = """
                SELECT ps.id, ps.code, ps.name, pc.name AS company_name
                FROM partner_staff ps
                LEFT JOIN partner_company pc ON pc.id = ps.company_id
                WHERE ps.status = 'ACTIVE'
                ORDER BY ps.name, ps.code
                """;
        return findAssignees(sql, true);
    }

    private List<ChecklistAssigneeResponseDTO> findAssignees(String sql, boolean includeCompany) {
        List<ChecklistAssigneeResponseDTO> assignees = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                assignees.add(new ChecklistAssigneeResponseDTO(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        includeCompany ? rs.getString("company_name") : null));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load checklist assignees.", e);
        }
        return assignees;
    }

    @Override
    public int create(String code, String name, String description, LocalDateTime deadline,
                      String status, List<Integer> staffIds, List<Integer> partnerStaffIds) {
        Set<Integer> uniqueStaffIds = new HashSet<>(staffIds);
        Set<Integer> uniquePartnerStaffIds = new HashSet<>(partnerStaffIds);
        String sql = """
                INSERT INTO checklist_task (code, name, description, deadline, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                validateAssignees(conn, "staff", uniqueStaffIds);
                validateAssignees(conn, "partner_staff", uniquePartnerStaffIds);

                int taskId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, code);
                    ps.setString(2, name);
                    ps.setString(3, description);
                    ps.setTimestamp(4, Timestamp.valueOf(deadline));
                    ps.setString(5, status);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No checklist task identity was returned.");
                        }
                        taskId = keys.getInt(1);
                    }
                }

                insertAssignments(conn, "checklist_task_staff", "staff_id", taskId, uniqueStaffIds);
                insertAssignments(conn, "checklist_task_partner_staff", "partner_staff_id",
                        taskId, uniquePartnerStaffIds);
                conn.commit();
                return taskId;
            } catch (SQLException | IllegalArgumentException e) {
                rollback(conn, e);
                if (e instanceof SQLException sqlException) {
                    throw translate("create checklist task", sqlException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw translate("create checklist task", e);
        }
    }

    @Override
    public ChecklistTaskDetailResponseDTO findDetailById(int taskId) {
        String sql = """
                SELECT id, code, name, description, deadline, status
                FROM checklist_task WHERE id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp deadline = rs.getTimestamp("deadline");
                ChecklistTaskResponseDTO task = new ChecklistTaskResponseDTO(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        deadline == null ? null : deadline.toLocalDateTime(),
                        rs.getString("status"));
                return new ChecklistTaskDetailResponseDTO(
                        task,
                        findAssignedStaff(conn, taskId),
                        findAssignedPartnerStaff(conn, taskId));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load checklist task details.", e);
        }
    }

    @Override
    public ChecklistAssignmentIdsDTO findAssigneeIds(int taskId) {
        List<Integer> staffIds = findIds("SELECT staff_id FROM checklist_task_staff WHERE task_id = ?", taskId);
        List<Integer> partnerStaffIds =
                findIds("SELECT partner_staff_id FROM checklist_task_partner_staff WHERE task_id = ?", taskId);
        return new ChecklistAssignmentIdsDTO(staffIds, partnerStaffIds);
    }

    private List<Integer> findIds(String sql, int taskId) {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load checklist assignees.", e);
        }
        return ids;
    }

    @Override
    public int update(int taskId, String code, String name, String description, LocalDateTime deadline,
                      String status, List<Integer> staffIds, List<Integer> partnerStaffIds) {
        Set<Integer> uniqueStaffIds = new HashSet<>(staffIds);
        Set<Integer> uniquePartnerStaffIds = new HashSet<>(partnerStaffIds);
        String sql = """
                UPDATE checklist_task
                SET code = ?, name = ?, description = ?, deadline = ?, status = ?
                WHERE id = ?
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                validateAssignees(conn, "staff", uniqueStaffIds);
                validateAssignees(conn, "partner_staff", uniquePartnerStaffIds);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, code);
                    ps.setString(2, name);
                    ps.setString(3, description);
                    ps.setTimestamp(4, Timestamp.valueOf(deadline));
                    ps.setString(5, status);
                    ps.setInt(6, taskId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return 0;
                    }
                }

                deleteAssignments(conn, "checklist_task_staff", taskId);
                deleteAssignments(conn, "checklist_task_partner_staff", taskId);
                insertAssignments(conn, "checklist_task_staff", "staff_id", taskId, uniqueStaffIds);
                insertAssignments(conn, "checklist_task_partner_staff", "partner_staff_id",
                        taskId, uniquePartnerStaffIds);
                conn.commit();
                return 1;
            } catch (SQLException | IllegalArgumentException e) {
                rollback(conn, e);
                if (e instanceof SQLException sqlException) {
                    throw translate("update checklist task", sqlException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw translate("update checklist task", e);
        }
    }

    @Override
    public boolean updateStatus(int taskId, String status) {
        String sql = "UPDATE checklist_task SET status = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update checklist task status.", e);
        }
    }

    @Override
    public boolean delete(int taskId) {
        String sql = "DELETE FROM checklist_task WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete checklist task.", e);
        }
    }

    private List<ChecklistAssigneeResponseDTO> findAssignedStaff(Connection conn, int taskId)
            throws SQLException {
        String sql = """
                SELECT s.id, s.code, s.name
                FROM checklist_task_staff cts
                JOIN staff s ON s.id = cts.staff_id
                WHERE cts.task_id = ?
                ORDER BY s.name, s.code
                """;
        return findAssigned(conn, sql, taskId, false);
    }

    private List<ChecklistAssigneeResponseDTO> findAssignedPartnerStaff(Connection conn, int taskId)
            throws SQLException {
        String sql = """
                SELECT ps.id, ps.code, ps.name, pc.name AS company_name
                FROM checklist_task_partner_staff ctps
                JOIN partner_staff ps ON ps.id = ctps.partner_staff_id
                LEFT JOIN partner_company pc ON pc.id = ps.company_id
                WHERE ctps.task_id = ?
                ORDER BY ps.name, ps.code
                """;
        return findAssigned(conn, sql, taskId, true);
    }

    private List<ChecklistAssigneeResponseDTO> findAssigned(
            Connection conn, String sql, int taskId, boolean includeCompany) throws SQLException {
        List<ChecklistAssigneeResponseDTO> assignees = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assignees.add(new ChecklistAssigneeResponseDTO(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            includeCompany ? rs.getString("company_name") : null));
                }
            }
        }
        return assignees;
    }

    private void validateAssignees(Connection conn, String table, Set<Integer> ids) throws SQLException {
        if (ids.isEmpty()) {
            return;
        }
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE status = 'ACTIVE' AND id IN ("
                + String.join(",", java.util.Collections.nCopies(ids.size(), "?")) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int parameterIndex = 1;
            for (int id : ids) {
                ps.setInt(parameterIndex++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) != ids.size()) {
                    throw new IllegalArgumentException("One or more selected assignees are no longer active.");
                }
            }
        }
    }

    private void insertAssignments(Connection conn, String table, String idColumn,
                                   int taskId, Set<Integer> assigneeIds) throws SQLException {
        if (assigneeIds.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO " + table + " (task_id, " + idColumn + ") VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int assigneeId : assigneeIds) {
                ps.setInt(1, taskId);
                ps.setInt(2, assigneeId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteAssignments(Connection conn, String table, int taskId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE task_id = ?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        }
    }

    private void rollback(Connection conn, Exception originalException) {
        try {
            conn.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private DataAccessException translate(String operation, SQLException e) {
        boolean duplicateKey = e.getErrorCode() == 2601 || e.getErrorCode() == 2627;
        return new DataAccessException("Could not " + operation + ".", e, duplicateKey);
    }

    @Override
    public List<ChecklistTaskResponseDTO> findForStaffAccount(int accountId) {
        String sql = TASK_COLUMNS + """
                FROM checklist_task t
                JOIN checklist_task_staff cts ON cts.task_id = t.id
                JOIN staff s ON s.id = cts.staff_id
                WHERE s.account_id = ?
                ORDER BY CASE WHEN t.deadline IS NULL THEN 1 ELSE 0 END, t.deadline, t.code
                """;
        return findTasks(sql, accountId, "staff");
    }

    @Override
    public List<ChecklistTaskResponseDTO> findForPartnerStaffAccount(int accountId) {
        String sql = TASK_COLUMNS + """
                FROM checklist_task t
                JOIN checklist_task_partner_staff ctps ON ctps.task_id = t.id
                JOIN partner_staff ps ON ps.id = ctps.partner_staff_id
                WHERE ps.account_id = ?
                ORDER BY CASE WHEN t.deadline IS NULL THEN 1 ELSE 0 END, t.deadline, t.code
                """;
        return findTasks(sql, accountId, "partner staff");
    }

    private List<ChecklistTaskResponseDTO> findTasks(String sql, Integer accountId, String accountType) {
        List<ChecklistTaskResponseDTO> tasks = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (accountId != null) {
                ps.setInt(1, accountId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp deadline = rs.getTimestamp("deadline");
                    tasks.add(new ChecklistTaskResponseDTO(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("description"),
                            deadline == null ? null : deadline.toLocalDateTime(),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load checklist tasks for " + accountType + ".", e);
        }
        return tasks;
    }
}
