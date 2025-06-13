package com.example.event_hub.Model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Data
@NoArgsConstructor
@Getter
@Setter
public class NotificationModel {

    @SerializedName("id")
    private Long id;

    @SerializedName("message")
    private String message;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("status")
    private String status;

    @SerializedName("eventId")
    private Long eventId;

    @SerializedName("eventName")
    private String eventName;

    @SerializedName("recipient")
    private UserSummary recipient;
}