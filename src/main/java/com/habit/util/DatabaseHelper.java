package com.habit.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for database connection and initialization.
 */
public class DatabaseHelper {
    // The database file will be created in the project root directory
    private static final String URL = "jdbc:sqlite:habit_tracker.db";

    public static Connection connect() throws SQLException {
        try {
            // Force load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("Fatal Error: SQLite Driver not found!");
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL);
    }

    public static void initDatabase() {
        // SQL to create the habits table
        String createHabitTable = "CREATE TABLE IF NOT EXISTS habits ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "frequency TEXT NOT NULL"
                + ");";

        // SQL to create the check-in entries table
        String createEntryTable = "CREATE TABLE IF NOT EXISTS habit_entries ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "habit_id INTEGER,"
                + "check_date TEXT,"
                + "FOREIGN KEY(habit_id) REFERENCES habits(id)"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createHabitTable);
            stmt.execute(createEntryTable);
            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            System.out.println("DB Init Error: " + e.getMessage());
        }
    }
}