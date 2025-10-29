package com.example.learnservice.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class TestCreateRequest {
    @NotNull(message = "Test name is required")
    private String testName;
    @NotNull(message = "Position ID is required")
    private Long positionId;
}
