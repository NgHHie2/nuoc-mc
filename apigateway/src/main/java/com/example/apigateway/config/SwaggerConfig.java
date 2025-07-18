package com.example.apigateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    @Autowired
    @Lazy
    private DiscoveryClient discoveryClient;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nuoc Microservice API Gateway")
                        .description("API tập trung cho tất cả các microservices")
                        .version("1.0.0"))
                .addServersItem(new Server().url("http://localhost:8080").description("API Gateway"));
    }

    @Bean
    public GroupedOpenApi gatewayApi() {
        return GroupedOpenApi.builder()
                .group("apigateway")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    @Lazy(false)
    public Set<SwaggerUiConfigParameters.SwaggerUrl> apis() {
        Set<SwaggerUiConfigParameters.SwaggerUrl> urls = new HashSet<>();

        // Thêm API Gateway
        urls.add(new SwaggerUiConfigParameters.SwaggerUrl("API Gateway", "/v3/api-docs", "apigateway"));

        // Thêm các service được discover
        List<String> services = discoveryClient.getServices();
        for (String service : services) {
            if (!service.equalsIgnoreCase("apigateway") &&
                    !service.equalsIgnoreCase("discoveryserver")) {

                List<ServiceInstance> instances = discoveryClient.getInstances(service);
                if (!instances.isEmpty()) {
                    String url = String.format("/%s/v3/api-docs", service.toLowerCase());
                    urls.add(new SwaggerUiConfigParameters.SwaggerUrl(
                            service.toUpperCase(), url, service.toLowerCase()));
                }
            }
        }

        return urls;
    }
}