package com.example.event_hub.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String name; // Re-added as per API requirement
    private String login; // Changed from email to login
    private String password;

}