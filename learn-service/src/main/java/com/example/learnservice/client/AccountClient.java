package com.example.learnservice.client;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.learnservice.dto.AccountDTO;
import com.example.learnservice.enums.Role;

import reactor.core.publisher.Mono;

@Component
public class AccountClient {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${account.service.url}")
    private String accountserviceUrl;

    public List<AccountDTO> getAccountsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String idsParam = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return webClientBuilder.build()
                .get()
                .uri(accountserviceUrl + "/bulk?ids={ids}", idsParam)
                .header("X-User-Role", Role.ADMIN.toString()) // 👈 gửi role trực tiếp
                .retrieve()
                .bodyToFlux(AccountDTO.class)
                .collectList()
                .block();
    }

}