package com.example.prm_ai;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.io.File;

public class HistoryDetailActivity extends AppCompatActivity {

    private ImageView imageViewDetail;
    private TextView textViewSummaryDetail, textViewScoreDetail;
    private Button buttonStartQuizDetail;

    private DatabaseHelper dbHelper;
    private long historyId;

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

        buttonStartQuizDetail.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryDetailActivity.this, QuizActivity.class);
            intent.putExtra("HISTORY_ID", historyId);
            startActivity(intent);
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
            String quizJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_JSON));
            
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));
            setTitle("Details from " + timestamp);
            
            textViewSummaryDetail.setText(summary);

            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE))) {
                int score = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE));
                int totalQuestions = getTotalQuestionsFromJson(quizJson);
                textViewScoreDetail.setText(score + "/" + totalQuestions);
            } else {
                textViewScoreDetail.setText("Not taken yet");
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
