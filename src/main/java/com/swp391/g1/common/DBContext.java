package com.swp391.g1.common;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBContext implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DBContext.class.getName());
    private static final String CONFIG_FILE = "ConnectDB.properties";
    private static final String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    protected Connection connection;

    public DBContext() {
        try {
            connection = openConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection could not be established.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return openConnection();
    }

    private static Connection openConnection() throws SQLException {
        // Docker / CI: check environment variables first
        String url  = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        // Local dev fallback: read from ConnectDB.properties
        if (url == null || url.isBlank()) {
            Properties properties = new Properties();
            try (InputStream input = DBContext.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (input == null) {
                    throw new SQLException("Database configuration file not found: " + CONFIG_FILE);
                }
                properties.load(input);
            } catch (IOException e) {
                throw new SQLException("Unable to read database configuration file: " + CONFIG_FILE, e);
            }
            url      = getRequiredProperty(properties, "url");
            user     = getRequiredProperty(properties, "userID");
            password = getRequiredProperty(properties, "password");
        }

        try {
            Class.forName(JDBC_DRIVER, true, DBContext.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC driver is missing from the application runtime.", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static String getRequiredProperty(Properties properties, String key) throws SQLException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing required database property: " + key);
        }
        return value.trim();
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Closing the database connection failed.", e);
            }
        }
    }
}
