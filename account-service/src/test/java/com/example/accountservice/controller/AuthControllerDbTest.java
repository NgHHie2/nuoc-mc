package com.example.accountservice.controller;

import com.example.accountservice.config.TestConfig;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.RedisTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test cho AuthController với H2 database
 */
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
class AuthControllerDbTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RedisTokenService redisTokenService; // Mock Redis để tránh dependency

    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Setup MockMvc manually
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Xóa dữ liệu cũ
        accountRepository.deleteAll();

        // Tạo test account với password đã mã hóa
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setPassword(passwordEncoder.encode("password123"));
        testAccount.setFirstName("Test");
        testAccount.setLastName("User");
        testAccount.setEmail("test@example.com");
        testAccount.setCccd("123456789");
        testAccount.setRole(Role.STUDENT);
        testAccount.setVisible(1);

        // Lưu vào database
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    void login_WithH2Database_Success() throws Exception {
        // Given
        AccountLogin loginRequest = new AccountLogin();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testAccount.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.message").value("Login successful. JWT set in cookie."))
                .andExpect(cookie().exists("jwt"));
    }

    @Test
    void login_WithH2Database_WrongPassword() throws Exception {
        // Given
        AccountLogin loginRequest = new AccountLogin();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        // When & Then
        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void login_WithH2Database_UserNotFound() throws Exception {
        // Given
        AccountLogin loginRequest = new AccountLogin();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void login_WithH2Database_DeletedUser() throws Exception {
        // Given - Xóa user (soft delete)
        testAccount.setVisible(0);
        accountRepository.save(testAccount);

        AccountLogin loginRequest = new AccountLogin();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Data
    private class AccountLogin {
        private String username;
        private String password;
    }
}