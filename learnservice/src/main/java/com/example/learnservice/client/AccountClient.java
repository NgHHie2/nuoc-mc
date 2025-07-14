package com.example.learnservice.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.learnservice.model.Account;

import reactor.core.publisher.Mono;

@Component
public class AccountClient {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${account.service.url}") 
    private String accountserviceUrl;

    public boolean checkAccountExists(Account account) {
        try {
            return webClientBuilder.build().get()
                    .uri(accountserviceUrl + "/" + account.getId())
                    .retrieve()
                    .onStatus(status -> status.equals(HttpStatus.NOT_FOUND),
                            response -> Mono.just(new RuntimeException("Tài khoản không tồn tại")))
                    .bodyToMono(Object.class)
                    .map(response -> true)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                            return Mono.just(false);
                        }
                        return Mono.error(ex);
                    })
                    .block();
        } catch (Exception e) {
            return false;
        }
    }
}