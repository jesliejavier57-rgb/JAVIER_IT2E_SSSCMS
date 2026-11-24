package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public boolean registerUser(String username, String plainPassword, String role, Integer customerId) {
        String sql = """
                INSERT INTO Users (username, password_hash, role, customer_id, date_created)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, SecurityUtil.hashPassword(plainPassword));
            ps.setString(3, role);
            if (customerId == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, customerId);
            }
            ps.setString(5, LocalDateTime.now().toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    public User authenticate(String username, String plainPassword) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (SecurityUtil.verifyPassword(plainPassword, storedHash)) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean customerExists(int customerId) {
        String sql = "SELECT 1 FROM Customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM Users ORDER BY user_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }
        return list;
    }

    public boolean updateUserRole(int userId, String newRole) {
        String sql = "UPDATE Users SET role = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE user_id = ? AND username != 'owner'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        int custIdRaw = rs.getInt("customer_id");
        Integer custId = rs.wasNull() ? null : custIdRaw;
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                custId,
                LocalDateTime.parse(rs.getString("date_created"))
        );
    }
}