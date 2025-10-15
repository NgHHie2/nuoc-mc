package com.example.statsservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AccountStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer totalAccounts;
    private Integer totalStudents;
    private Integer totalTeachers;
    private Integer totalAdmins;

    private LocalDateTime lastUpdated;
}