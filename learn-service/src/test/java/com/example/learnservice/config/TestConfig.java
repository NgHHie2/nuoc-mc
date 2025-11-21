package com.example.learnservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.example.learnservice.client.AccountClient;
import com.example.learnservice.controller.WebSocketController;
import com.example.learnservice.dto.AccountDTO;
import com.example.learnservice.enums.Role;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Configuration - Mock all external dependencies
 * 
 * Rules:
 * - AccountID < 100: ADMIN
 * - AccountID 100-199: STUDENT
 * - AccountID >= 200: TEACHER
 */
@TestConfiguration
public class TestConfig {

    /**
     * Mock KafkaTemplate to prevent real Kafka connections
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> mockKafkaTemplate() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> mock = mock(KafkaTemplate.class);

        // Mock partitionsFor - called by CommandLineRunner warmup
        when(mock.partitionsFor(anyString())).thenReturn(Collections.emptyList());

        // Mock send methods
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(mock.send(anyString(), any())).thenReturn(future);
        when(mock.send(anyString(), anyString(), any())).thenReturn(future);

        return mock;
    }

    /**
     * Mock AccountClient - No real account-service connection needed
     * 
     * Mock behavior:
     * - getAccountsByIds: Returns list of AccountDTOs with roles based on ID
     * - getAccountById: Returns single AccountDTO with role based on ID
     */
    @Bean
    @Primary
    public AccountClient mockAccountClient() {
        AccountClient mock = mock(AccountClient.class);

        // Mock getAccountsByIds
        when(mock.getAccountsByIds(anyList())).thenAnswer(invocation -> {
            List<Long> accountIds = invocation.getArgument(0);

            return accountIds.stream()
                    .map(id -> createMockAccountDTO(id))
                    .collect(Collectors.toList());
        });

        return mock;
    }

    /**
     * Helper method to create mock AccountDTO with role based on ID
     * 
     * @param id Account ID
     * @return AccountDTO with appropriate role
     */
    private AccountDTO createMockAccountDTO(Long id) {
        AccountDTO dto = new AccountDTO();
        dto.setId(id);

        // Assign role based on ID range
        if (id < 100) {
            dto.setRole(Role.ADMIN);
        } else if (id < 200) {
            dto.setRole(Role.STUDENT);
        } else {
            dto.setRole(Role.TEACHER);
        }

        return dto;
    }
}