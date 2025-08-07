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
import org.springframework.stereotype.Component;
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

    // Danh sách các path không cần xác thực - BỔ SUNG SWAGGER PATHS
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            // API Documentation
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/swagger-resources",
            "/configuration",

            // Service-specific API docs
            "/account-service/v3/api-docs",
            "/learn-service/v3/api-docs",
            "/stats-service/v3/api-docs",

            // Authentication endpoints
            "/account/login",
            "/account/register",
            "/account/validate-token",

            // Health check
            "/actuator",

            // Static resources
            "/favicon.ico",
            "/error");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        log.debug("Processing request for path: {}", path);

        // Bỏ qua authentication cho các path được định nghĩa
        if (shouldSkipAuthentication(path)) {
            log.debug("Skipping authentication for path: {}", path);
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());

        if (token == null || token.isEmpty()) {
            log.warn("No token found for path: {}", path);
            return handleUnauthorized(exchange);
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid token for path: {}", path);
            return handleUnauthorized(exchange);
        }

        // Lấy JTI từ token
        Long accountId = jwtTokenProvider.getUserIdFromToken(token);

        // Kiểm tra token có tồn tại trong Redis không
        Optional<RedisTokenInfo> redisTokenInfo = redisTokenService.getTokenInfo(accountId);

        if (redisTokenInfo.isEmpty()
                || !redisTokenInfo.get().getJti().equals(jwtTokenProvider.getJwtIdFromToken(token))) {
            log.warn("Token not found in Redis or revoked - accId: {}, Path: {}", accountId, path);
            return handleUnauthorized(exchange);
        }

        try {
            // Sử dụng thông tin từ Redis thay vì decode JWT
            RedisTokenInfo tokenInfo = redisTokenInfo.get();

            // Tạo CustomUserDetail từ Redis data
            CustomUserDetail userDetail = new CustomUserDetail(
                    tokenInfo.getAccountId().longValue(),
                    tokenInfo.getAccountId().toString(),
                    tokenInfo.getPositions(),
                    tokenInfo.getRole());

            // Convert positions list to comma-separated string
            String positionsHeader = tokenInfo.getPositions() != null ? tokenInfo.getPositions().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")) : "";

            // Thêm userId, userRole và positions vào header thay vì username
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userDetail.getUserId().toString())
                    .header("X-User-Role", userDetail.getUserRole())
                    .header("X-Positions", positionsHeader) // Thay X-Username bằng X-Positions
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange);

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
        boolean shouldSkip = EXCLUDED_PATHS.stream().anyMatch(path::startsWith);

        // Thêm logic đặc biệt cho các path pattern phức tạp
        if (!shouldSkip) {
            // Skip cho tất cả swagger-ui resources
            shouldSkip = path.contains("swagger-ui") ||
                    path.contains("api-docs") ||
                    path.contains("webjars") ||
                    path.endsWith(".css") ||
                    path.endsWith(".js") ||
                    path.endsWith(".png") ||
                    path.endsWith(".ico");
        }

        return shouldSkip;
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