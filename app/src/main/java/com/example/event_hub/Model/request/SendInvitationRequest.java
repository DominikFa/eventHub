package com.example.event_hub.Model.request;

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
public class SendInvitationRequest {
    private Long eventId;
    private Long invitedUserId;
}