package com.example.accountservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.accountservice.enums.Role;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKeyForJwtTokenThatShouldBeAtLeast256BitsLongToEnsureSecurityAndProperFunctioning}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Long userId, Role userRole, List<Long> positions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        String jwtId = UUID.randomUUID().toString(); // Tạo unique JWT ID

        return Jwts.builder()
                .subject(userId.toString()) // Dùng userId làm subject thay vì username
                .claim("userId", userId)
                .claim("userRole", userRole.name()) // Convert enum to string
                .claim("positions", positions) // Thêm mảng position IDs
                .id(jwtId) // Thêm JWT ID
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject(); // Sẽ trả về userId dưới dạng string
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }

    public Role getUserRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String roleString = claims.get("userRole", String.class);
        return Role.valueOf(roleString); // Convert string back to enum
    }

    // Method để lấy positions
    @SuppressWarnings("unchecked")
    public List<Long> getPositionsFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("positions", List.class);
    }

    // Method để lấy JWT ID
    public String getJwtIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getId();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Method để lấy tất cả thông tin từ token
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Method tiện ích để log JWT ID (cho debugging)
    public void logTokenInfo(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            log.info("Token Info - JTI: {}, Subject: {}, UserId: {}, Role: {}, Positions: {}, Issued: {}, Expires: {}",
                    claims.getId(),
                    claims.getSubject(),
                    claims.get("userId"),
                    claims.get("userRole"),
                    claims.get("positions"),
                    claims.getIssuedAt(),
                    claims.getExpiration());
        } catch (Exception e) {
            log.error("Cannot parse token info: {}", e.getMessage());
        }
    }
}