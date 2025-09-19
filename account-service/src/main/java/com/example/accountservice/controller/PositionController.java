package com.example.accountservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.model.Position;
import com.example.accountservice.service.PositionService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Transactional
@RequestMapping("/account")
public class PositionController {

    @Autowired
    private PositionService positionService;

    @GetMapping("/position")
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/positions")
    public List<Position> getPositionsByIds(@RequestParam List<Long> id) {
        return positionService.getPositionsByIds(id);
    }

}