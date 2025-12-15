package com.habit.dao;

import com.habit.model.Habit;
import com.habit.util.DatabaseHelper;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Data Access Object (DAO) handles all database interactions.
 */
public class HabitDao {

    // 1. CREATE
    public void addHabit(String name, String frequency) {
        String sql = "INSERT INTO habits(name, frequency) VALUES(?,?)";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, frequency);
            pstmt.executeUpdate();
            System.out.println("Habit added: " + name);
        } catch (SQLException e) {
            System.out.println("Add Habit Error: " + e.getMessage());
        }
    }

    // 2. READ
    public List<Habit> getAllHabits() {
        List<Habit> habits = new ArrayList<>();
        String sql = "SELECT * FROM habits";
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                habits.add(new Habit(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("frequency")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Get Habits Error: " + e.getMessage());
        }
        return habits;
    }

    // 3. UPDATE
    public void updateHabit(Habit habit) {
        String sql = "UPDATE habits SET name = ?, frequency = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, habit.getName());
            pstmt.setString(2, habit.getFrequency());
            pstmt.setInt(3, habit.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Update Error: " + e.getMessage());
        }
    }

    // 4. DELETE SINGLE HABIT
    public void deleteHabit(int habitId) {
        String deleteEntriesSql = "DELETE FROM habit_entries WHERE habit_id = ?";
        String deleteHabitSql = "DELETE FROM habits WHERE id = ?";

        try (Connection conn = DatabaseHelper.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(deleteEntriesSql);
                 PreparedStatement p2 = conn.prepareStatement(deleteHabitSql)) {

                p1.setInt(1, habitId);
                p1.executeUpdate();

                p2.setInt(1, habitId);
                p2.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            System.out.println("Connection Error: " + e.getMessage());
        }
    }

    // 5. CHECK-IN (升级版：支持 Daily/Weekly/Monthly 频率限制)
    // 返回值含义：
    // 0: 成功
    // 1: 失败 - 今天已打卡 (Daily)
    // 2: 失败 - 本周已打卡 (Weekly)
    // 3: 失败 - 本月已打卡 (Monthly)
    // -1: 数据库错误
    public int checkIn(Habit habit, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        String frequency = habit.getFrequency();

        // --- 1. 检查频率限制 ---
        if ("Daily".equals(frequency)) {
            if (isCheckInExist(habit.getId(), date, date)) return 1;
        }
        else if ("Weekly".equals(frequency)) {
            // 计算本周的周一和周日
            LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            if (isCheckInExist(habit.getId(), startOfWeek, endOfWeek)) return 2;
        }
        else if ("Monthly".equals(frequency)) {
            // 计算本月的第一天和最后一天
            LocalDate startOfMonth = date.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
            if (isCheckInExist(habit.getId(), startOfMonth, endOfMonth)) return 3;
        }

        // --- 2. 执行打卡 ---
        String sql = "INSERT INTO habit_entries(habit_id, check_date) VALUES(?,?)";
        try (Connection conn = DatabaseHelper.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, habit.getId());
                pstmt.setString(2, dateStr);
                pstmt.executeUpdate();
                conn.commit();
                return 0; // 成功
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) {
            System.out.println("Check-in Error: " + e.getMessage());
        }
        return -1; // 未知错误
    }

    // 通用辅助方法：检查某个时间段内是否打过卡
    private boolean isCheckInExist(int habitId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT count(*) FROM habit_entries WHERE habit_id = ? AND check_date >= ? AND check_date <= ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, habitId);
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 6. STATS
    public Map<String, Integer> getHabitStats(int daysLookBack) {
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder(
                "SELECT h.name, COUNT(e.id) as count " +
                        "FROM habits h " +
                        "LEFT JOIN habit_entries e ON h.id = e.habit_id "
        );

        if (daysLookBack > 0) {
            String startDate = LocalDate.now().minusDays(daysLookBack).toString();
            sql.append(" AND e.check_date >= '").append(startDate).append("' ");
        }

        sql.append("GROUP BY h.id");

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                stats.put(rs.getString("name"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.out.println("Stats Error: " + e.getMessage());
        }
        return stats;
    }

    // 7. 【NEW】DELETE ALL DATA
    public void deleteAllData() {
        String sql1 = "DELETE FROM habit_entries";
        String sql2 = "DELETE FROM habits";

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql1);
            stmt.executeUpdate(sql2);
            System.out.println("All data cleared from database.");
        } catch (SQLException e) {
            System.out.println("Clear All Error: " + e.getMessage());
        }
    }
}