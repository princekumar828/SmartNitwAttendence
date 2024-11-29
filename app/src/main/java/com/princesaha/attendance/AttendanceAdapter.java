package com.princesaha.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
    private List<AttendanceRecord> attendanceRecords;

    public AttendanceAdapter(List<AttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.attendance_item, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        AttendanceRecord record = attendanceRecords.get(position);

        // Set text for date, start time, end time, duration, and status with null checks
        holder.dateTextView.setText("Date: " + (record.getDate() != null ? record.getDate() : "N/A"));
        holder.startTimeTextView.setText("Start Time: " + (record.getStartTime() != null ? record.getStartTime() : "N/A"));
        holder.endTimeTextView.setText("End Time: " + (record.getEndTime() != null ? record.getEndTime() : "N/A"));
        holder.durationTextView.setText("Duration: " + record.getDuration()  + " mins" );
        holder.statusTextView.setText("Status: " + (record.getStatus() != null ? record.getStatus() : "N/A"));
        holder.statusTextView.setTextColor(record.getStatus()=="Present"?holder.itemView.getContext().getColor(R.color.green):holder.itemView.getContext().getColor(R.color.red));

// Check if startLocation and endLocation are null before accessing latitude and longitude
        if (record.getStartLocation() != null) {
            holder.startLocationTextView.setText("Start Location: (" +
                    record.getStartLocation().getLatitude() + ", " +
                    record.getStartLocation().getLongitude() + ")");
        } else {
            holder.startLocationTextView.setText("Start Location: Not available");
        }

        if (record.getEndLocation() != null) {
            holder.endLocationTextView.setText("End Location: (" +
                    record.getEndLocation().getLatitude() + ", " +
                    record.getEndLocation().getLongitude() + ")");
        } else {
            holder.endLocationTextView.setText("End Location: Not available");
        }
    }

    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }

    public static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView, startTimeTextView, endTimeTextView, durationTextView, statusTextView;
        TextView startLocationTextView, endLocationTextView;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            startTimeTextView = itemView.findViewById(R.id.startTimeTextView);
            endTimeTextView = itemView.findViewById(R.id.endTimeTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            startLocationTextView = itemView.findViewById(R.id.startLocationTextView);
            endLocationTextView = itemView.findViewById(R.id.endLocationTextView);
        }
    }
}