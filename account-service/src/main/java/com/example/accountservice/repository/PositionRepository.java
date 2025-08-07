package com.example.accountservice.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.Position;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    boolean existsByName(String name);
}