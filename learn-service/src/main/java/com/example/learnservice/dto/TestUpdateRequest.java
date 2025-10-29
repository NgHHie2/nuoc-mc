package com.example.learnservice.dto;

import lombok.Data;

@Data
public class TestUpdateRequest {
    private String testName;
    private Boolean visible;
}
