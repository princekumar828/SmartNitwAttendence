package com.princesaha.attendance;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Home extends Fragment {

    private RecyclerView scheduleRecyclerView;
    private ScheduleAdapter scheduleAdapter;
    private DatabaseReference db;
    private FirebaseFirestore fsdb;
    private TextView noticeTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduleRecyclerView = view.findViewById(R.id.recyview);
        db = FirebaseDatabase.getInstance().getReference(); // Initialize Realtime Database reference
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fsdb = FirebaseFirestore.getInstance();
        noticeTextView = view.findViewById(R.id.notice);

        // Retrieve the latest notice from Firebase Realtime Database
        DatabaseReference noticesRef = db.child("Notices");
        noticesRef.orderByChild("timestamp").limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot noticeSnapshot : snapshot.getChildren()) {
                        String noticeText = noticeSnapshot.child("text").getValue(String.class);
                        if (noticeText != null) {
                            noticeTextView.setText(noticeText);
                        } else {
                            noticeTextView.setText("No notices available ");
                        }
                    }
                } else {
                    noticeTextView.setText("No notices available");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                noticeTextView.setText("Error fetching notice");
            }
        });

        fetchScheduleForToday();

    }

    private void fetchScheduleForToday() {
        String currentDay = getCurrentDay();
        fsdb.collection("WeekdaySchedules").document(currentDay).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<ScheduleItem> scheduleList = new ArrayList<>();
                        for (String time : documentSnapshot.getData().keySet()) {
                            String courseCode = documentSnapshot.getString(time);
                            Log.d("princeSh", "Course Code: " + courseCode + ", Time: " + time);
                            fetchCourseDetails(courseCode, time, scheduleList);
                        }
                    }
                });
    }

    private void fetchCourseDetails(String courseCode, String time, List<ScheduleItem> scheduleList) {
        fsdb.collection("Courses").document(courseCode).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("ScheduleDebug", "Course details found for course code: " + courseCode);
                        String subjectName = documentSnapshot.getString("name");
                        String teacherName = documentSnapshot.getString("faculty");
                        String roomNo = documentSnapshot.getString("roomNo");
                        String subjectType = documentSnapshot.getString("type");
                        Long creditsLong = documentSnapshot.getLong("credits");
                        int credits = creditsLong != null ? creditsLong.intValue() : 0;
                        Boolean isAvailable = documentSnapshot.getBoolean("isAvl");
                        Map<String, Double> location = (Map<String, Double>) documentSnapshot.get("location");

                        Log.d("ScheduleDebug", "Subject Name: " + subjectName + ", Teacher Name: " + teacherName);
                        ScheduleItem scheduleItem = new ScheduleItem(time, subjectName, teacherName, subjectType, roomNo, credits, isAvailable, location, courseCode);
                        scheduleList.add(scheduleItem);

                        // After adding all items, set the adapter

                            Log.d("ScheduleDebug", "All schedule items added. Setting adapter.");
                            scheduleAdapter = new ScheduleAdapter(getContext(), scheduleList);
                            scheduleRecyclerView.setAdapter(scheduleAdapter);

                    } else {
                        Log.d("ScheduleDebug", "No document found for course code: " + courseCode);
                    }
                })
                .addOnFailureListener(e -> Log.e("ScheduleDebug", "Error fetching course details: ", e));
    }


    private String getCurrentDay() {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1];
    }
}