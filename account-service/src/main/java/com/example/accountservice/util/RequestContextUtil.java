package com.example.accountservice.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.accountservice.enums.Role;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestContextUtil {

    /**
     * Lấy thông tin user từ headers mà API Gateway đã set
     */
    public Integer getCurrentUserId() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null)
            return null;

        String userIdHeader = request.getHeader("X-User-Id");
        return userIdHeader != null ? Integer.valueOf(userIdHeader) : null;
    }

    public String getCurrentUsername() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null)
            return null;

        return request.getHeader("X-Username");
    }

    public Role getCurrentUserRole() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null)
            return null;

        String roleHeader = request.getHeader("X-User-Role");
        return roleHeader != null ? Role.valueOf(roleHeader) : null;
    }

    public boolean isCurrentUser(Long userId) {
        Integer currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    public boolean hasRole(Role role) {
        Role currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    public boolean isTeacher() {
        return hasRole(Role.TEACHER);
    }

    public boolean isStudent() {
        return hasRole(Role.STUDENT);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}