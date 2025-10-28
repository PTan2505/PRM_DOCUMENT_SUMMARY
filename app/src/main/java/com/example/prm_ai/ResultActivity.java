package com.example.prm_ai;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
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
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY; // THAY BẰNG API KEY THẬT CỦA BẠN

    private TextView extractedTextView;
    private TextView labelsTextView;
    private TextView summaryTextView;
    private ProgressBar progressBar;

    private TextRecognizer textRecognizer;
    private ImageLabeler imageLabeler;
    private GeminiApiService apiService;

    private String extractedText = "";
    private List<String> imageLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Khởi tạo views
        extractedTextView = findViewById(R.id.extractedTextView);
        labelsTextView = findViewById(R.id.labelsTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        progressBar = findViewById(R.id.progressBar);

        // Khởi tạo ML Kit services
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        // Khởi tạo API service
        apiService = ApiClient.getClient().create(GeminiApiService.class);

        // Kiểm tra API Key
        if (API_KEY.equals("YOUR_API_KEY")) {
            Toast.makeText(this, "⚠️ Chưa cấu hình API Key!", Toast.LENGTH_LONG).show();
            summaryTextView.setText("❌ Lỗi: Chưa thêm Gemini API Key vào code.\n\nHướng dẫn:\n1. Truy cập: https://makersuite.google.com/app/apikey\n2. Tạo API key mới\n3. Thay thế YOUR_API_KEY trong ResultActivity.java");
            progressBar.setVisibility(View.GONE);
        }

        // Lấy URI ảnh từ Intent
        String imageUriString = getIntent().getStringExtra("IMAGE_URI");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            processImage(imageUri);
        } else {
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void processImage(Uri imageUri) {
        try {
            // Load bitmap từ URI
            Bitmap bitmap = loadBitmapFromUri(imageUri);
            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

            progressBar.setVisibility(View.VISIBLE);

            // Bước 2: Image Labeling (phân loại nội dung)
            performImageLabeling(inputImage);

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source);
            } else {
                return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading bitmap", e);
            return null;
        }
    }

    // Bước 2: Image Labeling
    private void performImageLabeling(InputImage image) {
        imageLabeler.process(image)
                .addOnSuccessListener(labels -> {
                    // Lưu các labels
                    for (ImageLabel label : labels) {
                        if (label.getConfidence() > 0.5f) {
                            imageLabels.add(label.getText());
                            Log.d(TAG, "Label: " + label.getText() + " (" + label.getConfidence() + ")");
                        }
                    }

                    // Hiển thị labels
                    if (!imageLabels.isEmpty()) {
                        labelsTextView.setText("📋 Phân loại: " + String.join(", ", imageLabels));
                    } else {
                        labelsTextView.setText("📋 Không xác định được loại tài liệu");
                    }

                    // Tiếp tục bước 3: Text Recognition
                    performTextRecognition(image);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Labeling failed", e);
                    Toast.makeText(this, "Labeling failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Vẫn tiếp tục OCR dù labeling thất bại
                    performTextRecognition(image);
                });
    }

    // Bước 3: Text Recognition (OCR)
    private void performTextRecognition(InputImage image) {
        textRecognizer.process(image)
                .addOnSuccessListener(text -> {
                    extractedText = text.getText();

                    Log.d(TAG, "Extracted text length: " + extractedText.length());

                    if (extractedText.isEmpty()) {
                        extractedTextView.setText("⚠️ Không tìm thấy văn bản trong ảnh");
                        summaryTextView.setText("Không có nội dung để tóm tắt");
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    extractedTextView.setText(extractedText);

                    // Bước 4 & 5: Chuẩn bị và gọi Gemini API
                    callGeminiApi();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text recognition failed", e);
                    Toast.makeText(this, "Text recognition failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    // Bước 4 & 5: Chuẩn bị prompt và gọi Gemini API
    private void callGeminiApi() {
        if (API_KEY.equals("YOUR_API_KEY")) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Tạo prompt dựa trên labels và text
        String prompt = buildPrompt();

        Log.d(TAG, "Calling Gemini API with prompt length: " + prompt.length());

        GeminiApiRequest request = new GeminiApiRequest(prompt);

        apiService.generateContent(API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiApiResponse> call,
                                   @NonNull Response<GeminiApiResponse> response) {
                progressBar.setVisibility(View.GONE);

                Log.d(TAG, "API Response code: " + response.code());

                // Bước 6: Xử lý kết quả
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        GeminiApiResponse apiResponse = response.body();

                        if (apiResponse.getCandidates() == null || apiResponse.getCandidates().isEmpty()) {
                            Log.e(TAG, "No candidates in response");
                            summaryTextView.setText("❌ API trả về rỗng");
                            return;
                        }

                        GeminiApiResponse.Candidate candidate = apiResponse.getCandidates().get(0);

                        if (candidate.getContent() == null || candidate.getContent().getParts() == null
                                || candidate.getContent().getParts().isEmpty()) {
                            Log.e(TAG, "No content in candidate");
                            summaryTextView.setText("❌ Không có nội dung trong response");
                            return;
                        }

                        String summary = candidate.getContent().getParts().get(0).getText();

                        Log.d(TAG, "Summary received, length: " + summary.length());

                        // Bước 7: Hiển thị kết quả
                        summaryTextView.setText(summary);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        summaryTextView.setText("❌ Lỗi khi phân tích kết quả: " + e.getMessage());
                        Toast.makeText(ResultActivity.this,
                                "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "API call failed: " + response.code() + " - " + response.message());

                    // Đọc error body để debug
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }

                    String errorMessage = "❌ API call failed\n\n";
                    errorMessage += "Code: " + response.code() + "\n";
                    errorMessage += "Message: " + response.message() + "\n\n";

                    if (response.code() == 400) {
                        errorMessage += "⚠️ Bad Request - Kiểm tra:\n";
                        errorMessage += "• API Key có đúng không?\n";
                        errorMessage += "• Prompt có hợp lệ không?\n";
                        errorMessage += "• Text có quá dài không? (" + extractedText.length() + " ký tự)";
                    } else if (response.code() == 403) {
                        errorMessage += "⚠️ Forbidden - API Key không hợp lệ hoặc chưa được kích hoạt";
                    } else if (response.code() == 429) {
                        errorMessage += "⚠️ Too Many Requests - Đã vượt quá giới hạn API";
                    } else if (response.code() == 404) {
                        errorMessage += "⚠️ Not Found - Kiểm tra endpoint URL";
                    }

                    if (!errorBody.isEmpty()) {
                        errorMessage += "\n\nChi tiết: " + errorBody.substring(0, Math.min(200, errorBody.length()));
                    }

                    summaryTextView.setText(errorMessage);
                    Toast.makeText(ResultActivity.this,
                            "API Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeminiApiResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network error", t);

                String errorMessage = "❌ Lỗi kết nối\n\n";
                errorMessage += "Lỗi: " + t.getMessage() + "\n\n";
                errorMessage += "Kiểm tra:\n";
                errorMessage += "• Kết nối Internet\n";
                errorMessage += "• URL API có đúng không?\n";
                errorMessage += "• Firewall/VPN có chặn không?";

                summaryTextView.setText(errorMessage);
                Toast.makeText(ResultActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Bước 4: Xây dựng prompt thông minh
    private String buildPrompt() {
        StringBuilder prompt = new StringBuilder();

        // Thêm context từ image labels
        if (!imageLabels.isEmpty()) {
            prompt.append("Document type: ").append(String.join(", ", imageLabels)).append("\n\n");
        }

        // Giới hạn độ dài text để tránh vượt quá giới hạn token
        String limitedText = extractedText;
        if (extractedText.length() > 3000) {
            limitedText = extractedText.substring(0, 3000) + "...";
            Log.d(TAG, "Text truncated to 3000 chars");
        }

        prompt.append("Text content:\n").append(limitedText).append("\n\n");

        // Yêu cầu ngắn gọn hơn
        prompt.append("Please:\n");
        prompt.append("1. Summarize the main points\n");
        prompt.append("2. Create 3 multiple choice questions about the content\n");
        prompt.append("\nRespond in Vietnamese.");

        return prompt.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        if (imageLabeler != null) {
            imageLabeler.close();
        }
    }
}