package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.INotificationDAO;
import com.swp391.g1.dto.response.NotificationDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAOImpl implements INotificationDAO {

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