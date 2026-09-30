package com.swp391.g1.common;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBContext {
    public Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("ConnectDB.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException(
                        "Missing src/main/resources/ConnectDB.properties. Copy ConnectDB.properties.example "
                                + "to ConnectDB.properties and configure the SQL Server connection.");
            }
            properties.load(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read ConnectDB.properties.", ex);
        }

        String url = properties.getProperty("url");
        String user = properties.getProperty("userID");
        String password = properties.getProperty("password");
        if (isMissing(url) || isMissing(user) || isMissing(password)
                || url.contains("YOUR_DB_") || user.contains("YOUR_DB_")
                || password.contains("YOUR_DB_")) {
            throw new IllegalStateException(
                    "ConnectDB.properties must contain a real SQL Server URL, userID, and password.");
        }

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("SQL Server JDBC driver is not available.", ex);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}