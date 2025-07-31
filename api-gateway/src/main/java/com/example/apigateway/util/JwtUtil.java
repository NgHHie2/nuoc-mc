package com.example.apigateway.util;

import com.example.apigateway.config.jwt.CustomUserDetail;
import com.example.apigateway.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Tạo JWT token từ thông tin user
     */
    public String generateToken(Integer userId, String username, String userRole) {
        CustomUserDetail userDetail = new CustomUserDetail(userId, username, null, userRole);
        return jwtTokenProvider.generateToken(userDetail);
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * Lấy username từ token
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
}