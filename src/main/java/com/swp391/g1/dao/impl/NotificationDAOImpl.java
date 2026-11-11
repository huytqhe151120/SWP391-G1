package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.INotificationDAO;
import com.swp391.g1.dto.response.ChecklistTaskAlertDTO;
import com.swp391.g1.dto.response.NotificationDTO;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAOImpl implements INotificationDAO {

    @Override
    public ChecklistTaskAlertDTO createChecklistTaskNotification(int taskId) {
        String taskSql = """
                SELECT id, activity_id, code, name, priority, deadline
                FROM checklist_task
                WHERE id = ?
                """;
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                ChecklistTaskAlertDTO task;
                String priority;
                try (PreparedStatement ps = c.prepareStatement(taskSql)) {
                    ps.setInt(1, taskId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return null;
                        }
                        priority = rs.getString("priority");
                        if (!"MEDIUM".equalsIgnoreCase(priority) && !"HIGH".equalsIgnoreCase(priority)) {
                            c.rollback();
                            return null;
                        }
                        String code = rs.getString("code");
                        String name = rs.getString("name");
                        Timestamp deadline = rs.getTimestamp("deadline");
                        int activityId = rs.getInt("activity_id");
                        String deadlineDisplay = deadline == null ? "Chưa có hạn"
                                : deadline.toLocalDateTime().format(
                                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                        task = new ChecklistTaskAlertDTO(code, name, priority, deadlineDisplay,
                                "/checklist?action=detail&id=" + taskId + "&activityId=" + activityId,
                                "HIGH".equalsIgnoreCase(priority)
                                        ? findChecklistTaskRecipientEmails(c, taskId) : List.of());
                    }
                }

                List<Integer> recipientIds = findChecklistTaskRecipientIds(c, taskId);
                if (!recipientIds.isEmpty()) {
                    String title = "HIGH".equalsIgnoreCase(priority)
                            ? "Công việc ưu tiên cao được phân công"
                            : "Có công việc mới được phân công";
                    String content = "Công việc " + task.code() + " - " + task.name()
                            + " có mức ưu tiên " + ("HIGH".equalsIgnoreCase(priority) ? "cao" : "trung bình")
                            + ". Deadline: " + task.deadlineDisplay() + ".";
                    int notificationId;
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO notifications (title, content, type, event_type, target_url) "
                                    + "VALUES (?, ?, 'IN_APP', 'CHECKLIST_TASK_ASSIGNED', ?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, title);
                        ps.setString(2, content);
                        ps.setString(3, task.targetUrl());
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (!rs.next()) {
                                throw new SQLException("Checklist notification ID was not generated.");
                            }
                            notificationId = rs.getInt(1);
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO user_notifications (notification_id, user_id) VALUES (?, ?)")) {
                        for (int recipientId : recipientIds) {
                            ps.setInt(1, notificationId);
                            ps.setInt(2, recipientId);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                c.commit();
                return task;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create checklist task notification.", e);
        }
    }

    @Override
    public List<ChecklistTaskAlertDTO> createOverdueChecklistTaskNotifications() {
        String overdueTasksSql = """
                SELECT task.id, task.activity_id, task.code, task.name, task.deadline,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM checklist_task_overdue_alert overdue_alert
                           WHERE overdue_alert.task_id = task.id
                             AND overdue_alert.sent_at <= DATEADD(HOUR, -24, SYSUTCDATETIME())
                       ) THEN 1 ELSE 0 END AS escalation_due
                FROM checklist_task task WITH (UPDLOCK, HOLDLOCK)
                WHERE task.deadline < CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2)
                  AND UPPER(LTRIM(RTRIM(task.status))) NOT IN ('COMPLETED', N'ĐÃ HOÀN THÀNH')
                ORDER BY task.deadline, task.id
                """;
        String escalationStateSql = """
                SELECT
                    (SELECT TOP 1 TRY_CONVERT(INT, history.new_value)
                     FROM checklist_task_history history WITH (UPDLOCK, HOLDLOCK)
                     WHERE history.task_id = ?
                       AND history.action_type = 'UPDATED'
                       AND history.field_name = N'ASSIGNING_ACCOUNT_ID'
                     ORDER BY history.changed_at DESC, history.id DESC) AS assigning_account_id,
                    CASE WHEN EXISTS (
                        SELECT 1
                        FROM checklist_task_history history WITH (UPDLOCK, HOLDLOCK)
                        WHERE history.task_id = ?
                          AND history.action_type = 'UPDATED'
                          AND history.field_name = N'OVERDUE_ESCALATED'
                    ) THEN 1 ELSE 0 END AS already_escalated
                """;
        String alreadyAlertedSql = """
                SELECT 1
                FROM checklist_task_overdue_alert WITH (UPDLOCK, HOLDLOCK)
                WHERE task_id = ?
                  AND alert_date = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATE)
                """;
        String hasPriorAlertSql = """
                SELECT 1
                FROM checklist_task_overdue_alert WITH (UPDLOCK, HOLDLOCK)
                WHERE task_id = ?
                """;
        String insertAlertSql = """
                INSERT INTO checklist_task_overdue_alert (task_id, alert_date)
                VALUES (?, CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATE))
                """;
        DateTimeFormatter deadlineFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<OverdueChecklistTask> overdueTasks = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(overdueTasksSql)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Timestamp deadline = rs.getTimestamp("deadline");
                            overdueTasks.add(new OverdueChecklistTask(
                                    rs.getInt("id"),
                                    rs.getInt("activity_id"),
                                    rs.getString("code"),
                                    rs.getString("name"),
                                    deadline == null ? "" : deadline.toLocalDateTime().format(deadlineFormat),
                                    rs.getBoolean("escalation_due")));
                        }
                    }
                }

                List<ChecklistTaskAlertDTO> createdAlerts = new ArrayList<>();
                for (OverdueChecklistTask task : overdueTasks) {
                    Integer assigningAccountId;
                    boolean alreadyEscalated;
                    try (PreparedStatement ps = c.prepareStatement(escalationStateSql)) {
                        ps.setInt(1, task.id());
                        ps.setInt(2, task.id());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException("Could not read checklist task history for task "
                                        + task.id() + ".");
                            }
                            int accountId = rs.getInt("assigning_account_id");
                            assigningAccountId = rs.wasNull() ? null : accountId;
                            alreadyEscalated = rs.getBoolean("already_escalated");
                        }
                    }

                    if (alreadyEscalated) {
                        continue;
                    }

                    if (task.escalationDue()) {
                        EscalationAssignee assignee = findEscalationAssignee(
                                c, task, assigningAccountId);
                        if (assignee == null) {
                            continue;
                        }
                        ensureEscalationAssignee(c, task.id(), assignee);
                        recordOverdueEscalation(c, task.id(), assignee);
                        createOverdueNotification(c, task, findChecklistTaskRecipientIds(c, task.id()),
                                "Công việc quá hạn 24 giờ đã được giao bổ sung",
                                "Công việc " + task.code() + " - " + task.name()
                                        + " đã quá hạn hơn 24 giờ. Người giao việc được thêm vào danh sách phụ trách.",
                                "CHECKLIST_TASK_ESCALATED");
                        createdAlerts.add(toAlertDTO(task, List.of()));
                        continue;
                    }

                    List<Integer> recipientIds = findChecklistTaskRecipientIds(c, task.id());
                    if (recipientIds.isEmpty()) {
                        continue;
                    }
                    boolean alreadyAlerted;
                    try (PreparedStatement ps = c.prepareStatement(alreadyAlertedSql)) {
                        ps.setInt(1, task.id());
                        try (ResultSet rs = ps.executeQuery()) {
                            alreadyAlerted = rs.next();
                        }
                    }
                    if (alreadyAlerted) continue;

                    boolean firstOverdueAlert;
                    try (PreparedStatement ps = c.prepareStatement(hasPriorAlertSql)) {
                        ps.setInt(1, task.id());
                        try (ResultSet rs = ps.executeQuery()) {
                            firstOverdueAlert = !rs.next();
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement(insertAlertSql)) {
                        ps.setInt(1, task.id());
                        ps.executeUpdate();
                    }
                    createOverdueNotification(c, task, recipientIds,
                            "Công việc checklist đã quá hạn",
                            "Công việc " + task.code() + " - " + task.name()
                                    + " đã quá hạn từ " + task.deadlineDisplay() + ".",
                            "CHECKLIST_TASK_OVERDUE");
                    List<String> recipientEmails = firstOverdueAlert
                            ? findChecklistTaskRecipientEmails(c, task.id()) : List.of();
                    createdAlerts.add(toAlertDTO(task, recipientEmails));
                }

                c.commit();
                return createdAlerts;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create overdue checklist notifications.", e);
        }
    }

    private record OverdueChecklistTask(
            int id, int activityId, String code, String name, String deadlineDisplay, boolean escalationDue) {
    }

    private record EscalationAssignee(String type, int id) {
    }

    private ChecklistTaskAlertDTO toAlertDTO(
            OverdueChecklistTask task, List<String> recipientEmails) {
        return new ChecklistTaskAlertDTO(
                task.code(),
                task.name(),
                "OVERDUE",
                task.deadlineDisplay(),
                "/checklist?action=detail&id=" + task.id() + "&activityId=" + task.activityId(),
                recipientEmails);
    }

    private EscalationAssignee findEscalationAssignee(
            Connection c, OverdueChecklistTask task, Integer assigningAccountId) throws SQLException {
        if (assigningAccountId != null) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM staff WHERE account_id = ? AND status = 'ACTIVE'")) {
                ps.setInt(1, assigningAccountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new EscalationAssignee("STAFF", rs.getInt(1));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM partner_staff WHERE account_id = ? AND status = 'ACTIVE'")) {
                ps.setInt(1, assigningAccountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new EscalationAssignee("PARTNER_STAFF", rs.getInt(1));
                }
            }
        }

        try (PreparedStatement ps = c.prepareStatement("""
                SELECT staff.id
                FROM extracurricular_activity activity
                JOIN staff ON staff.id = activity.responsible_staff_id
                WHERE activity.id = ? AND staff.status = 'ACTIVE'
                """)) {
            ps.setInt(1, task.activityId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new EscalationAssignee("STAFF", rs.getInt(1)) : null;
            }
        }
    }

    private void ensureEscalationAssignee(
            Connection c, int taskId, EscalationAssignee assignee) throws SQLException {
        String table = "STAFF".equals(assignee.type())
                ? "checklist_task_staff" : "checklist_task_partner_staff";
        String idColumn = "STAFF".equals(assignee.type()) ? "staff_id" : "partner_staff_id";
        String existsSql = "SELECT 1 FROM " + table + " WHERE task_id = ? AND " + idColumn + " = ?";
        try (PreparedStatement ps = c.prepareStatement(existsSql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, assignee.id());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO " + table + " (task_id, " + idColumn + ") VALUES (?, ?)")) {
            ps.setInt(1, taskId);
            ps.setInt(2, assignee.id());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO checklist_task_assignment_history
                    (task_id, assignee_type, assignee_id, action_type, assigned_at, ended_at)
                VALUES (?, ?, ?, 'ASSIGNED', SYSUTCDATETIME(), NULL)
                """)) {
            ps.setInt(1, taskId);
            ps.setString(2, assignee.type());
            ps.setInt(3, assignee.id());
            ps.executeUpdate();
        }
    }

    private void recordOverdueEscalation(
            Connection c, int taskId, EscalationAssignee assignee) throws SQLException {
        String sql = """
                INSERT INTO checklist_task_history
                    (task_id, action_type, field_name, old_value, new_value, note,
                     changed_by_account_id, changed_at)
                VALUES (?, 'UPDATED', N'OVERDUE_ESCALATED', NULL, ?, N'Đã quá hạn 24 giờ; thêm người giao việc.',
                        NULL, SYSUTCDATETIME())
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setString(2, assignee.type() + ":" + assignee.id());
            ps.executeUpdate();
        }
    }

    private void createOverdueNotification(
            Connection c, OverdueChecklistTask task, List<Integer> recipientIds,
            String title, String content, String eventType) throws SQLException {
        if (recipientIds.isEmpty()) return;

        String targetUrl = "/checklist?action=detail&id=" + task.id()
                + "&activityId=" + task.activityId();
        int notificationId;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO notifications (title, content, type, event_type, target_url) "
                        + "VALUES (?, ?, 'IN_APP', ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, eventType);
            ps.setString(4, targetUrl);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Overdue checklist notification ID was not generated.");
                }
                notificationId = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO user_notifications (notification_id, user_id) VALUES (?, ?)")) {
            for (int recipientId : recipientIds) {
                ps.setInt(1, notificationId);
                ps.setInt(2, recipientId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<Integer> findChecklistTaskRecipientIds(Connection c, int taskId) throws SQLException {
        String sql = """
                SELECT DISTINCT recipient.account_id
                FROM (
                    SELECT s.account_id
                    FROM checklist_task_staff assignment
                    JOIN staff s ON s.id = assignment.staff_id
                    WHERE assignment.task_id = ? AND s.account_id IS NOT NULL
                    UNION
                    SELECT ps.account_id
                    FROM checklist_task_partner_staff assignment
                    JOIN partner_staff ps ON ps.id = assignment.partner_staff_id
                    WHERE assignment.task_id = ? AND ps.account_id IS NOT NULL
                ) recipient
                """;
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    private List<String> findChecklistTaskRecipientEmails(Connection c, int taskId) throws SQLException {
        String sql = """
                SELECT recipient.email
                FROM (
                    SELECT a.username AS email
                    FROM checklist_task_staff assignment
                    JOIN staff s ON s.id = assignment.staff_id
                    JOIN account a ON a.id = s.account_id
                    WHERE assignment.task_id = ? AND a.username LIKE '%_@_%._%'
                    UNION
                    SELECT a.username AS email
                    FROM checklist_task_partner_staff assignment
                    JOIN partner_staff ps ON ps.id = assignment.partner_staff_id
                    JOIN account a ON a.id = ps.account_id
                    WHERE assignment.task_id = ? AND a.username LIKE '%_@_%._%'
                    UNION
                    SELECT company.email
                    FROM checklist_task_partner_staff assignment
                    JOIN partner_staff ps ON ps.id = assignment.partner_staff_id
                    JOIN partner_company company ON company.id = ps.company_id
                    WHERE assignment.task_id = ? AND company.email LIKE '%_@_%._%'
                ) recipient
                WHERE recipient.email IS NOT NULL AND LTRIM(RTRIM(recipient.email)) <> ''
                """;
        List<String> emails = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, taskId);
            ps.setInt(3, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    emails.add(rs.getString(1));
                }
            }
        }
        return emails;
    }

    private static String filterClause(String filter) {
        if ("unread".equals(filter)) return " AND un.is_read = 0";
        if ("read".equals(filter)) return " AND un.is_read = 1";
        return "";
    }

    @Override
    public int createApprovalNotification(String title, String content, String targetUrl) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO notifications (title, content, type, event_type, target_url) "
                        + "VALUES (?, ?, 'IN_APP', 'ACTIVITY_APPROVAL_REQUEST', ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, content);
                    ps.setString(3, targetUrl);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        id = (int) rs.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO user_notifications (notification_id, user_id) "
                        + "SELECT ?, id FROM account WHERE type = 'ADMIN'")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create notification.", e);
        }
    }

    @Override
    public int createActivityStatusNotification(
            int activityId, String eventType, String title, String content, String targetUrl) {
        String recipientsSql = """
                SELECT s.account_id
                FROM extracurricular_activity a
                JOIN staff s ON s.id = a.responsible_staff_id
                WHERE a.id = ? AND s.account_id IS NOT NULL
                UNION
                SELECT s.account_id
                FROM activity_staff ast
                JOIN staff s ON s.id = ast.staff_id
                WHERE ast.activity_id = ? AND s.account_id IS NOT NULL
                """;
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql,
                activityId, activityId);
    }

    @Override
    public int createPartnerInvitationNotification(
            int invitationId, String eventType, String title, String content, String targetUrl) {
        String recipientsSql = """
                SELECT ps.account_id
                FROM activity_collaboration_invitation i
                JOIN partner_staff ps ON ps.id = i.partner_staff_id
                WHERE i.id = ? AND ps.account_id IS NOT NULL
                UNION
                SELECT ps.account_id
                FROM activity_collaboration_invitation i
                JOIN partner_staff ps ON ps.company_id = i.partner_company_id
                JOIN account acc ON acc.id = ps.account_id
                WHERE i.id = ? AND acc.type = 'PARTNER_STAFF'
                  AND acc.role IN ('PARTNER_LEADER', 'PARTNER_SECRETARY')
                """;
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql,
                invitationId, invitationId);
    }

    @Override
    public int createPartnerInvitationOwnerNotification(
            int invitationId, String eventType, String title, String content, String targetUrl) {
        String recipientsSql = """
                SELECT responsible.account_id
                FROM activity_collaboration_invitation i
                JOIN extracurricular_activity a ON a.id = i.activity_id
                JOIN staff responsible ON responsible.id = a.responsible_staff_id
                WHERE i.id = ? AND responsible.account_id IS NOT NULL
                """;
        return createNotificationForRecipients(eventType, title, content, targetUrl, recipientsSql,
                invitationId);
    }

    private int createNotificationForRecipients(
            String eventType, String title, String content, String targetUrl,
            String recipientsSql, int... recipientParams) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO notifications (title, content, type, event_type, target_url) "
                                + "VALUES (?, ?, 'IN_APP', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, content);
                    ps.setString(3, eventType);
                    ps.setString(4, targetUrl);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Notification ID was not generated.");
                        }
                        id = (int) rs.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO user_notifications (notification_id, user_id) "
                                + "SELECT ?, recipients.account_id FROM (" + recipientsSql + ") recipients")) {
                    ps.setInt(1, id);
                    for (int i = 0; i < recipientParams.length; i++) {
                        ps.setInt(i + 2, recipientParams[i]);
                    }
                    ps.executeUpdate();
                }
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create notification.", e);
        }
    }

    @Override
    public List<NotificationDTO> findByAccount(int accountId, String filter, int offset, int limit) {
        String sql = "SELECT n.id, n.title, n.content, n.target_url, n.created_at, un.is_read "
                + "FROM user_notifications un JOIN notifications n ON n.id = un.notification_id "
                + "WHERE un.user_id = ?" + filterClause(filter)
                + " ORDER BY n.created_at DESC, n.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<NotificationDTO> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationDTO d = new NotificationDTO();
                    d.setId(rs.getInt("id"));
                    d.setTitle(rs.getString("title"));
                    d.setContent(rs.getString("content"));
                    d.setLink(rs.getString("target_url"));
                    d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    d.setRead(rs.getBoolean("is_read"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load notifications.", e);
        }
        return list;
    }

    @Override
    public int countByAccount(int accountId, String filter) {
        return count("SELECT COUNT(*) FROM user_notifications un WHERE un.user_id = ?"
                + filterClause(filter), accountId);
    }

    @Override
    public int countUnread(int accountId) {
        return countByAccount(accountId, "unread");
    }

    private int count(String sql, int accountId) {
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count notifications.", e);
        }
    }

    @Override
    public boolean markRead(int accountId, int notificationId) {
        return update("UPDATE user_notifications SET is_read = 1, read_at = SYSDATETIME() "
                + "WHERE user_id = ? AND notification_id = ? AND is_read = 0", accountId, notificationId) > 0;
    }

    @Override
    public int markAllRead(int accountId) {
        return update("UPDATE user_notifications SET is_read = 1, read_at = SYSDATETIME() "
                + "WHERE user_id = ? AND is_read = 0", accountId, null);
    }

    private int update(String sql, int accountId, Integer notificationId) {
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            if (notificationId != null) ps.setInt(2, notificationId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update notification.", e);
        }
    }
}