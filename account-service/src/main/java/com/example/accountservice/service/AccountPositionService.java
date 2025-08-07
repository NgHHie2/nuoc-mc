package com.example.accountservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.Position;
import com.example.accountservice.repository.AccountPositionRepository;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.repository.PositionRepository;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.service.PositionService;

@Service
public class AccountPositionService {

    @Autowired
    private AccountPositionRepository accountPositionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PositionRepository positionRepository;

    public List<AccountPosition> getPositionsByAccount(Long accountId) {
        Optional<Account> account = accountRepository.findById(accountId);
        if (account.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }
        return accountPositionRepository.findByAccount(account.get());
    }

    public List<AccountPosition> getAccountsByPosition(Long positionId) {
        Optional<Position> position = positionRepository.findById(positionId);
        if (position.isEmpty()) {
            throw new IllegalArgumentException("Position not found");
        }
        return accountPositionRepository.findByPosition(position.get());
    }

    public AccountPosition assignPosition(Long accountId, Long positionId) {
        Optional<Account> account = accountRepository.findById(accountId);
        Optional<Position> position = positionRepository.findById(positionId);

        if (account.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }
        if (position.isEmpty()) {
            throw new IllegalArgumentException("Position not found");
        }

        // Kiểm tra đã tồn tại chưa
        if (accountPositionRepository.existsByAccountAndPosition(account.get(), position.get())) {
            throw new IllegalArgumentException("Account already has this position");
        }

        AccountPosition accountPosition = new AccountPosition();
        accountPosition.setAccount(account.get());
        accountPosition.setPosition(position.get());

        return accountPositionRepository.save(accountPosition);
    }

    public void removePosition(Long accountId, Long positionId) {
        Optional<Account> account = accountRepository.findById(accountId);
        Optional<Position> position = positionRepository.findById(positionId);

        if (account.isEmpty() || position.isEmpty()) {
            throw new IllegalArgumentException("Account or Position not found");
        }

        accountPositionRepository.deleteByAccountAndPosition(account.get(), position.get());
    }
}