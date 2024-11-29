package com.princesaha.attendance;

import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class AttendanceRecord {
    private GeoPoint startLocation;
    private GeoPoint endLocation;
    private String date;
    private String startTime;
    private String endTime;
    private String duration;
    private String status;

    // Constructor
    public AttendanceRecord(GeoPoint startLocation, GeoPoint endLocation, String date, String startTime, String endTime, String duration, String status) {
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.status = status;
    }

    // Getters and Setters
    public GeoPoint getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(GeoPoint startLocation) {
        this.startLocation = startLocation;
    }

    public GeoPoint getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(GeoPoint endLocation) {
        this.endLocation = endLocation;
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

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Map<String, Double> getStartAndEndCoordinates() {
        Map<String, Double> coordinates = new HashMap<>();
        if (startLocation != null) {
            coordinates.put("startLatitude", startLocation.getLatitude());
            coordinates.put("startLongitude", startLocation.getLongitude());
        }
        if (endLocation != null) {
            coordinates.put("endLatitude", endLocation.getLatitude());
            coordinates.put("endLongitude", endLocation.getLongitude());
        }
        return coordinates;
    }
}