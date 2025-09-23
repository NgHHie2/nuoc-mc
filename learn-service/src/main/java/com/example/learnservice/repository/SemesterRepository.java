package com.example.learnservice.repository;

import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Semester;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

}
