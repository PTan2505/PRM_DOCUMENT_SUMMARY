package com.example.prm_ai;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryDetailActivity extends BaseActivity {

    private ImageView imageViewDetail;
    private TextView textViewSummaryDetail, textViewScoreDetail, textViewTranslatedSummaryTitle, textViewTranslatedSummaryDetail;
    private Button buttonStartQuizDetail, buttonReviewQuiz;

    private DatabaseHelper dbHelper;
    private long historyId;
    private String quizJsonCache;
    private String userAnswersJsonCache;
    private List<QuizQuestion> questionsCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        historyId = getIntent().getLongExtra("HISTORY_ID", -1);
        if (historyId == -1) {
            Toast.makeText(this, "Error: History ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        imageViewDetail = findViewById(R.id.imageViewDetail);
        textViewSummaryDetail = findViewById(R.id.textViewSummaryDetail);
        textViewScoreDetail = findViewById(R.id.textViewScoreDetail);
        buttonStartQuizDetail = findViewById(R.id.buttonStartQuizDetail);
        buttonReviewQuiz = findViewById(R.id.buttonReviewQuiz);
        textViewTranslatedSummaryTitle = findViewById(R.id.textViewTranslatedSummaryTitle);
        textViewTranslatedSummaryDetail = findViewById(R.id.textViewTranslatedSummaryDetail);

        buttonStartQuizDetail.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryDetailActivity.this, QuizActivity.class);
            intent.putExtra("HISTORY_ID", historyId);
            startActivity(intent);
        });

        buttonReviewQuiz.setOnClickListener(v -> {
            if (questionsCache != null && userAnswersJsonCache != null) {
                Intent intent = new Intent(HistoryDetailActivity.this, StatisticsActivity.class);
                intent.putExtra("QUESTIONS", (Serializable) questionsCache);
                
                Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                ArrayList<String> userAnswers = new Gson().fromJson(userAnswersJsonCache, listType);

                intent.putStringArrayListExtra("USER_ANSWERS", userAnswers);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryDetails();
    }

    private void loadHistoryDetails() {
        Cursor cursor = dbHelper.getHistoryItem(historyId);
        if (cursor != null && cursor.moveToFirst()) {
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_PATH));
            String summary = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SUMMARY_TEXT));
            String translatedSummary = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSLATED_SUMMARY));
            quizJsonCache = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_JSON));
            userAnswersJsonCache = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_USER_ANSWERS_JSON));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));

            setTitle(getString(R.string.history_title) + " - " + timestamp);

            textViewSummaryDetail.setText(summary);

            if (!TextUtils.isEmpty(translatedSummary)) {
                textViewTranslatedSummaryTitle.setVisibility(View.VISIBLE);
                textViewTranslatedSummaryDetail.setVisibility(View.VISIBLE);
                textViewTranslatedSummaryDetail.setText(translatedSummary);
            } else {
                textViewTranslatedSummaryTitle.setVisibility(View.GONE);
                textViewTranslatedSummaryDetail.setVisibility(View.GONE);
            }

            questionsCache = parseQuestions(quizJsonCache);

            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE))) {
                int score = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE));
                int totalQuestions = questionsCache != null ? questionsCache.size() : 0;
                textViewScoreDetail.setText(score + "/" + totalQuestions);
            } else {
                textViewScoreDetail.setText(getString(R.string.history_detail_not_taken));
            }

            buttonReviewQuiz.setVisibility(userAnswersJsonCache != null ? View.VISIBLE : View.GONE);

            Glide.with(this).load(new File(imagePath)).into(imageViewDetail);
            cursor.close();
        }
    }

    // ✅ Sửa lại logic parse JSON để đọc đúng cấu trúc
    private List<QuizQuestion> parseQuestions(String json) {
        if (json == null) return new ArrayList<>();
        try {
            // Sử dụng một lớp nội bộ tương tự như trong QuizActivity để Gson có thể hiểu
            QuizActivity.QuizResponse response = new Gson().fromJson(json, QuizActivity.QuizResponse.class);
            if (response != null && response.getQuestions() != null) {
                return response.getQuestions();
            }
        } catch (Exception e) {
            // Log lỗi hoặc xử lý nếu cần
        }
        return new ArrayList<>();
    }
}
