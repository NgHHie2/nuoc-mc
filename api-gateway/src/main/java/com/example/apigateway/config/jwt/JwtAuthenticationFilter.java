package com.example.apigateway.config.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.example.apigateway.model.RedisTokenInfo;
import com.example.apigateway.service.RedisTokenService;

import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenService redisTokenService;

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String JWT_COOKIE_NAME = "jwt";

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/v3/api-docs", "/swagger-ui", "/webjars", "/swagger-resources", "/configuration",
            "/account-service/v3/api-docs", "/learn-service/v3/api-docs", "/stats-service/v3/api-docs",
            "/account/login",
            "/actuator", "/favicon.ico", "/error");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (shouldSkipAuthentication(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return handleUnauthorized(exchange);
        }

        Long accountId = jwtTokenProvider.getUserIdFromToken(token);
        Optional<RedisTokenInfo> redisTokenInfo = redisTokenService.getTokenInfo(accountId);

        if (redisTokenInfo.isEmpty()
                || !redisTokenInfo.get().getJti().equals(jwtTokenProvider.getJwtIdFromToken(token))) {
            return handleUnauthorized(exchange);
        }

        RedisTokenInfo tokenInfo = redisTokenInfo.get();
        String positionsHeader = tokenInfo.getPositions() != null
                ? tokenInfo.getPositions().stream().map(String::valueOf).collect(Collectors.joining(","))
                : "";

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", tokenInfo.getAccountId().toString())
                .header("X-User-Role", tokenInfo.getRole())
                .header("X-Positions", positionsHeader)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private String resolveToken(ServerHttpRequest request) {
        // 1. Check Authorization Header first (for Swagger)
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }

        // 2. Check Cookie
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (cookies.containsKey(JWT_COOKIE_NAME)) {
            HttpCookie jwtCookie = cookies.getFirst(JWT_COOKIE_NAME);
            if (jwtCookie != null && StringUtils.hasText(jwtCookie.getValue())) {
                return jwtCookie.getValue();
            }
        }

        return null;
    }

    private boolean shouldSkipAuthentication(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith) ||
                path.contains("swagger-ui") || path.contains("api-docs") || path.contains("webjars");
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
        return -100;
    }
}