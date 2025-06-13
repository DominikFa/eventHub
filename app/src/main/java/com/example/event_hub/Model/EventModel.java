package com.example.event_hub.Model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@Getter
@Setter
public class EventModel {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("location")
    private LocationData location;

    @SerializedName("startDate")
    private Date startDate;

    @SerializedName("endDate")
    private Date endDate;

    @SerializedName("maxParticipants")
    private int maxParticipants;

    @SerializedName("organizer")
    private UserSummary organizer;

    @SerializedName("public")
    private boolean isPublic;

    @SerializedName("participantsCount")
    private int participantsCount;
}