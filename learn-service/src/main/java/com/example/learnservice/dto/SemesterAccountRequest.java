package com.example.learnservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SemesterAccountRequest {
    @NotNull(message = "Account assignments are required")
    @NotEmpty(message = "Account assignments cannot be empty")
    private List<AccountPositionAssignment> accountAssignments;

    @Data
    public static class AccountPositionAssignment {
        @NotNull(message = "Account ID is required")
        private Long accountId;

        @NotNull(message = "Position ID is required")
        private Long positionId;
    }
}