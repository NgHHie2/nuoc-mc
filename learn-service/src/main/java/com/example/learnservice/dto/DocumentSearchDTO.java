package com.example.learnservice.dto;

import com.example.learnservice.enums.DocumentFormat;
import lombok.Data;

import java.util.List;

@Data
public class DocumentSearchDTO {
    private String keyword;
    private DocumentFormat format;
    private List<Long> catalogIds; // Tương tự positionIds bên account
    private List<String> searchFields;
}