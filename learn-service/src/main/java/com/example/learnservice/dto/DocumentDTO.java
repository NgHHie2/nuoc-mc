package com.example.learnservice.dto;

import com.example.learnservice.enums.DocumentFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private DocumentFormat format;
}
