package com.example.accountservice.model;

import java.time.LocalDateTime;

import com.example.accountservice.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username; // Sẽ được tạo tự động

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password; // Mặc định là 123456Aa@

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDateTime birthDay;

    private String phoneNumber;

    @Email(message = "Email should be valid")
    private String email; // Có thể để trống

    @NotBlank(message = "CCCD is required")
    @Size(min = 9, max = 12, message = "CCCD must be between 9 and 12 characters")
    private String cccd; // Căn cước công dân - UNIQUE

    private String note; // Ghi chú

    private Integer visible = 1; // 1: hiển thị, 0: đã xóa (soft delete)

    private Role role;
}