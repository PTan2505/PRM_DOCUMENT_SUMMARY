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
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
public class ResultActivity extends BaseActivity {

    private TextView extractedTextView, summaryTextView, imageLabelsTextView;
    private Button buttonTakeQuiz, buttonTranslate;
    private ProgressBar progressBarSummary, progressBarExtractedText, progressBarImageLabels;

    private TextRecognizer recognizer;
    private ImageLabeler imageLabeler;
    private GeminiApiService apiService;
    private DatabaseHelper dbHelper;
    private LanguageIdentifier languageIdentifier;

    private int userId = -1;
    private long currentHistoryId = -1;
    private String currentPhotoPath;

    private String originalSummaryCache;
    private String translatedSummaryCache;
    private boolean isShowingTranslated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Toolbar toolbar = findViewById(R.id.toolbarResult);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        extractedTextView = findViewById(R.id.extractedTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        imageLabelsTextView = findViewById(R.id.imageLabelsTextView);
        buttonTakeQuiz = findViewById(R.id.buttonTakeQuiz);
        buttonTranslate = findViewById(R.id.buttonTranslate);
        progressBarSummary = findViewById(R.id.progressBarSummary);
        progressBarExtractedText = findViewById(R.id.progressBarExtractedText);
        progressBarImageLabels = findViewById(R.id.progressBarImageLabels);

        dbHelper = new DatabaseHelper(this);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        apiService = ApiClient.getClient().create(GeminiApiService.class);
        languageIdentifier = LanguageIdentification.getClient();

        currentPhotoPath = getIntent().getStringExtra("PHOTO_PATH");
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (currentPhotoPath != null) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (imageBitmap != null) {
                recognizeText(imageBitmap);
                labelImage(imageBitmap);
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

        buttonTranslate.setOnClickListener(v -> toggleSummaryTranslation());
        buttonTranslate.setVisibility(View.GONE);
    }

    private void labelImage(Bitmap bitmap) {
        progressBarImageLabels.setVisibility(View.VISIBLE);
        imageLabelsTextView.setVisibility(View.GONE);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        imageLabeler.process(image)
                .addOnSuccessListener(labels -> {
                    progressBarImageLabels.setVisibility(View.GONE);
                    imageLabelsTextView.setVisibility(View.VISIBLE);

                    StringBuilder labelsText = new StringBuilder();
                    for (ImageLabel label : labels) {
                        String text = label.getText();
                        float confidence = label.getConfidence();
                        labelsText.append(String.format("%s (%.0f%%)\n", text, confidence * 100));
                    }

                    if (labelsText.length() > 0) {
                        imageLabelsTextView.setText(labelsText.toString().trim());
                    } else {
                        imageLabelsTextView.setText("No labels detected");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBarImageLabels.setVisibility(View.GONE);
                    imageLabelsTextView.setVisibility(View.VISIBLE);
                    imageLabelsTextView.setText("Image labeling failed: " + e.getMessage());
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
                    detectLanguageAndProcess(extractedText);
                })
                .addOnFailureListener(e -> {
                    progressBarExtractedText.setVisibility(View.GONE);
                    extractedTextView.setVisibility(View.VISIBLE);
                    extractedTextView.setText("Text recognition failed: " + e.getMessage());
                });
    }

    private void detectLanguageAndProcess(String text) {
        if (text == null || text.trim().isEmpty()) {
            summaryTextView.setText("No text to summarize.");
            return;
        }
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) languageCode = "en";
                    summarizeAndTranslate(text, languageCode);
                })
                .addOnFailureListener(e -> summarizeAndTranslate(text, "en"));
    }

    private void summarizeAndTranslate(String originalText, String detectedLanguageCode) {
        progressBarSummary.setVisibility(View.VISIBLE);
        summaryTextView.setVisibility(View.GONE);

        String summaryPrompt = "Summarize the following text in its original language, which is '" + detectedLanguageCode + "':\n\n" + originalText;
        makeApiCall(summaryPrompt, new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call, @NonNull Response<GeminiApiResponse> response) {
                if (isSuccess(response)) {
                    originalSummaryCache = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                    summaryTextView.setText(originalSummaryCache);
                    progressBarSummary.setVisibility(View.GONE);
                    summaryTextView.setVisibility(View.VISIBLE);

                    if (!detectedLanguageCode.equals("vi")) {
                        buttonTranslate.setVisibility(View.VISIBLE);
                        buttonTranslate.setText(R.string.result_translating);
                        buttonTranslate.setEnabled(false);
                        translateSummaryAndSave(originalText, detectedLanguageCode);
                    } else {
                        buttonTakeQuiz.setVisibility(View.VISIBLE);
                        saveHistory(originalText, originalSummaryCache, null, detectedLanguageCode);
                    }
                } else {
                    handleApiFailure("Summarization failed.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                handleApiFailure(t.getMessage());
            }
        });
    }

    private void translateSummaryAndSave(String originalText, String detectedLanguageCode) {
        String translatePrompt = "Translate the following text to Vietnamese:\n\n" + originalSummaryCache;
        makeApiCall(translatePrompt, new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call, @NonNull Response<GeminiApiResponse> response) {
                if (isSuccess(response)) {
                    translatedSummaryCache = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                    buttonTranslate.setText(R.string.result_translate);
                    buttonTranslate.setEnabled(true);
                } else {
                    buttonTranslate.setVisibility(View.GONE);
                }
                buttonTakeQuiz.setVisibility(View.VISIBLE);
                saveHistory(originalText, originalSummaryCache, translatedSummaryCache, detectedLanguageCode);
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                buttonTranslate.setVisibility(View.GONE);
                buttonTakeQuiz.setVisibility(View.VISIBLE);
                saveHistory(originalText, originalSummaryCache, null, detectedLanguageCode);
            }
        });
    }

    private void saveHistory(String originalText, String originalSummary, String translatedSummary, String detectedLanguage) {
        if (userId != -1 && currentPhotoPath != null) {
            currentHistoryId = dbHelper.addScanHistory(userId, currentPhotoPath, originalText, originalSummary, translatedSummary, detectedLanguage);
        }
    }

    private void toggleSummaryTranslation() {
        if (isShowingTranslated) {
            summaryTextView.setText(originalSummaryCache);
            buttonTranslate.setText(R.string.result_translate);
            isShowingTranslated = false;
        } else {
            if (translatedSummaryCache != null && !translatedSummaryCache.isEmpty()) {
                summaryTextView.setText(translatedSummaryCache);
                buttonTranslate.setText(R.string.result_view_original);
                isShowingTranslated = true;
            }
        }
    }

    private void makeApiCall(String prompt, Callback<GeminiApiResponse> callback) {
        GeminiApiRequest request = new GeminiApiRequest(prompt);
        apiService.generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(callback);
    }

    private boolean isSuccess(Response<GeminiApiResponse> response) {
        return response.isSuccessful() && response.body() != null && !response.body().getCandidates().isEmpty();
    }

    private void handleApiFailure(String message) {
        progressBarSummary.setVisibility(View.GONE);
        summaryTextView.setVisibility(View.VISIBLE);
        summaryTextView.setText(message);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Kiểm tra xem ID có phải là nút "Home" (mũi tên back) không
        if (item.getItemId() == android.R.id.home) {
            // Quay về trang trước
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}