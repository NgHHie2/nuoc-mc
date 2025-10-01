package com.example.learnservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class SemesterDetailDTO {
    private Long id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalAccounts;
    private LocalDateTime createdAt;
    private Long createdBy;
}