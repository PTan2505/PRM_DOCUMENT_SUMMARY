package com.example.prm_ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private PreviewView previewView;
    private ImageView overlayView;
    private Button captureButton;
    private Button analyzeButton;

    private TextRecognizer textRecognizer;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    private Bitmap lastCapturedBitmap;
    private Text lastDetectedText;
    private boolean isAnalyzing = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        captureButton = findViewById(R.id.captureButton);
        analyzeButton = findViewById(R.id.analyzeButton);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Capture button - chụp và dừng preview
        captureButton.setOnClickListener(v -> captureImage());

        // Analyze button - gửi đến ResultActivity
        analyzeButton.setOnClickListener(v -> {
            if (lastCapturedBitmap != null) {
                // Lưu bitmap vào file
                File imageFile = saveBitmapToFile(lastCapturedBitmap);
                if (imageFile != null) {
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("IMAGE_URI", Uri.fromFile(imageFile).toString());
                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "Please capture an image first", Toast.LENGTH_SHORT).show();
            }
        });

        // Kiểm tra và xin quyền camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image Analysis với Text Recognition
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        // Camera selector (back camera)
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (isAnalyzing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        textRecognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    if (!text.getText().isEmpty()) {
                        lastDetectedText = text;
                        drawBoundingBoxes(text, imageProxy.getWidth(), imageProxy.getHeight());
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text recognition failed", e);
                    imageProxy.close();
                });
    }

    private void drawBoundingBoxes(Text text, int imageWidth, int imageHeight) {
        runOnUiThread(() -> {
            // Tạo bitmap trong suốt để vẽ boxes
            Bitmap overlayBitmap = Bitmap.createBitmap(
                    overlayView.getWidth(),
                    overlayView.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(overlayBitmap);

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);

            // Scale factors
            float scaleX = (float) overlayView.getWidth() / imageWidth;
            float scaleY = (float) overlayView.getHeight() / imageHeight;

            // Vẽ bounding box cho từng text block
            for (Text.TextBlock block : text.getTextBlocks()) {
                Rect boundingBox = block.getBoundingBox();
                if (boundingBox != null) {
                    Rect scaledRect = new Rect(
                            (int) (boundingBox.left * scaleX),
                            (int) (boundingBox.top * scaleY),
                            (int) (boundingBox.right * scaleX),
                            (int) (boundingBox.bottom * scaleY)
                    );
                    canvas.drawRect(scaledRect, paint);
                }
            }

            overlayView.setImageBitmap(overlayBitmap);
        });
    }

    private void captureImage() {
        isAnalyzing = true;

        // Chụp frame hiện tại từ PreviewView
        Bitmap bitmap = previewView.getBitmap();
        if (bitmap != null) {
            lastCapturedBitmap = bitmap;

            // Vẽ bounding boxes lên ảnh chụp
            if (lastDetectedText != null) {
                Bitmap annotatedBitmap = drawBoundingBoxesOnBitmap(bitmap, lastDetectedText);
                overlayView.setImageBitmap(annotatedBitmap);
            }

            // Hiện nút analyze
            analyzeButton.setVisibility(View.VISIBLE);
            captureButton.setText("Chụp Lại");

            Toast.makeText(this, "Đã chụp! Nhấn 'Phân Tích' để tiếp tục",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Không thể chụp ảnh", Toast.LENGTH_SHORT).show();
            isAnalyzing = false;
        }
    }

    private Bitmap drawBoundingBoxesOnBitmap(Bitmap original, Text text) {
        Bitmap mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        for (Text.TextBlock block : text.getTextBlocks()) {
            Rect boundingBox = block.getBoundingBox();
            if (boundingBox != null) {
                canvas.drawRect(boundingBox, paint);
            }
        }

        return mutableBitmap;
    }

    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "captured_image.jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error saving bitmap", e);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}