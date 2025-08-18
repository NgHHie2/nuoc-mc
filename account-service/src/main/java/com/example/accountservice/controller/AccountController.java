package com.example.accountservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.annotation.RequireRole;
import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/search")
    @RequireRole({ Role.ADMIN })
    public Page<Account> searchAccounts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "positionIds", required = false) List<Long> positionIds,
            @RequestParam(value = "searchFields", required = false) List<String> searchFields,
            Pageable pageable) {

        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setRole(role);
        searchDTO.setPositionIds(positionIds);
        searchDTO.setSearchFields(searchFields);

        return accountService.universalSearch(searchDTO, pageable);
    }

    @DeleteMapping("/{id}")
    @RequireRole({ Role.ADMIN })
    public String deleteAccount(@PathVariable Long id) {
        Boolean success = accountService.deleteAccount(id);
        return success ? "Tài khoản đã được xóa thành công!" : "Không tìm thấy tài khoản!";
    }

    @GetMapping("/{id}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public Optional<Account> getAccountById(@PathVariable Long id) {
        log.info("get user by id: " + id);
        return accountService.getAccountById(id);
    }

    @GetMapping("/me")
    public Optional<Account> getAccountByMe(@RequestHeader(value = "X-User-Id", required = false) String userIdString) {
        try {
            Long accountId = Long.valueOf(userIdString);
            log.info("get user by id: " + accountId);
            return accountService.getAccountById(accountId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @PostMapping
    @RequireRole({ Role.ADMIN })
    public Account createAccount(@Valid @RequestBody Account account) {
        Account savedAccount = accountService.createAccount(account);
        return savedAccount;
    }

    @PutMapping("/{id}")
    @RequireRole({ Role.ADMIN })
    public Account updateAccount(@PathVariable Long id, @Valid @RequestBody Account account) {
        account.setId((id));
        return accountService.updateAccount(account);
    }
}