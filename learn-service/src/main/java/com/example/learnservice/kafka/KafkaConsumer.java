package com.example.learnservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.learnservice.dto.AccountDTO;
import com.example.learnservice.service.KafkaService;

@Service
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    @Autowired
    private KafkaService kafkaService;

    @KafkaListener(topics = "account-updated", groupId = "learn-account-group", containerFactory = "accountKafkaListenerContainerFactory")
    public void handleAccountUpdated(AccountDTO account) {
        logger.info("Received account updated event for accountId: {}, role: {}",
                account.getId(), account.getRole());

        try {
            kafkaService.handleAccountUpdated(account);
            logger.info("Successfully processed account update for accountId: {}", account.getId());
        } catch (Exception e) {
            logger.error("Error processing account update for accountId: {}", account.getId(), e);
        }
    }

    @KafkaListener(topics = "account-deleted", groupId = "learn-account-group", containerFactory = "accountKafkaListenerContainerFactory")
    public void handleAccountDeleted(AccountDTO account) {
        logger.info("Received account deleted event for accountId: {}", account.getId());

        try {
            kafkaService.handleAccountDeleted(account.getId());
            logger.info("Successfully processed account deletion for accountId: {}", account.getId());
        } catch (Exception e) {
            logger.error("Error processing account deletion for accountId: {}", account.getId(), e);
        }
    }

}