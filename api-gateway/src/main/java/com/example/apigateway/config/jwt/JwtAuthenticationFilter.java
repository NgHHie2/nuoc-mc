package com.example.apigateway.config.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    // Danh sách các path không cần xác thực
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/account/login",
            "/account/register",
            "/actuator");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Bỏ qua authentication cho các path được định nghĩa
        if (shouldSkipAuthentication(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());

        if (token == null || token.isEmpty()) {
            return handleUnauthorized(exchange);
        }

        if (!jwtTokenProvider.validateToken(token)) {
            return handleUnauthorized(exchange);
        }

        try {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();

            // Thêm userId và userRole vào header trước khi forward đến các service khác
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userDetail.getUserId().toString())
                    .header("X-User-Role", userDetail.getUserRole())
                    .header("X-Username", userDetail.getUsername())
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            SecurityContext securityContext = new SecurityContextImpl(authentication);

            return chain.filter(modifiedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            return handleUnauthorized(exchange);
        }
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private boolean shouldSkipAuthentication(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = "{\"error\":\"Unauthorized\",\"message\":\"Access token is missing or invalid\"}";

        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // Chạy trước các filter khác
    }
}