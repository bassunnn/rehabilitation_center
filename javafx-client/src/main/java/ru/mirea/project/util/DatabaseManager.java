package ru.mirea.project.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String PROPERTIES_FILE = "database.properties";

    private static String url;
    private static String user;
    private static String password;

    static {
        loadProperties();
    }

    private DatabaseManager() {}

    private static void loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
                logger.info("Конфигурация БД успешно загружена из {}", PROPERTIES_FILE);
            } else {
                logger.warn("Файл {} не найден в classpath, используются значения по умолчанию", PROPERTIES_FILE);
            }
        } catch (IOException e) {
            logger.error("Ошибка при чтении {}: {}", PROPERTIES_FILE, e.getMessage());
        }

        url = System.getProperty("db.url",
                System.getenv().getOrDefault("DB_URL",
                        properties.getProperty("db.url", "jdbc:postgresql://localhost:5432/rehabilitation_center")));

        user = System.getProperty("db.user",
                System.getenv().getOrDefault("DB_USER",
                        properties.getProperty("db.user", "postgres")));

        password = System.getProperty("db.password",
                System.getenv().getOrDefault("DB_PASSWORD",
                        properties.getProperty("db.password", "qwerTY89#")));
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Драйвер PostgreSQL не найден в classpath: {}", e.getMessage());
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean checkConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            logger.error("Проверка подключения к БД не удалась: {}", e.getMessage());
            return false;
        }
    }

    public static String getDatabaseUrl() {
        return url;
    }

    public static String getDatabaseUser() {
        return user;
    }
}
