package com.example.prm_ai;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private TextView textViewNoHistory;
    private HistoryAdapter historyAdapter;
    private List<ScanHistoryItem> historyList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        userId = getIntent().getIntExtra("USER_ID", -1);

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        textViewNoHistory = findViewById(R.id.textViewNoHistory);

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(this, historyList);
        recyclerViewHistory.setAdapter(historyAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list every time the activity is shown
        loadHistory();
    }

    private void loadHistory() {
        if (userId == -1) {
            textViewNoHistory.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.GONE);
            return;
        }

        historyList.clear(); // Clear old data before loading new
        Cursor cursor = dbHelper.getScanHistory(userId);

        if (cursor != null && cursor.moveToFirst()) {
            textViewNoHistory.setVisibility(View.GONE);
            recyclerViewHistory.setVisibility(View.VISIBLE);

            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HISTORY_ID));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_PATH));
                String originalText = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ORIGINAL_TEXT));
                String summaryText = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SUMMARY_TEXT));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));
                String quizJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_JSON));
                
                // Use Integer to handle possible null scores
                Integer quizScore = null;
                if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE))) {
                    quizScore = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_SCORE));
                }

                historyList.add(new ScanHistoryItem(id, imagePath, summaryText, originalText, timestamp, quizJson, quizScore));
            } while (cursor.moveToNext());

            cursor.close();
            historyAdapter.notifyDataSetChanged();
        } else {
            textViewNoHistory.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.GONE);
        }
    }
}
