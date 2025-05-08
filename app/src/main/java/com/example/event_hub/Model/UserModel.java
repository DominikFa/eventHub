package com.example.event_hub.Model;

import java.util.Date; // Import Date
import java.util.Objects;

/**
 * Represents a user account in the application, primarily based on the 'account' table.
 * Includes related information from 'account_auth', 'account_role', 'account_status', and 'profile'.
 */
public class UserModel {

    // Corresponds to account.account_id (PK)
    private String userId; // Using String for flexibility, though DB might be INT/BIGINT

    // Corresponds to account_auth.login (Unique Index IX_account_auth_login suggests this is the username)
    private String login;

    // Note: Email is not explicitly in the core account/profile/auth tables provided.
    // It might be part of 'login' or stored elsewhere. Kept for common use cases.
    private String email;

    // Corresponds to account_role.role_name via account.role_id
    // DB Check Constraint: ('admin','user','organizer')
    private String role;

    // Corresponds to account.created_at
    private Date createdAt;

    // Corresponds to account_status.status_name via account.status_id
    // DB Check Constraint: ('banned','active','deactivated')
    private String status;

    // Represents data primarily from the 'profile' table, linked by account_id
    private UserDetails userDetails;

    /**
     * Default constructor. Initializes UserDetails.
     */
    public UserModel() {
        this.userDetails = new UserDetails();
        this.createdAt = new Date(); // Default to now if not specified
    }

    /**
     * Constructs a new UserModel based on database fields.
     *
     * @param userId      The unique ID of the user (from account.account_id).
     * @param login       The user's login/username (from account_auth.login).
     * @param email       The user's email (common field, though not explicit in provided DB schema).
     * @param role        The user's role (from account_role.role_name).
     * @param createdAt   The timestamp when the account was created (from account.created_at).
     * @param status      The current status of the account (from account_status.status_name).
     * @param userDetails Detailed profile information (from profile table). Can be null.
     */
    public UserModel(String userId, String login, String email, String role, Date createdAt, String status, UserDetails userDetails) {
        this.userId = userId;
        this.login = login;
        this.email = email; // Kept for convenience
        this.role = role;
        this.createdAt = (createdAt != null) ? new Date(createdAt.getTime()) : new Date(); // Defensive copy
        this.status = status;
        this.userDetails = (userDetails != null) ? userDetails : new UserDetails(); // Use provided or default
        // If UserDetails was passed, ensure its fields are set correctly based on profile table data
        if (userDetails != null) {
            // Example: Assuming profile data was fetched separately and put into userDetails
            // this.userDetails.setFullName(profile.getName());
            // this.userDetails.setDescription(profile.getDescription());
            // this.userDetails.setProfileImageUrl(profile.getProfile_image()); // Needs conversion/URL mapping
        } else {
            // Initialize UserDetails with defaults if none provided
            this.userDetails.setFullName(login); // Default full name to login
            this.userDetails.setDescription("No description available.");
            this.userDetails.setProfileImageUrl("default_avatar.png");
        }
    }

    // --- Getters ---
    public String getUserId() { return userId; }
    public String getLogin() { return login; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Date getCreatedAt() {
        // Defensive copy for mutable Date object
        return (createdAt != null) ? new Date(createdAt.getTime()) : null;
    }
    public String getStatus() { return status; }
    public UserDetails getUserDetails() { return userDetails; }

    // --- Setters ---
    public void setUserId(String userId) { this.userId = userId; }
    public void setLogin(String login) { this.login = login; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(Date createdAt) {
        // Defensive copy for mutable Date object
        this.createdAt = (createdAt != null) ? new Date(createdAt.getTime()) : null;
    }
    public void setStatus(String status) { this.status = status; }
    public void setUserDetails(UserDetails userDetails) { this.userDetails = userDetails; }

    /**
     * Provides a string representation of the UserModel object.
     * @return A string containing basic user information.
     */
    @Override
    public String toString() {
        return "UserModel{" +
                "userId='" + userId + '\'' +
                ", login='" + login + '\'' +
                ", email='" + email + '\'' + // Kept for info
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", userDetails=" + (userDetails != null ? userDetails.toString() : "null") +
                '}';
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Equality is based on the userId field.
     * @param o The reference object with which to compare.
     * @return true if this object is the same as the o argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserModel userModel = (UserModel) o;
        // Use userId for equality check, assuming it's the unique identifier (account_id)
        return Objects.equals(userId, userModel.userId);
    }

    /**
     * Returns a hash code value for the object.
     * This method is supported for the benefit of hash tables such as those provided by HashMap.
     * The hash code is based on the userId field.
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        // Use userId for hash code generation
        return Objects.hash(userId);
    }
}