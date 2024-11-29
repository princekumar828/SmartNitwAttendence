package com.princesaha.attendance;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
    private final List<ScheduleItem> scheduleList;
    private final Context context;

    public ScheduleAdapter(Context context, List<ScheduleItem> scheduleList) {
        this.context = context;
        this.scheduleList = scheduleList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subject_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItem item = scheduleList.get(position);
        Log.d("ScheduleDebug", "Binding item at position: " + item.toString());
        holder.time.setText(item.getTime());
        holder.subject.setText(item.getSubjectName());
        holder.teacherName.setText(item.getTeacherName());
        holder.roomNo.setText(item.getRoomNo() != null ? item.getRoomNo() : "No room no.");
        holder.date.setText(new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date()));
        if (item.isAvailable()) {
            holder.isAvl.setImageResource(R.drawable.check);
        } else {
            holder.isAvl.setImageResource(R.drawable.power_on);
        }
        holder.btn.setOnClickListener(v -> {
            Intent intent = new Intent(context, AttendenceA.class);
            intent.putExtra("scheduleItem", item);  // Pass the ScheduleItem object
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView time, subject, teacherName, roomNo, date;
        Button btn;
        ImageView isAvl;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.time);
            subject = itemView.findViewById(R.id.subject);
            teacherName = itemView.findViewById(R.id.teachername);
            roomNo = itemView.findViewById(R.id.roomNo);
            date = itemView.findViewById(R.id.date);
            btn=itemView.findViewById(R.id.markAttendence);
            isAvl=itemView.findViewById(R.id.isAvlImg);
        }
    }
}