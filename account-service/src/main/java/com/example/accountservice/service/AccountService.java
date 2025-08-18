package com.example.accountservice.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.specification.AccountSpecification;
import com.example.accountservice.util.UsernameGenerator;
import com.example.accountservice.util.listener.event.UserDeletedEvent;
import com.example.accountservice.util.listener.event.UserRegisteredEvent;
import com.example.accountservice.util.listener.event.UserUpdatedEvent;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsernameGenerator usernameGenerator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findByIdAndVisible(id, 1);
    }

    public Optional<Account> findByUsernameAndVisible(String username) {
        return accountRepository.findByUsernameAndVisible(username, 1);
    }

    @Transactional
    public Boolean deleteAccount(Long id) {
        // Soft delete: set visible = 0
        Optional<Account> account = accountRepository.findByIdAndVisible(id, 1);
        if (account.isPresent()) {
            Account acc = account.get();
            acc.setVisible(0);
            Account deletedAccount = accountRepository.save(acc);
            log.info("Soft deleted account id: {}", id);
            applicationEventPublisher.publishEvent(new UserDeletedEvent(deletedAccount));
            return true;
        }
        return false;
    }

    @Transactional
    public Account createAccount(Account account) {
        if (existsByCccd(account.getCccd())) {
            throw new IllegalArgumentException("CCCD already exists");
        }

        // Kiểm tra email trùng lặp (nếu có email)
        if (existsByEmail(account.getEmail())) {
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

        Account savedAccount = saveAccount(account);
        applicationEventPublisher.publishEvent(new UserRegisteredEvent(savedAccount));
        return savedAccount;
    }

    @Transactional
    public Account updateAccount(Account account) {
        Optional<Account> existingAccount = getAccountById(account.getId());
        if (existingAccount.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }

        // Kiểm tra CCCD trùng lặp (trừ chính nó)
        Optional<Account> accountWithSameCccd = findByCccd(account.getCccd());
        if (accountWithSameCccd.isPresent() && !accountWithSameCccd.get().getId().equals(account.getId())) {
            throw new IllegalArgumentException("CCCD already exists");
        }

        // Kiểm tra email trùng lặp (nếu có email)
        if (account.getEmail() != null && !account.getEmail().trim().isEmpty()) {
            Optional<Account> accountWithSameEmail = findByEmail(account.getEmail());
            if (accountWithSameEmail.isPresent() && !accountWithSameEmail.get().getId().equals(account.getId())) {
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

        Account savedAccount = saveAccount(account);
        applicationEventPublisher.publishEvent(new UserUpdatedEvent(savedAccount));
        return savedAccount;
    }

    public Account saveAccount(Account account) {
        // Set visible = 1 nếu chưa có giá trị
        if (account.getVisible() == null) {
            account.setVisible(1);
        }

        return accountRepository.save(account);
    }

    public Page<Account> universalSearch(AccountSearchDTO searchDTO, Pageable pageable) {
        // Validate input
        searchDTO.setKeyword(validateKeyword(searchDTO.getKeyword()));
        searchDTO.setPositionIds(cleanPositionIds(searchDTO.getPositionIds()));
        searchDTO.setSearchFields(validateSearchFields(searchDTO.getSearchFields()));

        Specification<Account> spec = AccountSpecification.build(searchDTO);
        return accountRepository.findAll(spec, pageable);
    }

    // ===================== HELPER METHODS =====================

    private String validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String trimmed = keyword.trim();

        return trimmed.toLowerCase();
    }

    /**
     * Clean và validate position IDs
     */
    private List<Long> cleanPositionIds(List<Long> positionIds) {
        if (positionIds == null || positionIds.isEmpty()) {
            return null;
        }

        // Remove null và invalid IDs
        positionIds.removeIf(id -> id == null || id <= 0);

        return positionIds.isEmpty() ? null : positionIds;
    }

    private List<String> validateSearchFields(List<String> searchFields) {
        if (searchFields == null || searchFields.isEmpty()) {
            return null;
        }

        Set<String> allowedFields = Set.of(
                "username", "firstName", "lastName", "fullName",
                "phoneNumber", "email", "cccd");

        return searchFields.stream()
                .filter(field -> field != null && allowedFields.contains(field.trim()))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean existsByCccd(String cccd) {
        return accountRepository.existsByCccdAndVisible(cccd, 1);
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return accountRepository.existsByEmailAndVisible(email, 1);
    }

    public Optional<Account> findByCccd(String cccd) {
        return accountRepository.findByCccdAndVisible(cccd, 1);
    }

    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmailAndVisible(email, 1);
    }
}