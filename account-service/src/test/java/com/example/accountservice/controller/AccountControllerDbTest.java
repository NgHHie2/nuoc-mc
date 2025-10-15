package com.example.accountservice.controller;

import com.example.accountservice.config.TestConfig;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test cho AccountController với H2 database
 */
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
class AccountControllerDbTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account studentAccount;
    private Account teacherAccount;
    private Account adminAccount;

    @BeforeEach
    void setUp() {
        // Setup MockMvc manually
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Xóa dữ liệu cũ
        accountRepository.deleteAll();

        // Tạo test accounts
        studentAccount = createAccount("student", "Student", "User", Role.STUDENT, "111111111");
        teacherAccount = createAccount("teacher", "Teacher", "User", Role.TEACHER, "222222222");
        adminAccount = createAccount("admin", "Admin", "User", Role.ADMIN, "333333333");

        // Lưu vào database
        studentAccount = accountRepository.save(studentAccount);
        teacherAccount = accountRepository.save(teacherAccount);
        adminAccount = accountRepository.save(adminAccount);
    }

    @Test
    void searchAccounts_WithH2Database_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/account/search")
                .header("X-User-Role", "ADMIN")
                .param("keyword", "user")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].visible").value(1));
    }

    @Test
    void searchAccounts_WithRoleFilter() throws Exception {
        // When & Then
        mockMvc.perform(get("/account/search")
                .header("X-User-Role", "ADMIN")
                .param("role", "STUDENT")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.content[0].username").value("student"));
    }

    @Test
    void getAccountById_WithH2Database_Success() throws Exception {
        System.out.println("hiephiephiep " + studentAccount.getId().toString());
        // When & Then
        mockMvc.perform(get("/account/" + studentAccount.getId().toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAccount.getId()))
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.firstName").value("Student"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void createAccount_WithH2Database_Success() throws Exception {
        // Given
        Account newAccount = new Account();
        newAccount.setFirstName("New");
        newAccount.setLastName("User");
        newAccount.setEmail("new@example.com");
        newAccount.setCccd("444444444");
        newAccount.setRole(Role.STUDENT);

        // When & Then
        mockMvc.perform(post("/account")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.username").exists()) // Username được tạo tự động
                .andExpect(jsonPath("$.role").value("STUDENT"));

        // Verify database
        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 4; // 3 ban đầu + 1 mới tạo
    }

    @Test
    void createAccount_WithDuplicateCCCD_ShouldFail() throws Exception {
        // Given - Sử dụng CCCD đã tồn tại
        Account newAccount = new Account();
        newAccount.setFirstName("Duplicate");
        newAccount.setLastName("User");
        newAccount.setCccd("111111111"); // CCCD của studentAccount
        newAccount.setRole(Role.STUDENT);

        // When & Then
        mockMvc.perform(post("/account")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CCCD already exists"));

        // Verify database không thay đổi
        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 3;
    }

    @Test
    void updateAccount_WithH2Database_Success() throws Exception {
        // Given
        Account updateRequest = new Account();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Student");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCccd("111111111"); // Giữ nguyên CCCD
        updateRequest.setRole(Role.TEACHER); // Đổi role

        // When & Then
        mockMvc.perform(put("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAccount.getId()))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.role").value("TEACHER"));

        // Verify database
        Account updatedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert updatedAccount != null;
        assert updatedAccount.getFirstName().equals("Updated");
        assert updatedAccount.getRole() == Role.TEACHER;
    }

    @Test
    void deleteAccount_WithH2Database_SoftDelete() throws Exception {
        // When & Then
        mockMvc.perform(delete("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));

        // Verify soft delete
        Account deletedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert deletedAccount != null;
        assert deletedAccount.getVisible() == 0; // Soft deleted

        // Verify không tìm thấy với findByIdAndVisible
        assert accountRepository.findByIdAndVisible(studentAccount.getId(), 1).isEmpty();
    }

    @Test
    void deleteAccount_WithH2Database_InvalidUser() throws Exception {
        // When & Then
        mockMvc.perform(delete("/account/" + 9999)
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found"))
                .andExpect(jsonPath("$.error").value("Invalid User"));
    }

    @Test
    void deleteAccount_WithH2Database_AlreadyDeleted() throws Exception {
        // Given - Xóa trước đó
        studentAccount.setVisible(0);
        accountRepository.save(studentAccount);

        Account deletedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert deletedAccount != null;
        assert deletedAccount.getVisible() == 0; // Already soft deleted

        // Verify không tìm thấy với findByIdAndVisible
        assert accountRepository.findByIdAndVisible(studentAccount.getId(), 1).isEmpty();
        
        // When & Then
        mockMvc.perform(delete("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account already deleted"))
                .andExpect(jsonPath("$.error").value("Invalid account data"));
    }

    @Test
    void getAccountByMe_WithH2Database_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/account/me")
                .header("X-User-Id", studentAccount.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAccount.getId()))
                .andExpect(jsonPath("$.username").value("student"));
    }

    // Helper method
    private Account createAccount(String username, String firstName, String lastName, Role role, String cccd) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode("password123"));
        account.setFirstName(firstName);
        account.setLastName(lastName);
        account.setEmail(username + "@example.com");
        account.setCccd(cccd);
        account.setRole(role);
        account.setVisible(1);
        account.setBirthDay(LocalDateTime.of(1990, 1, 1, 0, 0));
        return account;
    }
}