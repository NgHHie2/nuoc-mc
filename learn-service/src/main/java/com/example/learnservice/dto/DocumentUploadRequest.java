package com.example.learnservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadRequest {
    private MultipartFile file;

    // @NotBlank(message = "Document number is required")
    @Size(max = 50, message = "Document number must not exceed 50 characters")
    private String documentNumber; // Mã tài liệu do người dùng nhập

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description; // Mô tả tài liệu

    private List<String> tags; // Danh sách tên tags

    private List<Long> catalogs; // Danh sách position IDs cho catalogs
}