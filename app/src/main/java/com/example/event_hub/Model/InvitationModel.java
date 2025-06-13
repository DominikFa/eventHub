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
public class InvitationModel {

    @SerializedName("id")
    private Long invitationId;

    @SerializedName("eventSummary")
    private EventSummary event;

    @SerializedName("invitedUser")
    private UserSummary invitedUser;

    @SerializedName("invitingUser")
    private UserSummary invitingUser;

    @SerializedName("status")
    private String invitationStatus;

    @SerializedName("sentAt")
    private Date sentAt;

    @SerializedName("respondedAt")
    private Date respondedAt;
}