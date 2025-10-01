package com.example.learnservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SemesterUpdateRequest {
    @NotBlank
    @Size(max = 200, message = "Semester name must not exceed 200 characters")
    private String name;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    // private List<Long> teacherIds;
}