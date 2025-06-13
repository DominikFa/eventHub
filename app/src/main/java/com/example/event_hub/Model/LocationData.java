package com.example.event_hub.Model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationData {
    @SerializedName("id")
    private Long id; // Changed from String to Long
    @SerializedName("streetName")
    private String streetName;
    @SerializedName("streetNumber")
    private String streetNumber;
    private String apartment;
    @SerializedName("postalCode")
    private String postalCode;
    @SerializedName("city")
    private String city;
    @SerializedName("region")
    private String region;
    @SerializedName("countryIsoCode")
    private String countryIsoCode;
    @SerializedName("fullAddress")
    private String fullAddress;
    private double latitude;
    private double longitude;

    // Constructor for creating a new location without an ID (for POST requests)
    public LocationData(String streetName, String streetNumber, String postalCode, String city,
                        String region, String countryIsoCode) {
        this.streetName = streetName;
        this.streetNumber = streetNumber;
        this.postalCode = postalCode;
        this.city = city;
        this.region = region;
        this.countryIsoCode = countryIsoCode;
    }

    // Constructor including apartment
    public LocationData(String streetName, String streetNumber, String apartment, String postalCode, String city,
                        String region, String countryIsoCode) {
        this.streetName = streetName;
        this.streetNumber = streetNumber;
        this.apartment = apartment;
        this.postalCode = postalCode;
        this.city = city;
        this.region = region;
        this.countryIsoCode = countryIsoCode;
    }
}