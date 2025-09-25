package com.example.learnservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SemesterDocumentRequest {
    @NotNull(message = "Document codes are required")
    @NotEmpty(message = "Document codes cannot be empty")
    private List<String> documentCodes;
}