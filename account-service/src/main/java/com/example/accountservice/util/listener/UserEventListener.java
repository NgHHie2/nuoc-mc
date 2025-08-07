package com.example.accountservice.util.listener;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.example.accountservice.repository.RedisTokenRepository;
import com.example.accountservice.util.listener.event.UserDeletedEvent;
import com.example.accountservice.util.listener.event.UserRegisteredEvent;
import com.example.accountservice.util.listener.event.UserUpdatedEvent;
import com.example.accountservice.kafka.KafkaProducer;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.model.RedisTokenInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserEventListener {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("sending kafka event: account-created");
        try {
            kafkaProducer.sendAccount("account-created", event.getAccount());
            log.info("Kafka sent for: {}", event.getAccount().getUsername());
        } catch (Exception e) {
            log.error("Kafka failed: {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeleted(UserDeletedEvent event) {
        log.info("sending kafka event: account-deleted");
        try {
            redisTokenRepository.deleteById(event.getAccount().getId());
            kafkaProducer.sendAccount("account-deleted", event.getAccount());
            log.info("Kafka sent for: {}", event.getAccount().getUsername());
        } catch (Exception e) {
            log.error("Kafka failed: {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("sending redis event: account-updated");
        try {
            Optional<RedisTokenInfo> redisTokenInfo = redisTokenRepository.findById(event.getAccount().getId());
            if (redisTokenInfo.isPresent()) {
                RedisTokenInfo newRedisTokenInfo = redisTokenInfo.get();
                newRedisTokenInfo.setRole(event.getAccount().getRole());
                newRedisTokenInfo.setPositions(event.getAccount().getAccountPositions().stream()
                        .map(ap -> ap.getPosition().getId()).collect(Collectors.toList()));
                redisTokenRepository.save(newRedisTokenInfo);
            }
            log.info("Redis sent for: {}", event.getAccount().getUsername());
        } catch (Exception e) {
            log.error("Redis failed: {}", e.getMessage());
        }
    }
}