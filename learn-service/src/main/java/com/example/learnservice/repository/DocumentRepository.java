package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.learnservice.model.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

}
