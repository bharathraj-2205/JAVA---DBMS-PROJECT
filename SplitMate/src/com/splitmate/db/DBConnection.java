package com.splitmate.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Central place for JDBC connections. Credentials come from env vars, with defaults. */
public final class DBConnection {
    private static final String URL = env("SPLITMATE_DB_URL",
        "jdbc:mysql://localhost:3306/splitmate?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = env("SPLITMATE_DB_USER", "root");
    private static final String PASSWORD = env("SPLITMATE_DB_PASSWORD", "root");

    private DBConnection() { }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
