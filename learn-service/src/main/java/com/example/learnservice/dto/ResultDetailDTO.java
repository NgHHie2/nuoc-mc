package com.example.learnservice.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class ResultDetailDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private LocalDateTime startDateTime;
    private LocalDateTime submitDateTime;
    private Float score;
    private JsonNode detailTest;
    private JsonNode studentAnswers;
    private JsonNode trueAnswers; // Chỉ hiển thị cho ADMIN/TEACHER hoặc sau khi submit
}