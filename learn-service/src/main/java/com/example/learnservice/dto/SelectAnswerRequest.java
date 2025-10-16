package com.example.learnservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectAnswerRequest {
    @NotNull(message = "Answer indices is required")
    private List<Integer> answerIndices;
}