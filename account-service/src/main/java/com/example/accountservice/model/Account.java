package com.example.accountservice.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.accountservice.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // Sẽ được tạo tự động

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountPosition> accountPositions;

    @JsonIgnore
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
