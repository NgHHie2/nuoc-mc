package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Answer;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

}
