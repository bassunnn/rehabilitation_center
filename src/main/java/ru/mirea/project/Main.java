package ru.mirea.project;

import java.sql.Connection;
import ru.mirea.project.util.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Подключение к БД успешно установлено!");
        } catch (Exception ex) {
            System.err.println("Ошибка подключения к БД: " + ex.getMessage());
        }
    }
}