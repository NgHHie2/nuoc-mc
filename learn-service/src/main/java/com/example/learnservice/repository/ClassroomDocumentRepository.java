package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.ClassroomDocument;

@Repository
public interface ClassroomDocumentRepository extends JpaRepository<ClassroomDocument, Long> {

}
