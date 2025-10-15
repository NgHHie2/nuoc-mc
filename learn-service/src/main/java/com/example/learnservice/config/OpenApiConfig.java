package com.example.learnservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Value("${learnservice.api-gateway-url}")
        private String apiGatewayUrl;

        @Value("${learnservice.learn-service-url}")
        private String learnServiceUrl;

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Learn Service API")
                                                .description("API for Learn Management")
                                                .version("1.0.0"))
                                .addServersItem(new Server()
                                                .url(apiGatewayUrl)
                                                .description("API Gateway"))
                                .addServersItem(new Server()
                                                .url(learnServiceUrl)
                                                .description("Direct to Learn Service"))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new io.swagger.v3.oas.models.Components()
                                                .addSecuritySchemes("bearerAuth",
                                                                new SecurityScheme()
                                                                                .name("bearerAuth")
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("JWT Bearer Token")));

        }
}