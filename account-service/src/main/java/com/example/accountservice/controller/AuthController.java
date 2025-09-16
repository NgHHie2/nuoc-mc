package com.example.accountservice.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.dto.LoginDTO;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.service.RedisTokenService;
import com.example.accountservice.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
    private HttpServletRequest request;

    /**
     * Đăng nhập vào hệ thống
     * Account service sẽ tạo jwt và lưu thông tin cần thiết của account vào redis
     * nếu đăng nhập thành công
     * 
     * Input:
     * - username
     * - password
     * 
     * Output:
     * - Trả về thông báo thành công hoặc thất bại tùy trường hợp.
     * - Nếu đăng nhập thành công thì trong response có gắn kèm cookie chứa jwt.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginRequest, HttpServletResponse response) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            Optional<Account> accountOpt = accountService.findByUsernameAndVisible(username);
            if (accountOpt.isEmpty() || !passwordEncoder.matches(password, accountOpt.get().getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }

            Account account = accountOpt.get();
            String token = jwtUtil.generateToken(account);
            String jwtId = jwtUtil.getJwtIdFromToken(token);

            redisTokenService.saveTokenInfo(jwtId, account);

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

    /**
     * Đăng xuất khỏi hệ thống
     * Xóa thông tin account khỏi redis
     * 
     * Input:
     * 
     * Output:
     * - Trả về thông báo thành công hoặc thất bại tùy trường hợp.
     * - Nếu đăng xuất thành công thì response set jwt về null.
     */
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

}