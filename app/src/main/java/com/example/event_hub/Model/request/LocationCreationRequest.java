package com.example.event_hub.Model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationCreationRequest {
    private String streetName;
    private String streetNumber;
    private String apartment;
    private String postalCode;
    private String city;
    private String region;
    private String countryIsoCode;
    private double latitude;
    private double longitude;
}