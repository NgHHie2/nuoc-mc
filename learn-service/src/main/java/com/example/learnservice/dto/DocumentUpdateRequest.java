package com.example.learnservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DocumentUpdateRequest {

    @Size(max = 255, message = "Document name must not exceed 255 characters")
    private String name;

    @Size(max = 100, message = "Document number must not exceed 100 characters")
    private String documentNumber;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private List<String> tags;
}