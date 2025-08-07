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

    // Position filter (multiple choice - tìm accounts có ít nhất 1 position trong
    // list)
    private List<Long> positionIds;

    // Advanced filters (nếu muốn tìm kiếm chính xác)
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String cccd;

    // Birthday range search
    private LocalDate birthdayFrom;
    private LocalDate birthdayTo;

    // Visibility filter
    private Integer visible = 1; // Default search visible accounts only
}