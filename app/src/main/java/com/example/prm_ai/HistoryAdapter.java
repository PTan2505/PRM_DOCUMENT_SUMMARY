package com.example.prm_ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private Context context;
    private List<ScanHistoryItem> historyList;

    public HistoryAdapter(Context context, List<ScanHistoryItem> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_item_layout, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ScanHistoryItem item = historyList.get(position);

        holder.summaryPreview.setText(item.getSummaryText());
        holder.timestamp.setText(item.getTimestamp());

        // Use Glide to load the image from the file path
        Glide.with(context)
                .load(new File(item.getImagePath()))
                .placeholder(R.mipmap.ic_launcher) // Optional placeholder
                .error(R.drawable.ic_launcher_background) // Optional error image
                .into(holder.thumbnail);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView summaryPreview;
        TextView timestamp;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.imageViewThumbnail);
            summaryPreview = itemView.findViewById(R.id.textViewSummaryPreview);
            timestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}
