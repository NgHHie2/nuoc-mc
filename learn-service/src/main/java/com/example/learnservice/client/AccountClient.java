package com.example.learnservice.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class AccountClient {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${account.service.url}")
    private String accountserviceUrl;

}