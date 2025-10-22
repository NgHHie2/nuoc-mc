package com.example.learnservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.SemesterTest;

@Repository
public interface SemeterTestRepository extends JpaRepository<SemesterTest, Long> {
}
