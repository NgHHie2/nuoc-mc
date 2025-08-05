package com.example.accountservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.Position;
import com.example.accountservice.repository.AccountPositionRepository;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.service.PositionService;

@Service
public class AccountPositionService {

    @Autowired
    private AccountPositionRepository accountPositionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PositionService positionService;

    public List<AccountPosition> getPositionsByAccount(Long accountId) {
        Optional<Account> account = accountService.getAccountById(accountId);
        if (account.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }
        return accountPositionRepository.findByAccountAndVisible(account.get(), 1);
    }

    public List<AccountPosition> getAccountsByPosition(Long positionId) {
        Optional<Position> position = positionService.getPositionById(positionId);
        if (position.isEmpty()) {
            throw new IllegalArgumentException("Position not found");
        }
        return accountPositionRepository.findByPositionAndVisible(position.get(), 1);
    }

    public AccountPosition assignPosition(Long accountId, Long positionId) {
        Optional<Account> account = accountService.getAccountById(accountId);
        Optional<Position> position = positionService.getPositionById(positionId);

        if (account.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }
        if (position.isEmpty()) {
            throw new IllegalArgumentException("Position not found");
        }

        // Kiểm tra đã tồn tại chưa
        if (accountPositionRepository.existsByAccountAndPositionAndVisible(account.get(), position.get(), 1)) {
            throw new IllegalArgumentException("Account already has this position");
        }

        AccountPosition accountPosition = new AccountPosition();
        accountPosition.setAccount(account.get());
        accountPosition.setPosition(position.get());
        accountPosition.setVisible(1);

        return accountPositionRepository.save(accountPosition);
    }

    public void removePosition(Long accountId, Long positionId) {
        Optional<Account> account = accountService.getAccountById(accountId);
        Optional<Position> position = positionService.getPositionById(positionId);

        if (account.isEmpty() || position.isEmpty()) {
            throw new IllegalArgumentException("Account or Position not found");
        }

        List<AccountPosition> positions = accountPositionRepository.findByAccountAndVisible(account.get(), 1);
        for (AccountPosition ap : positions) {
            if (ap.getPosition().getId().equals(positionId)) {
                ap.setVisible(0);
                accountPositionRepository.save(ap);
                break;
            }
        }
    }
}