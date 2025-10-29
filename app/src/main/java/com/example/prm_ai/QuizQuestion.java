package com.example.prm_ai;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

// ✅ Implement Serializable
public class QuizQuestion implements Serializable {

    @SerializedName("question")
    private String question;

    @SerializedName("options")
    private List<String> options;

    @SerializedName("answer")
    private String answer;

    // Getters
    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getAnswer() {
        return answer;
    }
}
