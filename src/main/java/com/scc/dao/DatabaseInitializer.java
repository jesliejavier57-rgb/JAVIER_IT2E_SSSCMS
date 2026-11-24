package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DatabaseInitializer {

    public static void initialize() {
        String createCustomers = """
                CREATE TABLE IF NOT EXISTS Customers (
                    customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    address TEXT NOT NULL,
                    contact_no TEXT NOT NULL,
                    credit_limit REAL NOT NULL CHECK(credit_limit >= 0),
                    date_registered TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'Active' CHECK(status IN ('Active', 'Inactive'))
                );
                """;

        String createUtangRecords = """
                CREATE TABLE IF NOT EXISTS Utang_Records (
                    utang_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    item_description TEXT NOT NULL,
                    amount REAL NOT NULL CHECK(amount > 0),
                    date_taken TEXT NOT NULL,
                    due_date TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'Unpaid' CHECK(status IN ('Unpaid', 'Partial', 'Paid')),
                    FOREIGN KEY (customer_id) REFERENCES Customers(customer_id)
                );
                """;

        String createPayments = """
                CREATE TABLE IF NOT EXISTS Payments (
                    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    utang_id INTEGER NOT NULL,
                    amount_paid REAL NOT NULL CHECK(amount_paid > 0),
                    date_paid TEXT NOT NULL,
                    received_by INTEGER NOT NULL,
                    FOREIGN KEY (utang_id) REFERENCES Utang_Records(utang_id) ON DELETE CASCADE,
                    FOREIGN KEY (received_by) REFERENCES Users(user_id)
                );
                """;

        String createUsers = """
                CREATE TABLE IF NOT EXISTS Users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('Owner', 'Staff', 'Customer')),
                    customer_id INTEGER,
                    date_created TEXT NOT NULL,
                    FOREIGN KEY (customer_id) REFERENCES Customers(customer_id)
                );
                """;

        String createActivityLogs = """
                CREATE TABLE IF NOT EXISTS Activity_Logs (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    username TEXT NOT NULL,
                    role TEXT NOT NULL,
                    action_type TEXT NOT NULL,
                    details TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id)
                );
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createCustomers);
            stmt.execute(createUtangRecords);
            stmt.execute(createPayments);
            stmt.execute(createUsers);
            stmt.execute(createActivityLogs);

            seedDefaultAdmin(conn);
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    private static void seedDefaultAdmin(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM Users WHERE username = 'owner'";
        try (PreparedStatement ps = conn.prepareStatement(checkSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = """
                        INSERT INTO Users (username, password_hash, role, customer_id, date_created)
                        VALUES (?, ?, 'Owner', NULL, ?)
                        """;
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    ins.setString(1, "owner");
                    ins.setString(2, SecurityUtil.hashPassword("owner123"));
                    ins.setString(3, LocalDateTime.now().toString());
                    ins.executeUpdate();
                }

                // Seed a sample customer & staff account for convenience if empty
                String custCheck = "SELECT COUNT(*) FROM Customers";
                try (Statement s = conn.createStatement(); ResultSet crs = s.executeQuery(custCheck)) {
                    if (crs.next() && crs.getInt(1) == 0) {
                        String cIns = """
                                INSERT INTO Customers (full_name, address, contact_no, credit_limit, date_registered, status)
                                VALUES ('Juan Dela Cruz', 'Purok 3, Poblacion', '09171234567', 1500.00, ?, 'Active')
                                """;
                        try (PreparedStatement cps = conn.prepareStatement(cIns)) {
                            cps.setString(1, LocalDate.now().toString());
                            cps.executeUpdate();
                        }
                    }
                }
            }
        }
    }
}