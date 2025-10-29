package com.example.prm_ai;

import java.io.Serializable;

public class QuizResultItem implements Serializable {
    private QuizQuestion question;
    private String userAnswer;

    public QuizResultItem(QuizQuestion question, String userAnswer) {
        this.question = question;
        this.userAnswer = userAnswer;
    }

    public QuizQuestion getQuestion() {
        return question;
    }

    public String getUserAnswer() {
        return userAnswer;
    }
}
