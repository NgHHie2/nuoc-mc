package com.example.learnservice.util.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.learnservice.kafka.KafkaProducer;
import com.example.learnservice.util.listener.event.DocumentCreatedEvent;
import com.example.learnservice.util.listener.event.DocumentDeletedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DocumentEventListener {

    @Autowired
    private KafkaProducer documentProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentDeleted(DocumentDeletedEvent event) {
        log.info("sending kafka event: document-deleted");
        try {
            documentProducer.sendDocument("document-deleted", event.getDocument());
            log.info("Kafka sent for: {}", event.getDocument().getId());
        } catch (Exception e) {
            log.error("Kafka failed: {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentCreated(DocumentCreatedEvent event) {
        log.info("sending kafka event: document-created");
        try {
            documentProducer.sendDocument("document-created", event.getDocument());
            log.info("Kafka sent for: {}", event.getDocument().getId());
        } catch (Exception e) {
            log.error("Kafka failed: {}", e.getMessage());
        }
    }

}
