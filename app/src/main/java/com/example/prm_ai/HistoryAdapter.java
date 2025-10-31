package com.example.prm_ai;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

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

        // Hiển thị điểm số
        if (item.getQuizScore() != null) {
            // Cần lấy tổng số câu hỏi từ JSON để hiển thị chính xác
            int totalQuestions = getTotalQuestionsFromJson(item.getQuizJson());
            holder.score.setText("Điểm: " + item.getQuizScore() + "/" + totalQuestions);
            holder.score.setVisibility(View.VISIBLE);
        } else {
            holder.score.setText("Điểm: Chưa làm");
            holder.score.setVisibility(View.VISIBLE);
        }

        Glide.with(context)
                .load(new File(item.getImagePath()))
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.thumbnail);

        // Xử lý sự kiện nhấn vào item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HistoryDetailActivity.class);
            intent.putExtra("HISTORY_ID", item.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }
    
    private int getTotalQuestionsFromJson(String json) {
        if (json == null || json.isEmpty()) return 0;
        try {
            QuizResponse response = new Gson().fromJson(json, QuizResponse.class);
            return response.getQuestions().size();
        } catch (Exception e) {
            return 0;
        }
    }


    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView summaryPreview;
        TextView timestamp;
        TextView score; // ✅

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.imageViewThumbnail);
            summaryPreview = itemView.findViewById(R.id.textViewSummaryPreview);
            timestamp = itemView.findViewById(R.id.textViewTimestamp);
            score = itemView.findViewById(R.id.textViewItemScore); // ✅
        }
    }
}
