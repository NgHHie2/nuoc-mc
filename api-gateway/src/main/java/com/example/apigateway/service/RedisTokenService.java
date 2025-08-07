package com.example.apigateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.apigateway.model.RedisTokenInfo;
import com.example.apigateway.repository.RedisTokenRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Service
public class RedisTokenService {

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    /**
     * Kiểm tra token có tồn tại trong Redis và lấy thông tin
     */
    public Optional<RedisTokenInfo> getTokenInfo(Long accountId) {
        try {
            Optional<RedisTokenInfo> tokenInfo = redisTokenRepository.findById(accountId);

            if (tokenInfo.isPresent()) {
                log.debug("Found token info in Redis - accId: {}, jti: {}, Role: {}",
                        accountId, tokenInfo.get().getJti(), tokenInfo.get().getRole());
                return tokenInfo;
            } else {
                log.debug("Token not found in Redis - accId: {}", accountId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to get token info from Redis - accId: {}, Error: {}", accountId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Kiểm tra token có hợp lệ không (tồn tại trong Redis)
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

}