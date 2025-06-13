package com.example.event_hub.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private boolean success;
    private String message;
    private UserModel user; // Changed from userId to the full user object from API doc
    private String token;

    public AuthResponse(boolean success, String message, UserModel user, String token) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.token = token;
    }
}