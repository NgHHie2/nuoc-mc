package com.example.statsservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.statsservice.model.AccountStats;
import com.example.statsservice.repository.AccountStatsRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatsService {
    
    @Autowired
    private AccountStatsRepository statsRepository;
    
    public void updateAccountCreatedStats() {
        AccountStats stats = getCurrentStats();
        stats.setTotalAccounts(stats.getTotalAccounts() + 1);
        stats.setLastUpdated(LocalDateTime.now());
        statsRepository.save(stats);
        System.out.println("Stats updated - Total accounts: " + stats.getTotalAccounts());
    }
    
    public void updateAccountDeletedStats() {
        AccountStats stats = getCurrentStats();
        stats.setTotalAccounts(Math.max(0, stats.getTotalAccounts() - 1)); 
        stats.setLastUpdated(LocalDateTime.now());
        statsRepository.save(stats);
        System.out.println("Stats updated - Total accounts: " + stats.getTotalAccounts());
    }
    
    public AccountStats getCurrentStats() {
        List<AccountStats> statsList = statsRepository.findAll();
        if (statsList.isEmpty()) {
            AccountStats newStats = new AccountStats();
            newStats.setTotalAccounts(0);
            newStats.setLastUpdated(LocalDateTime.now());
            return statsRepository.save(newStats);
        }
        return statsList.get(0);
    }
}