package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Position;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

}
