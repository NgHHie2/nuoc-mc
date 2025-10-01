package com.example.learnservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SemesterTeacherRequest {
    @NotNull(message = "Teacher assignments are required")
    @NotEmpty(message = "Teacher assignments cannot be empty")
    private List<Long> teacherIds;

}