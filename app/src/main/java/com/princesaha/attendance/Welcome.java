package com.princesaha.attendance;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.princesaha.attendance.databinding.ActivityWelcomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Welcome extends AppCompatActivity {

    // Declare the View Binding instance
    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Check if the user is already logged in with FirebaseAuth
        if (isUserLoggedIn()) {
            // Redirect to MainActivity
            Intent intent = new Intent(Welcome.this, DashBord.class);
            startActivity(intent);
            finish(); // Prevent returning to the Welcome screen
            return;
        }

        // Initialize View Binding
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Window Insets for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.welcomeMain, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set onClickListeners for buttons
        binding.rlLoginButton.setOnClickListener(v -> {
            // Navigate to LoginActivity
            Intent intent = new Intent(Welcome.this, Login.class);
            startActivity(intent);
        });

        binding.rlSignupButton.setOnClickListener(v -> {
            // Navigate to SignUpActivity
            Intent intent = new Intent(Welcome.this, SignUp.class);
            startActivity(intent);
        });
    }

    // Check if the user is logged in with FirebaseAuth
    private boolean isUserLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null; // Returns true if a user is logged in
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clear binding reference to avoid memory leaks
    }
}