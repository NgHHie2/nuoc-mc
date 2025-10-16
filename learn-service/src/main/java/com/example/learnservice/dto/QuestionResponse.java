package com.example.learnservice.dto;

import java.util.List;

import lombok.Data;

@Data
public class QuestionResponse {
    private Integer questionIndex;
    private String questionText;
    private List<AnswerOption> answers;
    private List<Integer> selectedAnswers;
    private Boolean flagged;

    @Data
    public static class AnswerOption {
        private Integer answerIndex;
        private String answerText;
    }
}