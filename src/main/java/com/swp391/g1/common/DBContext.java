package com.swp391.g1.common;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens the JDBC connection described by the developer-local
 * ConnectDB.properties. A missing resource or missing property is logged and
 * leaves {@link #getConnection()} null instead of throwing, so the DAO layer
 * reports its usual user-safe error.
 */
public class DBContext implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DBContext.class.getName());

    private static final String CONFIG_RESOURCE = "ConnectDB.properties";

    protected Connection connection;

    public DBContext() {
        Properties properties = loadProperties();
        String url = properties.getProperty("url");
        String user = properties.getProperty("userID");
        String pass = properties.getProperty("password");
        if (isBlank(url) || isBlank(user) || isBlank(pass)) {
            // Property values are never echoed: they contain credentials.
            LOGGER.log(Level.SEVERE, CONFIG_RESOURCE + " is missing one of url, userID or password.");
            return;
        }
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException | SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database connection could not be established.", ex);
        }
    }

    /** Returns empty properties when the local configuration file is absent. */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DBContext.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (inputStream == null) {
                LOGGER.log(Level.SEVERE, CONFIG_RESOURCE + " was not found on the classpath.");
                return properties;
            }
            properties.load(inputStream);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, CONFIG_RESOURCE + " could not be read.", ex);
        }
        return properties;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public Connection getConnection() {
        return connection;
    }

    /** Closes the underlying connection (used through try-with-resources). */
    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Closing the database connection failed.", ex);
            }
        }
    }
}