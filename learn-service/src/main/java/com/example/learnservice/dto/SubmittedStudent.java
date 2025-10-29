package com.example.learnservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmittedStudent {
    private Long userId;
    private Float score;
    private Long resultId;
}
