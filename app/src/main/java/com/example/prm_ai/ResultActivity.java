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
            } else {
                Toast.makeText(this, "Could not start quiz, history not saved.", Toast.LENGTH_SHORT).show();
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
                    summarizeText(extractedText);
                })
                .addOnFailureListener(e -> {
                    progressBarExtractedText.setVisibility(View.GONE);
                    extractedTextView.setVisibility(View.VISIBLE);
                    extractedTextView.setText("Text recognition failed: " + e.getMessage());
                });
    }

    private void summarizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            summaryTextView.setText("No text to summarize.");
            return;
        }

        progressBarSummary.setVisibility(View.VISIBLE);
        summaryTextView.setVisibility(View.GONE);

        String prompt = "Summarize the following text:\n\n" + text;
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
                        currentHistoryId = dbHelper.addScanHistory(userId, currentPhotoPath, text, summary);
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
