package com.example.statsservice.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.statsservice.model.Account;
import com.example.statsservice.service.StatsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KafkaConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    
    @Autowired
    private StatsService statsService;

    @KafkaListener(topics = "account-created", groupId = "stats-group")
    public void handleAccountCreated(Account account) {
        logger.info("Received account created event for user: {}", account.getUsername());
        statsService.updateAccountCreatedStats();
    }

    @KafkaListener(topics = "account-deleted", groupId = "stats-group")
    public void handleAccountDeleted(Account account) {
        logger.info("Received account deleted event for user: {}", account.getUsername());
        statsService.updateAccountDeletedStats();
    }
}