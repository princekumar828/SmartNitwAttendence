package com.princesaha.attendance;

import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class AttendanceRecord {
    private GeoPoint startLocation;
    private String date;
    private String startTime;
    private String status;

    // Constructor
    public AttendanceRecord(GeoPoint startLocation, String date, String startTime, String status) {
        this.startLocation = startLocation;
        this.date = date;
        this.startTime = startTime;
        this.status = status;
    }

    // Getters and Setters
    public GeoPoint getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(GeoPoint startLocation) {
        this.startLocation = startLocation;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Double> getStartCoordinates() {
        Map<String, Double> coordinates = new HashMap<>();
        if (startLocation != null) {
            coordinates.put("latitude", startLocation.getLatitude());
            coordinates.put("longitude", startLocation.getLongitude());
        }
        return coordinates;
    }
}