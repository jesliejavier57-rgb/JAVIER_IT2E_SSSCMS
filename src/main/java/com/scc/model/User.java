package com.scc.model;

import java.time.LocalDateTime;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String role; // Owner, Staff, Customer
    private Integer customerId; // Nullable FK
    private LocalDateTime dateCreated;

    public User() {}

    public User(int userId, String username, String passwordHash, String role, Integer customerId, LocalDateTime dateCreated) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customerId = customerId;
        this.dateCreated = dateCreated;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
}