package com.example.prm_ai;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "UserAuth.db";
    public static final int DATABASE_VERSION = 3; // ✅ Phiên bản đã tăng

    // Bảng Users
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // Bảng Scan History
    public static final String TABLE_SCAN_HISTORY = "scanHistory";
    public static final String COLUMN_HISTORY_ID = "id";
    public static final String COLUMN_HISTORY_USER_ID = "user_id";
    public static final String COLUMN_IMAGE_PATH = "image_path"; // ✅ Cột mới
    public static final String COLUMN_ORIGINAL_TEXT = "original_text";
    public static final String COLUMN_SUMMARY_TEXT = "summary_text";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " (...)"; // Giữ nguyên

    private static final String CREATE_TABLE_SCAN_HISTORY = "CREATE TABLE " + TABLE_SCAN_HISTORY + " (" +
            COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_HISTORY_USER_ID + " INTEGER, " +
            COLUMN_IMAGE_PATH + " TEXT, " + // ✅ Thêm cột mới vào câu lệnh
            COLUMN_ORIGINAL_TEXT + " TEXT, " +
            COLUMN_SUMMARY_TEXT + " TEXT, " +
            COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(" + COLUMN_HISTORY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "));";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tái sử dụng câu lệnh gốc để tránh lỗi
        String createUsersSql = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUsersSql);
        db.execSQL(CREATE_TABLE_SCAN_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Nâng cấp an toàn: chỉ thêm cột nếu chưa tồn tại
            db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_IMAGE_PATH + " TEXT;");
        }
    }

    public boolean addUser(String username, String password) {
        // Giữ nguyên
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public Cursor checkUser(String username, String password) {
        // Giữ nguyên
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID, COLUMN_USERNAME};
        String selection = COLUMN_USERNAME + " = ?" + " AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {username, password};
        return db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
    }

    // ✅ Cập nhật phương thức addScanHistory
    public boolean addScanHistory(int userId, String imagePath, String originalText, String summaryText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HISTORY_USER_ID, userId);
        values.put(COLUMN_IMAGE_PATH, imagePath);
        values.put(COLUMN_ORIGINAL_TEXT, originalText);
        values.put(COLUMN_SUMMARY_TEXT, summaryText);
        long result = db.insert(TABLE_SCAN_HISTORY, null, values);
        return result != -1;
    }

    // ✅ Thêm phương thức getScanHistory
    public Cursor getScanHistory(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SCAN_HISTORY,
                null, // Lấy tất cả các cột
                COLUMN_HISTORY_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, 
                COLUMN_TIMESTAMP + " DESC"); // Sắp xếp theo thứ tự mới nhất
    }
}
