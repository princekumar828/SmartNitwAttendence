package com.princesaha.attendance;

public class ChatMessage {
    private String id;
    private String userId;
    private String userName;
    private String message;
    private long timestamp;

    // Empty constructor for Firebase
    public ChatMessage() {}

    public ChatMessage(String id, String userId, String userName, String message, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}