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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {

    private static final String API_KEY = "YOUR_API_KEY"; // Replace with your API key

    private TextView extractedTextView;
    private TextView summaryTextView;
    private TextRecognizer recognizer;
    private GeminiApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        extractedTextView = findViewById(R.id.extractedTextView);
        summaryTextView = findViewById(R.id.summaryTextView);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        apiService = ApiClient.getClient().create(GeminiApiService.class);

        String currentPhotoPath = getIntent().getStringExtra("PHOTO_PATH");

        if (currentPhotoPath != null) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (imageBitmap != null) {
                recognizeText(imageBitmap);
            }
        }
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        String extractedText = visionText.getText();
                        extractedTextView.setText(extractedText);
                        summarizeText(extractedText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ResultActivity.this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void summarizeText(String text) {
        String prompt = "Summarize the following text and generate 3 quiz questions with answers based on it:\n\n" + text;
        GeminiApiRequest request = new GeminiApiRequest(prompt);

        apiService.generateContent(API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(Call<GeminiApiResponse> call, Response<GeminiApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeminiApiResponse.Candidate candidate = response.body().getCandidates().get(0);
                    String summary = candidate.getContent().getParts().get(0).getText();
                    summaryTextView.setText(summary);
                } else {
                    Toast.makeText(ResultActivity.this, "Summarization failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeminiApiResponse> call, Throwable t) {
                Toast.makeText(ResultActivity.this, "Summarization failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
