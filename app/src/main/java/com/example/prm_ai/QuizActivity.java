package com.example.prm_ai;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.prm_ai.network.ApiClient;
import com.example.prm_ai.network.GeminiApiRequest;
import com.example.prm_ai.network.GeminiApiResponse;
import com.example.prm_ai.network.GeminiApiService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizActivity extends AppCompatActivity {

    private ProgressBar progressBarQuiz;
    private LinearLayout quizContainer, scoreContainer;
    private TextView textViewQuestionNumber, textViewQuestion, textViewScore;
    private RadioGroup radioGroupOptions;
    private Button buttonSubmitQuiz, buttonFinishQuiz;

    private GeminiApiService apiService;
    private DatabaseHelper dbHelper;
    private long historyId;
    private List<QuizQuestion> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Khởi tạo Views
        progressBarQuiz = findViewById(R.id.progressBarQuiz);
        quizContainer = findViewById(R.id.quizContainer);
        scoreContainer = findViewById(R.id.scoreContainer);
        textViewQuestionNumber = findViewById(R.id.textViewQuestionNumber);
        textViewQuestion = findViewById(R.id.textViewQuestion);
        textViewScore = findViewById(R.id.textViewScore);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        buttonSubmitQuiz = findViewById(R.id.buttonSubmitQuiz);
        buttonFinishQuiz = findViewById(R.id.buttonFinishQuiz);

        apiService = ApiClient.getClient().create(GeminiApiService.class);
        dbHelper = new DatabaseHelper(this);

        historyId = getIntent().getLongExtra("HISTORY_ID", -1);

        if (historyId != -1) {
            loadQuiz();
        } else {
            showError("Error: Could not load quiz data.");
        }

        buttonSubmitQuiz.setOnClickListener(v -> handleSubmit());
        buttonFinishQuiz.setOnClickListener(v -> finish());
    }

    private void loadQuiz() {
        progressBarQuiz.setVisibility(View.VISIBLE);
        quizContainer.setVisibility(View.GONE);

        Cursor cursor = dbHelper.getHistoryItem(historyId);
        if (cursor != null && cursor.moveToFirst()) {
            String quizJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_JSON));
            String originalText = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ORIGINAL_TEXT));
            cursor.close();

            if (quizJson != null && !quizJson.trim().isEmpty()) {
                // Đã có quiz, tải và bắt đầu
                parseQuizJson(quizJson);
            } else {
                // Chưa có quiz, tạo mới
                generateQuiz(originalText);
            }
        } else {
            showError("Could not find history item to start quiz.");
        }
    }

    private void generateQuiz(String text) {
        String prompt = "Based on the following text, generate exactly 5 multiple-choice questions. " +
                "Provide the output in a clean JSON format. The JSON should be an object with a single key 'questions'. " +
                "This key should hold an array of question objects. Each object must have three keys: 'question' (the question text), " +
                "'options' (an array of 4 string choices), and 'answer' (the correct choice text). " +
                "Do not include any text outside of the JSON object.\n\nText: " + text;

        GeminiApiRequest request = new GeminiApiRequest(prompt);

        apiService.generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call, @NonNull Response<GeminiApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getCandidates().isEmpty()) {
                    String jsonResponse = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                    String cleanJson = jsonResponse.substring(jsonResponse.indexOf("{"), jsonResponse.lastIndexOf("}") + 1);

                    // ✅ Lưu quiz vừa tạo vào DB
                    dbHelper.updateQuizData(historyId, cleanJson);

                    parseQuizJson(cleanJson);
                } else {
                    showError("Failed to generate quiz. Please try again.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                showError("Network error. Could not generate quiz.");
            }
        });
    }

    private void parseQuizJson(String json) {
        try {
            Gson gson = new Gson();
            QuizResponse quizResponse = gson.fromJson(json, QuizResponse.class);
            questions = quizResponse.getQuestions();

            if (questions != null && !questions.isEmpty()) {
                progressBarQuiz.setVisibility(View.GONE);
                quizContainer.setVisibility(View.VISIBLE);
                displayQuestion();
            } else {
                showError("Could not parse quiz questions.");
            }
        } catch (JsonSyntaxException e) {
            showError("Error in quiz data format from server.");
        }
    }

    private void displayQuestion() {
        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
        textViewQuestionNumber.setText("Question " + (currentQuestionIndex + 1) + "/" + questions.size());
        textViewQuestion.setText(currentQuestion.getQuestion());

        radioGroupOptions.removeAllViews();
        for (String option : currentQuestion.getOptions()) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(option);
            radioGroupOptions.addView(radioButton);
        }
        radioGroupOptions.clearCheck();
    }

    private void handleSubmit() {
        int selectedId = radioGroupOptions.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer.", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedId);
        String selectedAnswer = selectedRadioButton.getText().toString();
        String correctAnswer = questions.get(currentQuestionIndex).getAnswer();

        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Wrong! The correct answer was: " + correctAnswer, Toast.LENGTH_LONG).show();
        }

        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion();
        } else {
            showFinalScore();
        }
    }

    private void showFinalScore() {
        quizContainer.setVisibility(View.GONE);
        scoreContainer.setVisibility(View.VISIBLE);
        textViewScore.setText(score + "/" + questions.size());

        // ✅ Lưu điểm số cuối cùng vào DB
        dbHelper.updateQuizScore(historyId, score);
    }

    private void showError(String message) {
        progressBarQuiz.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
