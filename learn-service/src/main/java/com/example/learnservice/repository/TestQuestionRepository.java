package com.example.learnservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.TestQuestion;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {
    Optional<TestQuestion> findByTestIdAndQuestionId(Long testId, Long questionId);

    List<TestQuestion> findAllByTestId(Long testId);
}
