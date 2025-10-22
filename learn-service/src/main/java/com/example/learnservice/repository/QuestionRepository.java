package com.example.learnservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
                SELECT q
                FROM Question q
                JOIN FETCH q.answers a
                WHERE q.id IN :questionIds
            """)
    List<Question> findAllWithAnswers(@Param("questionIds") List<Long> questionIds);
}
