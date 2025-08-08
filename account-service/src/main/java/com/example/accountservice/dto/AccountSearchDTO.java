package com.example.accountservice.dto;

import com.example.accountservice.enums.Role;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AccountSearchDTO {

    // Keyword search (tìm đồng thời: username, name, phone, email, cccd, birthday)
    private String keyword;

    // Role filter (single choice)
    private Role role;
    private List<Long> positionIds;
}