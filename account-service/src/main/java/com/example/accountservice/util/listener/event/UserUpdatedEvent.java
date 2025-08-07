package com.example.accountservice.util.listener.event;

import com.example.accountservice.model.Account;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserUpdatedEvent {
    private Account account;
}