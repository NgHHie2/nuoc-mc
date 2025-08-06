package com.example.apigateway.config.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class CustomUserDetail implements UserDetails {

    private Integer userId;
    private String userIdStr; // Subject của JWT (userId dưới dạng string)
    private List<Long> positions; // Mảng position IDs
    private String userRole;

    @JsonIgnore
    private String password;

    // Constructor không cần password (vì API Gateway chỉ decode JWT)
    public CustomUserDetail(Integer userId, String userIdStr, List<Long> positions, String userRole) {
        this.userId = userId;
        this.userIdStr = userIdStr;
        this.positions = positions;
        this.userRole = userRole;
        this.password = null; // API Gateway không cần password
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userIdStr; // Trả về userId dưới dạng string
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Helper methods
    public boolean hasPosition(Long positionId) {
        return positions != null && positions.contains(positionId);
    }

    public boolean hasAnyPosition(List<Long> positionIds) {
        if (positions == null || positionIds == null)
            return false;
        return positions.stream().anyMatch(positionIds::contains);
    }
}