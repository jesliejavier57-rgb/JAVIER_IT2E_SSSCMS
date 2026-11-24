package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseConnection;
import com.scc.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

public class CustomerMenu {
    private final Scanner scanner;
    private final User currentUser;
    private final ActivityLogDao logDao;

    public CustomerMenu(Scanner scanner, User currentUser, ActivityLogDao logDao) {
        this.scanner = scanner;
        this.currentUser = currentUser;
        this.logDao = logDao;
    }

    public void show() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("      CUSTOMER MENU (" + currentUser.getUsername() + ")");
            System.out.println("==========================================");
            System.out.println("1. Create (Submit Payment Entry)");
            System.out.println("2. Read (View My Utang, Payments & Balance)");
            System.out.println("3. Update (Update My Contact / Address)");
            System.out.println("4. Delete (Remove My Payment Entry)");
            System.out.println("0. Logout");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createMyPayment();
                case "2" -> readMyAccount();
                case "3" -> updateMyProfile();
                case "4" -> deleteMyPayment();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private Integer requireCustomerId() {
        if (currentUser.getCustomerId() == null) {
            System.out.println("Your user account is not linked to a Customer record yet. Ask the store owner/staff.");
            return null;
        }
        return currentUser.getCustomerId();
    }

    private void createMyPayment() {
        Integer cid = requireCustomerId();
        if (cid == null) return;

        try {
            System.out.print("Enter your Utang ID to pay: ");
            int utangId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Amount to pay: ");
            double amt = Double.parseDouble(scanner.nextLine().trim());

            String checkSql = "SELECT 1 FROM Utang_Records WHERE utang_id = ? AND customer_id = ?";
            String insSql = "INSERT INTO Payments (utang_id, amount_paid, date_paid, received_by) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement chk = conn.prepareStatement(checkSql)) {
                chk.setInt(1, utangId);
                chk.setInt(2, cid);
                try (ResultSet rs = chk.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Utang #" + utangId + " does not belong to your account.");
                        return;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(insSql)) {
                    ps.setInt(1, utangId);
                    ps.setDouble(2, amt);
                    ps.setString(3, LocalDate.now().toString());
                    ps.setInt(4, currentUser.getUserId());
                    ps.executeUpdate();
                    System.out.println("Payment recorded.");
                    logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                            "PAYMENT", "Customer #" + cid + " paid " + amt + " on utang #" + utangId);
                }
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void readMyAccount() {
        Integer cid = requireCustomerId();
        if (cid == null) return;

        String utangSql = "SELECT * FROM Utang_Records WHERE customer_id = ? ORDER BY utang_id DESC";
        String paySql = """
                SELECT p.* FROM Payments p
                JOIN Utang_Records u ON p.utang_id = u.utang_id
                WHERE u.customer_id = ?
                ORDER BY p.payment_id DESC
                """;

        double totalUtang = 0;
        double totalPaid = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("\n--- MY UTANG RECORDS ---");
            try (PreparedStatement ps = conn.prepareStatement(utangSql)) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double amt = rs.getDouble("amount");
                        totalUtang += amt;
                        System.out.printf("Utang#%d | %s | %.2f | Due: %s | %s%n",
                                rs.getInt("utang_id"), rs.getString("item_description"),
                                amt, rs.getString("due_date"), rs.getString("status"));
                    }
                }
            }

            System.out.println("\n--- MY PAYMENTS ---");
            try (PreparedStatement ps = conn.prepareStatement(paySql)) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double paid = rs.getDouble("amount_paid");
                        totalPaid += paid;
                        System.out.printf("Payment#%d | Utang#%d | %.2f | Date: %s%n",
                                rs.getInt("payment_id"), rs.getInt("utang_id"),
                                paid, rs.getString("date_paid"));
                    }
                }
            }

            System.out.printf("%nTotal Utang: %.2f | Total Paid: %.2f | Remaining Balance: %.2f%n",
                    totalUtang, totalPaid, Math.max(0, totalUtang - totalPaid));
        } catch (SQLException e) {
            System.out.println("Error loading account: " + e.getMessage());
        }
    }

    private void updateMyProfile() {
        Integer cid = requireCustomerId();
        if (cid == null) return;

        System.out.print("New Contact No: ");
        String contact = scanner.nextLine().trim();
        System.out.print("New Address: ");
        String address = scanner.nextLine().trim();

        String sql = "UPDATE Customers SET contact_no = ?, address = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contact);
            ps.setString(2, address);
            ps.setInt(3, cid);
            if (ps.executeUpdate() > 0) {
                System.out.println("Profile updated.");
                logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                        "EDIT_CUSTOMER", "Customer #" + cid + " updated contact/address");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void deleteMyPayment() {
        Integer cid = requireCustomerId();
        if (cid == null) return;

        try {
            System.out.print("Enter Payment ID to delete: ");
            int payId = Integer.parseInt(scanner.nextLine().trim());

            String sql = """
                    DELETE FROM Payments
                    WHERE payment_id = ?
                      AND utang_id IN (SELECT utang_id FROM Utang_Records WHERE customer_id = ?)
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, payId);
                ps.setInt(2, cid);
                if (ps.executeUpdate() > 0) {
                    System.out.println("Payment entry removed.");
                    logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                            "DELETE_PAYMENT", "Customer #" + cid + " deleted payment #" + payId);
                } else {
                    System.out.println("Payment not found for your account.");
                }
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}