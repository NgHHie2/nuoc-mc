package com.example.apigateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SwaggerController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/swagger-config")
    public ResponseEntity<Map<String, Object>> swaggerConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, String>> urls = new ArrayList<>();

        // API Gateway
        Map<String, String> gatewayUrl = new HashMap<>();
        gatewayUrl.put("name", "API Gateway");
        gatewayUrl.put("url", "/v3/api-docs");
        urls.add(gatewayUrl);

        // Dynamic services từ Eureka
        List<String> services = discoveryClient.getServices();
        for (String service : services) {
            if (!service.equalsIgnoreCase("apigateway") &&
                    !service.equalsIgnoreCase("discoveryserver")) {

                List<ServiceInstance> instances = discoveryClient.getInstances(service);
                if (!instances.isEmpty()) {
                    Map<String, String> serviceUrl = new HashMap<>();
                    serviceUrl.put("name", service.toUpperCase() + " Service");
                    // Sử dụng pattern cho API docs (với service prefix)
                    serviceUrl.put("url", "/" + service.toLowerCase() + "/v3/api-docs");
                    urls.add(serviceUrl);
                }
            }
        }

        config.put("urls", urls);

        // Swagger UI config
        config.put("deepLinking", true);
        config.put("displayOperationId", false);
        config.put("defaultModelsExpandDepth", 1);
        config.put("defaultModelExpandDepth", 1);
        config.put("defaultModelRendering", "example");
        config.put("displayRequestDuration", false);
        config.put("docExpansion", "none");
        config.put("filter", false);
        config.put("operationsSorter", null);
        config.put("showExtensions", false);
        config.put("tagsSorter", "alpha");
        config.put("validatorUrl", "");
        config.put("tryItOutEnabled", true);

        return ResponseEntity.ok(config);
    }

    @GetMapping("/available-services")
    public ResponseEntity<List<String>> getAvailableServices() {
        List<String> services = new ArrayList<>();

        for (String service : discoveryClient.getServices()) {
            if (!service.equalsIgnoreCase("apigateway") &&
                    !service.equalsIgnoreCase("discoveryserver")) {

                List<ServiceInstance> instances = discoveryClient.getInstances(service);
                if (!instances.isEmpty()) {
                    services.add(service);
                }
            }
        }

        return ResponseEntity.ok(services);
    }

    // Debug endpoint để kiểm tra URL config
    @GetMapping("/swagger-urls")
    public ResponseEntity<List<Map<String, String>>> getSwaggerUrls() {
        List<Map<String, String>> urls = new ArrayList<>();

        // Gateway
        Map<String, String> gateway = new HashMap<>();
        gateway.put("name", "API Gateway");
        gateway.put("url", "/v3/api-docs");
        urls.add(gateway);

        // Services
        for (String service : discoveryClient.getServices()) {
            if (!service.equalsIgnoreCase("apigateway") &&
                    !service.equalsIgnoreCase("discoveryserver")) {

                List<ServiceInstance> instances = discoveryClient.getInstances(service);
                if (!instances.isEmpty()) {
                    Map<String, String> serviceMap = new HashMap<>();
                    serviceMap.put("name", service.toUpperCase() + " Service");
                    serviceMap.put("url", "/" + service.toLowerCase() + "/v3/api-docs");
                    urls.add(serviceMap);
                }
            }
        }

        return ResponseEntity.ok(urls);
    }
}