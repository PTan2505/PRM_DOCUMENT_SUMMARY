package com.example.prm_ai;

public class ScanHistoryItem {
    private long id;
    private String imagePath;
    private String originalText;
    private String summaryText;
    private String timestamp;

    public ScanHistoryItem(long id, String imagePath, String originalText, String summaryText, String timestamp) {
        this.id = id;
        this.imagePath = imagePath;
        this.originalText = originalText;
        this.summaryText = summaryText;
        this.timestamp = timestamp;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
