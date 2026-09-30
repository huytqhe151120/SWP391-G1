package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO extends DBContext {

    private Message mapRow(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setStudentId(rs.getInt("student_id"));
        m.setStaffId(rs.getInt("staff_id"));
        m.setSenderType(rs.getString("sender_type"));
        m.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("sent_at");
        if (ts != null) m.setSentAt(ts.toLocalDateTime());
        m.setRead(rs.getBoolean("is_read"));
        try { m.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        try { m.setStudentCode(rs.getString("student_code")); } catch (SQLException ignored) {}
        try { m.setStaffName(rs.getString("staff_name")); } catch (SQLException ignored) {}
        try { m.setStaffCode(rs.getString("staff_code")); } catch (SQLException ignored) {}
        return m;
    }

    /** Get all messages in a conversation between student and staff */
    public List<Message> getConversation(int studentId, int staffId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, " +
                     "s.name AS student_name, s.code AS student_code, " +
                     "st.name AS staff_name, st.code AS staff_code " +
                     "FROM [dbo].[Message] m " +
                     "LEFT JOIN [dbo].[student] s ON m.student_id = s.id " +
                     "LEFT JOIN [dbo].[staff] st ON m.staff_id = st.id " +
                     "WHERE m.student_id = ? AND m.staff_id = ? " +
                     "ORDER BY m.sent_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get all student conversations for the organizer dashboard (latest message per student) */
    public List<Message> getLatestMessagePerStudent(int staffId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, " +
                     "s.name AS student_name, s.code AS student_code, " +
                     "st.name AS staff_name, st.code AS staff_code " +
                     "FROM [dbo].[Message] m " +
                     "INNER JOIN (" +
                     "  SELECT student_id, MAX(sent_at) AS max_sent " +
                     "  FROM [dbo].[Message] WHERE staff_id = ? GROUP BY student_id" +
                     ") latest ON m.student_id = latest.student_id AND m.sent_at = latest.max_sent " +
                     "LEFT JOIN [dbo].[student] s ON m.student_id = s.id " +
                     "LEFT JOIN [dbo].[staff] st ON m.staff_id = st.id " +
                     "WHERE m.staff_id = ? " +
                     "ORDER BY m.sent_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setInt(2, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Get all messages sent by this student (student inbox view) */
    public List<Message> getStudentMessages(int studentId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, " +
                     "s.name AS student_name, s.code AS student_code, " +
                     "st.name AS staff_name, st.code AS staff_code " +
                     "FROM [dbo].[Message] m " +
                     "LEFT JOIN [dbo].[student] s ON m.student_id = s.id " +
                     "LEFT JOIN [dbo].[staff] st ON m.staff_id = st.id " +
                     "WHERE m.student_id = ? " +
                     "ORDER BY m.sent_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Send a message */
    public boolean send(Message msg) {
        String sql = "INSERT INTO [dbo].[Message] (student_id, staff_id, sender_type, content, sent_at, is_read) " +
                     "VALUES (?, ?, ?, ?, GETDATE(), 0)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, msg.getStudentId());
            ps.setInt(2, msg.getStaffId());
            ps.setString(3, msg.getSenderType());
            ps.setString(4, msg.getContent());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Mark messages as read (for staff reading student messages) */
    public void markAsRead(int studentId, int staffId) {
        String sql = "UPDATE [dbo].[Message] SET is_read = 1 " +
                     "WHERE student_id = ? AND staff_id = ? AND sender_type = 'STUDENT'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, staffId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Count unread messages for a staff */
    public int countUnread(int staffId) {
        String sql = "SELECT COUNT(*) FROM [dbo].[Message] WHERE staff_id = ? AND sender_type = 'STUDENT' AND is_read = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
