package com.example.learnservice.client;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class StatsClient {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${stats.service.url}") 
    private String statsServiceUrl;

    

}