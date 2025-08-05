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
        return positionRepository.findByVisible(1);
    }

    public Optional<Position> getPositionById(Long id) {
        return positionRepository.findByIdAndVisible(id, 1);
    }

    public Position savePosition(Position position) {
        if (position.getVisible() == null) {
            position.setVisible(1);
        }
        return positionRepository.save(position);
    }

    public void deletePosition(Long id) {
        Optional<Position> position = positionRepository.findById(id);
        if (position.isPresent()) {
            Position pos = position.get();
            pos.setVisible(0);
            positionRepository.save(pos);
        }
    }

    public boolean existsByName(String name) {
        return positionRepository.existsByNameAndVisible(name, 1);
    }
}