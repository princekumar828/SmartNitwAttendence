package com.princesaha.attendance;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class Profile extends Fragment {
    private TextInputEditText editName, editEmail, editRollNumber;
    private Button editButton, saveButton;
    private ImageView profileImage;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public Profile() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
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
        editButton = view.findViewById(R.id.editButton);
        saveButton = view.findViewById(R.id.saveButton);

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

    private void fetchUserData() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String name = document.getString("fullName");
                            String email = document.getString("email");
                            String rollNo = document.getString("rollNo");
                            String imageUrl = document.getString("profileImageUrl"); // Assuming URL is stored

                            // Set pre-existing data in EditText fields
                            editName.setText(name);
                            editEmail.setText(email);
                            editRollNumber.setText(rollNo);

                            // Set the profile image using an image loading library (e.g., Picasso)
                            if (imageUrl != null) {
                              //  Picasso.get().load(imageUrl).into(profileImage);
                            }
                        }
                    } else {
                        // Handle error, e.g., show a toast or log an error message
                    }
                });
    }

    private void saveUserData() {
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
                    if (task.isSuccessful()) {
                        editName.setEnabled(false);
                        saveButton.setVisibility(View.GONE);
                        editButton.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "Name Updated Successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Name Not Updated Failed ", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}