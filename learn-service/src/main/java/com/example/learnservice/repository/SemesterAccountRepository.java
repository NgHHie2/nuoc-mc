package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.SemesterAccount;

@Repository
public interface SemesterAccountRepository extends JpaRepository<SemesterAccount, Long> {

        @Query(value = "SELECT COUNT(*) > 0 FROM semester_account sa " +
                        "JOIN position p ON sa.position_id = p.id " +
                        "JOIN catalog c ON c.position_id = p.id " +
                        "JOIN document d ON c.document_id = d.id " +
                        "JOIN semester_document sd ON sd.document_id = d.id AND sd.semester_id = sa.semester_id " +
                        "WHERE sa.semester_id = :semesterId " +
                        "AND sa.account_id = :accountId " +
                        "AND d.code = :documentCode", nativeQuery = true)
        boolean existsAccessThroughPosition(
                        @Param("semesterId") Long semesterId,
                        @Param("accountId") Long accountId,
                        @Param("documentCode") String documentCode);
}
