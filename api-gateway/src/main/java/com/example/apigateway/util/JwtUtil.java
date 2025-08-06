package com.example.apigateway.util;

import com.example.apigateway.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtTokenProvider jwtTokenProvider;

    // API Gateway chỉ decode JWT, không generate

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * Lấy userId string từ token (subject)
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    /**
     * Lấy userId từ token
     */
    public Integer getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * Lấy userRole từ token
     */
    public String getUserRoleFromToken(String token) {
        return jwtTokenProvider.getUserRoleFromToken(token);
    }

    /**
     * Lấy positions từ token
     */
    public List<Long> getPositionsFromToken(String token) {
        return jwtTokenProvider.getPositionsFromToken(token);
    }

    /**
     * Lấy JWT ID từ token
     */
    public String getJwtIdFromToken(String token) {
        return jwtTokenProvider.getJwtIdFromToken(token);
    }

    /**
     * Extract token từ Bearer header
     */
    public String extractTokenFromBearer(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Tạo Bearer token từ JWT
     */
    public String createBearerToken(String jwt) {
        return "Bearer " + jwt;
    }

    /**
     * Log thông tin token (cho debugging)
     */
    public void logTokenInfo(String token) {
        jwtTokenProvider.logTokenInfo(token);
    }

    /**
     * Check token có expired không
     */
    public boolean isTokenExpired(String token) {
        return jwtTokenProvider.isTokenExpired(token);
    }
}