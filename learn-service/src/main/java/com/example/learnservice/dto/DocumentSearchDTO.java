package com.example.learnservice.dto;

import com.example.learnservice.enums.DocumentFormat;
import lombok.Data;

import java.util.List;

@Data
public class DocumentSearchDTO {
    private String keyword;
    private DocumentFormat format;
    private List<Long> positionIds; // Đổi từ catalogIds thành positionIds
    private List<String> searchFields;
}
