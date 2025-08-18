package com.example.accountservice.controller;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.service.RedisTokenService;
import com.example.accountservice.service.AccountPositionService;
import com.example.accountservice.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Transactional
@RequestMapping("/account")
public class AuthController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RedisTokenService redisTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountPositionService accountPositionService;

    @Autowired
    private HttpServletRequest request;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Account loginRequest, HttpServletResponse response) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }

            Optional<Account> accountOpt = accountService.findByUsernameAndVisible(username);
            if (accountOpt.isEmpty() || !passwordEncoder.matches(password, accountOpt.get().getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }

            Account account = accountOpt.get();
            List<Long> positions = getPositionsByAccount(account.getId());
            String token = jwtUtil.generateToken(account.getId());
            String jwtId = jwtUtil.getJwtIdFromToken(token);

            redisTokenService.saveTokenInfo(jwtId, account.getId(), account.getRole(), positions);

            // Set JWT cookie
            Cookie jwtCookie = new Cookie("jwt", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // true for HTTPS
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 hours
            jwtCookie.setAttribute("SameSite", "Lax");
            response.addCookie(jwtCookie);

            return ResponseEntity.ok(Map.of(
                    "token", token, // For Swagger
                    "jwtId", jwtId,
                    "userId", account.getId(),
                    "username", account.getUsername(),
                    "message", "Login successful. JWT set in cookie."));

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing X-User-Id header"));
        }

        try {
            Long accountId = Long.valueOf(userIdHeader);
            // Clear token in redis
            redisTokenService.revokeToken(accountId);

            // Clear JWT cookie
            Cookie jwtCookie = new Cookie("jwt", null);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(0);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok(Map.of("message", "Logout successful. Cookie cleared."));
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
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
}