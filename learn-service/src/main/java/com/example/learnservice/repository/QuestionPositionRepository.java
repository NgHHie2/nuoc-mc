package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.QuestionPosition;

@Repository
public interface QuestionPositionRepository extends JpaRepository<QuestionPosition, Long> {

}
