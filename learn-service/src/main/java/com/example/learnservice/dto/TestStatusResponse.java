package com.example.learnservice.dto;

import lombok.Data;

@Data
public class TestStatusResponse {
    private Long semesterTestId;
    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED
    private Long resultId;
    private Float score;
}