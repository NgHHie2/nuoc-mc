package com.example.learnservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.example.learnservice.model.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    Optional<Document> findByCode(String code);

    List<Document> findAllByCodeIn(List<String> codes);

    Optional<Document> findByDocumentNumber(String documentNumber);
}
