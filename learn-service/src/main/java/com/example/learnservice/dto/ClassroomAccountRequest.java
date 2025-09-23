package com.example.learnservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ClassroomAccountRequest {
    @NotNull(message = "Account IDs are required")
    @NotEmpty(message = "Account IDs cannot be empty")
    private List<Long> accountIds;
}