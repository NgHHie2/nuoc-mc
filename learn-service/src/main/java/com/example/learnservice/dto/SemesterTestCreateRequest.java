package com.example.learnservice.dto;

import java.time.LocalDateTime;

import com.example.learnservice.enums.TestType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SemesterTestCreateRequest {
    @NotNull(message = "Test name is required")
    private String testName;
    @NotNull(message = "Position is required")
    private Long positionId;
    @NotNull(message = "Test type is required")
    private TestType type;
    
    private Integer minutes;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
