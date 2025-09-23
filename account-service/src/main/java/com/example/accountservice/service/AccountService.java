package com.example.accountservice.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.dto.PasswordChangeDTO;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.specification.AccountSpecification;
import com.example.accountservice.util.ValidateUtil;
import com.example.accountservice.util.UsernameGenerator;
import com.example.accountservice.util.listener.event.UserDeletedEvent;
import com.example.accountservice.util.listener.event.UserRegisteredEvent;
import com.example.accountservice.util.listener.event.UserUpdatedEvent;

import jakarta.persistence.EntityNotFoundException;
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

        // Tạo username tự động theo format
        String generatedUsername = usernameGenerator.generateUsername(
                account.getFirstName(),
                account.getLastName());
        account.setUsername(generatedUsername);

        // Set password mặc định "123456Aa@"
        String password = usernameGenerator.getDefaultPassword();
        account.setPassword(passwordEncoder.encode(password));

        Account savedAccount = saveAccount(account);

        // Bắn sự kiện sau khi commit
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

        // Giữ nguyên password cũ
        account.setPassword(existingAccount.get().getPassword());

        // Giữ nguyên visible từ account cũ
        account.setVisible(existingAccount.get().getVisible());

        Account savedAccount = saveAccount(account);
        applicationEventPublisher.publishEvent(new UserUpdatedEvent(savedAccount));
        return savedAccount;
    }

    @Transactional
    public Boolean updatePasswordByAdmin(Long accountId, PasswordChangeDTO passwordChangeDTO) {
        Account account = getAccountById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));

        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        String encodedPassword = passwordEncoder.encode(passwordChangeDTO.getNewPassword());
        account.setPassword(encodedPassword);
        accountRepository.save(account);

        return true;
    }

    @Transactional
    public Boolean updatePasswordByUser(Long accountId, PasswordChangeDTO passwordChangeDTO) {
        Account account = getAccountById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));

        if (!passwordEncoder.matches(passwordChangeDTO.getOldPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Old password is wrong");
        }

        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        String encodedPassword = passwordEncoder.encode(passwordChangeDTO.getNewPassword());
        account.setPassword(encodedPassword);
        accountRepository.save(account);

        return true;
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
        searchDTO.setKeyword(ValidateUtil.validateKeyword(searchDTO.getKeyword()));
        searchDTO.setPositionIds(ValidateUtil.cleanPositionIds(searchDTO.getPositionIds()));
        searchDTO.setSearchFields(ValidateUtil.validateSearchFields(searchDTO.getSearchFields()));

        Sort sort = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "createdAt"));
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Account> spec = AccountSpecification.build(searchDTO);
        return accountRepository.findAll(spec, pageable);
    }

}