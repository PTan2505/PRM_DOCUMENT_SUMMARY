package com.example.prm_ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView imageView;
    private Button extractButton;
    private String currentPhotoPath;
    private int userId;

    // Document Scanner components
    private GmsDocumentScanner documentScanner;
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userId = getIntent().getIntExtra("USER_ID", -1);

        imageView = findViewById(R.id.imageView);
        extractButton = findViewById(R.id.extractButton);

        // Khởi tạo Document Scanner
        initializeDocumentScanner();

        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                // Sử dụng Document Scanner thay vì camera thông thường
                startDocumentScanner();
            }
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

    private void initializeDocumentScanner() {
        // Cấu hình Document Scanner
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();

        documentScanner = GmsDocumentScanning.getClient(options);

        // Đăng ký ActivityResultLauncher cho Document Scanner
        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        GmsDocumentScanningResult scanningResult =
                                GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                        if (scanningResult != null && scanningResult.getPages() != null && !scanningResult.getPages().isEmpty()) {
                            // Lấy ảnh đã scan
                            Uri imageUri = scanningResult.getPages().get(0).getImageUri();
                            try {
                                // Lưu ảnh vào file
                                saveScannedImage(imageUri);
                                // Hiển thị ảnh
                                Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                                imageView.setImageBitmap(imageBitmap);
                                extractButton.setVisibility(View.VISIBLE);
                            } catch (IOException e) {
                                Toast.makeText(this, "Error saving scanned image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void startDocumentScanner() {
        documentScanner.getStartScanIntent(this)
                .addOnSuccessListener(intentSender ->
                        scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to start scanner: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveScannedImage(Uri imageUri) throws IOException {
        // Tạo file để lưu ảnh
        File photoFile = createImageFile();

        // Copy dữ liệu từ URI sang file
        try (FileOutputStream outputStream = new FileOutputStream(photoFile)) {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
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

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.prm_ai.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDocumentScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            imageView.setImageBitmap(imageBitmap);
            extractButton.setVisibility(View.VISIBLE);
        }
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