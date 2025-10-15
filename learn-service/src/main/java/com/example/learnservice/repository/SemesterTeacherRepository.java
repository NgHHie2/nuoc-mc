package com.example.learnservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.model.SemesterTeacher;

@Repository
public interface SemesterTeacherRepository extends JpaRepository<SemesterTeacher, Long> {
    boolean existsBySemesterIdAndTeacherId(Long semesterId, Long teacherId);

    @Modifying
    @Query("DELETE FROM SemesterTeacher st WHERE st.semester.id = :semesterId AND st.teacherId = :teacherId")
    int deleteBySemesterIdAndTeacherId(@Param("semesterId") Long semesterId,
            @Param("teacherId") Long teacherId);

    int deleteByTeacherId(Long teacherId);
}
