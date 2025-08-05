package com.example.accountservice.controller;

import java.util.Map;
import java.util.Optional;

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
import com.example.accountservice.service.AccountService;
import com.example.accountservice.util.JwtUtil;
import com.example.accountservice.util.UsernameGenerator;
import com.example.accountservice.util.listener.event.UserRegisteredEvent;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.Data;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsernameGenerator usernameGenerator;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Account account) {
        try {
            // Kiểm tra trùng lặp CCCD
            if (accountService.existsByCccd(account.getCccd())) {
                return ResponseEntity.badRequest().body(Map.of("error", "CCCD already exists"));
            }

            // Kiểm tra trùng lặp email
            if (accountService.existsByEmail(account.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }

            // Tạo username tự động từ họ tên
            String generatedUsername = usernameGenerator.generateUsername(
                    account.getFirstName(),
                    account.getLastName());
            account.setUsername(generatedUsername);

            // Set password mặc định nếu không có
            if (account.getPassword() == null || account.getPassword().trim().isEmpty()) {
                account.setPassword(usernameGenerator.getDefaultPassword());
            }

            // Mã hóa password
            account.setPassword(passwordEncoder.encode(account.getPassword()));

            // Set role mặc định
            if (account.getRole() == null) {
                account.setRole(Role.STUDENT);
            }

            Account savedAccount = accountService.saveAccount(account);
            applicationEventPublisher.publishEvent(new UserRegisteredEvent(savedAccount));

            String token = jwtUtil.generateToken(savedAccount.getId(),
                    savedAccount.getUsername(), savedAccount.getRole());

            log.info("User registered successfully: {} with username: {}",
                    savedAccount.getFirstName() + " " + savedAccount.getLastName(),
                    savedAccount.getUsername());

            return ResponseEntity.ok(token);

        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }

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
            String token = jwtUtil.generateToken(account.getId(), account.getUsername(), account.getRole());

            log.info("User logged in successfully: {}", account.getUsername());
            return ResponseEntity.ok(token);

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("valid", false, "error", "Token is required"));
            }

            // Remove Bearer prefix if present
            token = token.startsWith("Bearer ") ? token.substring(7) : token;

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.ok(Map.of("valid", false, "error", "Invalid token"));
            }

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", jwtUtil.getUserIdFromToken(token),
                    "username", jwtUtil.getUsernameFromToken(token),
                    "userRole", jwtUtil.getUserRoleFromToken(token).name()));

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("valid", false, "error", "Token validation failed"));
        }
    }
}