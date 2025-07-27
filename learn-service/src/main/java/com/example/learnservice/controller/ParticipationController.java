package com.example.learnservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.learnservice.client.AccountClient;
import com.example.learnservice.dto.ParticipationDTO;
import com.example.learnservice.model.Account;
import com.example.learnservice.model.Participation;
import com.example.learnservice.service.ParticipationService;

@RestController
@RequestMapping("/participation")
public class ParticipationController {
    @Autowired
    private ParticipationService participationService;

    @Autowired
    private AccountClient accountClient;

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getParticipationByAccount(@PathVariable int accountId) {
        Account account = Account.builder().id(accountId).build();

        boolean exists = accountClient.checkAccountExists(account);
        if (!exists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account không tồn tại");
        }

        List<ParticipationDTO> participations = participationService.getParticipationByAccount(account);
        return ResponseEntity.ok(participations);
    }

}
