package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public void log(Integer userId, String username, String role, String actionType, String details) {
        String sql = """
                INSERT INTO Activity_Logs (user_id, username, role, action_type, details, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, username);
            ps.setString(3, role);
            ps.setString(4, actionType);
            ps.setString(5, details);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to write activity log: " + e.getMessage());
        }
    }

    public List<ActivityLog> getAllLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Activity_Logs ORDER BY log_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int uidRaw = rs.getInt("user_id");
                Integer uid = rs.wasNull() ? null : uidRaw;
                logs.add(new ActivityLog(
                        rs.getInt("log_id"),
                        uid,
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("action_type"),
                        rs.getString("details"),
                        LocalDateTime.parse(rs.getString("timestamp"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error reading activity logs: " + e.getMessage());
        }
        return logs;
    }

    public boolean clearLogs() {
        String sql = "DELETE FROM Activity_Logs";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}