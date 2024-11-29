package com.princesaha.attendance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.princesaha.attendance.databinding.ActivityDashBordBinding;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashBord extends AppCompatActivity {

    private ActivityDashBordBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize View Binding
        binding = ActivityDashBordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load default fragment on activity launch
        if (savedInstanceState == null) {
            switchFragment(new Home());
        }


        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();


        // Fetch and display user data
        fetchUserData();

        // Set up BottomNavigationView listener for fragment switching
        binding.bottomBar.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                switchFragment(new Home());
                return true;
            } else if (itemId == R.id.menu_chat) {
                switchFragment(new Chat());
                return true;
            } else if (itemId == R.id.menu_user) {
                switchFragment(new Profile());
                return true;
            } else {
                return false;
            }
        });
        binding.profileImageCard.setOnClickListener(v -> {
            Intent intent = new Intent(DashBord.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void fetchUserData() {
        String userId = auth.getCurrentUser().getUid(); // Get current user's Firebase auth ID

        // Reference to the current user's data in Firestore
        DocumentReference userRef = firestore.collection("users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Retrieve data
                    String name = document.getString("fullName");
                    String rollNo = document.getString("rollNo");

                    // Update UI
                    binding.uNameDasbsord.setText("Hi, " + name + "!\nWelcome to Nitw ");
                    binding.rollNoDashbord.setText(rollNo);
                } else {
                    Log.e("Firebase", "User data not found!");
                }
            } else {
                Log.e("Firebase", "Error fetching user data: " + task.getException());
            }
        });
    }

    /**
     * Helper method to replace the current fragment with the provided fragment.
     *
     * @param fragment The fragment to be displayed.
     */
    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,  // Enter animation
                        android.R.anim.fade_out // Exit animation
                )
                .replace(R.id.navHostFragment, fragment)
                .commit();
    }
}