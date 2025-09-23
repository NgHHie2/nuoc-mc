package com.example.learnservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassroomCreateRequest {
    @NotBlank(message = "Classroom name is required")
    @Size(max = 200, message = "Classroom name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Semester ID is required")
    private Long semesterId;
}