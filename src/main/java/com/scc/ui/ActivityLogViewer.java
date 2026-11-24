package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseConnection;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.sql.*;
import java.util.List;
import java.util.Scanner;

public class ActivityLogViewer {
    private final Scanner scanner;
    private final User currentUser;
    private final ActivityLogDao logDao;

    public ActivityLogViewer(Scanner scanner, User currentUser, ActivityLogDao logDao) {
        this.scanner = scanner;
        this.currentUser = currentUser;
        this.logDao = logDao;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- ACTIVITY LOGS (CRUD) ---");
            System.out.println("1. Create Manual Audit Note");
            System.out.println("2. View All / Filter Activity Logs");
            System.out.println("3. Update Log Entry Details");
            System.out.println("4. Delete Log Entry / Clear Logs");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createLogNote();
                case "2" -> viewLogs();
                case "3" -> updateLogDetails();
                case "4" -> deleteLog();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void createLogNote() {
        System.out.print("Enter audit note / event details: ");
        String details = scanner.nextLine().trim();
        if (details.isEmpty()) {
            System.out.println("Details cannot be empty.");
            return;
        }
        logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(), "AUDIT_NOTE", details);
        System.out.println("Audit note recorded.");
    }

    private void viewLogs() {
        System.out.print("Filter by action type or username (or press Enter for all): ");
        String filter = scanner.nextLine().trim().toLowerCase();

        List<ActivityLog> logs = logDao.getAllLogs();
        System.out.printf("%-5s | %-20s | %-10s | %-8s | %-15s | %s%n",
                "ID", "Timestamp", "User", "Role", "Action", "Details");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (ActivityLog l : logs) {
            boolean matches = filter.isEmpty()
                    || l.getUsername().toLowerCase().contains(filter)
                    || l.getActionType().toLowerCase().contains(filter)
                    || l.getDetails().toLowerCase().contains(filter);
            if (matches) {
                System.out.printf("%-5d | %-20s | %-10s | %-8s | %-15s | %s%n",
                        l.getLogId(),
                        l.getTimestamp().toString().replace('T', ' ').substring(0, Math.min(19, l.getTimestamp().toString().length())),
                        l.getUsername(),
                        l.getRole(),
                        l.getActionType(),
                        l.getDetails());
            }
        }
    }

    private void updateLogDetails() {
        System.out.print("Enter Log ID to update: ");
        try {
            int logId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter updated details: ");
            String newDetails = scanner.nextLine().trim();
            String sql = "UPDATE Activity_Logs SET details = ? WHERE log_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newDetails);
                ps.setInt(2, logId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Log #" + logId + " updated.");
                } else {
                    System.out.println("Log ID not found.");
                }
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Invalid input or error updating log: " + e.getMessage());
        }
    }

    private void deleteLog() {
        System.out.println("1. Delete single Log ID");
        System.out.println("2. Clear all logs");
        System.out.print("Choice: ");
        String opt = scanner.nextLine().trim();
        if ("1".equals(opt)) {
            System.out.print("Enter Log ID: ");
            try {
                int logId = Integer.parseInt(scanner.nextLine().trim());
                String sql = "DELETE FROM Activity_Logs WHERE log_id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, logId);
                    System.out.println(ps.executeUpdate() > 0 ? "Deleted." : "Log not found.");
                }
            } catch (NumberFormatException | SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if ("2".equals(opt)) {
            if (logDao.clearLogs()) {
                System.out.println("All activity logs cleared.");
            }
        }
    }
}