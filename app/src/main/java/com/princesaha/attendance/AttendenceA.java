package com.princesaha.attendance;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.princesaha.attendance.databinding.ActivityAttendenceBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class AttendenceA extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private ActivityAttendenceBinding binding;
    private ScheduleItem scheduleItem;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isAttendanceStarted = false;
    private CountDownTimer attendanceTimer;
    private FirebaseFirestore firestore;

    private List<AttendanceRecord> attendanceList = new ArrayList<>();
    private AttendanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firestore=FirebaseFirestore.getInstance();

        // Initialize View Binding
        binding = ActivityAttendenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if(!checkLocationPermission()){
            if(!checkLocationPermission()){
                Toast.makeText(this, "Permission denied. Cannot mark attendance without location access.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }


        // Get ScheduleItem from Intent
        Intent intent = getIntent();
        scheduleItem = (ScheduleItem) intent.getSerializableExtra("scheduleItem");

        if (scheduleItem != null) {
            // Populate the UI with the schedule item details using View Binding
            binding.time.setText("Time: " + scheduleItem.getTime());
            binding.subjectName.setText("Subject: " + scheduleItem.getSubjectName());
            binding.teacherName.setText("Teacher: " + scheduleItem.getTeacherName());
            binding.roomNo.setText("Room No: " + (scheduleItem.getRoomNo() != null ? scheduleItem.getRoomNo() : "Not available"));
            binding.subjectType.setText("Type: " + scheduleItem.getSubjectType());
            binding.credits.setText("Credits: " + scheduleItem.getCredits());
            binding.availability.setText("Available: " + (scheduleItem.isAvailable() ? "Yes" : "No"));
            binding.courseCode.setText("Course Code: " + scheduleItem.getCourseCode());

            // Display location details if available
            if (scheduleItem.getLocation() != null) {
                Map<String, Double> location = scheduleItem.getLocation();
                binding.location.setText("Location: Lat " + location.get("latitude") + ", Long " + location.get("longitude"));
            } else {
                binding.location.setText("Location: Not available");
            }



        }


        binding.attenBtn.setOnClickListener(v -> {
            if (!isAttendanceStarted) {
                // Start Attendance
                isAttendanceStarted = true;
                binding.attenBtn.setText("Commit Attendance");

                try {
                    startAttendanceTimer();
                } catch (Exception e) {
                    // Handle any unexpected errors during attendance start
                    Log.e("AttendanceError", "Error starting attendance: " + e.getMessage(), e);
                    resetAttendanceState("Unable to start attendance. Please try again.");
                }

            } else {
                // Finalize Attendance
                if (attendanceTimer != null) {
                    attendanceTimer.cancel(); // Stop the timer
                }
                commitAttendance();
                resetAttendanceState(null); // Reset to default state after attendance commit
            }
        });


        binding.reportAttendance.setLayoutManager(new LinearLayoutManager(this));
        fetchAttendanceRecords();
        adapter = new AttendanceAdapter(attendanceList);
        binding.reportAttendance.setAdapter(adapter);
    }


    private void fetchAttendanceRecords() {
        String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("Attendance")
                .document(scheduleItem.getCourseCode())
                .collection("Students")
                .document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<Map<String, Object>> records = (List<Map<String, Object>>) document.get("attendanceRecords");
                            for (Map<String, Object> record : records) {
                                GeoPoint startLocation = (GeoPoint) record.get("startLocation");
                                GeoPoint endLocation = (GeoPoint) record.get("endLocation");
                                String date= (String) record.get("date");
                                String startTime= (String) record.get("startTime");
                                String endTime= (String) record.get("endTime");
                                String duration=record.get("duration").toString();
                                String status= (String) record.get("status");
                                AttendanceRecord tempRecord = new AttendanceRecord(startLocation, endLocation, date, startTime, endTime, duration, status);
                                attendanceList.add(tempRecord);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        Log.e("FirestoreError", "Failed to get document", task.getException());
                    }
                });
    }


    private void resetAttendanceState(String message) {
        isAttendanceStarted = false;
        binding.attenBtn.setText("Start Attendance"); // Reset button text
        binding.timer.setVisibility(View.GONE); // Hide timer

        if (attendanceTimer != null) {
            attendanceTimer.cancel(); // Stop the timer if running
        }

        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }

        Log.d("AttendanceReset", "Attendance state reset to default.");
    }




    private void startAttendanceTimer() {
        binding.timer.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Attendance Button clicked: Starting attendance process.", Toast.LENGTH_SHORT).show();

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required. Please allow to proceed.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

            resetAttendanceState("Location permission not granted. Attendance process aborted.");
            Log.d("Attendance", "Location permission not granted. Prompting user to allow.");
            return;
        }

        // Get the current location
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null && isLocationValid(location)) {
                        // Log: Valid location obtained
                        Log.d("Attendance", "Valid location obtained. Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                        Toast.makeText(this, "Valid location obtained. Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude(), Toast.LENGTH_SHORT).show();

                        // Start the timer

                        long courseDuration = courceDuration(); // Must return duration in milliseconds
                        if (courseDuration <= 0) {
                            resetAttendanceState("Invalid course duration. Attendance process aborted.");
                            Log.d("Attendance", "Invalid course duration.");
                            return;
                        }

                        long startTime = System.currentTimeMillis();

                        // Save the start time in SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("AttendancePrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("startLatitude", String.valueOf(location.getLatitude()));
                        editor.putString("startLongitude", String.valueOf(location.getLongitude()));
                        editor.putLong("startTime", startTime);
                        editor.apply();



                        // Log: Timer started
                        Log.d("Attendance", "Attendance timer started at: " + startTime);

                        attendanceTimer = new CountDownTimer(courseDuration, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                binding.timer.setText(String.format(Locale.getDefault(), "Time left: %02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60));
                            }

                            @Override
                            public void onFinish() {
                                binding.timer.setText("Course Ended");
                                commitAttendance();

                                // Log: Timer finished
                                Log.d("Attendance", "Attendance timer finished.");
                            }
                        }.start();

                    } else {
                        resetAttendanceState("Invalid location. Move closer to the scheduled location.");
                        Log.d("Attendance", "Invalid location. Current location does not match the scheduled location.");
                    }
                }).addOnFailureListener(e -> {
                    resetAttendanceState("Failed to get location: " + e.getMessage());
                    Log.d("Attendance", "Failed to retrieve location. Error: " + e.getMessage());
                }).addOnCompleteListener(task -> {
                    // Cleanup token
                    cancellationTokenSource.cancel();
                });
    }

    private long courceDuration() {
        String timeRange = scheduleItem.getTime(); // Example format: "10:00-11:30"
        String[] times = timeRange.split("-");

        if (times.length == 2) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getDefault());

                // Parse start and end times
                Date startTime = sdf.parse(times[0].trim());
                Date endTime = sdf.parse(times[1].trim());

                if (startTime != null && endTime != null) {
                    // Calculate the course duration in milliseconds
                    long durationMillis = endTime.getTime() - startTime.getTime();
                    // Log the calculated duration for debugging
                    Log.d("CourseDuration", "Course duration in milliseconds: " + durationMillis);

                    return durationMillis;
                }
            } catch (Exception e) {
                Log.e("CourseDuration", "Error parsing course time: " + e.getMessage(), e);
                Toast.makeText(this, "Unable to calculate course duration. Please check the time format.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid time format in schedule. Please check.", Toast.LENGTH_SHORT).show();
            Log.e("CourseDuration", "Time range format invalid: " + timeRange);
        }

        // Return a default value of 1 hour in milliseconds as a fallback
        return 3600000; // 1 hour
    }

    private void commitAttendance() {
        SharedPreferences sharedPreferences = getSharedPreferences("AttendancePrefs", MODE_PRIVATE);
        long startTime = sharedPreferences.getLong("startTime", 0);

        if (startTime == 0) {
            Toast.makeText(this, "Attendance not started!", Toast.LENGTH_SHORT).show();
            return;
        }

        long endTime = System.currentTimeMillis();
        int duration = (int) ((endTime - startTime) / 60000); // Convert to minutes

        // Check location permissions and request if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && isLocationValid(location)) {
                // Create an attendance record
                Map<String, Object> attendanceRecord = new HashMap<>();
                attendanceRecord.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                attendanceRecord.put("startTime", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(startTime)));
                attendanceRecord.put("endTime", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(endTime)));
                attendanceRecord.put("duration", duration);
                attendanceRecord.put("startLocation", new GeoPoint(
                        Double.parseDouble(sharedPreferences.getString("startLatitude", "0")),
                        Double.parseDouble(sharedPreferences.getString("startLongitude", "0"))
                ));
                attendanceRecord.put("endLocation", new GeoPoint(location.getLatitude(), location.getLongitude()));
                attendanceRecord.put("status", duration > 1 ? "Present" : "Absent");

                // Replace 'subjectId' and 'studentId' with actual dynamic identifiers
                String subjectId = scheduleItem.getCourseCode(); // Replace this with the actual value
                String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Or use another method to get the student ID

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("Attendance")
                        .document(subjectId)
                        .collection("Students")
                        .document(studentId)
                        .set(
                                new HashMap<String, Object>() {{
                                    put("attendanceRecords", FieldValue.arrayUnion(attendanceRecord));
                                }},
                                SetOptions.merge() // This will merge data if document exists or create it if it doesn't
                        )
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Attendance committed!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to commit attendance. " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Invalid location data. Attendance not recorded.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to get location.", Toast.LENGTH_SHORT).show());
    }



    private boolean isLocationValid(Location currentLocation) {
        if (scheduleItem.getLocation() == null) {
            Toast.makeText(this, "Scheduled location not available for validation.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Get scheduled location
        Map<String, Double> scheduledLocation = scheduleItem.getLocation();
        double distance = calculateDistance(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                scheduledLocation.get("latitude"), scheduledLocation.get("longitude")
        );

        // Log: Distance calculation
        Toast.makeText(this, "Calculated distance to scheduled location: " + distance + " meters.", Toast.LENGTH_SHORT).show();
        System.out.println("Calculated distance to scheduled location: " + distance + " meters.");

        // Allow a maximum radius of 100 meters for attendance validation
        return distance <= 1000;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Earth's radius in kilometers
        final double EARTH_RADIUS = 6371.0;

        // Convert latitude and longitude to radians
        double latRad1 = Math.toRadians(lat1);
        double lonRad1 = Math.toRadians(lon1);
        double latRad2 = Math.toRadians(lat2);
        double lonRad2 = Math.toRadians(lon2);

        // Differences in coordinates
        double deltaLat = latRad2 - latRad1;
        double deltaLon = lonRad2 - lonRad1;

        // Haversine formula
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(latRad1) * Math.cos(latRad2) *
                        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);

        double centralAngle = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        // Distance in meters
        return EARTH_RADIUS * centralAngle * 1000;
    }


    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }




    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences("AttendancePrefs", MODE_PRIVATE);
        long startTime = sharedPreferences.getLong("startTime", -1);

        if (startTime != -1) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;

            if (elapsedTime >= courceDuration()) { // Check if the course duration has passed
                commitAttendance();
            } else {
                // Update the UI to show the elapsed time
                binding.timer.setText(String.format(Locale.getDefault(), "Time spent: %02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(elapsedTime),
                        TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60,
                        TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60));
            }
        }
    }


    @Override
    protected void onDestroy() {
        if (attendanceTimer != null) {
            attendanceTimer.cancel();
        }
        super.onDestroy();
    }
}