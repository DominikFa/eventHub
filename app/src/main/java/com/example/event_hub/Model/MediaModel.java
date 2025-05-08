package com.example.event_hub.Model; // Correct package

import java.util.Date;
import java.util.Objects;
import java.util.UUID; // For generating default ID

/**
 * Represents a media item (e.g., image, video, document) associated with an event or user,
 * corresponding to a row in the 'media' table from the database schema.
 */
public class MediaModel {

    // Corresponds to media.media_id (PK)
    private String mediaId;

    // Corresponds to media.event_id (FK to event, Not Null)
    private String eventId; // Using String, adjust if needed (e.g., Long)

    // Corresponds to media.account_id (FK to account, Nullable - uploader/owner)
    private String accountId; // Using String, adjust if needed (e.g., Long)

    // Represents media.media_file (DB type bytea). Store URL/path or reference here.
    private String mediaFileReference; // Renamed from media_file for clarity

    // Corresponds to media.media_type (VARCHAR(100), Not Null)
    // DB Check Constraint: ('image/jpeg','image/png','image/gif','image/webp','image/svg+xml','video/mp4','video/webm','video/ogg','application/pdf')
    private String mediaType;

    // Corresponds to media.uploaded_at (TIMESTAMP WITH TIME ZONE, Not Null)
    private Date uploadedAt;

    // Corresponds to media.usage (VARCHAR(30), Not Null)
    // DB Check Constraint: ('gallery','schedule','logo')
    private String usage;

    // Optional: Add fields like file name or description if needed, though not in the DB schema
    private String fileName; // Not in DB schema, but often useful
    private String description; // Not in DB schema

    /**
     * Default constructor. Initializes with default values.
     */
    public MediaModel() {
        this.mediaId = UUID.randomUUID().toString(); // Generate default ID
        this.uploadedAt = new Date(); // Default to now
    }

    /**
     * Constructs a new MediaModel based on database fields.
     * Performs defensive copy for Date object.
     *
     * @param mediaId            The unique ID of the media item.
     * @param eventId            The ID of the event this media is associated with.
     * @param accountId          The ID of the user who uploaded/owns the media (can be null).
     * @param mediaFileReference A reference (URL, path, identifier) to the actual media file.
     * @param mediaType          The MIME type of the media (e.g., "image/png").
     * @param uploadedAt         The timestamp when the media was uploaded.
     * @param usage              The intended use of the media (e.g., "gallery", "logo").
     * @param fileName           (Optional) The original file name.
     * @param description        (Optional) A description for the media.
     */
    public MediaModel(String mediaId, String eventId, String accountId, String mediaFileReference,
                      String mediaType, Date uploadedAt, String usage, String fileName, String description) {
        this.mediaId = (mediaId != null && !mediaId.isEmpty()) ? mediaId : UUID.randomUUID().toString();
        this.eventId = eventId; // Required by DB schema (Not Null)
        this.accountId = accountId; // Nullable in DB schema
        this.mediaFileReference = mediaFileReference; // Required by DB schema (Not Null)
        this.mediaType = mediaType; // Required by DB schema (Not Null)
        this.uploadedAt = (uploadedAt != null) ? new Date(uploadedAt.getTime()) : new Date(); // Defensive copy, Not Null in DB
        this.usage = usage; // Required by DB schema (Not Null)
        this.fileName = fileName; // Optional field
        this.description = description; // Optional field

        // Basic validation based on DB constraints (can be expanded)
        if (this.eventId == null || this.eventId.isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty for MediaModel.");
        }
        if (this.mediaFileReference == null || this.mediaFileReference.isEmpty()) {
            throw new IllegalArgumentException("Media File Reference cannot be null or empty for MediaModel.");
        }
        if (this.mediaType == null || this.mediaType.isEmpty()) {
            throw new IllegalArgumentException("Media Type cannot be null or empty for MediaModel.");
        }
        if (this.usage == null || this.usage.isEmpty()) {
            throw new IllegalArgumentException("Usage cannot be null or empty for MediaModel.");
        }
    }

    // Convenience constructor without optional fields
    public MediaModel(String mediaId, String eventId, String accountId, String mediaFileReference,
                      String mediaType, Date uploadedAt, String usage) {
        this(mediaId, eventId, accountId, mediaFileReference, mediaType, uploadedAt, usage, null, null);
    }


    // --- Getters ---
    public String getMediaId() { return mediaId; }
    public String getEventId() { return eventId; }
    public String getAccountId() { return accountId; }
    public String getMediaFileReference() { return mediaFileReference; }
    public String getMediaType() { return mediaType; }
    public Date getUploadedAt() {
        // Return defensive copy
        return (uploadedAt != null) ? new Date(uploadedAt.getTime()) : null;
    }
    public String getUsage() { return usage; }
    public String getFileName() { return fileName; }
    public String getDescription() { return description; }

    // --- Setters ---
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public void setEventId(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty.");
        }
        this.eventId = eventId;
    }
    public void setAccountId(String accountId) { this.accountId = accountId; } // Nullable
    public void setMediaFileReference(String mediaFileReference) {
        if (mediaFileReference == null || mediaFileReference.isEmpty()) {
            throw new IllegalArgumentException("Media File Reference cannot be null or empty.");
        }
        this.mediaFileReference = mediaFileReference;
    }
    public void setMediaType(String mediaType) {
        if (mediaType == null || mediaType.isEmpty()) {
            throw new IllegalArgumentException("Media Type cannot be null or empty.");
        }
        // Optional: Add validation against DB check constraints
        this.mediaType = mediaType;
    }
    public void setUploadedAt(Date uploadedAt) {
        // Defensive copy, ensure not null
        this.uploadedAt = (uploadedAt != null) ? new Date(uploadedAt.getTime()) : new Date();
    }
    public void setUsage(String usage) {
        if (usage == null || usage.isEmpty()) {
            throw new IllegalArgumentException("Usage cannot be null or empty.");
        }
        // Optional: Add validation against DB check constraints ('gallery','schedule','logo')
        this.usage = usage;
    }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Provides a string representation of the MediaModel object.
     * @return A string summary of the media details.
     */
    @Override
    public String toString() {
        return "MediaModel{" +
                "mediaId='" + mediaId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", mediaFileReference='" + mediaFileReference + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", usage='" + usage + '\'' +
                ", fileName='" + fileName + '\'' +
                ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 20))+"..." : "N/A") + '\'' +
                '}';
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Equality is based on the mediaId field (Primary Key).
     * @param o The reference object with which to compare.
     * @return true if this object is the same as the o argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaModel that = (MediaModel) o;
        // Primary key comparison
        return Objects.equals(mediaId, that.mediaId);
    }

    /**
     * Returns a hash code value for the object.
     * The hash code is based on the mediaId field (Primary Key).
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        // Hash code based on the primary key
        return Objects.hash(mediaId);
    }
}
