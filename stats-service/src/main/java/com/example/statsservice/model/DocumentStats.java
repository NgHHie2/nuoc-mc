package com.example.statsservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class DocumentStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer totalDocuments;
    private Integer totalPdf;
    private Integer totalVideo;

    private LocalDateTime lastUpdated;
}