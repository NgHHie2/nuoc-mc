package com.example.accountservice.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.accountservice.enums.Role;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public Role getCurrentUserRole() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null)
            return null;

        String roleHeader = request.getHeader("X-User-Role");
        return roleHeader != null ? Role.valueOf(roleHeader) : null;
    }

    /**
     * Lấy danh sách position IDs từ header (thay vì username)
     */
    public List<Long> getCurrentUserPositions() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null)
            return List.of();

        String positionsHeader = request.getHeader("X-Positions");
        if (positionsHeader == null || positionsHeader.trim().isEmpty()) {
            return List.of();
        }

        try {
            return Arrays.stream(positionsHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return List.of();
        }
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

    /**
     * Kiểm tra user có position cụ thể không
     */
    public boolean hasPosition(Long positionId) {
        List<Long> positions = getCurrentUserPositions();
        return positions.contains(positionId);
    }

    /**
     * Kiểm tra user có bất kỳ position nào trong danh sách không
     */
    public boolean hasAnyPosition(List<Long> positionIds) {
        if (positionIds == null || positionIds.isEmpty())
            return false;
        List<Long> userPositions = getCurrentUserPositions();
        return userPositions.stream().anyMatch(positionIds::contains);
    }

    /**
     * Kiểm tra user có tất cả positions trong danh sách không
     */
    public boolean hasAllPositions(List<Long> positionIds) {
        if (positionIds == null || positionIds.isEmpty())
            return true;
        List<Long> userPositions = getCurrentUserPositions();
        return userPositions.containsAll(positionIds);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}