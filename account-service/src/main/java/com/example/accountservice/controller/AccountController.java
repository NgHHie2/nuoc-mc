package com.example.accountservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.kafka.KafkaProducer;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.util.UsernameGenerator;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@Transactional
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsernameGenerator usernameGenerator;

    // @GetMapping
    // public List<Account> getAllAccount() {
    // return accountService.getAllAccount();
    // }

    @GetMapping("/{id}")
    public Optional<Account> getAccountById(@PathVariable int id) {
        log.info("get user by id: " + id);
        return accountService.getAccountById(id);
    }

    @PostMapping("/ids")
    public List<Account> getAccountsByIds(@RequestBody List<Integer> ids) {
        return accountService.getAccountsByIds(ids);
    }

    @GetMapping("/search")
    public Page<Account> searchAccounts(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "field", required = false, defaultValue = "all") String field,
            Pageable pageable) {
        return accountService.searchAccounts(keyword, field, pageable);
    }

    @PostMapping
    public Account createAccount(@Valid @RequestBody Account account) {
        // Kiểm tra CCCD trùng lặp
        if (accountService.existsByCccd(account.getCccd())) {
            throw new IllegalArgumentException("CCCD already exists");
        }

        // Kiểm tra email trùng lặp (nếu có email)
        if (account.getEmail() != null && !account.getEmail().trim().isEmpty()
                && accountService.existsByEmail(account.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Tạo username tự động nếu chưa có
        if (account.getUsername() == null || account.getUsername().trim().isEmpty()) {
            String generatedUsername = usernameGenerator.generateUsername(
                    account.getFirstName(),
                    account.getLastName());
            account.setUsername(generatedUsername);
        }

        // Set password mặc định nếu chưa có
        if (account.getPassword() == null || account.getPassword().trim().isEmpty()) {
            account.setPassword(usernameGenerator.getDefaultPassword());
        }

        account.setPassword(passwordEncoder.encode(account.getPassword()));
        Account savedAccount = accountService.saveAccount(account);
        kafkaProducer.sendAccount("account-created", savedAccount);
        return savedAccount;
    }

    @PutMapping("/{id}")
    public Account updateAccount(@PathVariable int id, @Valid @RequestBody Account account) {
        Optional<Account> existingAccount = accountService.getAccountById(id);
        if (existingAccount.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }

        account.setId(id);

        // Kiểm tra CCCD trùng lặp (trừ chính nó)
        Optional<Account> accountWithSameCccd = accountService.findByCccd(account.getCccd());
        if (accountWithSameCccd.isPresent() && !accountWithSameCccd.get().getId().equals(id)) {
            throw new IllegalArgumentException("CCCD already exists");
        }

        // Kiểm tra email trùng lặp (nếu có email)
        if (account.getEmail() != null && !account.getEmail().trim().isEmpty()) {
            Optional<Account> accountWithSameEmail = accountService.findByEmail(account.getEmail());
            if (accountWithSameEmail.isPresent() && !accountWithSameEmail.get().getId().equals(id)) {
                throw new IllegalArgumentException("Email already exists");
            }
        }

        // Nếu có password mới thì mã hóa, nếu không thì giữ nguyên
        if (account.getPassword() != null && !account.getPassword().trim().isEmpty()) {
            account.setPassword(passwordEncoder.encode(account.getPassword()));
        } else {
            // Giữ nguyên password cũ
            account.setPassword(existingAccount.get().getPassword());
        }

        // Giữ nguyên visible từ account cũ
        account.setVisible(existingAccount.get().getVisible());

        return accountService.saveAccount(account);
    }

    @DeleteMapping("/{id}")
    public String deleteAccount(@PathVariable int id) {
        Optional<Account> account = accountService.getAccountById(id);

        if (account.isPresent()) {
            accountService.deleteAccount(id); // Soft delete
            kafkaProducer.sendAccount("account-deleted", account.get());
            return "Tài khoản đã được xóa thành công!";
        }

        return "Không tìm thấy tài khoản!";
    }
}