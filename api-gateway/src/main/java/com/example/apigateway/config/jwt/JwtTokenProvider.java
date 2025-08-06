package com.example.apigateway.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:mySecretKeyForJwtTokenThatShouldBeAtLeast256BitsLongToEnsureSecurityAndProperFunctioning}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // API Gateway chỉ decode, không tạo JWT

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject(); // Sẽ trả về userId dưới dạng string
    }

    public Integer getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Integer.class);
    }

    public String getUserRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userRole", String.class);
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

    public Authentication getAuthentication(String token) {
        String userIdStr = getUsernameFromToken(token); // Subject là userId
        String userRole = getUserRoleFromToken(token);
        Integer userId = getUserIdFromToken(token);
        List<Long> positions = getPositionsFromToken(token);

        CustomUserDetail userDetail = new CustomUserDetail(userId, userIdStr, positions, userRole);

        return new UsernamePasswordAuthenticationToken(
                userDetail,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole)));
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

    // Method để check token có expired không
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Method để lấy expiration date
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }
}