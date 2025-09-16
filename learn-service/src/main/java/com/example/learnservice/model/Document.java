package com.example.learnservice.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.learnservice.enums.DocumentFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    private String documentNumber;

    @Enumerated(EnumType.STRING)
    private DocumentFormat format; // PDF hoặc VIDEO

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long size; // Kích thước (dung lượng) tính bằng bytes
    private Integer pages; // Số trang (cho PDF)
    private Integer minutes; // Số phút (cho video)
    // private String filePath; // Đường dẫn vị trí lưu file
    // private String previewPath; // Đường dẫn vị trí lưu ảnh preview

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Catalog> catalogs;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Tag> tags;

    @JsonIgnore
    @Column(updatable = false)
    private Long createdBy; // ID của account tạo

    @JsonIgnore
    private Long updatedBy; // ID của account cập nhật

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}