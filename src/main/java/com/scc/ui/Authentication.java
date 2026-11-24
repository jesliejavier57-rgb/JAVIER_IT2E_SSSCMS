package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {
    private final Scanner scanner;
    private final UserDao userDao;
    private final ActivityLogDao logDao;

    public Authentication(Scanner scanner) {
        this.scanner = scanner;
        this.userDao = new UserDao();
        this.logDao = new ActivityLogDao();
    }

    public void start() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("   UTANGTRACK - SARI-SARI STORE SYSTEM");
            System.out.println("==========================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> {
                    System.out.println("Exiting UtangTrack. Salamat!");
                    return;
                }
                default -> System.out.println("Invalid option. Please enter 1, 2, or 3.");
            }
        }
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        User user = userDao.authenticate(username, password);
        if (user == null) {
            System.out.println("Invalid username or password.");
            logDao.log(null, username, "Unknown", "LOGIN_FAILED", "Failed login attempt for username: " + username);
            return;
        }

        logDao.log(user.getUserId(), user.getUsername(), user.getRole(), "LOGIN", "User logged in");
        System.out.println("Welcome, " + user.getUsername() + " [" + user.getRole() + "]!");

        switch (user.getRole()) {
            case "Owner" -> new AdminMenu(scanner, user, logDao).show();
            case "Staff" -> new StaffMenu(scanner, user, logDao).show();
            case "Customer" -> new CustomerMenu(scanner, user, logDao).show();
            default -> System.out.println("Unrecognized role: " + user.getRole());
        }

        logDao.log(user.getUserId(), user.getUsername(), user.getRole(), "LOGOUT", "User logged out");
    }

    private void register() {
        System.out.println("\n--- REGISTER ACCOUNT ---");
        System.out.print("Choose Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username already taken.");
            return;
        }

        System.out.print("Choose Password (min 4 chars): ");
        String password = scanner.nextLine().trim();
        if (password.length() < 4) {
            System.out.println("Password must be at least 4 characters.");
            return;
        }

        System.out.println("Select Role: 1) Staff  2) Customer");
        System.out.print("Choice: ");
        String roleChoice = scanner.nextLine().trim();
        String role;
        Integer customerId = null;

        if ("1".equals(roleChoice)) {
            role = "Staff";
        } else if ("2".equals(roleChoice)) {
            role = "Customer";
            System.out.print("Enter existing Customer ID (or leave blank if none yet): ");
            String cidInput = scanner.nextLine().trim();
            if (!cidInput.isEmpty()) {
                try {
                    int parsedId = Integer.parseInt(cidInput);
                    if (!userDao.customerExists(parsedId)) {
                        System.out.println("Customer ID " + parsedId + " does not exist. Registering without link.");
                    } else {
                        customerId = parsedId;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid Customer ID format. Registering without link.");
                }
            }
        } else {
            System.out.println("Invalid role choice. Registration cancelled.");
            return;
        }

        boolean created = userDao.registerUser(username, password, role, customerId);
        if (created) {
            System.out.println("Registration successful! You can now log in.");
            logDao.log(null, username, role, "REGISTER", "New user registered with role: " + role);
        } else {
            System.out.println("Registration failed.");
        }
    }
}