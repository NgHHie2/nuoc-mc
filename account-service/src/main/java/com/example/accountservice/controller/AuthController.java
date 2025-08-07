package com.example.accountservice.controller;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.service.RedisTokenService;
import com.example.accountservice.service.AccountPositionService;
import com.example.accountservice.util.JwtUtil;
import com.example.accountservice.util.UsernameGenerator;
import com.example.accountservice.util.listener.event.UserRegisteredEvent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
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
    private UsernameGenerator usernameGenerator;

    @Autowired
    private AccountPositionService accountPositionService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private HttpServletRequest request;

    // @PostMapping("/register")
    // public ResponseEntity<?> register(@Valid @RequestBody Account account) {
    // try {
    // // Kiểm tra trùng lặp CCCD
    // if (accountService.existsByCccd(account.getCccd())) {
    // return ResponseEntity.badRequest().body(Map.of("error", "CCCD already
    // exists"));
    // }

    // // Kiểm tra trùng lặp email
    // if (accountService.existsByEmail(account.getEmail())) {
    // return ResponseEntity.badRequest().body(Map.of("error", "Email already
    // exists"));
    // }

    // // Tạo username tự động từ họ tên
    // String generatedUsername = usernameGenerator.generateUsername(
    // account.getFirstName(),
    // account.getLastName());
    // account.setUsername(generatedUsername);

    // // Set password mặc định nếu không có
    // if (account.getPassword() == null || account.getPassword().trim().isEmpty())
    // {
    // account.setPassword(usernameGenerator.getDefaultPassword());
    // }

    // // Mã hóa password
    // account.setPassword(passwordEncoder.encode(account.getPassword()));

    // // Set role mặc định
    // if (account.getRole() == null) {
    // account.setRole(Role.STUDENT);
    // }

    // Account savedAccount = accountService.saveAccount(account);
    // applicationEventPublisher.publishEvent(new
    // UserRegisteredEvent(savedAccount));

    // // Lấy positions của user
    // List<Long> positions = getPositionsByAccount(savedAccount.getId());

    // String token = jwtUtil.generateToken(savedAccount.getId(),
    // savedAccount.getRole(), positions);

    // // Log thông tin JWT (bao gồm JWT ID)
    // log.info("User registered successfully: {} with username: {}",
    // savedAccount.getFirstName() + " " + savedAccount.getLastName(),
    // savedAccount.getUsername());

    // // Log JWT ID cho debugging
    // String jwtId = jwtUtil.getJwtIdFromToken(token);
    // log.info("Generated JWT ID for user {}: {}", savedAccount.getUsername(),
    // jwtId);

    // return ResponseEntity.ok(Map.of(
    // "token", token,
    // "jwtId", jwtId,
    // "userId", savedAccount.getId(),
    // "username", savedAccount.getUsername(),
    // "role", savedAccount.getRole().name(),
    // "positions", positions));

    // } catch (Exception e) {
    // log.error("Registration failed: {}", e.getMessage());
    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    // .body(Map.of("error", "Registration failed", "message", e.getMessage()));
    // }
    // }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Account loginRequest) {
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

            // Lấy positions của user
            List<Long> positions = getPositionsByAccount(account.getId());

            String token = jwtUtil.generateToken(account.getId(), account.getRole(), positions);

            // Log thông tin JWT (bao gồm JWT ID)
            String jwtId = jwtUtil.getJwtIdFromToken(token);
            log.info("User logged in successfully: {} with JWT ID: {}", account.getUsername(), jwtId);

            // Log full token info for debugging
            jwtUtil.logTokenInfo(token);

            redisTokenService.saveTokenInfo(jwtId, account.getId(), account.getRole(), positions);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "jwtId", jwtId,
                    "userId", account.getId(),
                    "username", account.getUsername(),
                    "role", account.getRole().name(),
                    "positions", positions));

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing X-User-Id header"));
        }

        try {
            Long accountId = Long.valueOf(userIdHeader);
            redisTokenService.revokeToken(accountId);
            return ResponseEntity.ok(Map.of("message", "Logout successful"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid X-User-Id format"));
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
        }
    }

    // Helper method để lấy position IDs của user
    private List<Long> getPositionsByAccount(Long accountId) {
        try {
            List<AccountPosition> accountPositions = accountPositionService.getPositionsByAccount(accountId);
            return accountPositions.stream()
                    .map(ap -> ap.getPosition().getId()) // Lấy positionId thay vì name
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Cannot get positions for account {}: {}", accountId, e.getMessage());
            return List.of(); // Trả về empty list nếu có lỗi
        }
    }
}