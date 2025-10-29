package com.example.prm_ai;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewStatistics;
    private StatisticsAdapter adapter;
    private List<QuizResultItem> resultItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Toolbar toolbar = findViewById(R.id.toolbarStatistics);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerViewStatistics = findViewById(R.id.recyclerViewStatistics);
        recyclerViewStatistics.setLayoutManager(new LinearLayoutManager(this));

        List<QuizQuestion> questions = (List<QuizQuestion>) getIntent().getSerializableExtra("QUESTIONS");
        // User answers are optional now
        List<String> userAnswers = getIntent().getStringArrayListExtra("USER_ANSWERS");

        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                String userAnswer = (userAnswers != null && userAnswers.size() > i) ? userAnswers.get(i) : null;
                resultItems.add(new QuizResultItem(questions.get(i), userAnswer));
            }
        }

        adapter = new StatisticsAdapter(this, resultItems);
        recyclerViewStatistics.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); 
        return true;
    }
}
