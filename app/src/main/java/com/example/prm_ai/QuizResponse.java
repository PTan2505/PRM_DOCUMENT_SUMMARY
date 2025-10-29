package com.example.prm_ai;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Lớp này đại diện cho toàn bộ đối tượng JSON trả về
public class QuizResponse {

    @SerializedName("questions")
    private List<QuizQuestion> questions;

    public List<QuizQuestion> getQuestions() {
        return questions;
    }
}
