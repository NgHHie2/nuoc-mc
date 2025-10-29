package com.example.learnservice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Position;
import com.example.learnservice.model.Question;
import com.example.learnservice.model.QuestionPosition;

@Repository
public interface QuestionPositionRepository extends JpaRepository<QuestionPosition, Long> {
    Optional<QuestionPosition> findByQuestionIdAndPositionId(Long questionId, Long positionId);
    
    Page<QuestionPosition> findAllByPositionId(Long positionId, Pageable pageable);
}
