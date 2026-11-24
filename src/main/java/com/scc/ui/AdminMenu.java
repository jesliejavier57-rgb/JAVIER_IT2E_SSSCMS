package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseConnection;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner scanner;
    private final User currentUser;
    private final ActivityLogDao logDao;
    private final UserDao userDao;

    public AdminMenu(Scanner scanner, User currentUser, ActivityLogDao logDao) {
        this.scanner = scanner;
        this.currentUser = currentUser;
        this.logDao = logDao;
        this.userDao = new UserDao();
    }

    public void show() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("      STORE OWNER MENU (" + currentUser.getUsername() + ")");
            System.out.println("==========================================");
            System.out.println("1. Customers (CRUD)");
            System.out.println("2. Utang Records (CRUD)");
            System.out.println("3. Payments (CRUD)");
            System.out.println("4. Outstanding Balance & Overdue Report");
            System.out.println("5. User Accounts (CRUD)");
            System.out.println("6. Activity Logs (CRUD)");
            System.out.println("0. Logout");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> customerCrudMenu();
                case "2" -> utangCrudMenu();
                case "3" -> paymentCrudMenu();
                case "4" -> outstandingBalanceReport();
                case "5" -> userCrudMenu();
                case "6" -> new ActivityLogViewer(scanner, currentUser, logDao).show();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ---------------- CUSTOMERS CRUD ----------------
    private void customerCrudMenu() {
        while (true) {
            System.out.println("\n--- CUSTOMERS CRUD ---");
            System.out.println("1. Create (Register New Customer)");
            System.out.println("2. Read (View All / Search by Name)");
            System.out.println("3. Update (Edit Name, Address, Contact, Credit Limit, Status)");
            System.out.println("4. Delete (Soft-Delete / Deactivate or Hard Delete)");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createCustomer();
                case "2" -> readCustomers();
                case "3" -> updateCustomer();
                case "4" -> deleteOrDeactivateCustomer();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void createCustomer() {
        try {
            System.out.print("Full Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Address: ");
            String address = scanner.nextLine().trim();
            System.out.print("Contact No: ");
            String contact = scanner.nextLine().trim();
            System.out.print("Credit Limit: ");
            double limit = Double.parseDouble(scanner.nextLine().trim());

            String sql = """
                    INSERT INTO Customers (full_name, address, contact_no, credit_limit, date_registered, status)
                    VALUES (?, ?, ?, ?, ?, 'Active')
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, address);
                ps.setString(3, contact);
                ps.setDouble(4, limit);
                ps.setString(5, LocalDate.now().toString());
                ps.executeUpdate();
                System.out.println("Customer registered successfully.");
                logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                        "CREATE_CUSTOMER", "Registered customer: " + name + " (Limit: " + limit + ")");
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error creating customer: " + e.getMessage());
        }
    }

    private void readCustomers() {
        System.out.print("Search by name (or press Enter for all): ");
        String keyword = scanner.nextLine().trim();
        String sql = keyword.isEmpty()
                ? "SELECT * FROM Customers ORDER BY customer_id ASC"
                : "SELECT * FROM Customers WHERE LOWER(full_name) LIKE LOWER(?) ORDER BY customer_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!keyword.isEmpty()) {
                ps.setString(1, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-4s | %-20s | %-18s | %-12s | %-10s | %-8s%n",
                        "ID", "Full Name", "Address", "Contact", "Limit", "Status");
                System.out.println("------------------------------------------------------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-4d | %-20s | %-18s | %-12s | %-10.2f | %-8s%n",
                            rs.getInt("customer_id"),
                            rs.getString("full_name"),
                            rs.getString("address"),
                            rs.getString("contact_no"),
                            rs.getDouble("credit_limit"),
                            rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error reading customers: " + e.getMessage());
        }
    }

    private void updateCustomer() {
        try {
            System.out.print("Enter Customer ID to update: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("New Full Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("New Address: ");
            String address = scanner.nextLine().trim();
            System.out.print("New Contact No: ");
            String contact = scanner.nextLine().trim();
            System.out.print("New Credit Limit: ");
            double limit = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Status (Active/Inactive): ");
            String status = scanner.nextLine().trim();

            String sql = """
                    UPDATE Customers
                    SET full_name = ?, address = ?, contact_no = ?, credit_limit = ?, status = ?
                    WHERE customer_id = ?
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, address);
                ps.setString(3, contact);
                ps.setDouble(4, limit);
                ps.setString(5, status);
                ps.setInt(6, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Customer updated.");
                    logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                            "EDIT_CUSTOMER", "Updated customer #" + id + " (" + name + ")");
                } else {
                    System.out.println("Customer not found.");
                }
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error updating customer: " + e.getMessage());
        }
    }

    private void deleteOrDeactivateCustomer() {
        try {
            System.out.print("Enter Customer ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            System.out.println("1. Soft-Delete (Set status to Inactive)");
            System.out.println("2. Permanently Delete Customer");
            System.out.print("Choice: ");
            String opt = scanner.nextLine().trim();

            if ("1".equals(opt)) {
                String sql = "UPDATE Customers SET status = 'Inactive' WHERE customer_id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    if (ps.executeUpdate() > 0) {
                        System.out.println("Customer marked as Inactive.");
                        logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                "DEACTIVATE_CUSTOMER", "Deactivated customer #" + id);
                    } else {
                        System.out.println("Customer not found.");
                    }
                }
            } else if ("2".equals(opt)) {
                String sql = "DELETE FROM Customers WHERE customer_id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    if (ps.executeUpdate() > 0) {
                        System.out.println("Customer permanently deleted.");
                        logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                                "DELETE_CUSTOMER", "Deleted customer #" + id);
                    } else {
                        System.out.println("Customer not found.");
                    }
                }
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ---------------- UTANG RECORDS CRUD ----------------
    private void utangCrudMenu() {
        while (true) {
            System.out.println("\n--- UTANG RECORDS CRUD ---");
            System.out.println("1. Create (Record New Utang)");
            System.out.println("2. Read (All Utang / Overdue Accounts / By Customer)");
            System.out.println("3. Update (Extend Due Date / Correct Entry)");
            System.out.println("4. Delete (Remove Wrongly Encoded Utang)");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createUtang();
                case "2" -> readUtang();
                case "3" -> updateUtang();
                case "4" -> deleteUtang();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void createUtang() {
        try {
            System.out.print("Customer ID: ");
            int custId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Item Description: ");
            String items = scanner.nextLine().trim();
            System.out.print("Amount: ");
            double amount = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Due Date (YYYY-MM-DD): ");
            String dueDate = scanner.nextLine().trim();

            String sql = """
                    INSERT INTO Utang_Records (customer_id, item_description, amount, date_taken, due_date, status)
                    VALUES (?, ?, ?, ?, ?, 'Unpaid')
                    """;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, custId);
                ps.setString(2, items);
                ps.setDouble(3, amount);
                ps.setString(4, LocalDate.now().toString());
                ps.setString(5, dueDate);
                ps.executeUpdate();
                System.out.println("Utang recorded.");
                logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                        "CREATE_UTANG", "Customer #" + custId + " utang " + amount + " (" + items + ")");
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error recording utang: " + e.getMessage());
        }
    }

    private void readUtang() {
        System.out.println("1. All Utang Records");
        System.out.println("2. Overdue Accounts Only");
        System.out.print("Choice: ");
        String opt = scanner.nextLine().trim();

        String sql = "2".equals(opt)
                ? """
                  SELECT u.*, c.full_name
                  FROM Utang_Records u
                  JOIN Customers c ON u.customer_id = c.customer_id
                  WHERE u.status != 'Paid' AND u.due_date < ?
                  ORDER BY u.due_date ASC
                  """
                : """
                  SELECT u.*, c.full_name
                  FROM Utang_Records u
                  JOIN Customers c ON u.customer_id = c.customer_id
                  ORDER BY u.utang_id DESC
                  """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("2".equals(opt)) {
                ps.setString(1, LocalDate.now().toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-4s | %-18s | %-20s | %-8s | %-10s | %-10s | %-8s%n",
                        "ID", "Customer", "Items", "Amount", "Taken", "Due", "Status");
                System.out.println("-------------------------------------------------------------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-4d | %-18s | %-20s | %-8.2f | %-10s | %-10s | %-8s%n",
                            rs.getInt("utang_id"),
                            rs.getString("full_name"),
                            rs.getString("item_description"),
                            rs.getDouble("amount"),
                            rs.getString("date_taken"),
                            rs.getString("due_date"),
                            rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error reading utang: " + e.getMessage());
        }
    }

    private void updateUtang() {
        try {
            System.out.print("Utang ID to update: ");
            int utangId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Updated Item Description: ");
            String items = scanner.nextLine().trim();
            System.out.print("Updated Amount: ");
            double amount = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("New Due Date (YYYY-MM-DD): ");
            String dueDate = scanner.nextLine().trim();

            String sql = "UPDATE Utang_Records SET item_description = ?, amount = ?, due_date = ? WHERE utang_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, items);
                ps.setDouble(2, amount);
                ps.setString(3, dueDate);
                ps.setInt(4, utangId);
                if (ps.executeUpdate() > 0) {
                    System.out.println("Utang record updated.");
                    logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                            "EDIT_UTANG", "Updated utang #" + utangId + " (Due: " + dueDate + ", Amt: " + amount + ")");
                } else {
                    System.out.println("Utang ID not found.");
                }
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error updating utang: " + e.getMessage());
        }
    }

    private void deleteUtang() {
        try {
            System.out.print("Utang ID to delete: ");
            int utangId = Integer.parseInt(scanner.nextLine().trim());
            String sql = "DELETE FROM Utang_Records WHERE utang_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, utangId);
                if (ps.executeUpdate() > 0) {
                    System.out.println("Utang record deleted.");
                    logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                            "DELETE_UTANG", "Deleted utang #" + utangId);
                } else {
                    System.out.println("Utang ID not found.");
                }
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error deleting utang: " + e.getMessage());
        }
    }

    // ---------------- PAYMENTS CRUD ----------------
    private void paymentCrudMenu() {
        while (true) {
            System.out.println("\n--- PAYMENTS CRUD ---");
            System.out.println("1. Create (Record Payment)");
            System.out.println("2. Read (View All Payments)");
            System.out.println("3. Update (Edit Payment Amount)");
            System.out.println("4. Delete (Remove Payment)");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createPayment();
                case "2" -> readPayments();
                case "3" -> updatePayment();
                case "4" -> deletePayment();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void createPayment() {
        try {
            System.out.print("Utang ID: ");
            int utangId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Amount Paid: ");
            double paid = Double.parseDouble(scanner.nextLine().trim());

            String insertSql = "INSERT INTO Payments (utang_id, amount_paid, date_paid, received_by) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, utangId);
                    ps.setDouble(2, paid);
                    ps.setString(3, LocalDate.now().toString());
                    ps.setInt(4, currentUser.getUserId());
                    ps.executeUpdate();
                }
                refreshUtangStatus(conn, utangId);
                System.out.println("Payment recorded and utang status updated.");
                logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                        "PAYMENT", "Recorded payment of " + paid + " for utang #" + utangId);
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error recording payment: " + e.getMessage());
        }
    }

    private void readPayments() {
        String sql = """
                SELECT p.*, c.full_name, u.username AS staff_name
                FROM Payments p
                JOIN Utang_Records ur ON p.utang_id = ur.utang_id
                JOIN Customers c ON ur.customer_id = c.customer_id
                JOIN Users u ON p.received_by = u.user_id
                ORDER BY p.payment_id DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-4s | %-6s | %-18s | %-10s | %-10s | %-12s%n",
                    "ID", "Utang#", "Customer", "Paid", "Date", "Received By");
            System.out.println("-------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-4d | %-6d | %-18s | %-10.2f | %-10s | %-12s%n",
                        rs.getInt("payment_id"),
                        rs.getInt("utang_id"),
                        rs.getString("full_name"),
                        rs.getDouble("amount_paid"),
                        rs.getString("date_paid"),
                        rs.getString("staff_name"));
            }
        } catch (SQLException e) {
            System.out.println("Error reading payments: " + e.getMessage());
        }
    }

    private void updatePayment() {
        try {
            System.out.print("Payment ID to update: ");
            int payId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("New Amount Paid: ");
            double newAmt = Double.parseDouble(scanner.nextLine().trim());

            try (Connection conn = DatabaseConnection.getConnection()) {
                int utangId = -1;
                try (PreparedStatement sel = conn.prepareStatement("SELECT utang_id FROM Payments WHERE payment_id = ?")) {
                    sel.setInt(1, payId);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) utangId = rs.getInt("utang_id");
                    }
                }
                if (utangId == -1) {
                    System.out.println("Payment not found.");
                    return;
                }
                try (PreparedStatement upd = conn.prepareStatement("UPDATE Payments SET amount_paid = ? WHERE payment_id = ?")) {
                    upd.setDouble(1, newAmt);
                    upd.setInt(2, payId);
                    upd.executeUpdate();
                }
                refreshUtangStatus(conn, utangId);
                System.out.println("Payment updated.");
                logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                        "EDIT_PAYMENT", "Updated payment #" + payId + " to " + newAmt);
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error updating payment: " + e.getMessage());
        }
    }

    private void deletePayment() {
        try {
            System.out.print("Payment ID to delete: ");
            int payId = Integer.parseInt(scanner.nextLine().trim());

            try (Connection conn = DatabaseConnection.getConnection()) {
                int utangId = -1;
                try (PreparedStatement sel = conn.prepareStatement("SELECT utang_id FROM Payments WHERE payment_id = ?")) {
                    sel.setInt(1, payId);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) utangId = rs.getInt("utang_id");
                    }
                }
                if (utangId == -1) {
                    System.out.println("Payment not found.");
                    return;
                }
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM Payments WHERE payment_id = ?")) {
                    del.setInt(1, payId);
                    del.executeUpdate();
                }
                refreshUtangStatus(conn, utangId);
                System.out.println("Payment deleted.");
                logDao.log(currentUser.getUserId(), currentUser.getUsername(), currentUser.getRole(),
                        "DELETE_PAYMENT", "Deleted payment #" + payId);
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("Error deleting payment: " + e.getMessage());
        }
    }

    private void refreshUtangStatus(Connection conn, int utangId) throws SQLException {
        double totalUtang = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT amount FROM Utang_Records WHERE utang_id = ?")) {
            ps.setInt(1, utangId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalUtang = rs.getDouble("amount");
            }
        }
        double totalPaid = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount_paid), 0) FROM Payments WHERE utang_id = ?")) {
            ps.setInt(1, utangId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalPaid = rs.getDouble(1);
            }
        }
        String newStatus = totalPaid >= totalUtang ? "Paid" : (totalPaid > 0 ? "Partial" : "Unpaid");
        try (PreparedStatement upd = conn.prepareStatement("UPDATE Utang_Records SET status = ? WHERE utang_id = ?")) {
            upd.setString(1, newStatus);
            upd.setInt(2, utangId);
            upd.executeUpdate();
        }
    }

    // ---------------- REPORT ----------------
    private void outstandingBalanceReport() {
        System.out.println("\n--- OUTSTANDING BALANCE REPORT ---");
        String sql = """
                SELECT c.customer_id, c.full_name,
                       COALESCE((SELECT SUM(u.amount) FROM Utang_Records u WHERE u.customer_id = c.customer_id), 0) AS total_utang,
                       COALESCE((SELECT SUM(p.amount_paid)
                                 FROM Payments p
                                 JOIN Utang_Records u2 ON p.utang_id = u2.utang_id
                                 WHERE u2.customer_id = c.customer_id), 0) AS total_paid,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM Utang_Records u3
                           WHERE u3.customer_id = c.customer_id AND u3.status != 'Paid' AND u3.due_date < ?
                       ) THEN 'OVERDUE' ELSE 'OK' END AS overdue_flag
                FROM Customers c
                ORDER BY c.full_name ASC
                """;

        double grandTotalUncollected = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-4s | %-20s | %-11s | %-11s | %-11s | %-8s%n",
                        "ID", "Customer", "Total Utang", "Total Paid", "Balance", "Overdue");
                System.out.println("-----------------------------------------------------------------------------");
                while (rs.next()) {
                    double totalUtang = rs.getDouble("total_utang");
                    double totalPaid = rs.getDouble("total_paid");
                    double balance = Math.max(0, totalUtang - totalPaid);
                    grandTotalUncollected += balance;
                    System.out.printf("%-4d | %-20s | %-11.2f | %-11.2f | %-11.2f | %-8s%n",
                            rs.getInt("customer_id"),
                            rs.getString("full_name"),
                            totalUtang,
                            totalPaid,
                            balance,
                            rs.getString("overdue_flag"));
                }
            }
            System.out.println("-----------------------------------------------------------------------------");
            System.out.printf("STORE TOTAL UNCOLLECTED AMOUNT: %.2f%n", grandTotalUncollected);
        } catch (SQLException e) {
            System.out.println("Error generating report: " + e.getMessage());
        }
    }

    // ---------------- USERS CRUD ----------------
    private void userCrudMenu() {
        while (true) {
            System.out.println("\n--- USER ACCOUNTS CRUD ---");
            System.out.println("1. Create User");
            System.out.println("2. Read Users");
            System.out.println("3. Update User Role");
            System.out.println("4. Delete User");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    System.out.print("Username: ");
                    String u = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    String p = scanner.nextLine().trim();
                    System.out.print("Role (Owner/Staff/Customer): ");
                    String r = scanner.nextLine().trim();
                    System.out.println(userDao.registerUser(u, p, r, null) ? "User created." : "Failed.");
                }
                case "2" -> {
                    for (User u : userDao.getAllUsers()) {
                        System.out.printf("ID: %d | Username: %s | Role: %s | CustID: %s%n",
                                u.getUserId(), u.getUsername(), u.getRole(), u.getCustomerId());
                    }
                }
                case "3" -> {
                    System.out.print("User ID: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("New Role (Owner/Staff/Customer): ");
                    String r = scanner.nextLine().trim();
                    System.out.println(userDao.updateUserRole(id, r) ? "Role updated." : "Failed.");
                }
                case "4" -> {
                    System.out.print("User ID to delete: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    System.out.println(userDao.deleteUser(id) ? "User deleted." : "Failed (cannot delete owner).");
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}