package com.scc.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private int logId;
    private Integer userId;
    private String username;
    private String role;
    private String actionType; // LOGIN, LOGOUT, CREATE_UTANG, PAYMENT, EDIT, DELETE, etc.
    private String details;
    private LocalDateTime timestamp;

    public ActivityLog() {}

    public ActivityLog(int logId, Integer userId, String username, String role, String actionType, String details, LocalDateTime timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.actionType = actionType;
        this.details = details;
        this.timestamp = timestamp;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}