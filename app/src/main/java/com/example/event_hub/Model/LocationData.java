package com.example.event_hub.Model;

public class LocationData {
    private double latitude;
    private double longitude;
    private String address; // Original address string, if available
    private String eventTitle; // Optional: for map marker title

    public LocationData(double latitude, double longitude, String address, String eventTitle) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.eventTitle = eventTitle;
    }

    public LocationData(String address, String eventTitle) {
        this.address = address;
        this.eventTitle = eventTitle;
        // Latitude and longitude would be resolved via geocoding if only address is provided
        this.latitude = 0; // Default
        this.longitude = 0; // Default
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", address='" + address + '\'' +
                ", eventTitle='" + eventTitle + '\'' +
                '}';
    }
}
