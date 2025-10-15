package com.example.statsservice.model;

import java.time.LocalDateTime;

import com.example.statsservice.enums.DocumentFormat;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
public class Document {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private DocumentFormat format;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}