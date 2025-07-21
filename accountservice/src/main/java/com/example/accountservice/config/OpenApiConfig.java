package com.example.accountservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .description("API for Account Management")
                        .version("1.0.0"))
                // Point về Gateway để tránh CORS khi test API
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("API Gateway"));
    }
}