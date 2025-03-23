package com.princesaha.attendance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.princesaha.attendance.databinding.ActivityAttendenceBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.os.Environment;
import java.io.File;

public class AttendenceA extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final String TAG = "AttendenceA";
    private static final int MAX_DISTANCE_METERS = 1000000000; // Realistic distance threshold for attendance
    private static final int IMAGE_COMPRESSION_QUALITY = 95; // Optimal compression for facial recognition
    private static final String API_ENDPOINT = ApiConfig.FACE_RECOGNITION_API; // Replace with your production API

    private ActivityAttendenceBinding binding;
    private Uri photoUri;
    private ScheduleItem scheduleItem;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private Location currentLocation;

    private List<AttendanceRecord> attendanceList = new ArrayList<>();
    private AttendanceAdapter adapter;
    private OkHttpClient client = new OkHttpClient();
    private String userRollNo;
    private boolean isProcessingAttendance = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        // Initialize View Binding
        binding = ActivityAttendenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get ScheduleItem from Intent
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("scheduleItem")) {
            Toast.makeText(this, "Error: Schedule information is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scheduleItem = (ScheduleItem) intent.getSerializableExtra("scheduleItem");
        if (scheduleItem == null) {
            Toast.makeText(this, "Error: Invalid schedule data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Populate the UI with the schedule item details
        displayScheduleDetails();

        // Hide the timer view as we no longer need it
        binding.timer.setVisibility(View.GONE);

        // Check location permissions
        if (!checkLocationPermission()) {
            requestLocationPermission();
        }

        // Get user's roll number from Firestore
        getUserRollNumber();

        binding.attenBtn.setOnClickListener(v -> {
            if (!isProcessingAttendance) {
                isProcessingAttendance = true;
                checkExistingAttendance();
            } else {
                Toast.makeText(this, "Please wait, processing your attendance...", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up RecyclerView for attendance records
        binding.reportAttendance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(attendanceList);
        binding.reportAttendance.setAdapter(adapter);
        fetchAttendanceRecords();
    }

    private void navigateToLogin() {
        // Navigate to login screen (you need to implement this method based on your app's structure)
        // Intent loginIntent = new Intent(this, LoginActivity.class);
        // startActivity(loginIntent);
        finish();
    }

    private void displayScheduleDetails() {
        // Safely populate the UI with the schedule item details
        binding.time.setText("Time: " + (scheduleItem.getTime() != null ? scheduleItem.getTime() : "Not available"));
        binding.subjectName.setText("Subject: " + (scheduleItem.getSubjectName() != null ? scheduleItem.getSubjectName() : "Not available"));
        binding.teacherName.setText("Teacher: " + (scheduleItem.getTeacherName() != null ? scheduleItem.getTeacherName() : "Not available"));
        binding.roomNo.setText("Room No: " + (scheduleItem.getRoomNo() != null ? scheduleItem.getRoomNo() : "Not available"));
        binding.subjectType.setText("Type: " + (scheduleItem.getSubjectType() != null ? scheduleItem.getSubjectType() : "Not available"));
        binding.credits.setText("Credits: " + scheduleItem.getCredits());
        binding.availability.setText("Available: " + (scheduleItem.isAvailable() ? "Yes" : "No"));
        binding.courseCode.setText("Course Code: " + (scheduleItem.getCourseCode() != null ? scheduleItem.getCourseCode() : "Not available"));

        // Display location details if available
        if (scheduleItem.getLocation() != null) {
            Map<String, Double> location = scheduleItem.getLocation();
            if (location.get("latitude") != null && location.get("longitude") != null) {
                binding.location.setText(String.format(Locale.getDefault(),
                        "Location: Lat %.6f, Long %.6f",
                        location.get("latitude"),
                        location.get("longitude")));
            } else {
                binding.location.setText("Location: Coordinates incomplete");
            }
        } else {
            binding.location.setText("Location: Not available");
        }
    }

    private void getUserRollNumber() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        String userId = currentUser.getUid();
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRollNo = documentSnapshot.getString("rollNo");
                        Log.d(TAG, "User roll number retrieved: " + userRollNo);
                    } else {
                        Log.e(TAG, "User document does not exist");
                        Toast.makeText(this, "Unable to retrieve user information", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user document", e);
                    Toast.makeText(this, "Error retrieving user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchAttendanceRecords() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || scheduleItem == null || scheduleItem.getCourseCode() == null) {
            Log.e(TAG, "Cannot fetch attendance records: User not logged in or invalid course code");
            return;
        }

        String studentId = currentUser.getUid();
        String courseCode = scheduleItem.getCourseCode();

        attendanceList.clear(); // Clear before loading new data

        firestore.collection("Attendance")
                .document(courseCode)
                .collection("Students")
                .document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            List<Map<String, Object>> records = (List<Map<String, Object>>) document.get("attendanceRecords");
                            if (records != null) {
                                for (Map<String, Object> record : records) {
                                    GeoPoint startLocation = (GeoPoint) record.get("startLocation");
                                    String date = (String) record.get("date");
                                    String startTime = (String) record.get("startTime");
                                    String status = (String) record.get("status");

                                    AttendanceRecord tempRecord = new AttendanceRecord(
                                            startLocation, date, startTime, status);
                                    attendanceList.add(tempRecord);
                                }
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.d(TAG, "No attendance records found");
                        }
                    } else {
                        Log.e(TAG, "Failed to get attendance records", task.getException());
                    }
                });
    }

    private void verifyLocation() {
        Toast.makeText(this, "Verifying your location...", Toast.LENGTH_SHORT).show();

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            isProcessingAttendance = false;
            return;
        }

        // Get the current location
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // Save current location for later use
                        currentLocation = location;

                        if (isLocationValid(location)) {
                            // Log: Valid location obtained
                            Log.d(TAG, "Valid location obtained. Latitude: " +
                                    location.getLatitude() + ", Longitude: " + location.getLongitude());
                            Toast.makeText(this, "Location verified! Taking selfie for recognition...",
                                    Toast.LENGTH_SHORT).show();

                            // Proceed to take a selfie for facial recognition
                            takeSelfie();
                        } else {
                            isProcessingAttendance = false;
                            Toast.makeText(this, "You are not in the class location. Attendance rejected.",
                                    Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Invalid location. Current location does not match the scheduled location.");
                        }
                    } else {
                        isProcessingAttendance = false;
                        Toast.makeText(this, "Couldn't get your location. Please try again.",
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Location is null");
                    }
                }).addOnFailureListener(e -> {
                    isProcessingAttendance = false;
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Failed to retrieve location. Error: " + e.getMessage());
                }).addOnCompleteListener(task -> {
                    // Cleanup token
                    cancellationTokenSource.cancel();
                });
    }

    private void takeSelfie() {
        // Check camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
    
        try {
            // Create file for high-resolution photo
            File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "selfie_" + System.currentTimeMillis() + ".jpg");
            
            // Generate content URI using FileProvider
            photoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider", photoFile);
    
            // Launch camera with the URI
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            
            // Grant permissions to the camera app
            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            } else {
                isProcessingAttendance = false;
                Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating camera intent: " + e.getMessage(), e);
            isProcessingAttendance = false;
            Toast.makeText(this, "Failed to launch camera", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == CAMERA_REQUEST_CODE) {
        if (resultCode == RESULT_OK) {
            try {
                // Load the full-resolution image from URI
                Bitmap fullResPhoto = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                
                // Optional: Scale down if image is extremely large while maintaining aspect ratio
                int maxDimension = 1024; // Good balance for face recognition
                if (fullResPhoto.getWidth() > maxDimension || fullResPhoto.getHeight() > maxDimension) {
                    float scale = Math.min(
                        (float) maxDimension / fullResPhoto.getWidth(),
                        (float) maxDimension / fullResPhoto.getHeight());
                    
                    int newWidth = Math.round(scale * fullResPhoto.getWidth());
                    int newHeight = Math.round(scale * fullResPhoto.getHeight());
                    
                    fullResPhoto = Bitmap.createScaledBitmap(fullResPhoto, newWidth, newHeight, true);
                }
                
                Log.d(TAG, "Using high-resolution image: " + fullResPhoto.getWidth() + "x" + fullResPhoto.getHeight());
                sendSelfieForRecognition(fullResPhoto);
                
            } catch (Exception e) {
                isProcessingAttendance = false;
                Log.e(TAG, "Error processing high-res image: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to process photo", Toast.LENGTH_SHORT).show();
            }
        } else {
            isProcessingAttendance = false;
            Toast.makeText(this, "Selfie capture cancelled", Toast.LENGTH_SHORT).show();
        }
    }
}

    private void sendSelfieForRecognition(Bitmap photo) {
        Toast.makeText(this, "Processing your image...", Toast.LENGTH_SHORT).show();

        ByteArrayOutputStream stream = null;
        try {
            // Convert bitmap to byte array with optimized compression
            stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, stream);
            byte[] byteArray = stream.toByteArray();

            // Create request body
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "selfie.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), byteArray))
                    .build();

            // Create request - Use the constant for API endpoint
            Request request = new Request.Builder()
                    .url(API_ENDPOINT)
                    .post(requestBody)
                    .build();

            // Execute the request asynchronously with timeout handling
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        isProcessingAttendance = false;
                        Log.e(TAG, "Failed to send request: " + e.getMessage(), e);
                        Toast.makeText(AttendenceA.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        try {
                            if (response.code() == 400) {
                                JSONObject errorJson = new JSONObject(responseBody);
                                // Check for both error and message fields
                                String errorMessage = errorJson.has("error") ?
                                        errorJson.getString("error") :
                                        errorJson.optString("message", "Unknown error");

                                Toast.makeText(AttendenceA.this, errorMessage, Toast.LENGTH_SHORT).show();
                                isProcessingAttendance = false;
                            } else if (response.isSuccessful() && !responseBody.isEmpty()) {
                                JSONObject json = new JSONObject(responseBody);
                                String message = json.optString("message", "");
                                double similarity = json.optDouble("similarity", 0.0);
                                String recognizedStudentId = json.optString("student_id", "");

                                Log.d(TAG, "Response: " + message +
                                        ", Similarity: " + similarity +
                                        ", Student ID: " + recognizedStudentId);

                                // Check if the recognized student ID matches the user's roll number
                                if (userRollNo != null && userRollNo.equals(recognizedStudentId)) {
                                    Toast.makeText(AttendenceA.this,
                                            "Face recognized successfully! Marking attendance...",
                                            Toast.LENGTH_SHORT).show();
                                    markAttendance();
                                } else {
                                    isProcessingAttendance = false;
                                    Toast.makeText(AttendenceA.this,
                                            "Student ID mismatch. Attendance rejected.",
                                            Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Student ID mismatch. User: " +
                                            userRollNo + ", Recognized: " + recognizedStudentId);
                                }
                            } else {
                                isProcessingAttendance = false;
                                Toast.makeText(AttendenceA.this,
                                        "Server error. Please try again later.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            isProcessingAttendance = false;
                            Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                            Toast.makeText(AttendenceA.this,
                                    "Error processing response. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            isProcessingAttendance = false;
            Log.e(TAG, "Error processing image: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        } finally {
            // Close the output stream to prevent resource leaks
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
    }

    private void markAttendance() {
        if (currentLocation == null) {
            isProcessingAttendance = false;
            Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || scheduleItem == null || scheduleItem.getCourseCode() == null) {
            isProcessingAttendance = false;
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create an attendance record
        Map<String, Object> attendanceRecord = new HashMap<>();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        attendanceRecord.put("date", currentDate);
        attendanceRecord.put("startTime", currentTime);
        attendanceRecord.put("startLocation", new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
        attendanceRecord.put("status", "Present");

        // Add additional info with proper separator
        attendanceRecord.put("verificationMethod", "Facial Recognition");
        attendanceRecord.put("verifiedAt", currentDate + " " + currentTime);

        // Get subject ID and student ID
        String subjectId = scheduleItem.getCourseCode();
        String studentId = currentUser.getUid();

        // Store the attendance record in Firestore
        firestore.collection("Attendance")
                .document(subjectId)
                .collection("Students")
                .document(studentId)
                .set(
                        new HashMap<String, Object>() {{
                            put("attendanceRecords", FieldValue.arrayUnion(attendanceRecord));
                        }},
                        SetOptions.merge()
                )
                .addOnSuccessListener(aVoid -> {
                    isProcessingAttendance = false;
                    Toast.makeText(this, "Attendance marked successfully!", Toast.LENGTH_SHORT).show();
                    // Refresh the attendance records
                    attendanceList.clear();
                    fetchAttendanceRecords();
                })
                .addOnFailureListener(e -> {
                    isProcessingAttendance = false;
                    Toast.makeText(this, "Failed to mark attendance", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error marking attendance", e);
                });
    }

    private void checkExistingAttendance() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || scheduleItem == null || scheduleItem.getCourseCode() == null ||
                scheduleItem.getTime() == null) {
            isProcessingAttendance = false;
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = currentUser.getUid();
        String subjectId = scheduleItem.getCourseCode();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String scheduleTime = scheduleItem.getTime(); // This is the scheduled class time

        // Parse the scheduled class time to extract the hour
        int scheduleHour = -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date scheduledTime = sdf.parse(scheduleTime);
            if (scheduledTime != null) {
                Calendar scheduleCal = Calendar.getInstance();
                scheduleCal.setTime(scheduledTime);
                scheduleHour = scheduleCal.get(Calendar.HOUR_OF_DAY);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing scheduled time: " + e.getMessage());
        }

        // Get the current hour
        Calendar now = Calendar.getInstance();
        final int currentHour = now.get(Calendar.HOUR_OF_DAY);

        // If we couldn't parse the schedule time, use current hour as fallback
        final int targetHour = (scheduleHour != -1) ? scheduleHour : currentHour;

        firestore.collection("Attendance")
                .document(subjectId)
                .collection("Students")
                .document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            List<Map<String, Object>> records = (List<Map<String, Object>>) document.get("attendanceRecords");
                            if (records != null) {
                                boolean alreadyMarkedForThisClass = false;

                                for (Map<String, Object> record : records) {
                                    String recordDate = (String) record.get("date");
                                    String recordStartTime = (String) record.get("startTime");

                                    if (recordDate != null && recordDate.equals(currentDate)) {
                                        // Extract hour from the recorded startTime
                                        try {
                                            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                            Date recordedTime = sdf.parse(recordStartTime);
                                            if (recordedTime != null) {
                                                Calendar recordedCal = Calendar.getInstance();
                                                recordedCal.setTime(recordedTime);
                                                int recordedHour = recordedCal.get(Calendar.HOUR_OF_DAY);

                                                // Check if attendance was already marked for this class
                                                // Allow a 1-hour window around the targeted hour
                                                if (Math.abs(recordedHour - targetHour) <= 1) {
                                                    alreadyMarkedForThisClass = true;
                                                    break;
                                                }
                                            }
                                        } catch (ParseException e) {
                                            Log.e(TAG, "Error parsing recorded time: " + e.getMessage());
                                        }
                                    }
                                }

                                if (alreadyMarkedForThisClass) {
                                    isProcessingAttendance = false;
                                    Toast.makeText(AttendenceA.this,
                                            "You've already marked attendance for this class!",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    // Proceed with location verification and attendance marking
                                    verifyLocation();
                                }
                            } else {
                                // No records yet, can proceed
                                verifyLocation();
                            }
                        } else {
                            // No document exists yet, can proceed
                            verifyLocation();
                        }
                    } else {
                        isProcessingAttendance = false;
                        Log.e(TAG, "Failed to get attendance document", task.getException());
                        Toast.makeText(AttendenceA.this,
                                "Error checking attendance. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isLocationValid(Location currentLocation) {
        if (scheduleItem == null || scheduleItem.getLocation() == null) {
            Log.d(TAG, "Scheduled location not available for validation");
            // For demo purposes, returning true if no location is specified
            return true;
        }

        // Get scheduled location
        Map<String, Double> scheduledLocation = scheduleItem.getLocation();
        Double scheduledLat = scheduledLocation.get("latitude");
        Double scheduledLong = scheduledLocation.get("longitude");

        if (scheduledLat == null || scheduledLong == null) {
            Log.d(TAG, "Incomplete location coordinates in schedule");
            // For demo purposes, returning true if coordinates are incomplete
            return true;
        }

        double distance = calculateDistance(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                scheduledLat, scheduledLong
        );

        // Log: Distance calculation
        Log.d(TAG, "Calculated distance to scheduled location: " + distance + " meters.");

        // Show distance to user
        runOnUiThread(() -> {
            Toast.makeText(this, "Distance to class: " + String.format(Locale.getDefault(), "%.1f", distance) + " meters",
                    Toast.LENGTH_SHORT).show();
        });

        // Check if within the allowed radius
        return distance <= MAX_DISTANCE_METERS;
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
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, can proceed when user tries again
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot mark attendance.", Toast.LENGTH_SHORT).show();
                isProcessingAttendance = false;
                finish();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, proceed with taking selfie
                takeSelfie();
            } else {
                Toast.makeText(this, "Camera permission denied. Cannot verify identity.", Toast.LENGTH_SHORT).show();
                isProcessingAttendance = false;
            }
        }
    }
}