package com.example.learnservice.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.learnservice.enums.DocumentFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Document name is required")
    private String name;

    @Column(unique = true)
    private String code;

    @Column(unique = true)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    private DocumentFormat format;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long size;
    private Integer pages;
    private Integer minutes;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Catalog> catalogs;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Tag> tags;

    @JsonIgnore
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SemesterDocument> semesterDocuments;

    @JsonIgnore
    @Column(updatable = false)
    private Long createdBy;

    @JsonIgnore
    private Long updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}