package com.example.event_hub.Model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParticipantModel {

    @SerializedName("id")
    private Long id;

    @SerializedName("user")
    private UserSummary user;

    private Long eventId;

    @SerializedName("eventRole")
    private String eventRole;

    @SerializedName("status")
    private String status;
}