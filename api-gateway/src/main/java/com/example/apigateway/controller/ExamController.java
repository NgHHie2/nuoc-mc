package com.example.apigateway.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.apigateway.model.Account;
import com.example.apigateway.model.Subject;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/subject")
public class ExamController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getAccountDetails(@PathVariable Integer id) {

        WebClient webClient = webClientBuilder.build();

        Mono<Subject> subjectMono = webClient.get()
                .uri("lb://learnservice/subject/{subject_id}", id)
                .retrieve()
                .bodyToMono(Subject.class)
                .doOnError(ex -> System.out.println("Error calling learnservice: " + ex.getMessage()))
                .onErrorReturn(new Subject());

        Mono<Map<String, Object>> resultMono = subjectMono.flatMap(subject -> {
            if (subject.getParticipations() == null || subject.getParticipations().isEmpty()) {
                return Mono.just(createErrorResponse("No participations found for this subject"));
            }

            List<Integer> accountIds = subject.getParticipations().stream()
                    .map(p -> p.getAccount() != null ? p.getAccount().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (accountIds.isEmpty()) {
                return Mono.just(createErrorResponse("No account IDs found in participations"));
            }

            return webClient.post()
                    .uri("lb://accountservice/account/ids")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(accountIds)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Account>>() {
                    })
                    .map(accounts -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("subject", subject);
                        response.put("accounts", accounts);
                        return response;
                    })
                    .doOnError(ex -> System.out.println("Error calling accountservice: " + ex.getMessage()))
                    .onErrorReturn(createErrorResponse("Unable to fetch accounts"));
        });

        return resultMono
                .doOnError(ex -> System.out.println("Final error in composition: " + ex.getMessage()))
                .onErrorReturn(createErrorResponse("Unexpected error"));
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        errorResponse.put("accounts", List.of());
        errorResponse.put("participations", List.of());
        return errorResponse;
    }
}
