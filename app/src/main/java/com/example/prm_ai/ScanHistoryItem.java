package com.example.prm_ai;

public class ScanHistoryItem {
    private long id;
    private String imagePath;
    private String originalText;
    private String summaryText;
    private String timestamp;
    private String quizJson; // ✅
    private Integer quizScore;  // ✅ Use Integer to allow null

    public ScanHistoryItem(long id, String imagePath, String originalText, String summaryText, String timestamp, String quizJson, Integer quizScore) {
        this.id = id;
        this.imagePath = imagePath;
        this.originalText = originalText;
        this.summaryText = summaryText;
        this.timestamp = timestamp;
        this.quizJson = quizJson;
        this.quizScore = quizScore;
    }

    // Getters
    public long getId() { return id; }
    public String getImagePath() { return imagePath; }
    public String getOriginalText() { return originalText; }
    public String getSummaryText() { return summaryText; }
    public String getTimestamp() { return timestamp; }
    public String getQuizJson() { return quizJson; }
    public Integer getQuizScore() { return quizScore; }
}
