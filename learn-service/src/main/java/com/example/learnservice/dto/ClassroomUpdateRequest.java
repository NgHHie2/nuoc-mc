package com.example.learnservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassroomUpdateRequest {
    @NotBlank(message = "Classroom name is required")
    @Size(max = 200, message = "Classroom name must not exceed 200 characters")
    private String name;

    private Long semesterId; // Optional - cho phép chuyển classroom sang semester khác
}