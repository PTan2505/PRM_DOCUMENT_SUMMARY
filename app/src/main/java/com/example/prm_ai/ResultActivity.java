package com.example.prm_ai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.prm_ai.network.ApiClient;
import com.example.prm_ai.network.GeminiApiRequest;
import com.example.prm_ai.network.GeminiApiResponse;
import com.example.prm_ai.network.GeminiApiService;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {

    private TextView extractedTextView;
    private TextView summaryTextView;
    private TextRecognizer recognizer;
    private GeminiApiService apiService;
    private DatabaseHelper dbHelper;
    private int userId = -1; // Default to -1 (for Google users or errors)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        extractedTextView = findViewById(R.id.extractedTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        dbHelper = new DatabaseHelper(this);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        apiService = ApiClient.getClient().create(GeminiApiService.class);

        // Get data from Intent
        String currentPhotoPath = getIntent().getStringExtra("PHOTO_PATH");
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (currentPhotoPath != null) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (imageBitmap != null) {
                recognizeText(imageBitmap);
            }
        } else {
             Toast.makeText(this, "Error: Photo not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String extractedText = visionText.getText();
                    extractedTextView.setText(extractedText);
                    summarizeText(extractedText);
                })
                .addOnFailureListener(e -> Toast.makeText(ResultActivity.this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void summarizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            summaryTextView.setText("No text to summarize.");
            return;
        }

        String prompt = "Summarize the following text and generate 3 quiz questions with answers based on it:\n\n" + text;
        GeminiApiRequest request = new GeminiApiRequest(prompt);

        // Use the API Key from BuildConfig
        apiService.generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call, @NonNull Response<GeminiApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getCandidates().isEmpty()) {
                    String summary = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                    summaryTextView.setText(summary);

                    // Save to history if we have a valid local user ID
                    if (userId != -1) {
                        dbHelper.addScanHistory(userId, text, summary);
                    }
                } else {
                    summaryTextView.setText("Summarization failed. Please check your API key and network connection.");
                    try {
                        if (response.errorBody() != null) {
                            Toast.makeText(ResultActivity.this, "Error: " + response.errorBody().string(), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                summaryTextView.setText("Summarization failed. Please check your network connection.");
                 Toast.makeText(ResultActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
