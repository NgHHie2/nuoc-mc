package com.example.learnservice.dto;

import java.time.LocalDateTime;

import com.drew.lang.annotations.NotNull;
import com.example.learnservice.enums.TestType;

import lombok.Data;

@Data
public class SemesterTestAssignRequest {
    private String name;
    @NotNull
    private LocalDateTime startDate;
    @NotNull
    private LocalDateTime endDate;
    @NotNull
    private Integer minutes;
    @NotNull
    private TestType type;
}
