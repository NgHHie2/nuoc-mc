package com.example.learnservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionCreateRequest {
    @NotNull(message = "Question is required")
    @NotEmpty(message = "Question cannot be empty")
    private String text;
    // @NotNull(message = "Question type is required")
    // @NotEmpty(message = "Question type cannot be empty")
    // private QuestionType type;
    @NotNull(message = "Position ID is required")
    private Long positionId;

    private List<AnswerDTO> answers;
}
