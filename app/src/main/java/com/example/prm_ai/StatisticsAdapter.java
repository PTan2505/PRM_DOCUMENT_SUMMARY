package com.example.prm_ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.ViewHolder> {

    private Context context;
    private List<QuizResultItem> resultItems;

    public StatisticsAdapter(Context context, List<QuizResultItem> resultItems) {
        this.context = context;
        this.resultItems = resultItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.statistics_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizResultItem item = resultItems.get(position);
        QuizQuestion question = item.getQuestion();
        String userAnswer = item.getUserAnswer();

        holder.questionText.setText("Question: " + question.getQuestion());
        holder.correctAnswerText.setText("Correct answer: " + question.getAnswer());

        // ✅ Chế độ xem lại hoặc thống kê
        if (userAnswer != null && !userAnswer.isEmpty()) {
            holder.userAnswerText.setVisibility(View.VISIBLE);
            holder.userAnswerText.setText("Your answer: " + userAnswer);

            if (userAnswer.equals(question.getAnswer())) {
                holder.userAnswerText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                holder.userAnswerText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            }
        } else {
            // Chế độ chỉ xem lại câu hỏi và đáp án đúng
            holder.userAnswerText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return resultItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView questionText, userAnswerText, correctAnswerText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.textViewStatQuestion);
            userAnswerText = itemView.findViewById(R.id.textViewStatUserAnswer);
            correctAnswerText = itemView.findViewById(R.id.textViewStatCorrectAnswer);
        }
    }
}
