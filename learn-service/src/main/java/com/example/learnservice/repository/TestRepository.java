package com.example.learnservice.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Test;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findAllByPositionId(Long positionId);
    @Query("""
                SELECT t
                FROM Test t
                JOIN FETCH t.testQuestions tq
                JOIN FETCH tq.question q
                WHERE t.id = :id
            """)
    Optional<Test> findWithQuestionsById(@Param("id") Long id);

    Page<Test> findAll(Pageable pageable);
}
