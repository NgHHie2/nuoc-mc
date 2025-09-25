package com.example.learnservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadRequest {
    private MultipartFile file;

    @Size(max = 50, message = "Document number must not exceed 50 characters")
    private String documentNumber;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private List<String> tags;

    private List<Long> positions; // Đổi từ catalogs thành positions
}