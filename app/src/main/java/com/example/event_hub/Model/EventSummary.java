package com.example.event_hub.Model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import lombok.Data;

@Data
public class EventSummary {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("startDate")
    private Date startDate;

    @SerializedName("endDate")
    private Date endDate;
}