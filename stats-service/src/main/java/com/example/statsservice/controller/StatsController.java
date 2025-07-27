package com.example.statsservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.statsservice.model.AccountStats;
import com.example.statsservice.service.StatsService;

@RestController
@RequestMapping("/stats")
public class StatsController {
    
    @Autowired
    private StatsService statsService;
    
    @GetMapping("/accounts")
    public AccountStats getAccountStats() {
        return statsService.getCurrentStats();
    }
}
