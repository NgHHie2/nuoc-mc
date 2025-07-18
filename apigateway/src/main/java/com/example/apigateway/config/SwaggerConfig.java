package com.example.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import jakarta.annotation.PostConstruct;

@Configuration
public class SwaggerConfig {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ConfigurableEnvironment env;

    @PostConstruct
    public void init() {
        int index = 0;
        for (String serviceId : discoveryClient.getServices()) {
            if (!serviceId.equalsIgnoreCase("apigateway")) {
                String nameKey = String.format("springdoc.swagger-ui.urls[%d].name", index);
                String urlKey = String.format("springdoc.swagger-ui.urls[%d].url", index);
                env.getSystemProperties().put(nameKey, serviceId);
                env.getSystemProperties().put(urlKey, "/" + serviceId.toLowerCase() + "/v3/api-docs");
                index++;
            }
        }
    }
}
