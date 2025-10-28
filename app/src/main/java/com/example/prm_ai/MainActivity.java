package com.example.prm_ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button extractButton;
    private GmsDocumentScanner scanner;
    private Uri scannedImageUri;

    // ActivityResultLauncher cho camera permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startDocumentScanner();
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    // ActivityResultLauncher cho document scanner
    private final ActivityResultLauncher<IntentSenderRequest> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    GmsDocumentScanningResult scanningResult =
                            GmsDocumentScanningResult.fromActivityResultIntent(result.getData());

                    if (scanningResult != null && scanningResult.getPages() != null
                            && !scanningResult.getPages().isEmpty()) {
                        // Lấy ảnh đã được xử lý (cắt góc, làm phẳng, chỉnh sáng)
                        scannedImageUri = scanningResult.getPages().get(0).getImageUri();

                        // Hiển thị ảnh
                        imageView.setImageURI(scannedImageUri);
                        extractButton.setVisibility(View.VISIBLE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        extractButton = findViewById(R.id.extractButton);

        // Khởi tạo Document Scanner với options
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)  // Cho phép chọn từ thư viện
                .setPageLimit(1)  // Giới hạn 1 trang
                .setResultFormats(
                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                        GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                )
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();

        scanner = GmsDocumentScanning.getClient(options);

        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> checkPermissionAndScan());

        extractButton.setOnClickListener(v -> {
            if (scannedImageUri != null) {
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("IMAGE_URI", scannedImageUri.toString());
                startActivity(intent);
            }
        });
    }

    private void checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startDocumentScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startDocumentScanner() {
        scanner.getStartScanIntent(this)
                .addOnSuccessListener(intentSender ->
                        scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}