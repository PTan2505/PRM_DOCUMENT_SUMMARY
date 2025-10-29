package com.example.prm_ai;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryDetailActivity extends AppCompatActivity {

    private ImageView imageViewDetail;
    private TextView textViewSummaryDetail, textViewScoreDetail;
    private Button buttonStartQuizDetail, buttonReviewQuiz;

    private DatabaseHelper dbHelper;
    private long historyId;
    private String quizJsonCache;
    private String userAnswersJsonCache; // Cache the user answers JSON

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        historyId = getIntent().getLongExtra("HISTORY_ID", -1);
        if (historyId == -1) {
            Toast.makeText(this, "Error: History item not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        imageViewDetail = findViewById(R.id.imageViewDetail);
        textViewSummaryDetail = findViewById(R.id.textViewSummaryDetail);
        textViewScoreDetail = findViewById(R.id.textViewScoreDetail);
        buttonStartQuizDetail = findViewById(R.id.buttonStartQuizDetail);
        buttonReviewQuiz = findViewById(R.id.buttonReviewQuiz);

        buttonStartQuizDetail.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryDetailActivity.this, QuizActivity.class);
            intent.putExtra("HISTORY_ID", historyId);
            startActivity(intent);
        });

        buttonReviewQuiz.setOnClickListener(v -> {
            if (quizJsonCache != null && !quizJsonCache.isEmpty()) {
                try {
                    QuizResponse quizResponse = new Gson().fromJson(quizJsonCache, QuizResponse.class);
                    if (quizResponse != null && quizResponse.getQuestions() != null) {
                        Intent intent = new Intent(HistoryDetailActivity.this, StatisticsActivity.class);
                        intent.putExtra("QUESTIONS", (Serializable) quizResponse.getQuestions());

                        // Pass the saved user answers to the statistics activity
                        if (userAnswersJsonCache != null) {
                            Type type = new TypeToken<ArrayList<String>>() {}.getType();
                            ArrayList<String> userAnswers = new Gson().fromJson(userAnswersJsonCache, type);
                            intent.putStringArrayListExtra("USER_ANSWERS", userAnswers);
                        }
                        
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Could not parse quiz data.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No quiz data available to review.", Toast.LENGTH_SHORT).show();
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
            quizJsonCache = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_JSON));
            userAnswersJsonCache = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_USER_ANSWERS_JSON)); // Load the answers
            
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));
            setTitle("Details from " + timestamp);
            
            textViewSummaryDetail.setText(summary);

            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE))) {
                int score = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE));
                int totalQuestions = getTotalQuestionsFromJson(quizJsonCache);
                textViewScoreDetail.setText(score + "/" + totalQuestions);
            } else {
                textViewScoreDetail.setText("Chưa làm");
            }

            // Show the review button if there are saved answers
            if (userAnswersJsonCache != null && !userAnswersJsonCache.isEmpty()) {
                buttonReviewQuiz.setVisibility(View.VISIBLE);
            } else {
                buttonReviewQuiz.setVisibility(View.GONE);
            }

            Glide.with(this).load(new File(imagePath)).into(imageViewDetail);
            cursor.close();
        } else {
            Toast.makeText(this, "Could not load history details.", Toast.LENGTH_SHORT).show();
        }
    }

    private int getTotalQuestionsFromJson(String json) {
        if (json == null || json.isEmpty()) return 0;
        try {
            QuizResponse response = new Gson().fromJson(json, QuizResponse.class);
            if (response != null && response.getQuestions() != null) {
                 return response.getQuestions().size();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
