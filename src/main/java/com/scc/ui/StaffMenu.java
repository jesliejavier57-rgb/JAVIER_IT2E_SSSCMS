package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseConnection;
import com.scc.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

public class StaffMenu {
    private final Scanner scanner;
    private final User currentUser;
    private final ActivityLogDao logDao;

    public StaffMenu(Scanner scanner, User currentUser, ActivityLogDao logDao) {
        this.scanner = scanner;
        this.currentUser = currentUser;
        this.logDao = logDao;
    }

    public void show() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("     STORE STAFF MENU (" + currentUser.getUsername() + ")");
            System.out.println("==========================================");
            System.out.println("1. Customers (CRUD)");
            System.out.println("2. Utang Records (CRUD)");
            System.out.println("3. Payments (CRUD)");
            System.out.println("4. Activity Logs (CRUD)");
            System.out.println("0. Logout");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> customerMenu();
                case "2" -> utangMenu();
                case "3" -> paymentMenu();
                case "4" -> new ActivityLogViewer(scanner, currentUser, logDao).show();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void customerMenu() {
        while (true) {
            System.out.println("\n--- STAFF: CUSTOMERS CRUD ---");
            System.out.println("1. Create Customer");
            System.out.println("2. Read / Search Customers");
            System.out.println("3. Update Customer Details");
            System.out.println("4. Delete / Deactivate Customer");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    try {
                        System.out.print("Full Name: ");
                        String name = scanner.nextLine().trim();
                        System.out.print("Address: ");
                        String addr = scanner.nextLine().trim();
                        System.out.print("Contact No: ");
                        String contact = scanner.nextLine().trim();
                        System.out.print("Credit Limit: ");
                        double limit = Double.parseDouble(scanner.nextLine().trim());

                        String sql = "INSERT INTO Customers (full_name, address, contact_no, credit_limit, date_registered, status) VALUES (?, ?, ?, ?, ?, 'Active')";
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, name);
                            ps.setString(2, addr);
                            ps.setString(3, contact);
                            ps.setDouble(4, limit);
                            ps.setString(5, LocalDate.now().toString());
                            ps.executeUpdate();
                            System.out.println("Customer created.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "CREATE_CUSTOMER", "Added customer: " + name);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "2" -> {
                    System.out.print("Name filter (or Enter for all): ");
                    String q = scanner.nextLine().trim();
                    String sql = q.isEmpty()
                            ? "SELECT * FROM Customers"
                            : "SELECT * FROM Customers WHERE LOWER(full_name) LIKE LOWER(?)";
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        if (!q.isEmpty()) ps.setString(1, "%" + q + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                System.out.printf("#%d | %s | %s | Limit: %.2f | %s%n",
                                        rs.getInt("customer_id"), rs.getString("full_name"),
                                        rs.getString("contact_no"), rs.getDouble("credit_limit"),
                                        rs.getString("status"));
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "3" -> {
                    try {
                        System.out.print("Customer ID: ");
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("New Name: ");
                        String name = scanner.nextLine().trim();
                        System.out.print("New Contact: ");
                        String contact = scanner.nextLine().trim();
                        System.out.print("New Credit Limit: ");
                        double limit = Double.parseDouble(scanner.nextLine().trim());

                        String sql = "UPDATE Customers SET full_name = ?, contact_no = ?, credit_limit = ? WHERE customer_id = ?";
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, name);
                            ps.setString(2, contact);
                            ps.setDouble(3, limit);
                            ps.setInt(4, id);
                            System.out.println(ps.executeUpdate() > 0 ? "Updated." : "Not found.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "EDIT_CUSTOMER", "Updated customer #" + id);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "4" -> {
                    try {
                        System.out.print("Customer ID to deactivate: ");
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        String sql = "UPDATE Customers SET status = 'Inactive' WHERE customer_id = ?";
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, id);
                            System.out.println(ps.executeUpdate() > 0 ? "Deactivated." : "Not found.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "DEACTIVATE_CUSTOMER", "Deactivated customer #" + id);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void utangMenu() {
        while (true) {
            System.out.println("\n--- STAFF: UTANG RECORDS CRUD ---");
            System.out.println("1. Create Utang");
            System.out.println("2. Read Utang");
            System.out.println("3. Update Due Date / Details");
            System.out.println("4. Delete Wrongly Encoded Utang");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    try {
                        System.out.print("Customer ID: ");
                        int cid = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Items: ");
                        String items = scanner.nextLine().trim();
                        System.out.print("Amount: ");
                        double amt = Double.parseDouble(scanner.nextLine().trim());
                        System.out.print("Due Date (YYYY-MM-DD): ");
                        String due = scanner.nextLine().trim();

                        String sql = "INSERT INTO Utang_Records (customer_id, item_description, amount, date_taken, due_date, status) VALUES (?, ?, ?, ?, ?, 'Unpaid')";
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, cid);
                            ps.setString(2, items);
                            ps.setDouble(3, amt);
                            ps.setString(4, LocalDate.now().toString());
                            ps.setString(5, due);
                            ps.executeUpdate();
                            System.out.println("Utang saved.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "CREATE_UTANG", "Added utang " + amt + " for customer #" + cid);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "2" -> {
                    String sql = "SELECT utang_id, customer_id, item_description, amount, due_date, status FROM Utang_Records";
                    try (Connection conn = DatabaseConnection.getConnection();
                         Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery(sql)) {
                        while (rs.next()) {
                            System.out.printf("Utang#%d | Cust#%d | %s | %.2f | Due: %s | %s%n",
                                    rs.getInt("utang_id"), rs.getInt("customer_id"),
                                    rs.getString("item_description"), rs.getDouble("amount"),
                                    rs.getString("due_date"), rs.getString("status"));
                        }
                    } catch (SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "3" -> {
                    try {
                        System.out.print("Utang ID: ");
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("New Due Date (YYYY-MM-DD): ");
                        String due = scanner.nextLine().trim();
                        String sql = "UPDATE Utang_Records SET due_date = ? WHERE utang_id = ?";
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, due);
                            ps.setInt(2, id);
                            System.out.println(ps.executeUpdate() > 0 ? "Updated." : "Not found.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "EDIT_UTANG", "Updated due date for utang #" + id);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "4" -> {
                    try {
                        System.out.print("Utang ID to delete: ");
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        String sql = "DELETE FROM Utang_Records WHERE utang_id = ?";
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, id);
                            System.out.println(ps.executeUpdate() > 0 ? "Deleted." : "Not found.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "DELETE_UTANG", "Deleted utang #" + id);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void paymentMenu() {
        while (true) {
            System.out.println("\n--- STAFF: PAYMENTS CRUD ---");
            System.out.println("1. Create Payment");
            System.out.println("2. Read Payments");
            System.out.println("3. Update Payment");
            System.out.println("4. Delete Payment");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    try {
                        System.out.print("Utang ID: ");
                        int uid = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Amount Paid: ");
                        double amt = Double.parseDouble(scanner.nextLine().trim());
                        String sql = "INSERT INTO Payments (utang_id, amount_paid, date_paid, received_by) VALUES (?, ?, ?, ?)";
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, uid);
                            ps.setDouble(2, amt);
                            ps.setString(3, LocalDate.now().toString());
                            ps.setInt(4, currentUser.getUserId());
                            ps.executeUpdate();
                            System.out.println("Payment recorded.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "PAYMENT", "Received " + amt + " for utang #" + uid);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "2" -> {
                    try (Connection conn = DatabaseConnection.getConnection();
                         Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery("SELECT * FROM Payments")) {
                        while (rs.next()) {
                            System.out.printf("Pay#%d | Utang#%d | %.2f | %s%n",
                                    rs.getInt("payment_id"), rs.getInt("utang_id"),
                                    rs.getDouble("amount_paid"), rs.getString("date_paid"));
                        }
                    } catch (SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "3" -> {
                    try {
                        System.out.print("Payment ID: ");
                        int pid = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("New Amount Paid: ");
                        double amt = Double.parseDouble(scanner.nextLine().trim());
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement("UPDATE Payments SET amount_paid = ? WHERE payment_id = ?")) {
                            ps.setDouble(1, amt);
                            ps.setInt(2, pid);
                            System.out.println(ps.executeUpdate() > 0 ? "Updated." : "Not found.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "EDIT_PAYMENT", "Updated payment #" + pid);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "4" -> {
                    try {
                        System.out.print("Payment ID to delete: ");
                        int pid = Integer.parseInt(scanner.nextLine().trim());
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement("DELETE FROM Payments WHERE payment_id = ?")) {
                            ps.setInt(1, pid);
                            System.out.println(ps.executeUpdate() > 0 ? "Deleted." : "Not found.");
                            logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                    "DELETE_PAYMENT", "Deleted payment #" + pid);
                        }
                    } catch (NumberFormatException | SQLException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}