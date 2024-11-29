package com.princesaha.attendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.princesaha.attendance.databinding.ActivitySignUpBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    private ActivitySignUpBinding binding; // View Binding instance
    private FirebaseAuth auth;            // Firebase Authentication instance
    private FirebaseFirestore firestore;  // Firebase Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize View Binding
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configure Edge-to-Edge layout
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Set up Sign-Up button click listener
        binding.signupbtn.setOnClickListener(v -> validateAndRegisterUser());
    }

    private void validateAndRegisterUser() {
        String fullName = binding.fnameet.getText().toString().trim();
        String email = binding.emailet.getText().toString().trim();
        String rollNo = binding.rollnoet.getText().toString().trim();
        String password = binding.passwordet.getText().toString().trim();

        // Input Validation
        if (fullName.isEmpty()) {
            showToast("Full Name is required");
            return;
        }
        if (email.isEmpty()) {
            showToast("Email is required");
            return;
        }
        if (rollNo.isEmpty()) {
            showToast("Roll Number is required");
            return;
        }
        if (password.isEmpty()) {
            showToast("Password is required");
            return;
        }
        if (password.length() < 6) {
            showToast("Password must be at least 6 characters long");
            return;
        }

        // Register user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    saveUserDetails(userId, fullName, rollNo, email);
                })
                .addOnFailureListener(e -> showToast("Registration failed: " + e.getMessage()));
    }

    private void saveUserDetails(String userId, String fullName, String rollNo, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("rollNo", rollNo);
        user.put("email", email);

        firestore.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    showToast("User registered successfully!");
                    startActivity(new Intent(SignUp.this, DashBord.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to save user details: " + e.getMessage()));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}