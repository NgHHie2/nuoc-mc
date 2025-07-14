package com.example.accountservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public List<Account> getAllAccount() {
        return accountRepository.findAll();
    }

    public Optional<Account> getAccountById(int id) {
        return accountRepository.findById(id);
    }

    public Account saveAccount(Account Account) {
        return accountRepository.save(Account);
    } 

    public void deleteAccount(int id) {
        accountRepository.deleteById(id);
    }

    public List<Account> getAccountsByIds(List<Integer> ids) {
        return accountRepository.findAllById(ids);
    }
}