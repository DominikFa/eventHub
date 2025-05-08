package com.example.event_hub.Model; // Correct package

/**
 * Data Transfer Object (DTO) for encapsulating user registration details.
 * Used to pass registration information, typically to an authentication service or repository.
 */
public class RegisterRequest {

    private String username; // Often required for display or uniqueness
    private String email;    // User's email address, often used as login
    private String password;

    /**
     * Default constructor.
     */
    public RegisterRequest() {
    }

    /**
     * Constructs a new RegisterRequest.
     *
     * @param username The desired username for the new account.
     * @param email    The user's email address.
     * @param password The desired password for the new account.
     */
    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // --- Getters ---
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    // --- Setters ---
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Provides a string representation of the RegisterRequest object.
     * Avoids logging the password directly for security.
     * @return A string summary of the registration request.
     */
    @Override
    public String toString() {
        return "RegisterRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" + // Do not log password
                '}';
    }
}
