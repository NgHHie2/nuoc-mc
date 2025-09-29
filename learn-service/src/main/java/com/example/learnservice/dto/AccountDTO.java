package com.example.learnservice.dto;

import com.example.learnservice.enums.Role;

import lombok.Data;

@Data
public class AccountDTO {
    private Long id;
    private Role role;
}
