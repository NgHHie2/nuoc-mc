package com.example.learnservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import com.example.learnservice.client.AccountClient;

import static org.mockito.Mockito.mock;

/**
 * Configuration cho test environment
 * Mock các external dependencies để test độc lập
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock KafkaTemplate để không cần Kafka server khi test
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> mockKafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    /**
     * Mock AccountClient để không cần account-service khi test
     */
    @Bean
    @Primary
    public AccountClient mockAccountClient() {
        return mock(AccountClient.class);
    }

    /**
     * Mock RestTemplate nếu cần
     */
    @Bean
    @Primary
    public RestTemplate mockRestTemplate() {
        return mock(RestTemplate.class);
    }
}