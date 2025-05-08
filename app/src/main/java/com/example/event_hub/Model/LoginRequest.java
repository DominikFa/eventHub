package com.example.event_hub.Model; // Correct package

/**
 * Data Transfer Object (DTO) for encapsulating user login credentials.
 * Used to pass login information, typically to an authentication service or repository.
 */
public class LoginRequest {

    private String email;    // Can be email or username depending on login method
    private String password;

    /**
     * Default constructor.
     */
    public LoginRequest() {
    }

    /**
     * Constructs a new LoginRequest.
     *
     * @param email    The user's email or login identifier.
     * @param password The user's password.
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // --- Getters ---
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    // --- Setters ---
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Provides a string representation of the LoginRequest object.
     * Avoids logging the password directly for security.
     * @return A string summary of the login request.
     */
    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" + // Do not log password
                '}';
    }
}
