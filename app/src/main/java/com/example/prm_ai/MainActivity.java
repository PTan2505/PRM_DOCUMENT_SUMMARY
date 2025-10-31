package com.example.prm_ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY_PICK = 2;

    private ImageView imageView;
    private Button extractButton;
    private String currentPhotoPath;
    private int userId;

    private GmsDocumentScanner documentScanner;
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userId = getIntent().getIntExtra("USER_ID", -1);

        imageView = findViewById(R.id.imageView);
        extractButton = findViewById(R.id.extractButton);
        Button captureButton = findViewById(R.id.captureButton);
        Button selectFromGalleryButton = findViewById(R.id.selectFromGalleryButton);

        // ✅ Khởi tạo Document Scanner
        initializeDocumentScanner();

        // ✅ Nút quét tài liệu (thay thế camera)
        captureButton.setOnClickListener(v -> startDocumentScanner());

        // Chọn từ thư viện (giữ nguyên)
        selectFromGalleryButton.setOnClickListener(v -> {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto, REQUEST_GALLERY_PICK);
        });

        extractButton.setOnClickListener(v -> {
            if (currentPhotoPath != null) {
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("PHOTO_PATH", currentPhotoPath);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            }
        });
    }

    /**
     * ✅ Khởi tạo Document Scanner với options
     */
    private void initializeDocumentScanner() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)  // Chỉ quét 1 trang
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();

        documentScanner = GmsDocumentScanning.getClient(options);

        // ✅ Đăng ký ActivityResultLauncher để nhận kết quả quét
        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        GmsDocumentScanningResult scanningResult =
                                GmsDocumentScanningResult.fromActivityResultIntent(result.getData());

                        if (scanningResult != null) {
                            handleScanResult(scanningResult);
                        }
                    }
                }
        );
    }

    /**
     * ✅ Bắt đầu quét tài liệu
     */
    private void startDocumentScanner() {
        documentScanner.getStartScanIntent(this)
                .addOnSuccessListener(intentSender -> {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                    scannerLauncher.launch(request);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to start scanner: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ Xử lý kết quả quét tài liệu
     */
    private void handleScanResult(GmsDocumentScanningResult result) {
        GmsDocumentScanningResult.Page firstPage = result.getPages().get(0);
        Uri imageUri = firstPage.getImageUri();

        try {
            // Chuyển đổi URI thành file và lưu đường dẫn
            File photoFile = createImageFile();

            // Copy ảnh từ URI vào file
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
                 OutputStream outputStream = new FileOutputStream(photoFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            // Hiển thị ảnh
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            imageView.setImageBitmap(imageBitmap);
            extractButton.setVisibility(View.VISIBLE);

        } catch (IOException e) {
            Toast.makeText(this, "Failed to process scanned document", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_GALLERY_PICK) {
            // Xử lý ảnh từ thư viện (giữ nguyên logic cũ)
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                try {
                    File photoFile = createImageFile();
                    try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
                         OutputStream outputStream = new FileOutputStream(photoFile)) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) > 0) {
                            outputStream.write(buf, 0, len);
                        }
                    }
                    Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    imageView.setImageBitmap(imageBitmap);
                    extractButton.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}