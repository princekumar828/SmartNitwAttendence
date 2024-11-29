package com.princesaha.attendance;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Chat extends Fragment {
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private DatabaseReference messagesRef;
    private List<ChatMessage> messageList;
    private ChatMessageAdapter messageAdapter;
    private String name;

    public Chat() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance(); // Initialize Firestore
        messagesRef = FirebaseDatabase.getInstance().getReference("group_messages");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize UI components
        recyclerViewMessages = view.findViewById(R.id.recycler_view_messages);
        editTextMessage = view.findViewById(R.id.edit_text_message);
        buttonSend = view.findViewById(R.id.button_send);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(requireContext(), messageList);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewMessages.setAdapter(messageAdapter);

        recyclerViewMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && isUserAtBottom()) {
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }
        });

        // Send message listener
        buttonSend.setOnClickListener(v -> sendMessage());

        // Load messages
        loadMessages();
        fetchUserData(); // Ensure this is called after `firestore` is initialized

        return view;
    }

    private boolean isUserAtBottom() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewMessages.getLayoutManager();
        if (layoutManager != null) {
            int totalItemCount = layoutManager.getItemCount();
            int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
            return lastVisibleItem == totalItemCount - 1;
        }
        return false;
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to send messages", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new message
        String messageId = messagesRef.push().getKey();
        ChatMessage message = new ChatMessage(
                messageId,
                currentUser.getUid(),
                name != null ? name : "Anonymous",
                messageText,
                System.currentTimeMillis()
        );

        // Send message to Firebase
        if (messageId != null) {
            messagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        // Clear input after successful send
                        editTextMessage.setText("");
                        recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(),
                                "Failed to send message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadMessages() {
        messagesRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }

                        // Sort messages by timestamp
                        messageList.sort((m1, m2) ->
                                Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                        messageAdapter.notifyDataSetChanged();

                        // Scroll to the bottom if a new message is added
                        if (!messageList.isEmpty() && isUserAtBottom()) {
                            recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(),
                                "Failed to load messages: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return; // Do not proceed if the user is not authenticated
        }

        String userId = currentUser.getUid(); // Get current user's Firebase auth ID

        // Reference to the current user's data in Firestore
        DocumentReference userRef = firestore.collection("users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Retrieve data
                    name = document.getString("fullName");
                } else {
                    Log.e("Firebase", "User data not found!");
                }
            } else {
                Log.e("Firebase", "Error fetching user data: " + task.getException());
            }
        });
    }
}