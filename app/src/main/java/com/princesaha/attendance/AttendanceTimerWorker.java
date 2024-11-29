package com.princesaha.attendance;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

public class AttendanceTimerWorker extends Worker {
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private boolean hasError = false; // Flag to track if any failure occurred

    public AttendanceTimerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve schedule data passed from the WorkManager
        ScheduleItem scheduleItem = (ScheduleItem) getInputData().getKeyValueMap().get("scheduleItem");

        if (scheduleItem == null) {
            logError("No schedule item data provided.");
            return Result.failure(); // No schedule item data provided
        }

        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            logError("Location permissions not granted.");
            return Result.failure(); // Permissions not granted, worker cannot proceed
        }

        CountDownLatch latch = new CountDownLatch(1); // Synchronize the asynchronous operation

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Compare with course location
                Map<String, Double> courseLocation = scheduleItem.getLocation();
                if (courseLocation != null) {
                    double courseLat = courseLocation.get("latitude");
                    double courseLng = courseLocation.get("longitude");

                    float[] results = new float[1];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), courseLat, courseLng, results);

                    // Check if the distance is within 50 meters
                    if (results[0] <= 50) {
                        // Check the current time against the course time
                        Calendar currentTime = Calendar.getInstance();
                        Calendar courseStartTime = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                        sdf.setTimeZone(TimeZone.getDefault());
                        try {
                            courseStartTime.setTime(sdf.parse(scheduleItem.getTime()));
                        } catch (Exception e) {
                            logError("Error parsing course start time: " + e.getMessage());
                            hasError = true;
                        }

                        if (currentTime.getTimeInMillis() >= courseStartTime.getTimeInMillis() && !hasError) {
                            // Log attendance data
                            logAttendance(location, results[0]);
                        } else {
                            logError("Current time is before the course start time or there was a parsing error.");
                            hasError = true;
                        }
                    } else {
                        logError("User is not within 50 meters of the course location.");
                        hasError = true;
                    }
                } else {
                    logError("Course location is not available.");
                    hasError = true;
                }
            } else {
                logError("Failed to retrieve current location.");
                hasError = true;
            }
            latch.countDown(); // Release the latch after processing
        }).addOnFailureListener(e -> {
            logError("Failed to retrieve location: " + e.getMessage());
            hasError = true;
            latch.countDown(); // Release the latch in case of failure
        });

        try {
            latch.await(); // Wait for the asynchronous task to complete
        } catch (InterruptedException e) {
            logError("Worker was interrupted: " + e.getMessage());
            return Result.failure();
        }

        // Return result based on error flag
        return hasError ? Result.failure() : Result.success();
    }

    private void logAttendance(Location location, float distance) {
        String studentId = "yourStudentId"; // Replace with the actual student ID logic
        String subjectId = "yourSubjectId"; // Replace with the actual subject ID logic

        // Create a new attendance record
        Map<String, Object> attendanceRecord = new HashMap<>();
        attendanceRecord.put("date", new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()));
        attendanceRecord.put("startTime", new SimpleDateFormat("hh:mm a").format(Calendar.getInstance().getTime()));
        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.MINUTE, 60); // Add 60 minutes to the current time
        attendanceRecord.put("endTime", new SimpleDateFormat("hh:mm a").format(endTime.getTime()));
        attendanceRecord.put("duration", 60); // Placeholder for duration in minutes
        attendanceRecord.put("startLocation", new HashMap<String, Double>() {{
            put("latitude", location.getLatitude());
            put("longitude", location.getLongitude());
        }});
        attendanceRecord.put("endLocation", new HashMap<String, Double>() {{
            put("latitude", location.getLatitude());
            put("longitude", location.getLongitude());
        }});
        attendanceRecord.put("status", "Present");

        // Store the attendance record in Firestore
        db.collection("Attendance")
                .document(subjectId)
                .collection("Students")
                .document(studentId)
                .collection("attendanceRecords")
                .add(attendanceRecord)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Attendance recorded successfully");
                })
                .addOnFailureListener(e -> {
                    logError("Error recording attendance in Firestore: " + e.getMessage());
                    hasError = true; // Set flag if Firestore writing fails
                });
    }

    private void logError(String message) {
        // Log errors to system output or a more sophisticated logging system
        System.err.println("AttendanceTimerWorker Error: " + message);
    }
}