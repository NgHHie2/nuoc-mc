package com.example.accountservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@EnableAsync
public class AccountserviceApplication implements CommandLineRunner {

	@Autowired
	private AccountService accountService;

	public static void main(String[] args) {
		SpringApplication.run(AccountserviceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (!accountService.existsByCccd("123456789012")) {
			Account admin = new Account();
			admin.setFirstName("Hiệp");
			admin.setLastName("Nguyễn Hoàng");
			admin.setCccd("123456789012");
			admin.setRole(Role.ADMIN);

			Account saved = accountService.createAccount(admin);
			log.info("Admin created: {} - password: 123456Aa@", saved.getUsername());
		}
	}
}