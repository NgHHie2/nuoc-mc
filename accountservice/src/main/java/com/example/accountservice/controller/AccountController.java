package com.example.accountservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.kafka.KafkaProducer;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;

import jakarta.transaction.Transactional;

@RestController
@Transactional
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private KafkaProducer kafkaProducer;

    @GetMapping
    public List<Account> getAllAccount() {
        return accountService.getAllAccount();
    }

    @GetMapping("/{id}")
    public Optional<Account> getAccountById(@PathVariable int id) {
        return accountService.getAccountById(id);
    }

    @PostMapping("/ids")
    public List<Account> getAccountsByIds(@RequestBody List<Integer> ids) {
        return accountService.getAccountsByIds(ids);
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        Account savedAccount = accountService.saveAccount(account);
        kafkaProducer.sendAccount("account-created", savedAccount);
        return accountService.saveAccount(account);
    }

    @PutMapping("/{id}")
    public Account updateAccount(@PathVariable int id, @RequestBody Account account) {
        account.setId(id);
        return accountService.saveAccount(account);
    }

    @DeleteMapping("/{id}")
    public String deleteAccount(@PathVariable int id) {
        Optional<Account> account = accountService.getAccountById(id);
        
        if (account.isPresent()) {
            accountService.deleteAccount(id);
            kafkaProducer.sendAccount("account-deleted", account.get());
            return "Tài khoản đã được xóa thành công!";
        } 
        
        return "Không tìm thấy tài khoản!";
        
    }
}