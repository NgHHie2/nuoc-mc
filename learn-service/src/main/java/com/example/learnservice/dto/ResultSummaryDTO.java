package com.example.learnservice.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ResultSummaryDTO {
    private Long id;
    private Long studentId;
    private LocalDateTime startDateTime;
    private LocalDateTime submitDateTime;
    private Float score;
}