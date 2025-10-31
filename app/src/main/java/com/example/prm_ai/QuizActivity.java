package com.example.prm_ai;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.prm_ai.network.ApiClient;
import com.example.prm_ai.network.GeminiApiRequest;
import com.example.prm_ai.network.GeminiApiResponse;
import com.example.prm_ai.network.GeminiApiService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizActivity extends BaseActivity {

    private ProgressBar progressBarQuiz;
    private LinearLayout quizContainer, scoreContainer;
    private TextView textViewQuestionNumber, textViewQuestion, textViewScore;
    private RadioGroup radioGroupOptions;
    private Button buttonSubmitQuiz, buttonFinishQuiz, buttonStatistics;

    private GeminiApiService apiService;
    private DatabaseHelper dbHelper;
    private long historyId;
    private List<QuizQuestion> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean isAnswerSubmitted = false;

    private ArrayList<String> userAnswers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        progressBarQuiz = findViewById(R.id.progressBarQuiz);
        quizContainer = findViewById(R.id.quizContainer);
        scoreContainer = findViewById(R.id.scoreContainer);
        textViewQuestionNumber = findViewById(R.id.textViewQuestionNumber);
        textViewQuestion = findViewById(R.id.textViewQuestion);
        textViewScore = findViewById(R.id.textViewScore);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        buttonSubmitQuiz = findViewById(R.id.buttonSubmitQuiz);
        buttonFinishQuiz = findViewById(R.id.buttonFinishQuiz);
        buttonStatistics = findViewById(R.id.buttonStatistics);

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

        buttonStatistics.setOnClickListener(v -> {
            if (questions != null && !questions.isEmpty()) {
                Intent intent = new Intent(QuizActivity.this, StatisticsActivity.class);
                intent.putExtra("QUESTIONS", (Serializable) questions);
                intent.putStringArrayListExtra("USER_ANSWERS", userAnswers);
                startActivity(intent);
            }
        });
    }

    private void loadQuiz() {
        progressBarQuiz.setVisibility(View.VISIBLE);
        quizContainer.setVisibility(View.GONE);

        Cursor cursor = dbHelper.getHistoryItem(historyId);
        if (cursor != null && cursor.moveToFirst()) {
            String quizJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_QUIZ_JSON));
            String originalSummary = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SUMMARY_TEXT));
            String translatedSummary = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSLATED_SUMMARY));
            cursor.close();

            // ✅ Ưu tiên sử dụng quiz đã có (instant load)
            if (quizJson != null && !quizJson.trim().isEmpty()) {
                parseQuizJson(quizJson);
            } else {
                // ✅ Sử dụng summary (ngắn hơn original text) để tạo quiz nhanh hơn
                String contentForQuiz = !TextUtils.isEmpty(translatedSummary) ? translatedSummary : originalSummary;
                generateQuizInVietnamese(contentForQuiz);
            }
        } else {
            showError("Could not find history item to start quiz.");
        }
    }

    private void generateQuizInVietnamese(String text) {
        // ✅ OPTIMIZATION 1: Giới hạn độ dài text (API xử lý nhanh hơn với text ngắn)
        String truncatedText = text.length() > 1500 ? text.substring(0, 1500) + "..." : text;

        // ✅ OPTIMIZATION 2: Giảm số câu hỏi từ 10 → 5 (giảm 50% thời gian xử lý)
        // ✅ OPTIMIZATION 3: Prompt ngắn gọn, rõ ràng hơn
        String prompt = "Tạo 5 câu hỏi trắc nghiệm từ văn bản sau.\n\n" +
                "Format JSON (không có markdown, không có text thừa):\n" +
                "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"question\": \"Câu hỏi?\",\n" +
                "      \"options\": [\"A\", \"B\", \"C\", \"D\"],\n" +
                "      \"answer\": \"A\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "Văn bản:\n" + truncatedText;

        GeminiApiRequest request = new GeminiApiRequest(prompt);

        apiService.generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call, @NonNull Response<GeminiApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getCandidates().isEmpty()) {
                    String jsonResponse = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();

                    // ✅ OPTIMIZATION 4: Làm sạch JSON nhanh và đơn giản
                    String cleanJson = cleanJsonResponse(jsonResponse);

                    dbHelper.updateQuizData(historyId, cleanJson);
                    parseQuizJson(cleanJson);
                } else {
                    showError("Failed to generate quiz from API.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                showError("Network error while generating quiz: " + t.getMessage());
            }
        });
    }

    /**
     * ✅ OPTIMIZATION 5: Làm sạch JSON response nhanh
     */
    private String cleanJsonResponse(String response) {
        // Loại bỏ markdown code blocks
        String cleaned = response.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // Tìm JSON object
        int jsonStart = cleaned.indexOf("{");
        int jsonEnd = cleaned.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        return cleaned;
    }

    private void parseQuizJson(String json) {
        try {
            Gson gson = new Gson();
            QuizResponse quizResponse = gson.fromJson(json, QuizResponse.class);

            if (quizResponse != null && quizResponse.getQuestions() != null && !quizResponse.getQuestions().isEmpty()) {
                questions = quizResponse.getQuestions();
                progressBarQuiz.setVisibility(View.GONE);
                quizContainer.setVisibility(View.VISIBLE);
                displayQuestion();
            } else {
                showError("Could not parse quiz questions from JSON. Check AI response format.");
            }
        } catch (JsonSyntaxException e) {
            showError("JSON Syntax Error: " + e.getMessage());
        }
    }

    private void displayQuestion() {
        isAnswerSubmitted = false;
        buttonSubmitQuiz.setText(R.string.quiz_submit);

        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
        textViewQuestionNumber.setText(getString(R.string.quiz_question_title, currentQuestionIndex + 1, questions.size()));
        textViewQuestion.setText(currentQuestion.getQuestion());

        radioGroupOptions.removeAllViews();
        for (String option : currentQuestion.getOptions()) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(option);
            radioButton.setPadding(20, 20, 20, 20);
            radioButton.setTextColor(Color.BLACK);
            radioGroupOptions.addView(radioButton);
        }
        radioGroupOptions.clearCheck();
    }

    private void handleSubmit() {
        if (!isAnswerSubmitted) {
            int selectedId = radioGroupOptions.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, R.string.quiz_please_select_answer, Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < radioGroupOptions.getChildCount(); i++) {
                radioGroupOptions.getChildAt(i).setEnabled(false);
            }

            RadioButton selectedRadioButton = findViewById(selectedId);
            String selectedAnswer = selectedRadioButton.getText().toString();
            userAnswers.add(selectedAnswer);
            String correctAnswer = questions.get(currentQuestionIndex).getAnswer();

            if (selectedAnswer.equals(correctAnswer)) {
                score++;
                selectedRadioButton.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                selectedRadioButton.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                for (int i = 0; i < radioGroupOptions.getChildCount(); i++) {
                    RadioButton button = (RadioButton) radioGroupOptions.getChildAt(i);
                    if (button.getText().toString().equals(correctAnswer)) {
                        button.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                        break;
                    }
                }
            }

            isAnswerSubmitted = true;
            if (currentQuestionIndex < questions.size() - 1) {
                buttonSubmitQuiz.setText(R.string.quiz_next);
            } else {
                buttonSubmitQuiz.setText(R.string.quiz_finish);
            }

        } else {
            currentQuestionIndex++;
            if (currentQuestionIndex < questions.size()) {
                displayQuestion();
            } else {
                showFinalScore();
            }
        }
    }

    private void showFinalScore() {
        quizContainer.setVisibility(View.GONE);
        scoreContainer.setVisibility(View.VISIBLE);
        textViewScore.setText(score + "/" + questions.size());

        String userAnswersJson = new Gson().toJson(userAnswers);
        dbHelper.updateQuizAttempt(historyId, score, userAnswersJson);
    }

    private void showError(String message) {
        progressBarQuiz.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    static class QuizResponse {
        private List<QuizQuestion> questions;
        public List<QuizQuestion> getQuestions() { return questions; }
    }
}