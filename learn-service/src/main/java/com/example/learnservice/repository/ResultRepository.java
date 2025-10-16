package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Result;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

}
