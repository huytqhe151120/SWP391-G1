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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChecklistTaskDAOImpl implements IChecklistTaskDAO {

    private static final String TASK_COLUMNS =
            """
            SELECT DISTINCT t.id, t.activity_id, t.code, t.name, t.description, t.deadline, t.status,
                   CASE WHEN t.deadline IS NULL THEN 1 ELSE 0 END AS deadline_sort,
                   t.phase, t.progress, t.priority,
                   COALESCE((
                       SELECT STRING_AGG(CONVERT(nvarchar(max), assigned.name), N', ')
                       FROM (
                           SELECT s.name
                           FROM checklist_task_staff cts
                           JOIN staff s ON s.id = cts.staff_id
                           WHERE cts.task_id = t.id
                           UNION ALL
                           SELECT ps.name
                           FROM checklist_task_partner_staff ctps
                           JOIN partner_staff ps ON ps.id = ctps.partner_staff_id
                           WHERE ctps.task_id = t.id
                       ) assigned
                   ), N'') AS assignees,
                   COALESCE((
                       SELECT STRING_AGG(CONVERT(nvarchar(max), s.name), N', ')
                       FROM checklist_task_staff cts
                       JOIN staff s ON s.id = cts.staff_id
                       WHERE cts.task_id = t.id
                   ), N'') AS staff_assignees,
                   COALESCE((
                       SELECT STRING_AGG(CONVERT(nvarchar(max), ps.name), N', ')
                       FROM checklist_task_partner_staff ctps
                       JOIN partner_staff ps ON ps.id = ctps.partner_staff_id
                       WHERE ctps.task_id = t.id
                   ), N'') AS partner_staff_assignees,
                   COALESCE((
                       SELECT STRING_AGG(
                           CONVERT(nvarchar(max), prerequisite.code + N' - ' + prerequisite.name), N', ')
                       FROM checklist_task_dependency dependency
                       JOIN checklist_task prerequisite ON prerequisite.id = dependency.depends_on_task_id
                       WHERE dependency.task_id = t.id
                   ), N'') AS prerequisites,
                   CASE WHEN EXISTS (
                       SELECT 1
                       FROM checklist_task_dependency dependency
                       JOIN checklist_task prerequisite ON prerequisite.id = dependency.depends_on_task_id
                       WHERE dependency.task_id = t.id
                         AND UPPER(LTRIM(RTRIM(prerequisite.status)))
                             NOT IN ('COMPLETED', N'ĐÃ HOÀN THÀNH')
                   ) THEN 0 ELSE 1 END AS prerequisites_completed
            """;

    @Override
    public List<ChecklistTaskResponseDTO> findAll() {
        String sql = TASK_COLUMNS + """
                FROM checklist_task t
                ORDER BY t.deadline, t.code
                """;
        return findTasks(sql, "all accounts");
    }

    @Override
    public List<ChecklistTaskResponseDTO> findForActivity(int activityId) {
        String sql = TASK_COLUMNS + """
                FROM checklist_task t
                WHERE t.activity_id = ?
                ORDER BY t.deadline, t.code
                """;
        return findTasks(sql, "activity", activityId);
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> findActiveStaff() {
        return findActiveStaff(0);
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> findActiveStaff(int taskId) {
        String sql = """
                SELECT s.id, s.code, s.name, s.status, staff_role.position AS position,
                       assigned.assigned_at
                FROM staff s
                OUTER APPLY (
                    SELECT TOP 1 sa.position
                    FROM staff_assignment sa
                    WHERE sa.staff_id = s.id
                    ORDER BY sa.department_id
                ) staff_role
                OUTER APPLY (
                    SELECT TOP 1 h.assigned_at
                    FROM checklist_task_assignment_history h
                    WHERE h.task_id = ? AND h.assignee_type = 'STAFF'
                      AND h.assignee_id = s.id AND h.action_type = 'ASSIGNED'
                    ORDER BY h.assigned_at DESC
                ) assigned
                WHERE s.status = 'ACTIVE'
                   OR EXISTS (SELECT 1 FROM checklist_task_staff current_assignment
                              WHERE current_assignment.task_id = ? AND current_assignment.staff_id = s.id)
                ORDER BY s.name, s.code
                """;
        return findAssignees(sql, taskId, false);
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> findActivePartnerStaff() {
        return findActivePartnerStaff(0);
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> findActivePartnerStaff(int taskId) {
        String sql = """
                SELECT ps.id, ps.code, ps.name, ps.status, ps.position,
                       pc.name AS company_name, assigned.assigned_at
                FROM partner_staff ps
                LEFT JOIN partner_company pc ON pc.id = ps.company_id
                OUTER APPLY (
                    SELECT TOP 1 h.assigned_at
                    FROM checklist_task_assignment_history h
                    WHERE h.task_id = ? AND h.assignee_type = 'PARTNER_STAFF'
                      AND h.assignee_id = ps.id AND h.action_type = 'ASSIGNED'
                    ORDER BY h.assigned_at DESC
                ) assigned
                WHERE ps.status = 'ACTIVE'
                   OR EXISTS (SELECT 1 FROM checklist_task_partner_staff current_assignment
                              WHERE current_assignment.task_id = ?
                                AND current_assignment.partner_staff_id = ps.id)
                ORDER BY ps.name, ps.code
                """;
        return findAssignees(sql, taskId, true);
    }

    private List<ChecklistAssigneeResponseDTO> findAssignees(String sql, int taskId, boolean includeCompany) {
        List<ChecklistAssigneeResponseDTO> assignees = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp assignedAt = rs.getTimestamp("assigned_at");
                    assignees.add(new ChecklistAssigneeResponseDTO(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            includeCompany ? rs.getString("company_name") : null,
                            rs.getString("status"),
                            rs.getString("position"),
                            assignedAt == null ? null : assignedAt.toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load checklist assignees.", e);
        }
        return assignees;
    }

    @Override
    public int create(int activityId, String phase, String priority, String code, String name,
                      String description, LocalDateTime deadline, String status, List<Integer> staffIds,
                      List<Integer> partnerStaffIds, List<Integer> prerequisiteTaskIds,
                      int assignedByAccountId) {
        Set<Integer> uniqueStaffIds = new HashSet<>(staffIds);
        Set<Integer> uniquePartnerStaffIds = new HashSet<>(partnerStaffIds);
        Set<Integer> uniquePrerequisiteTaskIds = new HashSet<>(prerequisiteTaskIds);
        String sql = """
                INSERT INTO checklist_task
                    (activity_id, phase, priority, code, name, description, deadline, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                validateAssignees(conn, "staff", uniqueStaffIds);
                validateAssignees(conn, "partner_staff", uniquePartnerStaffIds);
                validatePrerequisites(conn, activityId, null, uniquePrerequisiteTaskIds);
                validatePrerequisiteCompletion(conn, uniquePrerequisiteTaskIds, status);

                int taskId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, activityId);
                    ps.setString(2, phase);
                    ps.setString(3, priority);
                    ps.setString(4, code);
                    ps.setString(5, name);
                    ps.setString(6, description);
                    ps.setTimestamp(7, Timestamp.valueOf(deadline));
                    ps.setString(8, ChecklistTaskStatus.toDatabaseValue(status));
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
                recordTaskAssigner(conn, taskId, assignedByAccountId);
                insertAssignmentHistory(conn, taskId, "STAFF", uniqueStaffIds, "ASSIGNED");
                insertAssignmentHistory(conn, taskId, "PARTNER_STAFF", uniquePartnerStaffIds, "ASSIGNED");
                insertPrerequisites(conn, taskId, uniquePrerequisiteTaskIds);
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
    public boolean addAssignees(int taskId, List<Integer> staffIds, List<Integer> partnerStaffIds,
                                int assignedByAccountId) {
        Set<Integer> newStaffIds = new HashSet<>(staffIds);
        Set<Integer> newPartnerStaffIds = new HashSet<>(partnerStaffIds);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!taskExists(conn, taskId)) {
                    conn.rollback();
                    return false;
                }
                validateAssignees(conn, "staff", newStaffIds);
                validateAssignees(conn, "partner_staff", newPartnerStaffIds);

                Set<Integer> allStaffIds = new HashSet<>(findIds(
                        conn, "SELECT staff_id FROM checklist_task_staff WHERE task_id = ?", taskId));
                allStaffIds.addAll(newStaffIds);
                Set<Integer> allPartnerStaffIds = new HashSet<>(findIds(
                        conn, "SELECT partner_staff_id FROM checklist_task_partner_staff WHERE task_id = ?",
                        taskId));
                allPartnerStaffIds.addAll(newPartnerStaffIds);

                boolean staffChanged = syncAssignments(
                        conn, taskId, "checklist_task_staff", "staff_id", "STAFF", allStaffIds);
                boolean partnerStaffChanged = syncAssignments(conn, taskId, "checklist_task_partner_staff",
                        "partner_staff_id", "PARTNER_STAFF", allPartnerStaffIds);
                if (staffChanged || partnerStaffChanged) {
                    recordTaskAssigner(conn, taskId, assignedByAccountId);
                }
                conn.commit();
                return true;
            } catch (SQLException | IllegalArgumentException e) {
                rollback(conn, e);
                if (e instanceof SQLException sqlException) {
                    throw translate("add checklist task assignees", sqlException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw translate("add checklist task assignees", e);
        }
    }

    private boolean taskExists(Connection conn, int taskId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM checklist_task WHERE id = ?")) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Integer> findIds(Connection conn, String sql, int taskId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    @Override
    public ChecklistTaskDetailResponseDTO findDetailById(int taskId) {
        String sql = TASK_COLUMNS + " FROM checklist_task t WHERE t.id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp deadline = rs.getTimestamp("deadline");
                ChecklistTaskResponseDTO task = mapTask(rs, deadline);
                return new ChecklistTaskDetailResponseDTO(
                        task,
                        findAssignedStaff(conn, taskId),
                        findAssignedPartnerStaff(conn, taskId),
                        findRelatedTasks(conn, taskId, true),
                        findRelatedTasks(conn, taskId, false));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load checklist task details.", e);
        }
    }

    @Override
    public boolean isStaffAssignedToAccount(int taskId, int accountId) {
        String sql = """
                SELECT CASE WHEN EXISTS (
                    SELECT 1
                    FROM checklist_task_staff assignment
                    JOIN staff s ON s.id = assignment.staff_id
                    WHERE assignment.task_id = ? AND s.account_id = ?
                ) THEN 1 ELSE 0 END
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not verify staff checklist task assignment.", e);
        }
    }

    @Override
    public boolean isPartnerStaffAssignedToAccount(int taskId, int accountId) {
        String sql = """
                SELECT CASE WHEN EXISTS (
                    SELECT 1
                    FROM checklist_task_partner_staff assignment
                    JOIN partner_staff ps ON ps.id = assignment.partner_staff_id
                    WHERE assignment.task_id = ? AND ps.account_id = ?
                ) THEN 1 ELSE 0 END
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not verify partner checklist task assignment.", e);
        }
    }

    @Override
    public int findActiveStaffIdForAccount(int accountId) {
        return findActiveAssigneeId("staff", accountId);
    }

    @Override
    public int findActivePartnerStaffIdForAccount(int accountId) {
        return findActiveAssigneeId("partner_staff", accountId);
    }

    private int findActiveAssigneeId(String table, int accountId) {
        if (!"staff".equals(table) && !"partner_staff".equals(table)) {
            throw new IllegalArgumentException("Invalid checklist assignee type.");
        }
        String sql = "SELECT id FROM " + table + " WHERE account_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not find active checklist assignee.", e);
        }
    }

    private List<ChecklistTaskResponseDTO> findRelatedTasks(int taskId, boolean prerequisites) {
        try (Connection conn = DBContext.getConnection()) {
            return findRelatedTasks(conn, taskId, prerequisites);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load related checklist tasks.", e);
        }
    }

    private List<ChecklistTaskResponseDTO> findRelatedTasks(
            Connection conn, int taskId, boolean prerequisites) throws SQLException {
        String sql = TASK_COLUMNS + (prerequisites
                ? """
                  FROM checklist_task t
                  JOIN checklist_task_dependency dependency ON dependency.depends_on_task_id = t.id
                  WHERE dependency.task_id = ?
                  ORDER BY t.deadline, t.code
                  """
                : """
                  FROM checklist_task t
                  JOIN checklist_task_dependency dependency ON dependency.task_id = t.id
                  WHERE dependency.depends_on_task_id = ?
                  ORDER BY t.deadline, t.code
                  """);
        List<ChecklistTaskResponseDTO> tasks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp deadline = rs.getTimestamp("deadline");
                    tasks.add(mapTask(rs, deadline));
                }
            }
        }
        return tasks;
    }

    @Override
    public ChecklistAssignmentIdsDTO findAssigneeIds(int taskId) {
        List<Integer> staffIds = findIds("SELECT staff_id FROM checklist_task_staff WHERE task_id = ?", taskId);
        List<Integer> partnerStaffIds =
                findIds("SELECT partner_staff_id FROM checklist_task_partner_staff WHERE task_id = ?", taskId);
        List<Integer> prerequisiteTaskIds = findIds(
                "SELECT depends_on_task_id FROM checklist_task_dependency WHERE task_id = ?", taskId);
        return new ChecklistAssignmentIdsDTO(staffIds, partnerStaffIds, prerequisiteTaskIds);
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
    public int update(int taskId, int activityId, String phase, String priority, String code, String name,
                      String description, LocalDateTime deadline, String status, List<Integer> staffIds,
                      List<Integer> partnerStaffIds, List<Integer> prerequisiteTaskIds,
                      int assignedByAccountId) {
        Set<Integer> uniqueStaffIds = new HashSet<>(staffIds);
        Set<Integer> uniquePartnerStaffIds = new HashSet<>(partnerStaffIds);
        Set<Integer> uniquePrerequisiteTaskIds = new HashSet<>(prerequisiteTaskIds);
        String sql = """
                UPDATE checklist_task
                SET activity_id = ?, phase = ?, priority = ?, code = ?, name = ?, description = ?, deadline = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                validateAssigneesForTask(conn, "staff", "checklist_task_staff", "staff_id",
                        taskId, uniqueStaffIds);
                validateAssigneesForTask(conn, "partner_staff", "checklist_task_partner_staff",
                        "partner_staff_id", taskId, uniquePartnerStaffIds);
                validatePrerequisites(conn, activityId, taskId, uniquePrerequisiteTaskIds);
                validatePrerequisiteCompletion(conn, uniquePrerequisiteTaskIds, status);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, activityId);
                    ps.setString(2, phase);
                    ps.setString(3, priority);
                    ps.setString(4, code);
                    ps.setString(5, name);
                    ps.setString(6, description);
                    ps.setTimestamp(7, Timestamp.valueOf(deadline));
                    ps.setString(8, ChecklistTaskStatus.toDatabaseValue(status));
                    ps.setInt(9, taskId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return 0;
                    }
                }

                boolean staffChanged = syncAssignments(conn, taskId, "checklist_task_staff",
                        "staff_id", "STAFF", uniqueStaffIds);
                boolean partnerStaffChanged = syncAssignments(conn, taskId, "checklist_task_partner_staff",
                        "partner_staff_id", "PARTNER_STAFF", uniquePartnerStaffIds);
                if (staffChanged || partnerStaffChanged) {
                    recordTaskAssigner(conn, taskId, assignedByAccountId);
                }
                deleteAssignments(conn, "checklist_task_dependency", taskId);
                insertPrerequisites(conn, taskId, uniquePrerequisiteTaskIds);
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
        String databaseStatus = ChecklistTaskStatus.toDatabaseValue(status);
        String sql = """
                UPDATE checklist_task
                SET status = ?
                WHERE id = ?
                  AND (status = ? OR ? = 'TODO' OR NOT EXISTS (
                      SELECT 1
                      FROM checklist_task_dependency dependency
                      JOIN checklist_task prerequisite ON prerequisite.id = dependency.depends_on_task_id
                      WHERE dependency.task_id = checklist_task.id
                        AND UPPER(LTRIM(RTRIM(prerequisite.status)))
                            NOT IN ('COMPLETED', N'ĐÃ HOÀN THÀNH')
                  ))
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseStatus);
            ps.setInt(2, taskId);
            ps.setString(3, databaseStatus);
            ps.setString(4, databaseStatus);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                return true;
            }
            if (taskExists(conn, taskId)) {
                throw new IllegalStateException("PREREQUISITES_INCOMPLETE");
            }
            return false;
        } catch (IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update checklist task status.", e);
        }
    }

    @Override
    public int updateStatuses(int activityId, List<Integer> taskIds, String status) {
        if (!ChecklistTaskStatus.isValid(status)) {
            throw new IllegalArgumentException("Invalid checklist task status.");
        }
        Set<Integer> uniqueTaskIds = new LinkedHashSet<>(taskIds);
        if (uniqueTaskIds.isEmpty()) {
            throw new IllegalArgumentException("At least one checklist task must be selected.");
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(uniqueTaskIds.size(), "?"));
        String selectedTasksSql = "SELECT COUNT(*) FROM checklist_task WHERE activity_id = ? AND id IN ("
                + placeholders + ")";
        String dependenciesSql = """
                SELECT dependency.task_id, dependency.depends_on_task_id, prerequisite.status
                FROM checklist_task_dependency dependency WITH (UPDLOCK, HOLDLOCK)
                JOIN checklist_task prerequisite WITH (UPDLOCK, HOLDLOCK)
                  ON prerequisite.id = dependency.depends_on_task_id
                WHERE dependency.task_id IN (__IDS__)
                """.replace("__IDS__", placeholders);
        String updateSql = "UPDATE checklist_task SET status = ? WHERE activity_id = ? AND id IN ("
                + placeholders + ")";
        String updateSingleSql = """
                UPDATE checklist_task
                SET status = ?
                WHERE id = ? AND activity_id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM checklist_task_dependency dependency
                      JOIN checklist_task prerequisite
                        ON prerequisite.id = dependency.depends_on_task_id
                      WHERE dependency.task_id = checklist_task.id
                        AND UPPER(LTRIM(RTRIM(prerequisite.status)))
                            NOT IN ('COMPLETED', N'ĐÃ HOÀN THÀNH')
                  )
                """;
        String databaseStatus = ChecklistTaskStatus.toDatabaseValue(status);

        try (Connection conn = DBContext.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(selectedTasksSql)) {
                    ps.setInt(1, activityId);
                    bindIds(ps, uniqueTaskIds, 2);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) != uniqueTaskIds.size()) {
                            throw new IllegalArgumentException("Selected tasks must belong to the activity.");
                        }
                    }
                }

                Map<Integer, Integer> pendingPrerequisiteCount = new HashMap<>();
                Map<Integer, List<Integer>> dependentsByPrerequisite = new HashMap<>();
                Set<Integer> tasksWithIncompletePrerequisites = new HashSet<>();
                uniqueTaskIds.forEach(taskId -> pendingPrerequisiteCount.put(taskId, 0));
                try (PreparedStatement ps = conn.prepareStatement(dependenciesSql)) {
                    bindIds(ps, uniqueTaskIds, 1);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int taskId = rs.getInt("task_id");
                            int prerequisiteId = rs.getInt("depends_on_task_id");
                            boolean prerequisiteCompleted = isCompletedStatus(rs.getString("status"));
                            if (!prerequisiteCompleted) {
                                tasksWithIncompletePrerequisites.add(taskId);
                            }
                            if (uniqueTaskIds.contains(prerequisiteId)) {
                                pendingPrerequisiteCount.compute(taskId, (ignored, count) -> count + 1);
                                dependentsByPrerequisite
                                        .computeIfAbsent(prerequisiteId, ignored -> new ArrayList<>())
                                        .add(taskId);
                            } else if ("COMPLETED".equals(databaseStatus)
                                    || (!prerequisiteCompleted && !"TODO".equals(databaseStatus))) {
                                throw new IllegalArgumentException("PREREQUISITES_INCOMPLETE");
                            }
                        }
                    }
                }

                if ("COMPLETED".equals(databaseStatus)) {
                    List<Integer> completionOrder = topologicalOrder(
                            uniqueTaskIds, pendingPrerequisiteCount, dependentsByPrerequisite);
                    for (int taskId : completionOrder) {
                        try (PreparedStatement ps = conn.prepareStatement(updateSingleSql)) {
                            ps.setString(1, databaseStatus);
                            ps.setInt(2, taskId);
                            ps.setInt(3, activityId);
                            if (ps.executeUpdate() != 1) {
                                throw new IllegalArgumentException("PREREQUISITES_INCOMPLETE");
                            }
                        }
                    }
                } else {
                    if ("IN_PROGRESS".equals(databaseStatus) && !tasksWithIncompletePrerequisites.isEmpty()) {
                        throw new IllegalArgumentException("PREREQUISITES_INCOMPLETE");
                    }

                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, databaseStatus);
                        ps.setInt(2, activityId);
                        bindIds(ps, uniqueTaskIds, 3);
                        if (ps.executeUpdate() != uniqueTaskIds.size()) {
                            throw new IllegalStateException("Checklist task status update was incomplete.");
                        }
                    }
                }
                conn.commit();
                return uniqueTaskIds.size();
            } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
                rollback(conn, e);
                if (e instanceof SQLException sqlException) {
                    throw translate("update checklist task statuses", sqlException);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw translate("update checklist task statuses", e);
        }
    }

    private List<Integer> topologicalOrder(
            Set<Integer> taskIds,
            Map<Integer, Integer> pendingPrerequisiteCount,
            Map<Integer, List<Integer>> dependentsByPrerequisite) {
        ArrayDeque<Integer> readyTasks = new ArrayDeque<>();
        pendingPrerequisiteCount.forEach((taskId, count) -> {
            if (count == 0) {
                readyTasks.addLast(taskId);
            }
        });

        List<Integer> orderedTaskIds = new ArrayList<>(taskIds.size());
        while (!readyTasks.isEmpty()) {
            int completedPrerequisiteId = readyTasks.removeFirst();
            orderedTaskIds.add(completedPrerequisiteId);
            for (int dependentId : dependentsByPrerequisite.getOrDefault(
                    completedPrerequisiteId, List.of())) {
                int remainingPrerequisites = pendingPrerequisiteCount.compute(
                        dependentId, (ignored, count) -> count - 1);
                if (remainingPrerequisites == 0) {
                    readyTasks.addLast(dependentId);
                }
            }
        }

        if (orderedTaskIds.size() != taskIds.size()) {
            throw new IllegalArgumentException("Selected checklist tasks contain a prerequisite cycle.");
        }
        return orderedTaskIds;
    }

    private boolean isCompletedStatus(String status) {
        return status != null
                && ("COMPLETED".equalsIgnoreCase(status.trim())
                || "ĐÃ HOÀN THÀNH".equalsIgnoreCase(status.trim()));
    }

    private void bindIds(PreparedStatement ps, Set<Integer> ids, int startIndex) throws SQLException {
        int parameterIndex = startIndex;
        for (int id : ids) {
            ps.setInt(parameterIndex++, id);
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
                SELECT s.id, s.code, s.name, s.status, staff_role.position AS position,
                       assigned.assigned_at
                FROM checklist_task_staff cts
                JOIN staff s ON s.id = cts.staff_id
                OUTER APPLY (
                    SELECT TOP 1 sa.position
                    FROM staff_assignment sa
                    WHERE sa.staff_id = s.id
                    ORDER BY sa.department_id
                ) staff_role
                OUTER APPLY (
                    SELECT TOP 1 h.assigned_at
                    FROM checklist_task_assignment_history h
                    WHERE h.task_id = cts.task_id AND h.assignee_type = 'STAFF'
                      AND h.assignee_id = s.id AND h.action_type = 'ASSIGNED'
                    ORDER BY h.assigned_at DESC
                ) assigned
                WHERE cts.task_id = ?
                ORDER BY s.name, s.code
                """;
        return findAssigned(conn, sql, taskId, false);
    }

    private List<ChecklistAssigneeResponseDTO> findAssignedPartnerStaff(Connection conn, int taskId)
            throws SQLException {
        String sql = """
                SELECT ps.id, ps.code, ps.name, ps.status, ps.position,
                       pc.name AS company_name, assigned.assigned_at
                FROM checklist_task_partner_staff ctps
                JOIN partner_staff ps ON ps.id = ctps.partner_staff_id
                LEFT JOIN partner_company pc ON pc.id = ps.company_id
                OUTER APPLY (
                    SELECT TOP 1 h.assigned_at
                    FROM checklist_task_assignment_history h
                    WHERE h.task_id = ctps.task_id AND h.assignee_type = 'PARTNER_STAFF'
                      AND h.assignee_id = ps.id AND h.action_type = 'ASSIGNED'
                    ORDER BY h.assigned_at DESC
                ) assigned
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
                            includeCompany ? rs.getString("company_name") : null,
                            rs.getString("status"),
                            rs.getString("position"),
                            rs.getTimestamp("assigned_at") == null
                                    ? null : rs.getTimestamp("assigned_at").toLocalDateTime()));
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

    private void validateAssigneesForTask(Connection conn, String assigneeTable, String assignmentTable,
                                          String idColumn, int taskId, Set<Integer> ids)
            throws SQLException {
        if (ids.isEmpty()) {
            return;
        }
        String sql = "SELECT COUNT(*) FROM " + assigneeTable + " person WHERE person.id IN ("
                + String.join(",", java.util.Collections.nCopies(ids.size(), "?"))
                + ") AND (person.status = 'ACTIVE' OR EXISTS (SELECT 1 FROM " + assignmentTable
                + " current_assignment WHERE current_assignment.task_id = ? AND current_assignment."
                + idColumn + " = person.id))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int parameterIndex = 1;
            for (int id : ids) {
                ps.setInt(parameterIndex++, id);
            }
            ps.setInt(parameterIndex, taskId);
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

    private boolean syncAssignments(Connection conn, int taskId, String table, String idColumn,
                                    String assigneeType, Set<Integer> newIds) throws SQLException {
        Set<Integer> currentIds = new HashSet<>();
        String selectSql = "SELECT " + idColumn + " FROM " + table + " WHERE task_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    currentIds.add(rs.getInt(1));
                }
            }
        }

        Set<Integer> removedIds = new HashSet<>(currentIds);
        removedIds.removeAll(newIds);
        if (!removedIds.isEmpty()) {
            String deleteSql = "DELETE FROM " + table + " WHERE task_id = ? AND " + idColumn + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                for (int id : removedIds) {
                    ps.setInt(1, taskId);
                    ps.setInt(2, id);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            insertAssignmentHistory(conn, taskId, assigneeType, removedIds, "REMOVED");
        }

        Set<Integer> addedIds = new HashSet<>(newIds);
        addedIds.removeAll(currentIds);
        insertAssignments(conn, table, idColumn, taskId, addedIds);
        insertAssignmentHistory(conn, taskId, assigneeType, addedIds, "ASSIGNED");
        return !removedIds.isEmpty() || !addedIds.isEmpty();
    }

    private void recordTaskAssigner(Connection conn, int taskId, int assignedByAccountId)
            throws SQLException {
        String sql = """
                INSERT INTO checklist_task_history
                    (task_id, action_type, field_name, old_value, new_value, note,
                     changed_by_account_id, changed_at)
                VALUES (?, 'UPDATED', N'ASSIGNING_ACCOUNT_ID', NULL, ?, N'Người thực hiện phân công.',
                        ?, SYSUTCDATETIME())
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setString(2, Integer.toString(assignedByAccountId));
            ps.setInt(3, assignedByAccountId);
            ps.executeUpdate();
        }
    }

    private void insertAssignmentHistory(Connection conn, int taskId, String assigneeType,
                                         Set<Integer> assigneeIds, String actionType) throws SQLException {
        if (assigneeIds.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO checklist_task_assignment_history
                    (task_id, assignee_type, assignee_id, action_type, assigned_at, ended_at)
                VALUES (?, ?, ?, ?, SYSUTCDATETIME(),
                        CASE WHEN ? = 'REMOVED' THEN SYSUTCDATETIME() ELSE NULL END)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int assigneeId : assigneeIds) {
                ps.setInt(1, taskId);
                ps.setString(2, assigneeType);
                ps.setInt(3, assigneeId);
                ps.setString(4, actionType);
                ps.setString(5, actionType);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void validatePrerequisites(Connection conn, int activityId, Integer taskId,
                                       Set<Integer> prerequisiteTaskIds) throws SQLException {
        if (prerequisiteTaskIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(prerequisiteTaskIds.size(), "?"));
        String sql = "SELECT COUNT(*) FROM checklist_task WHERE activity_id = ? AND id IN ("
                + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            int parameterIndex = 2;
            for (int prerequisiteId : prerequisiteTaskIds) {
                if (taskId != null && prerequisiteId == taskId) {
                    throw new IllegalArgumentException("A task cannot depend on itself.");
                }
                ps.setInt(parameterIndex++, prerequisiteId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) != prerequisiteTaskIds.size()) {
                    throw new IllegalArgumentException(
                            "Prerequisite tasks must belong to the same activity.");
                }
            }
        }

        if (taskId == null) {
            return;
        }
        String cycleSql = """
                WITH dependency_path (task_id, path) AS (
                    SELECT id, CAST('/' + CONVERT(varchar(20), id) + '/' AS varchar(max))
                    FROM checklist_task
                    WHERE id IN (__IDS__)
                    UNION ALL
                    SELECT dependency.depends_on_task_id,
                           CAST(path + CONVERT(varchar(20), dependency.depends_on_task_id) + '/' AS varchar(max))
                    FROM dependency_path
                    JOIN checklist_task_dependency dependency ON dependency.task_id = dependency_path.task_id
                    WHERE dependency_path.path NOT LIKE
                          '%/' + CONVERT(varchar(20), dependency.depends_on_task_id) + '/%'
                )
                SELECT TOP (1) 1 FROM dependency_path WHERE task_id = ?
                OPTION (MAXRECURSION 0)
                """.replace("__IDS__", placeholders);
        try (PreparedStatement ps = conn.prepareStatement(cycleSql)) {
            int parameterIndex = 1;
            for (int prerequisiteId : prerequisiteTaskIds) {
                ps.setInt(parameterIndex++, prerequisiteId);
            }
            ps.setInt(parameterIndex, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalArgumentException("Task prerequisites cannot create a dependency cycle.");
                }
            }
        }
    }

    private void validatePrerequisiteCompletion(Connection conn, Set<Integer> prerequisiteTaskIds,
                                                String status) throws SQLException {
        String databaseStatus = ChecklistTaskStatus.toDatabaseValue(status);
        if ("TODO".equals(databaseStatus) || prerequisiteTaskIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(prerequisiteTaskIds.size(), "?"));
        String sql = """
                SELECT COUNT(*)
                FROM checklist_task
                WHERE id IN (__IDS__)
                  AND UPPER(LTRIM(RTRIM(status))) IN ('COMPLETED', N'ĐÃ HOÀN THÀNH')
                """.replace("__IDS__", placeholders);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int parameterIndex = 1;
            for (int prerequisiteId : prerequisiteTaskIds) {
                ps.setInt(parameterIndex++, prerequisiteId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) != prerequisiteTaskIds.size()) {
                    throw new IllegalArgumentException("PREREQUISITES_INCOMPLETE");
                }
            }
        }
    }

    private void insertPrerequisites(Connection conn, int taskId, Set<Integer> prerequisiteTaskIds)
            throws SQLException {
        String sql = """
                INSERT INTO checklist_task_dependency (task_id, depends_on_task_id)
                VALUES (?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int prerequisiteTaskId : prerequisiteTaskIds) {
                ps.setInt(1, taskId);
                ps.setInt(2, prerequisiteTaskId);
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
        String visiblePrerequisiteFilter = """
                        AND EXISTS (
                            SELECT 1
                            FROM checklist_task_staff prerequisite_assignment
                            JOIN staff prerequisite_staff
                              ON prerequisite_staff.id = prerequisite_assignment.staff_id
                            WHERE prerequisite_assignment.task_id = prerequisite.id
                              AND prerequisite_staff.account_id = ?
                        )
                """;
        String taskColumns = taskColumnsWithPrerequisiteFilter(visiblePrerequisiteFilter);
        String sql = taskColumns + """
                FROM checklist_task t
                JOIN checklist_task_staff cts ON cts.task_id = t.id
                JOIN staff s ON s.id = cts.staff_id
                WHERE s.account_id = ?
                ORDER BY deadline_sort, t.deadline, t.code
                """;
        return findTasks(sql, "staff", accountId, accountId);
    }

    @Override
    public List<ChecklistTaskResponseDTO> findForPartnerStaffAccount(int accountId) {
        String visiblePrerequisiteFilter = """
                        AND EXISTS (
                            SELECT 1
                            FROM checklist_task_partner_staff prerequisite_assignment
                            JOIN partner_staff prerequisite_staff
                              ON prerequisite_staff.id = prerequisite_assignment.partner_staff_id
                            WHERE prerequisite_assignment.task_id = prerequisite.id
                              AND prerequisite_staff.account_id = ?
                        )
                """;
        String taskColumns = taskColumnsWithPrerequisiteFilter(visiblePrerequisiteFilter);
        String sql = taskColumns + """
                FROM checklist_task t
                JOIN checklist_task_partner_staff ctps ON ctps.task_id = t.id
                JOIN partner_staff ps ON ps.id = ctps.partner_staff_id
                WHERE ps.account_id = ?
                ORDER BY deadline_sort, t.deadline, t.code
                """;
        return findTasks(sql, "partner staff", accountId, accountId);
    }

    private String taskColumnsWithPrerequisiteFilter(String filter) {
        int aliasIndex = TASK_COLUMNS.indexOf("AS prerequisites");
        int subqueryEnd = TASK_COLUMNS.lastIndexOf("), N'')", aliasIndex);
        if (aliasIndex < 0 || subqueryEnd < 0) {
            throw new IllegalStateException("Could not locate checklist prerequisite query.");
        }
        return TASK_COLUMNS.substring(0, subqueryEnd) + filter + TASK_COLUMNS.substring(subqueryEnd);
    }

    private List<ChecklistTaskResponseDTO> findTasks(String sql, String accountType, int... accountIds) {
        List<ChecklistTaskResponseDTO> tasks = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < accountIds.length; i++) {
                ps.setInt(i + 1, accountIds[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp deadline = rs.getTimestamp("deadline");
                    tasks.add(mapTask(rs, deadline));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load checklist tasks for " + accountType + ".", e);
        }
        return tasks;
    }

    private ChecklistTaskResponseDTO mapTask(ResultSet rs, Timestamp deadline) throws SQLException {
        return new ChecklistTaskResponseDTO(
                rs.getInt("id"),
                rs.getInt("activity_id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),
                deadline == null ? null : deadline.toLocalDateTime(),
                rs.getString("status"),
                rs.getString("assignees"),
                rs.getString("staff_assignees"),
                rs.getString("partner_staff_assignees"),
                rs.getString("prerequisites"),
                rs.getBoolean("prerequisites_completed"),
                rs.getString("phase"),
                rs.getInt("progress"),
                rs.getString("priority"));
    }
}
