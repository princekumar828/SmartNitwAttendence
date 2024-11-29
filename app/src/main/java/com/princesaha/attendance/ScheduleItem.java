package com.princesaha.attendance;


import java.io.Serializable;
import java.util.Map;

public class ScheduleItem implements Serializable {
    private String time;
    private String subjectName;
    private String teacherName; // Faculty name
    private String roomNo;
    private String subjectType;
    private int credits;
    private boolean isAvailable;
    private Map<String, Double> location; // Assuming location contains latitude and longitude
    private String courseCode; // New field for course code

    // Default constructor
    public ScheduleItem() {
    }

    // Constructor with only essential fields
    public ScheduleItem(String time, String subjectName, String teacherName, String courseCode) {
        this.time = time;
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.courseCode = courseCode;
    }

    @Override
    public String toString() {
        return "ScheduleItem{" +
                "time='" + time + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", roomNo='" + roomNo + '\'' +
                ", subjectType='" + subjectType + '\'' +
                ", credits=" + credits +
                ", isAvailable=" + isAvailable +
                ", location=" + location +
                ", courseCode='" + courseCode + '\'' +
                '}';
    }

    // Constructor with all fields
    public ScheduleItem(String time, String subjectName, String teacherName, String roomNo,
                        String subjectType, int credits, boolean isAvailable, Map<String, Double> location, String courseCode) {
        this.time = time;
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.roomNo = roomNo;
        this.subjectType = subjectType;
        this.credits = credits;
        this.isAvailable = isAvailable;
        this.location = location;
        this.courseCode = courseCode;
    }

    // Getters
    public String getTime() {
        return time;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public int getCredits() {
        return credits;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public Map<String, Double> getLocation() {
        return location;
    }

    public String getCourseCode() {
        return courseCode;
    }

    // Setters
    public void setTime(String time) {
        this.time = time;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public void setLocation(Map<String, Double> location) {
        this.location = location;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
}