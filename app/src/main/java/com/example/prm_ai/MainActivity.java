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

    private static final int REQUEST_DOCUMENT_SCAN = 1;
    private static final int REQUEST_GALLERY_PICK = 2;

    private ImageView imageView;
    private Button extractButton;
    private View imageViewPlaceholder;
    private String currentPhotoPath;
    private int userId;
    private GmsDocumentScanner documentScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userId = getIntent().getIntExtra("USER_ID", -1);

        imageView = findViewById(R.id.imageView);
        extractButton = findViewById(R.id.extractButton);
        imageViewPlaceholder = findViewById(R.id.imageViewPlaceholder);
        Button captureButton = findViewById(R.id.captureButton);
        Button selectFromGalleryButton = findViewById(R.id.selectFromGalleryButton);

        // Khởi tạo ML Kit Document Scanner
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();

        documentScanner = GmsDocumentScanning.getClient(options);

        captureButton.setOnClickListener(v -> {
            // Sử dụng Document Scanner thay vì camera truyền thống
            documentScanner.getStartScanIntent(this)
                    .addOnSuccessListener(intentSender -> {
                        try {
                            startIntentSenderForResult(intentSender, REQUEST_DOCUMENT_SCAN, null, 0, 0, 0);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error starting scanner", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to initialize scanner", Toast.LENGTH_SHORT).show();
                    });
        });

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_DOCUMENT_SCAN) {
                // Xử lý kết quả từ Document Scanner
                if (data != null) {
                    GmsDocumentScanningResult result = GmsDocumentScanningResult.fromActivityResultIntent(data);
                    if (result != null && result.getPages() != null && !result.getPages().isEmpty()) {
                        Uri imageUri = result.getPages().get(0).getImageUri();
                        try {
                            // Sao chép ảnh đã scan vào file tạm thời
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
                            imageViewPlaceholder.setVisibility(View.GONE);
                            findViewById(R.id.extractButtonCard).setVisibility(View.VISIBLE);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to process scanned image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else if (requestCode == REQUEST_GALLERY_PICK) {
                // Xử lý ảnh từ thư viện
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
                        imageViewPlaceholder.setVisibility(View.GONE);
                        findViewById(R.id.extractButtonCard).setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                    }
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