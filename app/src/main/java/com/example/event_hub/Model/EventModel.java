package com.example.event_hub.Model;

import java.util.Date;
import java.util.Objects; // For Objects.equals in a potential future equals() override
// Consider using java.time.LocalDateTime for more modern date/time handling if API level allows (API 26+)
// For simplicity with potential older Android versions or broader compatibility, java.util.Date is used here.

/**
 * Represents an event in the application.
 * This class holds all the details pertaining to an event,
 * such as its name, description, location, timing, creator, and public status.
 */
public class EventModel {

    private String id; // Unique identifier for the event
    private String title; // Title or name of the event
    private String description; // Detailed description of the event
    private String location; // Physical or virtual location of the event
    private Date startDate; // Start date and time of the event
    private Date endDate; // End date and time of the event
    private int maxParticipants; // Maximum number of participants allowed
    private String createdBy; // User ID of the event creator
    private boolean isPublic; // New field: true if the event is public, false otherwise

    /**
     * Default constructor.
     * Required for some deserialization libraries (e.g., Firebase).
     */
    public EventModel() {
        this.isPublic = true; // Default to public, can be changed by setter or constructor
    }

    /**
     * Constructs a new EventModel.
     *
     * @param id              The unique ID of the event.
     * @param title           The title of the event.
     * @param description     The description of the event.
     * @param location        The location of the event.
     * @param startDate       The start date and time of the event.
     * @param endDate         The end date and time of the event.
     * @param maxParticipants The maximum number of participants.
     * @param createdBy       The ID of the user who created the event.
     * @param isPublic        Whether the event is public or private.
     */
    public EventModel(String id, String title, String description, String location,
                      Date startDate, Date endDate, int maxParticipants, String createdBy, boolean isPublic) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        // Defensive copies for mutable Date objects
        this.startDate = (startDate != null) ? new Date(startDate.getTime()) : null;
        this.endDate = (endDate != null) ? new Date(endDate.getTime()) : null;
        this.maxParticipants = maxParticipants;
        this.createdBy = createdBy;
        this.isPublic = isPublic;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Date getStartDate() {
        // Return a defensive copy to protect internal state
        return startDate != null ? new Date(startDate.getTime()) : null;
    }

    public Date getEndDate() {
        // Return a defensive copy
        return endDate != null ? new Date(endDate.getTime()) : null;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public boolean isPublic() {
        return isPublic;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setStartDate(Date startDate) {
        // Store a defensive copy
        this.startDate = (startDate != null) ? new Date(startDate.getTime()) : null;
    }

    public void setEndDate(Date endDate) {
        // Store a defensive copy
        this.endDate = (endDate != null) ? new Date(endDate.getTime()) : null;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Provides a string representation of the EventModel object.
     * Useful for logging and debugging.
     * @return A string containing the event details.
     */
    @Override
    public String toString() {
        return "EventModel{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 50)) + "..." : "N/A") + '\'' +
                ", location='" + location + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", maxParticipants=" + maxParticipants +
                ", createdBy='" + createdBy + '\'' +
                ", isPublic=" + isPublic +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventModel that = (EventModel) o;
        return Objects.equals(id, that.id); // Primary key equality
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // Primary key hashcode
    }
}