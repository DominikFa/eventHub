package com.example.event_hub.Model.request;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCreationRequest {
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;

    // The @SerializedName("public") annotation has been removed from this field
    private boolean isPublic;

    private Integer maxParticipants;

    private Long locationId;
    private LocationCreationRequest location; // Zmieniono typ z LocationData

    // Konstruktor dla tworzenia z nową lokalizacją
    public EventCreationRequest(String name, String description, Date startDate, Date endDate, boolean isPublic, Integer maxParticipants, LocationCreationRequest location) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isPublic = isPublic;
        this.maxParticipants = maxParticipants;
        this.location = location;
    }

    // Konstruktor dla tworzenia z istniejącym ID lokalizacji
    public EventCreationRequest(String name, String description, Date startDate, Date endDate, boolean isPublic, Integer maxParticipants, Long locationId) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isPublic = isPublic;
        this.maxParticipants = maxParticipants;
        this.locationId = locationId;
    }
}