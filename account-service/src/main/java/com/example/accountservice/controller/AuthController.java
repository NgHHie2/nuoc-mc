package com.example.accountservice.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.enums.Role;
import com.example.accountservice.kafka.KafkaProducer;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.util.JwtUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/account")
public class AuthController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Account account) {
        try {
            // Simple validation
            if (account.getUsername() == null || account.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            if (account.getPassword() == null || account.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }
            if (account.getEmail() == null || account.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            // Check duplicates
            if (accountService.existsByUsername(account.getUsername())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
            if (accountService.existsByEmail(account.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }

            // Encrypt password and set default role
            account.setPassword(passwordEncoder.encode(account.getPassword()));
            if (account.getRole() == null) {
                account.setRole(Role.STUDENT); // Default role
            }

            Account savedAccount = accountService.saveAccount(account);
            kafkaProducer.sendAccount("account-registered", savedAccount);

            // Generate JWT token
            String token = jwtUtil.generateToken(savedAccount.getId(), savedAccount.getUsername(),
                    savedAccount.getRole());

            log.info("User registered successfully: {}", savedAccount.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, savedAccount));

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

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }

            Optional<Account> accountOpt = accountService.findByUsername(username);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }

            Account account = accountOpt.get();
            if (!passwordEncoder.matches(password, account.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(account.getId(), account.getUsername(), account.getRole());

            log.info("User logged in successfully: {}", account.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, account));

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

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("valid", false, "error", "Token is required"));
            }

            // Remove Bearer prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            boolean isValid = jwtUtil.validateToken(token);

            if (isValid) {
                String username = jwtUtil.getUsernameFromToken(token);
                Integer userId = jwtUtil.getUserIdFromToken(token);
                Role userRole = jwtUtil.getUserRoleFromToken(token);

                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "userId", userId,
                        "username", username,
                        "userRole", userRole.name() // Convert enum to string for JSON
                ));
            } else {
                return ResponseEntity.ok(Map.of("valid", false, "error", "Invalid token"));
            }

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("valid", false, "error", "Token validation failed"));
        }
    }

    @Data
    public static class AuthResponse {
        private String token;

        @JsonProperty("account")
        private Account account;

        public AuthResponse(String token, Account account) {
            this.token = token;
            // Remove password before sending response
            account.setPassword(null);
            this.account = account;
        }
    }
}