package com.swp391.g1.common;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBContext {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");

    protected Connection connection;

    public DBContext() {
        try {
            Properties properties = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ConnectDB.properties");
            if (inputStream == null) {
                throw new SQLException("ConnectDB.properties was not found on the classpath.");
            }
            try (InputStream resource = inputStream) {
                properties.load(resource);
            } catch (IOException ex) {
                throw new SQLException("Unable to read ConnectDB.properties.", ex);
            }
            String user = properties.getProperty("userID");
            String pass = properties.getProperty("password");
            String url = resolvePlaceholders(properties.getProperty("url"));
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(DBContext.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Could not establish the database connection.", ex);
        }
    }

    private String resolvePlaceholders(String value) {
        if (value == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2);
            String replacement = System.getProperty(key);
            if (replacement == null || replacement.isBlank()) {
                replacement = System.getenv(key);
            }
            if (replacement == null || replacement.isBlank()) {
                replacement = defaultValue == null ? "" : defaultValue;
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    public Connection getConnection() {
        return connection;
    }
}