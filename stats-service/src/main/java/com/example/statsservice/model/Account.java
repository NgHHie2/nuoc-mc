package com.example.statsservice.model;

import java.time.LocalDateTime;

import com.example.statsservice.enums.Role;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
@Entity
public class Account {
    private Integer id;
    private String username;

    @Enumerated(EnumType.STRING)
    private Role role;
}