package com.example.prm_ai;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "UserAuth.db";
    // ✅ Phiên bản cơ sở dữ liệu phải là 7
    public static final int DATABASE_VERSION = 7;

    // Bảng Users
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // Bảng Lịch sử quét
    public static final String TABLE_SCAN_HISTORY = "scanHistory";
    public static final String COLUMN_HISTORY_ID = "id";
    public static final String COLUMN_HISTORY_USER_ID = "user_id";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_ORIGINAL_TEXT = "original_text";
    public static final String COLUMN_SUMMARY_TEXT = "summary_text";
    public static final String COLUMN_TRANSLATED_SUMMARY = "translated_summary"; // Thêm cột bản dịch
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_QUIZ_JSON = "quiz_data_json";
    public static final String COLUMN_QUIZ_SCORE = "quiz_score";
    public static final String COLUMN_LAST_USER_ANSWERS_JSON = "last_user_answers_json";
    public static final String COLUMN_LANGUAGE_CODE = "language_code"; // Thêm cột mã ngôn ngữ

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersSql = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUsersSql);

        String createHistorySql = "CREATE TABLE " + TABLE_SCAN_HISTORY + " (" +
                COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_HISTORY_USER_ID + " INTEGER, " +
                COLUMN_IMAGE_PATH + " TEXT, " +
                COLUMN_ORIGINAL_TEXT + " TEXT, " +
                COLUMN_SUMMARY_TEXT + " TEXT, " +
                COLUMN_TRANSLATED_SUMMARY + " TEXT, " +
                COLUMN_QUIZ_JSON + " TEXT, " +
                COLUMN_QUIZ_SCORE + " INTEGER, " +
                COLUMN_LAST_USER_ANSWERS_JSON + " TEXT, " +
                COLUMN_LANGUAGE_CODE + " TEXT, " +
                COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(" + COLUMN_HISTORY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "));";
        db.execSQL(createHistorySql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
             db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_ORIGINAL_TEXT + " TEXT;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_IMAGE_PATH + " TEXT;");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_QUIZ_JSON + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_QUIZ_SCORE + " INTEGER;");
        }
        if (oldVersion < 5) {
             db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_LAST_USER_ANSWERS_JSON + " TEXT;");
        }
        if (oldVersion < 6) {
             db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_LANGUAGE_CODE + " TEXT;");
        }
        if (oldVersion < 7) {
             db.execSQL("ALTER TABLE " + TABLE_SCAN_HISTORY + " ADD COLUMN " + COLUMN_TRANSLATED_SUMMARY + " TEXT;");
        }
    }

    // --- Các phương thức cho User ---
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        return db.insert(TABLE_USERS, null, values) != -1;
    }

    public Cursor checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, new String[]{COLUMN_USER_ID, COLUMN_USERNAME}, COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?", new String[]{username, password}, null, null, null);
    }
    
    public Cursor getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, new String[]{COLUMN_USER_ID, COLUMN_USERNAME}, COLUMN_USERNAME + " = ?", new String[]{username}, null, null, null);
    }

    // --- Các phương thức cho Lịch sử ---
    // ✅ Sửa lại phương thức để chấp nhận 6 tham số
    public long addScanHistory(int userId, String imagePath, String originalText, String summaryText, String translatedSummary, String languageCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HISTORY_USER_ID, userId);
        values.put(COLUMN_IMAGE_PATH, imagePath);
        values.put(COLUMN_ORIGINAL_TEXT, originalText);
        values.put(COLUMN_SUMMARY_TEXT, summaryText);
        values.put(COLUMN_TRANSLATED_SUMMARY, translatedSummary);
        values.put(COLUMN_LANGUAGE_CODE, languageCode);
        return db.insert(TABLE_SCAN_HISTORY, null, values);
    }

    public Cursor getScanHistory(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SCAN_HISTORY, null, COLUMN_HISTORY_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, COLUMN_TIMESTAMP + " DESC");
    }

    public Cursor getHistoryItem(long historyId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SCAN_HISTORY, null, COLUMN_HISTORY_ID + " = ?",
                new String[]{String.valueOf(historyId)}, null, null, null);
    }

    public void updateQuizData(long historyId, String quizJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUIZ_JSON, quizJson);
        db.update(TABLE_SCAN_HISTORY, values, COLUMN_HISTORY_ID + " = ?", new String[]{String.valueOf(historyId)});
    }

    public void updateQuizAttempt(long historyId, int score, String userAnswersJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUIZ_SCORE, score);
        values.put(COLUMN_LAST_USER_ANSWERS_JSON, userAnswersJson);
        db.update(TABLE_SCAN_HISTORY, values, COLUMN_HISTORY_ID + " = ?", new String[]{String.valueOf(historyId)});
    }
}
