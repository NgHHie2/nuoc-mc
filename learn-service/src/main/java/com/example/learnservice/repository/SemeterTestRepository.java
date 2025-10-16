package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.SemesterTest;

@Repository
public interface SemeterTestRepository extends JpaRepository<SemesterTest, Long> {

}
