package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.SemesterDocument;

@Repository
public interface SemesterDocumentRepository extends JpaRepository<SemesterDocument, Long> {
    boolean existsBySemesterIdAndDocumentId(Long semesterId, Long documentId);
}
