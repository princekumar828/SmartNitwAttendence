package com.princesaha.attendance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.princesaha.attendance.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;  // Firebase Auth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize View Binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Handle Window Insets for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Login button click listener
        binding.loginbtn.setOnClickListener(v -> loginUser());

        // Redirect to SignUp activity if "Sign Up" is clicked
        binding.gotosignup.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String username = binding.signinloginet.getText().toString().trim();
        String password = binding.signinpasset.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            binding.signinloginet.setError("Username is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.signinpasset.setError("Password is required");
            return;
        }

        // Attempt to log in the user with Firebase Authentication
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful, navigate to the main activity
                        Intent intent = new Intent(Login.this, DashBord.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // If login fails, display a toast message
                        Toast.makeText(Login.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clear binding reference to avoid memory leaks
    }
}