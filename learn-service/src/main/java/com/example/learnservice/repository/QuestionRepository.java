package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

}
