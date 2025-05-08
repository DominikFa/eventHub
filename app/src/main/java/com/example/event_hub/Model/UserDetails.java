package com.example.event_hub.Model;

/**
 * Represents detailed profile information for a user, corresponding to the 'profile' table.
 * This can be a separate public class if needed elsewhere.
 */
public class UserDetails {
    // Corresponds to profile.name
    private String fullName;
    // Corresponds to profile.description
    private String description;
    // Corresponds to profile.profile_image (Note: DB type is bytea, storing URL/path here)
    private String profileImageUrl;
    // Note: 'interests' field removed as it's not present in the provided 'profile' table schema.

    /**
     * Constructor for UserDetails.
     *
     * @param fullName        The user's full name (from profile.name).
     * @param description     The user's description/bio (from profile.description).
     * @param profileImageUrl The URL/path to the user's profile image (represents profile.profile_image).
     */
    public UserDetails(String fullName, String description, String profileImageUrl) {
        this.fullName = fullName;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
    }

    // Default constructor
    public UserDetails() {
    }

    // --- Getters ---
    public String getFullName() { return fullName; }
    public String getDescription() { return description; }
    public String getProfileImageUrl() { return profileImageUrl; }

    // --- Setters ---
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDescription(String description) { this.description = description; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }


    @Override
    public String toString() {
        return "UserDetails{" +
                "fullName='" + fullName + '\'' +
                ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 20))+"..." : "N/A") + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                '}';
    }
}
