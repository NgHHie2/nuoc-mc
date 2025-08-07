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

    public Optional<Position> getPositionById(Long id) {
        return positionRepository.findById(id);
    }

    public Position savePosition(Position position) {
        return positionRepository.save(position);
    }

    public void deletePosition(Long id) {
        positionRepository.deleteById(id);
    }

    public boolean existsByName(String name) {
        return positionRepository.existsByName(name);
    }
}