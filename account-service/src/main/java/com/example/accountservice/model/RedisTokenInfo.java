package com.example.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import com.example.accountservice.enums.Role;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("token_info")
public class RedisTokenInfo {

    @Id
    private Long accountId;

    private String jti;
    private Role role;
    private List<Long> positions;

    @TimeToLive
    private Long ttl;

    public RedisTokenInfo(String jti, Long accountId, Role role, List<Long> positions) {
        this.jti = jti;
        this.accountId = accountId;
        this.role = role;
        this.positions = positions;
        this.ttl = 86400L;
    }
}