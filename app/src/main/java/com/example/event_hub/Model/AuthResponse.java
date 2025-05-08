package com.example.event_hub.Model;

// Updated AuthResponse model to include a JWT token
public class AuthResponse {
    private boolean success;
    private String message;
    private String userId;
    private String token; // JWT Token

    public AuthResponse(boolean success, String message, String userId, String token) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.token = token;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }
}