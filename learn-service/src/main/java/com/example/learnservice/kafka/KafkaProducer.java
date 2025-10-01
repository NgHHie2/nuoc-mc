package com.example.learnservice.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.learnservice.dto.DocumentDTO;

@Service
public class KafkaProducer {

    @Autowired
    private KafkaTemplate<String, DocumentDTO> documentTemplate;

    public void sendDocument(String topic, DocumentDTO document) {
        documentTemplate.send(topic, document);
    }
}
