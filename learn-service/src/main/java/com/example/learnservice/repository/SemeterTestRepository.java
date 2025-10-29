package com.example.learnservice.repository;

import java.util.List;
import java.util.Optional;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.enums.TestType;
import com.example.learnservice.model.SemesterTest;

@Repository
public interface SemeterTestRepository extends JpaRepository<SemesterTest, Long> {
    SemesterTest findByResultsId(Long resultId);

    List<SemesterTest> findAllBySemesterIdAndType(Long semesterId, TestType type);

    List<SemesterTest> findAllBySemesterId(Long semesterId);

    Optional<SemesterTest> findBySemesterIdAndTestId(Long semesterId, Long testId);

    List<SemesterTest> findAllByTestId(Long testId);
}
