package com.example.learnservice.dto;

import lombok.Data;

@Data
public class ApiResponse {
    private Boolean success;
    private String message;
}