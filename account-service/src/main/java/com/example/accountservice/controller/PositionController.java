package com.example.accountservice.controller;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.accountservice.model.Position;
import com.example.accountservice.dto.AssignDTO;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.service.PositionService;
import com.example.accountservice.service.AccountPositionService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@Transactional
@RequestMapping("/account")
public class PositionController {

    @Autowired
    private PositionService positionService;

    @Autowired
    private AccountPositionService accountPositionService;

    @GetMapping("/position")
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/position/{id}")
    public Optional<Position> getPositionById(@PathVariable Long id) {
        return positionService.getPositionById(id);
    }

    @PostMapping("/position")
    public Position createPosition(@Valid @RequestBody Position position) {
        if (positionService.existsByName(position.getName())) {
            throw new IllegalArgumentException("Position name already exists");
        }
        return positionService.savePosition(position);
    }

    @PutMapping("/position/{id}")
    public Position updatePosition(@PathVariable Long id, @Valid @RequestBody Position position) {
        position.setId(id);
        return positionService.savePosition(position);
    }

    @DeleteMapping("/position/{id}")
    public String deletePosition(@PathVariable Long id) {
        positionService.deletePosition(id);
        return "Position deleted successfully";
    }

    // Account Position endpoints
    @GetMapping("/{accountId}/positions")
    public List<AccountPosition> getPositionsByAccount(@PathVariable Long accountId) {
        return accountPositionService.getPositionsByAccount(accountId);
    }

    @GetMapping("/position/{positionId}/accounts")
    public List<AccountPosition> getAccountsByPosition(@PathVariable Long positionId) {
        return accountPositionService.getAccountsByPosition(positionId);
    }

    @PostMapping("/position/assign")
    public AccountPosition assignPosition(@RequestBody AssignDTO request) {
        return accountPositionService.assignPosition(request.getAccountId(), request.getPositionId());
    }

    @DeleteMapping("/position/remove")
    public String removePosition(@RequestBody AssignDTO request) {
        accountPositionService.removePosition(request.getAccountId(), request.getPositionId());
        return "Position removed successfully";
    }
}