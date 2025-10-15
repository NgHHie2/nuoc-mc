package com.example.learnservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.learnservice.model.Position;
import com.example.learnservice.service.PositionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/document/position")
public class PositionController {
    @Autowired
    private PositionService positionService;

    @GetMapping
    public List<Position> getAllPositions() {
        List<Position> positions = positionService.getAllPositions();
        return positions;
    }

    @GetMapping("/{id}")
    public Position getPositionById(@PathVariable Long id) {
        Position position = positionService.getPositionById(id);
        return position;
    }

}
