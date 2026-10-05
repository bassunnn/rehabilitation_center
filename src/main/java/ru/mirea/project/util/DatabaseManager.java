package ru.mirea.project.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {
    private static final String URL = System.getProperty("db.url",
            "jdbc:postgresql://localhost:5432/rehabilitation_center");
    private static final String USER = System.getProperty("db.user", "postgres");
    private static final String PASSWORD = System.getProperty("db.password", "qwerTY89#");

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
