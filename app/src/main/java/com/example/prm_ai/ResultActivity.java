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
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {

    private TextView extractedTextView;
    private TextView summaryTextView;
    private TextView labelsTextView;
    private TextRecognizer recognizer;
    private ImageLabeler imageLabeler;
    private GeminiApiService apiService;
    private DatabaseHelper dbHelper;
    private int userId = -1;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        extractedTextView = findViewById(R.id.extractedTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        labelsTextView = findViewById(R.id.labelsTextView);
        dbHelper = new DatabaseHelper(this);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        apiService = ApiClient.getClient().create(GeminiApiService.class);

        // Get data from Intent
        currentPhotoPath = getIntent().getStringExtra("PHOTO_PATH");
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (currentPhotoPath != null) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (imageBitmap != null) {
                // Phân loại nội dung ảnh
                labelImage(imageBitmap);
                // Trích xuất văn bản
                recognizeText(imageBitmap);
            }
        } else {
            Toast.makeText(this, "Error: Photo not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void labelImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        imageLabeler.process(image)
                .addOnSuccessListener(labels -> {
                    if (labels.isEmpty()) {
                        labelsTextView.setText("Không tìm thấy nhãn nào.");
                        return;
                    }

                    // Xây dựng chuỗi kết quả phân loại
                    StringBuilder labelText = new StringBuilder();
                    for (int i = 0; i < labels.size() && i < 5; i++) {
                        ImageLabel label = labels.get(i);
                        labelText.append("• ")
                                .append(label.getText())
                                .append(" (")
                                .append(String.format("%.1f%%", label.getConfidence() * 100))
                                .append(")");
                        if (i < labels.size() - 1 && i < 4) {
                            labelText.append("\n");
                        }
                    }

                    labelsTextView.setText(labelText.toString());
                    labelsTextView.setTypeface(null, android.graphics.Typeface.NORMAL);
                })
                .addOnFailureListener(e -> {
                    labelsTextView.setText("Phân loại thất bại: " + e.getMessage());
                    Toast.makeText(ResultActivity.this, "Image labeling failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

        apiService.generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call, @NonNull Response<GeminiApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getCandidates().isEmpty()) {
                    String summary = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                    summaryTextView.setText(summary);

                    // Lưu lịch sử với đường dẫn ảnh
                    if (userId != -1 && currentPhotoPath != null) {
                        dbHelper.addScanHistory(userId, currentPhotoPath, text, summary);
                    }
                } else {
                    summaryTextView.setText("Summarization failed. Please check your API key and network connection.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                summaryTextView.setText("Summarization failed. Please check your network connection.");
            }
        });
    }
}