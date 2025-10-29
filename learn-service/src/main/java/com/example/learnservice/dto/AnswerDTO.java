package com.example.learnservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerDTO {
    @NotNull(message = "Answer is required")
    @NotEmpty(message = "Answer cannot be empty")
    private String text;
    @NotNull(message = "True answer is required")
    private Boolean trueAnswer;
}
