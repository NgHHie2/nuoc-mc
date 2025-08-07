package com.example.accountservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.RedisTokenInfo;
import com.example.accountservice.repository.RedisTokenRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RedisTokenService {

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    /**
     * Lưu thông tin token vào Redis
     */
    public void saveTokenInfo(String jti, Long accountId, Role role, List<Long> positions) {
        try {
            redisTokenRepository.deleteById(accountId);
            RedisTokenInfo tokenInfo = new RedisTokenInfo(jti, accountId, role, positions);
            redisTokenRepository.save(tokenInfo);
            log.info("Saved token info to Redis - JTI: {}, AccountId: {}, Role: {}, Positions: {}",
                    jti, accountId, role, positions);
        } catch (Exception e) {
            log.error("Failed to save token info to Redis - JTI: {}, Error: {}", jti, e.getMessage());
        }
    }

    /**
     * Lấy thông tin token từ Redis
     */
    public Optional<RedisTokenInfo> getTokenInfo(Long accountId) {
        try {
            return redisTokenRepository.findById(accountId);
        } catch (Exception e) {
            log.error("Failed to get token info from Redis - accId: {}, Error: {}", accountId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Kiểm tra token có tồn tại trong Redis không
     */
    public boolean isTokenValid(Long accountId) {
        try {
            boolean exists = redisTokenRepository.existsById(accountId);
            log.debug("Token validation - accId: {}, Valid: {}", accountId, exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to validate token - accId: {}, Error: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Xóa token khỏi Redis (logout)
     */
    public void revokeToken(Long accountId) {
        try {
            redisTokenRepository.deleteById(accountId);
            log.info("Revoked token - accId: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to revoke token - accId: {}, Error: {}", accountId, e.getMessage());
        }
    }

    /**
     * Update thông tin user trong tất cả token của họ (khi role/position thay đổi)
     */
    // public void updateUserTokens(Long accountId, Role newRole, List<Long>
    // newPositions) {
    // try {
    // // Tìm tất cả token của user và update
    // // Với Spring Data Redis, cần implement custom query hoặc dùng RedisTemplate
    // log.info("Updated user tokens - AccountId: {}, NewRole: {}, NewPositions:
    // {}",
    // accountId, newRole, newPositions);
    // } catch (Exception e) {
    // log.error("Failed to update user tokens - AccountId: {}, Error: {}",
    // accountId, e.getMessage());
    // }
    // }
}