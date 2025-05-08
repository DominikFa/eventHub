package com.example.event_hub.Model; // Correct package

import java.util.Objects;

/**
 * Represents a participant's relationship with a specific event,
 * corresponding to a row in the 'participant' table from the database schema.
 * It links an account (user) to an event and defines their status and role within it.
 */
public class ParticipantModel {

    // Corresponds to participant.event_id (Part of Composite PK, FK to event)
    private String eventId; // Using String for flexibility, adjust if needed (e.g., Long)

    // Corresponds to participant.account_id (Part of Composite PK, FK to account)
    private String accountId; // Using String for flexibility, adjust if needed (e.g., Long)

    // Corresponds to participant.status
    // DB Check Constraint: ('banned','attending','cancelled')
    private String status;

    // Corresponds to participant.event_role
    // DB Check Constraint: ('moderator','participant','organizer')
    private String eventRole;

    /**
     * Default constructor.
     */
    public ParticipantModel() {
    }

    /**
     * Constructs a new ParticipantModel.
     *
     * @param eventId    The ID of the event.
     * @param accountId  The ID of the account (user) participating.
     * @param status     The status of the participant in the event (e.g., "attending", "cancelled").
     * @param eventRole  The role of the participant in the event (e.g., "participant", "moderator").
     */
    public ParticipantModel(String eventId, String accountId, String status, String eventRole) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.status = status;
        this.eventRole = eventRole;
    }

    // --- Getters ---
    public String getEventId() {
        return eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStatus() {
        return status;
    }

    public String getEventRole() {
        return eventRole;
    }

    // --- Setters ---
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setStatus(String status) {
        // Optional: Add validation here to ensure status matches DB constraints
        // if ("banned".equals(status) || "attending".equals(status) || "cancelled".equals(status)) {
        this.status = status;
        // } else {
        //     throw new IllegalArgumentException("Invalid participant status: " + status);
        // }
    }

    public void setEventRole(String eventRole) {
        // Optional: Add validation here to ensure role matches DB constraints
        // if ("moderator".equals(eventRole) || "participant".equals(eventRole) || "organizer".equals(eventRole)) {
        this.eventRole = eventRole;
        // } else {
        //     throw new IllegalArgumentException("Invalid event role: " + eventRole);
        // }
    }

    /**
     * Provides a string representation of the ParticipantModel object.
     * @return A string summary of the participant's relationship with the event.
     */
    @Override
    public String toString() {
        return "ParticipantModel{" +
                "eventId='" + eventId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", status='" + status + '\'' +
                ", eventRole='" + eventRole + '\'' +
                '}';
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Equality is based on the composite primary key (eventId and accountId).
     * @param o The reference object with which to compare.
     * @return true if this object is the same as the o argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantModel that = (ParticipantModel) o;
        // Compare based on the composite key fields
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(accountId, that.accountId);
    }

    /**
     * Returns a hash code value for the object.
     * This method is supported for the benefit of hash tables such as those provided by HashMap.
     * The hash code is based on the composite primary key (eventId and accountId).
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        // Generate hash code based on the composite key fields
        return Objects.hash(eventId, accountId);
    }
}