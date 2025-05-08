package com.example.event_hub.Model;

import java.util.Date;
// Consider using java.time.LocalDateTime for more modern date/time handling if API level allows (API 26+)
// For simplicity with potential older Android versions or broader compatibility, java.util.Date is used here.

/**
 * Represents an event in the application.
 * This class holds all the details pertaining to an event,
 * such as its name, description, location, timing, and creator.
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
    // private UserModel createdByUser; // Alternatively, you could store the UserModel object directly if needed

    /**
     * Default constructor.
     * Required for some deserialization libraries (e.g., Firebase).
     */
    public EventModel() {
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
     */
    public EventModel(String id, String title, String description, String location,
                      Date startDate, Date endDate, int maxParticipants, String createdBy) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxParticipants = maxParticipants;
        this.createdBy = createdBy;
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
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public String getCreatedBy() {
        return createdBy;
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
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
                ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 50)) + "..." : "N/A") + '\'' + // Truncate description for brevity
                ", location='" + location + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", maxParticipants=" + maxParticipants +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }

    // Consider adding equals() and hashCode() methods if you plan to store EventModel objects
    // in collections like HashSets or use them as keys in HashMaps.
    // For example:
    // @Override
    // public boolean equals(Object o) {
    //     if (this == o) return true;
    //     if (o == null || getClass() != o.getClass()) return false;
    //     EventModel that = (EventModel) o;
    //     return id.equals(that.id); // Assuming ID is unique and sufficient for equality
    // }
    //
    // @Override
    // public int hashCode() {
    //     return id.hashCode(); // Assuming ID is unique
    // }
}
