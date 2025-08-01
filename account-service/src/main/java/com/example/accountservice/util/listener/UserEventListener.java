package com.example.accountservice.util.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.example.accountservice.util.listener.event.UserRegisteredEvent;
import com.example.accountservice.kafka.KafkaProducer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserEventListener {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        try {
            kafkaProducer.sendAccount("account-created", event.getAccount());
            log.info("Kafka sent for: {}", event.getAccount().getUsername());
        } catch (Exception e) {
            log.error("Kafka failed: {}", e.getMessage());
        }
    }
}