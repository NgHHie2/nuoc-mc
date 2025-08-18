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

    private Long userId;
    private String userIdStr; // Subject của JWT (userId dưới dạng string)

    @JsonIgnore
    private String password;

    // Constructor không cần password (vì API Gateway chỉ decode JWT)
    public CustomUserDetail(Long userId, String userIdStr) {
        this.userId = userId;
        this.userIdStr = userIdStr;
        this.password = null; // API Gateway không cần password
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
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
}