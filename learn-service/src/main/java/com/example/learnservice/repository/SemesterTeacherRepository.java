package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.SemesterTeacher;

@Repository
public interface SemesterTeacherRepository extends JpaRepository<SemesterTeacher, Long> {
    boolean existsBySemesterIdAndTeacherId(Long semesterId, Long teacherId);

    int deleteByTeacherId(Long teacherId);
}
