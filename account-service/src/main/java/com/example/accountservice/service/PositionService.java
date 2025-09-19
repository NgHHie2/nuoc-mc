package com.example.accountservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.accountservice.model.Position;
import com.example.accountservice.repository.PositionRepository;

@Service
public class PositionService {

    @Autowired
    private PositionRepository positionRepository;

    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }

    public List<Position> getPositionsByIds(List<Long> ids) {
        return positionRepository.findByIdIn(ids);
    }

}