package com.princesaha.attendance;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Profile extends Fragment {
    private TextInputEditText editName, editEmail, editRollNumber;
    private TextView faceUpdateStatusText;
    private Button editButton, saveButton, uploadFaceButton;
    private ImageView profileImage;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Uri selectedImageUri;
    private int imageUpdateCount = 0;
    private static final int MAX_FACE_UPDATES = 3;
    private final OkHttpClient client = new OkHttpClient();
    private static final String FACE_RECOGNITION_API = ApiConfig.REGISTER_STUDENT_API;
    private static final String GET_STUDENT_IMAGE_API = ApiConfig.GET_STUDENT_IMAGE_API;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                        profileImage.setImageBitmap(bitmap);
                        faceUpdateStatusText.setText("Image selected. Click 'Register Face' to upload.");
                        // Make upload button visible when image is selected
                        uploadFaceButton.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    public Profile() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase for user data (not for image storage)
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Views
        editName = view.findViewById(R.id.editName);
        editEmail = view.findViewById(R.id.editEmail);
        editRollNumber = view.findViewById(R.id.editRollNumber);
        profileImage = view.findViewById(R.id.profileImage);
        faceUpdateStatusText = view.findViewById(R.id.faceUpdateStatusText);
        editButton = view.findViewById(R.id.editButton);
        saveButton = view.findViewById(R.id.saveButton);
        uploadFaceButton = view.findViewById(R.id.uploadFaceButton);

        // Initial state setup
        editName.setEnabled(false);
        editEmail.setEnabled(false);
        editRollNumber.setEnabled(false);
        saveButton.setVisibility(View.GONE);

        // Initially hide upload button until image is selected
        uploadFaceButton.setVisibility(View.GONE);

        // Set click listener for profile image
        profileImage.setOnClickListener(v -> {
            if (imageUpdateCount >= MAX_FACE_UPDATES) {
                Toast.makeText(getContext(), "You can only update your face image " + MAX_FACE_UPDATES + " times", Toast.LENGTH_SHORT).show();
                return;
            }
            openImagePicker();
        });

        // Set click listener for upload face button
        uploadFaceButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                registerFaceWithAPI();
            } else {
                Toast.makeText(getContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch and display user data
        fetchUserData();

        // Edit button logic
        editButton.setOnClickListener(v -> {
            editName.setEnabled(true);
            saveButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.GONE);
        });

        // Save button logic
        saveButton.setOnClickListener(v -> saveUserData());

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void fetchUserData() {
        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String name = document.getString("fullName");
                            String email = document.getString("email");
                            String rollNo = document.getString("rollNo");
                            Long imageUpdates = document.getLong("faceImageUpdates");

                            // Set pre-existing data in EditText fields
                            if (name != null) editName.setText(name);
                            if (email != null) editEmail.setText(email);
                            if (rollNo != null) editRollNumber.setText(rollNo);

                            // Update the image update count if it exists
                            if (imageUpdates != null) {
                                imageUpdateCount = imageUpdates.intValue();
                                updateFaceRegistrationStatus(imageUpdateCount);
                            }

                            // Load the profile image from the backend API
                            if (rollNo != null && !rollNo.isEmpty()) {
                                loadProfileImageFromApi(rollNo);
                            }
                        }
                    } else {
                        // Handle error
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadProfileImageFromApi(String rollNo) {
        if (getContext() == null || !isAdded()) return;

        // Add timestamp to force refresh - this helps prevent caching issues
        String imageUrl = GET_STUDENT_IMAGE_API + rollNo + "?t=" + System.currentTimeMillis();

        // Load the image using Glide with cache strategies to ensure refresh
        Glide.with(this)
                .load(imageUrl)
                .skipMemoryCache(true)  // Skip memory cache
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // Skip disk cache
                .placeholder(R.drawable.male)
                .error(R.drawable.male)
                .into(profileImage);
    }

    private void updateFaceRegistrationStatus(int count) {
        if (count >= MAX_FACE_UPDATES) {
            uploadFaceButton.setEnabled(false);
            uploadFaceButton.setText("Update Limit Reached");
            faceUpdateStatusText.setText("You've used all " + MAX_FACE_UPDATES + " face registration attempts");
        } else {
            int remainingUpdates = MAX_FACE_UPDATES - count;
            faceUpdateStatusText.setText("Face registrations remaining: " + remainingUpdates);
        }
    }

    private void saveUserData() {
        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String updatedName = editName.getText().toString().trim();

        if (TextUtils.isEmpty(updatedName)) {
            editName.setError("Name cannot be empty");
            return;
        }

        // Update name in Firestore
        db.collection("users").document(userId)
                .update("fullName", updatedName)
                .addOnCompleteListener(task -> {
                    if (getContext() == null || !isAdded()) return;

                    if (task.isSuccessful()) {
                        editName.setEnabled(false);
                        saveButton.setVisibility(View.GONE);
                        editButton.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "Name Updated Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Name Update Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerFaceWithAPI() {
        if (selectedImageUri == null) {
            return;
        }

        // Get the user's roll number - check if present first
        String rollNo = editRollNumber.getText().toString().trim();
        if (TextUtils.isEmpty(rollNo)) {
            Toast.makeText(getContext(), "Roll number is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        View view = getView();
        if (view == null || getContext() == null || !isAdded()) return;

        final Snackbar loadingSnackbar = Snackbar.make(view, "Registering face...", Snackbar.LENGTH_INDEFINITE);
        loadingSnackbar.show();

        try {
            // Convert URI to bitmap for API upload
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);

            // Convert bitmap to byte array for API request
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] imageData = baos.toByteArray();

            // Create multipart request body
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "face.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageData))
                    .addFormDataPart("student_id", rollNo)  // Using roll number as student_id for the API
                    .build();

            // Build the request
            Request request = new Request.Builder()
                    .url(FACE_RECOGNITION_API)
                    .post(requestBody)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() == null || !isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        // Dismiss the loading snackbar
                        loadingSnackbar.dismiss();
                        // Show error message
                        if (isAdded() && getView() != null) {
                            Snackbar.make(getView(), "Network error: " + e.getMessage(), Snackbar.LENGTH_LONG)
                                    .setAction("Retry", v -> registerFaceWithAPI())
                                    .show();
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (getActivity() == null || !isAdded()) return;

                    final String responseBody = response.body() != null ? response.body().string() : null;
                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        handleFaceRegistrationResponse(response, responseBody, loadingSnackbar);
                    });
                }
            });
        } catch (IOException e) {
            // Dismiss the loading snackbar in case of exception
            loadingSnackbar.dismiss();
            e.printStackTrace();
            if (isAdded()) {
                Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleFaceRegistrationResponse(Response response, String responseBody, Snackbar loadingSnackbar) {
        // Dismiss the loading indicator
        loadingSnackbar.dismiss();

        if (!isAdded() || getContext() == null) return;

        if (response.isSuccessful() && responseBody != null) {
            try {
                JSONObject jsonResponse = new JSONObject(responseBody);

                // Check for success message as per API docs
                if (jsonResponse.has("message") &&
                        jsonResponse.getString("message").equals("Student registered successfully")) {

                    // Face registration successful
                    // Update the face update count in Firestore
                    updateFaceUpdateCount();

                    String studentId = jsonResponse.optString("student_id", "");
                    String successMsg = "Face registered successfully" +
                            (!studentId.isEmpty() ? " for ID: " + studentId : "");

                    Snackbar.make(getView(), successMsg, Snackbar.LENGTH_LONG).show();

                    // Clear selected image
                    selectedImageUri = null;

                    // Hide upload button until a new image is selected
                    uploadFaceButton.setVisibility(View.GONE);

                    // Add a slight delay before refreshing the image to ensure server has processed it
                    // This helps when the server needs time to process the image before it's available via GET
                    new android.os.Handler().postDelayed(() -> {
                        // Refresh the image from API with force reload
                        String rollNo = editRollNumber.getText().toString().trim();
                        if (isAdded() && getContext() != null) {
                            // Clear any existing image first to ensure a refresh
                            Glide.with(requireContext()).clear(profileImage);
                            loadProfileImageFromApi(rollNo);
                        }
                    }, 500); // 500ms delay

                } else if (jsonResponse.has("error")) {
                    // Handle error based on API error response
                    String errorMessage = jsonResponse.getString("error");
                    handleSpecificError(errorMessage);
                } else {
                    // Unknown response format
                    Toast.makeText(getContext(), "Unknown response from server", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Error parsing API response: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle HTTP error codes
            String errorPrefix = "Registration failed: ";
            String errorMessage = errorPrefix + "Error " + response.code();

            try {
                if (responseBody != null) {
                    JSONObject errorJson = new JSONObject(responseBody);
                    if (errorJson.has("error")) {
                        errorMessage = errorPrefix + errorJson.getString("error");
                    }
                }
            } catch (JSONException e) {
                // Just use the default error message with status code
            }

            // Show error in a Snackbar with longer duration for better visibility
            if (getView() != null) {
                Snackbar.make(getView(), errorMessage, Snackbar.LENGTH_LONG)
                        .setAction("Dismiss", v -> {})
                        .show();
            } else {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    // Helper method to handle specific error types from the API
    private void handleSpecificError(String errorMessage) {
        View view = getView();
        if (view == null) return;

        // Create more user-friendly messages based on error type
        String displayMessage;

        switch (errorMessage) {
            case "No image uploaded":
                displayMessage = "Please select a valid face image";
                break;
            case "Invalid file type":
                displayMessage = "Please select a valid image format (JPG, PNG)";
                break;
            case "Invalid student ID":
                displayMessage = "Your roll number appears to be invalid";
                break;
            case "Internal server error":
                displayMessage = "Server error. Please try again later";
                break;
            default:
                // For quality errors and other specific messages
                if (errorMessage.contains("quality")) {
                    displayMessage = "Image quality issue: " + errorMessage;
                } else {
                    displayMessage = "Face registration failed: " + errorMessage;
                }
                break;
        }

        Snackbar.make(view, displayMessage, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> {
                    // Only offer retry for errors where it makes sense
                    if (!errorMessage.equals("Invalid student ID")) {
                        openImagePicker();
                    }
                })
                .show();
    }

    private void updateFaceUpdateCount() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("faceImageUpdates", imageUpdateCount + 1);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;

                    imageUpdateCount++;
                    updateFaceRegistrationStatus(imageUpdateCount);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    Toast.makeText(getContext(), "Failed to update face registration count", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Glide resources to avoid memory leaks
        if (getContext() != null && profileImage != null) {
            Glide.with(requireContext()).clear(profileImage);
        }
    }
}