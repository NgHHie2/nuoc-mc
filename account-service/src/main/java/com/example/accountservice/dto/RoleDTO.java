package com.example.accountservice.dto;

import com.example.accountservice.enums.Role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleDTO {
    private Long accountId;
    private String role;
}
