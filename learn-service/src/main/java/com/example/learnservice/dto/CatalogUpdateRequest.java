package com.example.learnservice.dto;

import java.util.List;

import lombok.Data;

@Data
public class CatalogUpdateRequest {
    private List<Long> positions;
}