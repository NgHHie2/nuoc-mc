package com.example.statsservice.model;

import java.time.LocalDateTime;

import com.example.statsservice.enums.Role;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
public class Account {
    @Id
    private Long id;

    private String username;

    @Enumerated(EnumType.STRING)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}