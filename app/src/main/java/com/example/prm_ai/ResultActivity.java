package com.example.prm_ai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.prm_ai.network.ApiClient;
import com.example.prm_ai.network.GeminiApiRequest;
import com.example.prm_ai.network.GeminiApiResponse;
import com.example.prm_ai.network.GeminiApiService;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {

    private TextView extractedTextView, summaryTextView;
    private Button buttonTakeQuiz;
    private ProgressBar progressBarSummary, progressBarExtractedText;

    private TextRecognizer recognizer;
    private GeminiApiService apiService;
    private DatabaseHelper dbHelper;
    private LanguageIdentifier languageIdentifier;

    private int userId = -1;
    private long currentHistoryId = -1;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        extractedTextView = findViewById(R.id.extractedTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        buttonTakeQuiz = findViewById(R.id.buttonTakeQuiz);
        progressBarSummary = findViewById(R.id.progressBarSummary);
        progressBarExtractedText = findViewById(R.id.progressBarExtractedText);

        dbHelper = new DatabaseHelper(this);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        apiService = ApiClient.getClient().create(GeminiApiService.class);
        languageIdentifier = LanguageIdentification.getClient();

        currentPhotoPath = getIntent().getStringExtra("PHOTO_PATH");
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (currentPhotoPath != null) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (imageBitmap != null) {
                recognizeText(imageBitmap);
            }
        } else {
            Toast.makeText(this, "Error: Photo not found.", Toast.LENGTH_SHORT).show();
        }

        buttonTakeQuiz.setOnClickListener(v -> {
            if (currentHistoryId != -1) {
                Intent intent = new Intent(ResultActivity.this, QuizActivity.class);
                intent.putExtra("HISTORY_ID", currentHistoryId);
                startActivity(intent);
            }
        });
    }

    private void recognizeText(Bitmap bitmap) {
        progressBarExtractedText.setVisibility(View.VISIBLE);
        extractedTextView.setVisibility(View.GONE);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    progressBarExtractedText.setVisibility(View.GONE);
                    extractedTextView.setVisibility(View.VISIBLE);
                    String extractedText = visionText.getText();
                    extractedTextView.setText(extractedText);
                    // ✅ Bắt đầu luồng xử lý mới: phát hiện ngôn ngữ rồi tóm tắt
                    detectLanguageAndSummarize(extractedText);
                })
                .addOnFailureListener(e -> {
                    progressBarExtractedText.setVisibility(View.GONE);
                    extractedTextView.setVisibility(View.VISIBLE);
                    extractedTextView.setText("Text recognition failed: " + e.getMessage());
                });
    }

    private void detectLanguageAndSummarize(String text) {
        if (text == null || text.trim().isEmpty()) {
            summaryTextView.setText("No text to summarize.");
            return;
        }
        // Phát hiện ngôn ngữ của văn bản
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        languageCode = "en"; // Mặc định là tiếng Anh nếu không xác định được
                    }
                    // Yêu cầu tóm tắt bằng ngôn ngữ đã phát hiện
                    summarizeInDetectedLanguage(text, languageCode);
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi, mặc định dùng tiếng Anh
                    summarizeInDetectedLanguage(text, "en");
                });
    }

    private void summarizeInDetectedLanguage(String text, String detectedLanguageCode) {
        progressBarSummary.setVisibility(View.VISIBLE);
        summaryTextView.setVisibility(View.GONE);

        // ✅ Sửa đổi prompt: Yêu cầu tóm tắt bằng ngôn ngữ gốc
        String prompt = "Summarize the following text in its original language, which is '" + detectedLanguageCode + "':\n\n" + text;
        GeminiApiRequest request = new GeminiApiRequest(prompt);

        apiService.generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call, @NonNull Response<GeminiApiResponse> response) {
                progressBarSummary.setVisibility(View.GONE);
                summaryTextView.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null && !response.body().getCandidates().isEmpty()) {
                    String summary = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                    summaryTextView.setText(summary);

                    if (userId != -1 && currentPhotoPath != null) {
                        // ✅ Lưu lại lịch sử với mã ngôn ngữ đã phát hiện
                        currentHistoryId = dbHelper.addScanHistory(userId, currentPhotoPath, text, summary, null, detectedLanguageCode);
                    }
                    buttonTakeQuiz.setVisibility(View.VISIBLE);
                } else {
                    summaryTextView.setText("Summarization failed.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                progressBarSummary.setVisibility(View.GONE);
                summaryTextView.setVisibility(View.VISIBLE);
                summaryTextView.setText("Summarization failed: " + t.getMessage());
            }
        });
    }
}
