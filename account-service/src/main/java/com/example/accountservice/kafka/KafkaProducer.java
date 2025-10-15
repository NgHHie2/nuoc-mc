package com.example.accountservice.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.accountservice.dto.AccountDTO;
import com.example.accountservice.model.Account;

@Service
public class KafkaProducer {
    @Autowired
    private KafkaTemplate<String, AccountDTO> kafkaTemplate;

    public void sendAccount(String topic, AccountDTO account) {
        kafkaTemplate.send(topic, account);
    }
}
