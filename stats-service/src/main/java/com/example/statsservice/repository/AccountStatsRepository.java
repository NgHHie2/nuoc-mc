package com.example.statsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.statsservice.model.AccountStats;

@Repository
public interface AccountStatsRepository extends JpaRepository<AccountStats, Integer> {
}
