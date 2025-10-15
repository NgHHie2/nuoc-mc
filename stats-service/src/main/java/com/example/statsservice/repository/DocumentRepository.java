package com.example.statsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.statsservice.model.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
}