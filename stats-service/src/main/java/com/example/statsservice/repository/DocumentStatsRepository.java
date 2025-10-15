package com.example.statsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.statsservice.model.DocumentStats;

@Repository
public interface DocumentStatsRepository extends JpaRepository<DocumentStats, Integer> {
}