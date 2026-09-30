package com.swp391.g1.common;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionCheck {
    public static void main(String[] args) {
        try (Connection connection = DBContext.getConnection()) {
            System.out.println("Kết nối thành công.");
            System.out.println("Database: " + connection.getCatalog());
        } catch (SQLException e) {
            System.err.println("Kết nối thất bại: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
