package com.example.accountservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Page<Account> getAllAccount(Pageable pageable) {
        return accountRepository.findByVisible(1, pageable);
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findByIdAndVisible(id, 1);
    }

    public Account saveAccount(Account account) {
        // Set visible = 1 nếu chưa có giá trị
        if (account.getVisible() == null) {
            account.setVisible(1);
        }
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        // Soft delete: set visible = 0
        Optional<Account> account = accountRepository.findById(id);
        if (account.isPresent()) {
            Account acc = account.get();
            acc.setVisible(0);
            accountRepository.save(acc);
        }
    }

    public List<Account> getAccountsByIds(List<Long> ids) {
        return accountRepository.findAllByIdInAndVisible(ids, 1);
    }

    // Methods for authentication
    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    public Optional<Account> findByUsernameAndVisible(String username) {
        return accountRepository.findByUsernameAndVisible(username, 1);
    }

    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmailAndVisible(email, 1);
    }

    public Optional<Account> findByCccd(String cccd) {
        return accountRepository.findByCccdAndVisible(cccd, 1);
    }

    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsernameAndVisible(username, 1);
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return accountRepository.existsByEmailAndVisible(email, 1);
    }

    public boolean existsByCccd(String cccd) {
        return accountRepository.existsByCccdAndVisible(cccd, 1);
    }

    // Search functionality
    public Page<Account> searchAccounts(String keyword, String field, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAccount(pageable);
        }

        keyword = "%" + keyword.toLowerCase() + "%";

        switch (field != null ? field.toLowerCase() : "all") {
            case "firstname":
                return accountRepository.findByFirstNameContainingIgnoreCaseAndVisible(keyword.replace("%", ""), 1,
                        pageable);
            case "lastname":
                return accountRepository.findByLastNameContainingIgnoreCaseAndVisible(keyword.replace("%", ""), 1,
                        pageable);
            case "username":
                return accountRepository.findByUsernameContainingIgnoreCaseAndVisible(keyword.replace("%", ""), 1,
                        pageable);
            case "email":
                return accountRepository.findByEmailContainingIgnoreCaseAndVisible(keyword.replace("%", ""), 1,
                        pageable);
            case "cccd":
                return accountRepository.findByCccdContainingAndVisible(keyword.replace("%", ""), 1, pageable);
            case "phone":
            case "phonenumber":
                return accountRepository.findByPhoneNumberContainingAndVisible(keyword.replace("%", ""), 1, pageable);
            default:
                return accountRepository.searchByKeywordAndVisible(keyword, 1, pageable);
        }
    }
}