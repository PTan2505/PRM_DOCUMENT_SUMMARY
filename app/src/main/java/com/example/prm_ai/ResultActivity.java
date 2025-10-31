package com.example.prm_ai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View; // Thêm
import android.widget.ProgressBar; // Thêm
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
import com.google.android.material.appbar.MaterialToolbar; // Thêm
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.prm_ai.BuildConfig;

public class ResultActivity extends AppCompatActivity {

    // XÓA DÒNG NÀY: private static final String API_KEY = "YOUR_API_KEY";
    // Chúng ta sẽ dùng BuildConfig.GEMINI_API_KEY mà bạn đã cấu hình trong gradle

    private TextView extractedTextView;
    private TextView summaryTextView;
    private ProgressBar extractionProgressBar; // Thêm
    private ProgressBar summaryProgressBar; // Thêm
    private TextRecognizer recognizer;
    private GeminiApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        extractedTextView = findViewById(R.id.extractedTextView);
        summaryTextView = findViewById(R.id.summaryTextView);
        extractionProgressBar = findViewById(R.id.extractionProgressBar); // Thêm
        summaryProgressBar = findViewById(R.id.summaryProgressBar); // Thêm

        // Thêm nút back cho Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed()); // Xử lý nút back

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
        // Bắt đầu xử lý: Hiển thị thanh tải, ẩn văn bản
        extractionProgressBar.setVisibility(View.VISIBLE);
        extractedTextView.setVisibility(View.GONE);

        // Cũng hiển thị thanh tải tóm tắt (vì nó đang chờ)
        summaryProgressBar.setVisibility(View.VISIBLE);
        summaryTextView.setVisibility(View.GONE);


        InputImage image = InputImage.fromBitmap(bitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        String extractedText = visionText.getText();

                        // Thành công: Ẩn thanh tải, hiển thị văn bản
                        extractionProgressBar.setVisibility(View.GONE);
                        extractedTextView.setText(extractedText);
                        extractedTextView.setVisibility(View.VISIBLE);

                        // Bắt đầu tác vụ tóm tắt
                        summarizeText(extractedText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Thất bại: Ẩn thanh tải, hiển thị lỗi
                        extractionProgressBar.setVisibility(View.GONE);
                        extractedTextView.setText("Lỗi trích xuất văn bản: " + e.getMessage());
                        extractedTextView.setVisibility(View.VISIBLE);

                        // Ẩn luôn thanh tải tóm tắt vì không thể tiếp tục
                        summaryProgressBar.setVisibility(View.GONE);
                        summaryTextView.setText("Không thể tóm tắt do lỗi trích xuất văn bản.");
                        summaryTextView.setVisibility(View.VISIBLE);

                        Toast.makeText(ResultActivity.this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void summarizeText(String text) {
        // Thanh tải đã được hiển thị từ hàm recognizeText

        String prompt = "Summarize the following text and generate 3 quiz questions with answers based on it:\n\n" + text;
        GeminiApiRequest request = new GeminiApiRequest(prompt);

        // *** SỬA LỖI QUAN TRỌNG: Dùng API Key từ BuildConfig ***
        apiService.generateContent(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiApiResponse>() {
            @Override
            public void onResponse(Call<GeminiApiResponse> call, Response<GeminiApiResponse> response) {
                // Tắt thanh tải
                summaryProgressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getCandidates() != null && !response.body().getCandidates().isEmpty()) {
                    try {
                        // Thêm kiểm tra lỗi
                        GeminiApiResponse.Candidate candidate = response.body().getCandidates().get(0);
                        String summary = candidate.getContent().getParts().get(0).getText();
                        summaryTextView.setText(summary);
                    } catch (Exception e) {
                        summaryTextView.setText("Lỗi khi đọc phản hồi: " + e.getMessage());
                    }
                } else {
                    summaryTextView.setText("Lỗi tóm tắt: " + response.message());
                    Toast.makeText(ResultActivity.this, "Summarization failed: " + response.code() + " " + response.message(), Toast.LENGTH_LONG).show();
                }
                // Hiển thị kết quả (hoặc lỗi)
                summaryTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<GeminiApiResponse> call, Throwable t) {
                // Tắt thanh tải
                summaryProgressBar.setVisibility(View.GONE);
                summaryTextView.setText("Lỗi mạng: " + t.getMessage());
                summaryTextView.setVisibility(View.VISIBLE);
                Toast.makeText(ResultActivity.this, "Summarization failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}