package com.example.learnservice.dto;

import java.util.List;

import lombok.Data;

@Data
public class QuestionPositionRequest {
    private Long positionId;
    private List<Long> questionIds;
}
