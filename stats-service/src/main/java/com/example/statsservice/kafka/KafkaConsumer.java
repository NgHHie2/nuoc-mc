package com.example.statsservice.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.statsservice.model.Account;
import com.example.statsservice.model.Document;
import com.example.statsservice.service.StatsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    @Autowired
    private StatsService statsService;

    // ============ ACCOUNT EVENTS ============

    @KafkaListener(topics = "account-created", groupId = "stats-group", containerFactory = "accountKafkaListenerContainerFactory")
    public void handleAccountCreated(Account account) {
        if (account.getRole() == null)
            return;
        logger.info("Received account-created event for user: {}", account.getUsername());
        statsService.saveAccountEvent(account);
    }

    @KafkaListener(topics = "account-deleted", groupId = "stats-group", containerFactory = "accountKafkaListenerContainerFactory")
    public void handleAccountDeleted(Account account) {
        logger.info("Received account-deleted event for user: {}", account.getUsername());
        statsService.deleteAccountEvent(account.getId());
    }

    @KafkaListener(topics = "account-updated", groupId = "stats-group", containerFactory = "accountKafkaListenerContainerFactory")
    public void handleAccountUpdated(Account account) {
        logger.info("Received account-updated event for user: {}", account.getUsername());
        statsService.saveAccountEvent(account);
    }

    // ============ DOCUMENT EVENTS ============

    @KafkaListener(topics = "document-created", groupId = "stats-group", containerFactory = "documentKafkaListenerContainerFactory")
    public void handleDocumentCreated(Document document) {
        logger.info("Received document-created event for document ID: {}", document.getId());
        statsService.saveDocumentEvent(document);
    }

    @KafkaListener(topics = "document-deleted", groupId = "stats-group", containerFactory = "documentKafkaListenerContainerFactory")
    public void handleDocumentDeleted(Document document) {
        logger.info("Received document-deleted event for document ID: {}", document.getId());
        statsService.deleteDocumentEvent(document.getId());
    }
}