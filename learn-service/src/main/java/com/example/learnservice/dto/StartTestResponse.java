package com.example.learnservice.dto;

import lombok.Data;

@Data
public class StartTestResponse {
    private Boolean success;
    private Long resultId;
    private String message;
    private Integer totalQuestions;
}