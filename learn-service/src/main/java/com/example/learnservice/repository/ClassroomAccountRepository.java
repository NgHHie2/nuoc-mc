package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.ClassroomAccount;

@Repository
public interface ClassroomAccountRepository extends JpaRepository<ClassroomAccount, Long> {
    boolean existsByClassroomIdAndAccountId(Long classroomId, Long accountId);
}
