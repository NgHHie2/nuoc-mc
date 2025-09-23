package com.example.learnservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ClassroomDocumentRequest {
    @NotNull(message = "Document codes are required")
    @NotEmpty(message = "Document codes cannot be empty")
    private List<String> documentCodes;
}