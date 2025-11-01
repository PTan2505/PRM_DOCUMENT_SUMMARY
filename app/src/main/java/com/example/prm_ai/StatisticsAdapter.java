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

        holder.questionText.setText("Câu hỏi: " + question.getQuestion());
        holder.correctAnswerText.setText("Câu trả lời đúng: " + question.getAnswer());

        // ✅ Chế độ xem lại hoặc thống kê
        if (userAnswer != null && !userAnswer.isEmpty()) {
            holder.userAnswerText.setVisibility(View.VISIBLE);
            holder.userAnswerText.setText("Câu trả lời của bạn: " + userAnswer);

            // ✅ FIX: So sánh đúng với method mới
            if (isAnswerCorrect(userAnswer, question.getAnswer())) {
                holder.userAnswerText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                holder.userAnswerText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            }
        } else {
            // Chế độ chỉ xem lại câu hỏi và đáp án đúng
            holder.userAnswerText.setVisibility(View.GONE);
        }
    }

    /**
     * ✅ So sánh đáp án chính xác
     * Hỗ trợ cả format "A. Paris" và "A" hoặc "Paris"
     */
    private boolean isAnswerCorrect(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }

        // Loại bỏ khoảng trắng thừa
        String user = userAnswer.trim();
        String correct = correctAnswer.trim();

        // So sánh trực tiếp
        if (user.equals(correct)) {
            return true;
        }

        // ✅ CASE 1: userAnswer = "B. Trái Đất", correctAnswer = "B"
        // Extract chữ cái đầu tiên từ userAnswer
        if (user.length() >= 1) {
            String firstChar = user.substring(0, 1);

            // Nếu correctAnswer là 1 chữ cái (A, B, C, D)
            if (correct.length() == 1 && firstChar.equals(correct)) {
                // Kiểm tra format "X." hoặc "X. "
                if (user.length() == 1 || (user.length() > 1 && (user.charAt(1) == '.' || user.charAt(1) == ' '))) {
                    return true;
                }
            }
        }

        // ✅ CASE 2: correctAnswer có thể là full text
        // Nếu user có format "A. Text", so sánh phần "Text" với correctAnswer
        if (user.contains(". ")) {
            String textPart = user.substring(user.indexOf(". ") + 2).trim();
            if (textPart.equals(correct)) {
                return true;
            }
        }

        return false;
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
