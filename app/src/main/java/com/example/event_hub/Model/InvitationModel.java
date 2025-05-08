package com.example.event_hub.Model; // Correct package

import java.util.Date;
import java.util.Objects;
import java.util.UUID; // For generating default ID

/**
 * Represents an invitation sent to a user (account) for a specific event,
 * corresponding to a row in the 'invitation' table from the database schema.
 */
public class InvitationModel {

    // Corresponds to invitation.invitation_id (PK)
    private String invitationId;

    // Corresponds to invitation.event_id (FK to event, Not Null)
    private String eventId; // Using String for flexibility, adjust if needed (e.g., Long)

    // Corresponds to invitation.account_id (FK to account, the recipient, Not Null)
    private String accountId; // Using String for flexibility, adjust if needed (e.g., Long)

    // Corresponds to invitation.invitation_status (Not Null)
    // DB Check Constraint: ('expired','accepted','declined','revoked','sent')
    private String invitationStatus;

    // Corresponds to invitation.sent_at (Not Null)
    private Date sentAt;

    // Corresponds to invitation.responded_at (Nullable)
    private Date respondedAt;

    /**
     * Default constructor. Initializes with default values.
     * Sets a default status and timestamp.
     */
    public InvitationModel() {
        // Generate a default ID for new instances, though the DB might override this upon insertion
        this.invitationId = UUID.randomUUID().toString();
        this.sentAt = new Date(); // Default to current time
        this.invitationStatus = "sent"; // Default status when creating a new invitation object
    }

    /**
     * Constructs a new InvitationModel.
     * Performs defensive copies for Date objects.
     *
     * @param invitationId     The unique ID of the invitation.
     * @param eventId          The ID of the event being invited to.
     * @param accountId        The ID of the account (user) being invited.
     * @param invitationStatus The current status of the invitation (e.g., "sent", "accepted").
     * @param sentAt           The timestamp when the invitation was sent. Should not be null.
     * @param respondedAt      The timestamp when the user responded (can be null).
     */
    public InvitationModel(String invitationId, String eventId, String accountId,
                           String invitationStatus, Date sentAt, Date respondedAt) {
        this.invitationId = invitationId;
        this.eventId = eventId;
        this.accountId = accountId;
        this.invitationStatus = invitationStatus;
        // Defensive copies for mutable Date objects
        this.sentAt = (sentAt != null) ? new Date(sentAt.getTime()) : new Date(); // Ensure sentAt is not null, default to now if it is
        this.respondedAt = (respondedAt != null) ? new Date(respondedAt.getTime()) : null;
    }

    // --- Getters ---
    public String getInvitationId() {
        return invitationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getInvitationStatus() {
        return invitationStatus;
    }

    public Date getSentAt() {
        // Return defensive copy to prevent external modification
        return (sentAt != null) ? new Date(sentAt.getTime()) : null;
    }

    public Date getRespondedAt() {
        // Return defensive copy to prevent external modification
        return (respondedAt != null) ? new Date(respondedAt.getTime()) : null;
    }

    // --- Setters ---
    public void setInvitationId(String invitationId) {
        this.invitationId = invitationId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setInvitationStatus(String invitationStatus) {
        // Optional: Add validation here to ensure status matches DB constraints
        // List<String> validStatuses = Arrays.asList("expired", "accepted", "declined", "revoked", "sent");
        // if (invitationStatus != null && validStatuses.contains(invitationStatus)) {
        this.invitationStatus = invitationStatus;
        // } else {
        //     System.err.println("Warning: Attempted to set invalid invitation status: " + invitationStatus);
        // Or throw new IllegalArgumentException("Invalid invitation status: " + invitationStatus);
        // }
    }

    public void setSentAt(Date sentAt) {
        // Defensive copy and null check
        this.sentAt = (sentAt != null) ? new Date(sentAt.getTime()) : null;
    }

    public void setRespondedAt(Date respondedAt) {
        // Defensive copy
        this.respondedAt = (respondedAt != null) ? new Date(respondedAt.getTime()) : null;
    }

    /**
     * Provides a string representation of the InvitationModel object.
     * @return A string summary of the invitation details.
     */
    @Override
    public String toString() {
        return "InvitationModel{" +
                "invitationId='" + invitationId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", invitationStatus='" + invitationStatus + '\'' +
                ", sentAt=" + sentAt +
                ", respondedAt=" + respondedAt +
                '}';
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Equality is based on the invitationId field (Primary Key).
     * @param o The reference object with which to compare.
     * @return true if this object is the same as the o argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvitationModel that = (InvitationModel) o;
        // Primary key comparison
        return Objects.equals(invitationId, that.invitationId);
    }

    /**
     * Returns a hash code value for the object.
     * This method is supported for the benefit of hash tables such as those provided by HashMap.
     * The hash code is based on the invitationId field (Primary Key).
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        // Hash code based on the primary key
        return Objects.hash(invitationId);
    }
}
