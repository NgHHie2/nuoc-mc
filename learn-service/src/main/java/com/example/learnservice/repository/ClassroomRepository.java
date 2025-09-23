package com.example.learnservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.Classroom;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findBySemesterId(Long semesterId);

    @Query("SELECT COUNT(c) > 0 FROM Classroom c " +
            "JOIN c.classroomAccounts ca " +
            "JOIN c.classroomDocuments cd " +
            "WHERE ca.accountId = :accountId AND cd.document.code = :documentCode")
    boolean existsByAccountIdAndDocumentCode(@Param("accountId") Long accountId,
            @Param("documentCode") String documentCode);
}