package com.example.learnservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Account;
import com.example.learnservice.model.Participation;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Integer>{
    List<Participation> findByAccount(Account account);
}
