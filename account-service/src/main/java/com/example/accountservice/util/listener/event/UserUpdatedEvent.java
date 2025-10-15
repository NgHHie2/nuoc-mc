package com.example.accountservice.util.listener.event;

import com.example.accountservice.dto.AccountDTO;
import com.example.accountservice.model.Account;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserUpdatedEvent {
    private AccountDTO account;

    public UserUpdatedEvent(Account acc) {
        this.account = new AccountDTO();
        this.account.setId(acc.getId());
        this.account.setUsername(acc.getUsername());
        this.account.setRole(acc.getRole());
    }
}