package com.example.learnservice.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SemesterTestUpdateRequest {
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer minutes;
}
