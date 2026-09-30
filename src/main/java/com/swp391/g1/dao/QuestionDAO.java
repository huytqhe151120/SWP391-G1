package com.swp391.g1.dao;

import com.swp391.g1.model.Question;
import com.swp391.g1.common.DBContext;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class QuestionDAO extends DBContext {

    // Task 1: Lấy danh sách câu hỏi ĐÃ ĐƯỢC TRẢ LỜI cho Sinh viên xem
    public List<Question> getAnsweredQuestions() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM Question WHERE status = 'ANSWERED' ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setTitle(rs.getString("title"));
                q.setContent(rs.getString("content"));
                q.setAnonymous(rs.getBoolean("is_anonymous"));
                q.setStatus(rs.getString("status"));
                q.setStudentId(rs.getInt("student_id"));
                q.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(q);
            }
        }
        return list;
    }

    // Task 2: Sinh viên gửi câu hỏi mới
    public boolean insertQuestion(Question q) throws SQLException {
        String sql = "INSERT INTO Question (title, content, is_anonymous, status, student_id, created_at) VALUES (?, ?, ?, 'PENDING', ?, GETDATE())";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, q.getTitle());
            ps.setString(2, q.getContent());
            ps.setBoolean(3, q.isAnonymous());
            ps.setInt(4, q.getStudentId());
            
            return ps.executeUpdate() > 0;
        }
    }

    // Task 3: Ban tổ chức xem danh sách câu hỏi ĐANG CHỜ DUYỆT (PENDING)
    public List<Question> getPendingQuestions() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM Question WHERE status = 'PENDING' ORDER BY created_at ASC";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setTitle(rs.getString("title"));
                q.setContent(rs.getString("content"));
                q.setAnonymous(rs.getBoolean("is_anonymous"));
                q.setStatus(rs.getString("status"));
                q.setStudentId(rs.getInt("student_id"));
                q.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(q);
            }
        }
        return list;
    }
}
