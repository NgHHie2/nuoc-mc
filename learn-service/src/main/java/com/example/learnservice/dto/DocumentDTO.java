package com.example.learnservice.dto;

import com.example.learnservice.enums.DocumentFormat;

import lombok.Data;

@Data
public class DocumentDTO {
    private Long id;
    private DocumentFormat format;
}
