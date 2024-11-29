package com.princesaha.attendance;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.princesaha.attendance.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    // Declare the View Binding instance
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Register Button Click
        binding.buttonregister.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Handle Recognize Button Click
        binding.buttonrecognize.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RecognitionActivity.class);
            startActivity(intent);
        });

        // Handle Logout Button Click
        binding.logout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut(); // Log out the user
            Intent intent = new Intent(MainActivity.this, Welcome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
            startActivity(intent);
            finish(); // Ensure the current activity is closed
        });
        binding.take.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, DashBord.class);
            startActivity(intent);
        });



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clear binding reference to avoid memory leaks
    }
}