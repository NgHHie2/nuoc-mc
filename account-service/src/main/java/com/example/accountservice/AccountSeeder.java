package com.example.accountservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;

@Order(2)
@Component
@Slf4j
@RequiredArgsConstructor
public class AccountSeeder implements CommandLineRunner {

    private final AccountService accountService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("second");
        seedAccounts(30);
    }

    public void seedAccounts(int count) {
        Role[] roles = { Role.ADMIN, Role.TEACHER, Role.STUDENT };
        for (int i = 0; i < count; i++) {
            String cccd = String.format("123456789%03d", i);
            if (accountService.existsByCccd(cccd)) {
                log.info("CCCD {} đã tồn tại, bỏ qua.", cccd);
                continue;
            }
            Account acc = new Account();
            acc.setFirstName("FirstName" + i);
            acc.setLastName("LastName" + i);
            acc.setCccd(cccd);
            acc.setRole(roles[i % roles.length]);

            accountService.createAccount(acc);
            log.info("Tạo account: cccd={} role={} (password mặc định)", cccd, acc.getRole());
        }
    }
}