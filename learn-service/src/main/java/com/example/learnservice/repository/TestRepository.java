package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Test;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

}
