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
public class MediaModel {

    @SerializedName("id")
    private Long mediaId;

    @SerializedName("mediaType")
    private String mediaType;

    @SerializedName("usage")
    private String usage;

    @SerializedName("uploadedAt")
    private Date uploadedAt;

    @SerializedName("uploader")
    private UserSummary uploader;

    @SerializedName("downloadUrl")
    private String downloadUrl;

    private Long eventId;
}