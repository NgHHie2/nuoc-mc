package com.example.accountservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Account Service API")
                                                .description("API for Account Management")
                                                .version("1.0.0"))
                                .addServersItem(new Server()
                                                .url("http://localhost:8082")
                                                .description("Direct to Account Service"))
                                .addServersItem(new Server()
                                                .url("http://localhost:8080")
                                                .description("API Gateway"));
        }

        @Bean
        public OperationCustomizer customGlobalHeaders() {
                return (Operation operation, HandlerMethod handlerMethod) -> {

                        // Thêm X-User-Id header
                        Parameter userIdParam = new Parameter()
                                        .in("header")
                                        .name("X-User-Id")
                                        .description("User ID from Gateway")
                                        .required(false)
                                        .schema(new StringSchema());

                        // Thêm X-User-Role header
                        Parameter userRoleParam = new Parameter()
                                        .in("header")
                                        .name("X-User-Role")
                                        .description("User Role from Gateway")
                                        .required(false)
                                        .schema(new StringSchema());

                        // Thêm X-Username header
                        Parameter usernameParam = new Parameter()
                                        .in("header")
                                        .name("X-Username")
                                        .description("Username from Gateway")
                                        .required(false)
                                        .schema(new StringSchema());

                        operation.addParametersItem(userIdParam);
                        operation.addParametersItem(userRoleParam);
                        operation.addParametersItem(usernameParam);

                        return operation;
                };
        }
}