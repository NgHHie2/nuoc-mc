package com.example.accountservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.model.RedisTokenInfo;
import com.example.accountservice.repository.RedisTokenRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RedisTokenService {

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    @Autowired
    private AccountPositionService accountPositionService;

    /**
     * Lưu thông tin token vào Redis
     */
    public void saveTokenInfo(String jti, Account account) {
        Long accountId = account.getId();
        Role role = account.getRole();
        List<Long> positions = getPositionsByAccount(account.getId());
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

    private List<Long> getPositionsByAccount(Long accountId) {
        try {
            List<AccountPosition> accountPositions = accountPositionService.getPositionsByAccount(accountId);
            return accountPositions.stream()
                    .map(ap -> ap.getPosition().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Cannot get positions for account {}: {}", accountId, e.getMessage());
            return List.of();
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

}