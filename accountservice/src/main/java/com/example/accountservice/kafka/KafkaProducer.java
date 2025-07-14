package com.example.accountservice.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.accountservice.model.Account;

@Service
public class KafkaProducer {
    @Autowired
    private KafkaTemplate<String, Account> kafkaTemplate;

    public void sendAccount(String topic, Account account) {
        kafkaTemplate.send(topic, account);
    }
}
