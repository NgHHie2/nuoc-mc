package com.example.learnservice.dto;

import lombok.Data;

@Data
public class EndTestResponse {
    private Boolean success;
    private Long resultId;
    private Float score;
    private String message;
}
