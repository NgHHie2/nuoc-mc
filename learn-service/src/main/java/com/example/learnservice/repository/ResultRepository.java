package com.example.learnservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.learnservice.dto.ResultStatusProjection;
import com.example.learnservice.model.Result;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

        /**
         * Lấy danh sách results của một user trong một semester test
         */
        @Query("SELECT r.id, r.studentId FROM Result r WHERE r.semesterTest.id = :semesterTestId AND r.studentId = :userId")
        List<Result> findBySemesterTestIdAndStudentId(@Param("semesterTestId") Long semesterTestId,
                        @Param("userId") Long userId);

        /**
         * Kiểm tra user đã từng làm một semester test nào đó chưa
         */
        boolean existsBySemesterTestIdAndStudentId(Long semesterTestId, Long userId);

        /**
         * Kiểm tra quyền truy cập - chỉ lấy studentId
         */
        @Query("SELECT r.studentId FROM Result r WHERE r.id = :resultId")
        Optional<Long> findStudentIdById(@Param("resultId") Long resultId);

        /**
         * Kiểm tra đã submit chưa
         */
        @Query("SELECT CASE WHEN r.submitDateTime IS NOT NULL THEN true ELSE false END FROM Result r WHERE r.id = :resultId")
        Optional<Boolean> isSubmitted(@Param("resultId") Long resultId);

        // ==================== JSONB OPERATORS - PostgreSQL ====================

        /**
         * Lấy 1 câu hỏi cụ thể từ detailTest bằng JSONB operator
         * detailTest->'questions'->:index
         */
        @Query(value = "SELECT " +
                        "detail_test->'questions'->:questionIndex as question, " +
                        "student_answers->:questionIndexStr as student_answer " +
                        "FROM result WHERE id = :resultId", nativeQuery = true)
        Optional<Object[]> findQuestionByIndex(@Param("resultId") Long resultId,
                        @Param("questionIndex") Integer questionIndex,
                        @Param("questionIndexStr") String questionIndexStr);

        /**
         * Lấy studentAnswer của 1 câu hỏi cụ thể
         * student_answers->'0'
         */
        @Query(value = "SELECT student_answers->:questionIndexStr FROM result WHERE id = :resultId", nativeQuery = true)
        Optional<String> findStudentAnswerByIndex(@Param("resultId") Long resultId,
                        @Param("questionIndexStr") String questionIndexStr);

        /**
         * Lấy trueAnswer của 1 câu hỏi cụ thể
         * true_answers->'0'
         */
        @Query(value = "SELECT true_answers->:questionIndexStr FROM result WHERE id = :resultId", nativeQuery = true)
        Optional<String> findTrueAnswerByIndex(@Param("resultId") Long resultId,
                        @Param("questionIndexStr") String questionIndexStr);

        /**
         * Update selectedAnswers của 1 câu hỏi cụ thể bằng jsonb_set
         * jsonb_set(student_answers, '{0,selectedAnswers}', '[1,2]')
         */
        @Modifying
        @Query(value = "UPDATE result SET student_answers = " +
                        "jsonb_set(student_answers, CAST(:path AS text[]), CAST(:value AS jsonb)) " +
                        "WHERE id = :resultId", nativeQuery = true)
        int updateStudentAnswerPath(@Param("resultId") Long resultId,
                        @Param("path") String path,
                        @Param("value") String value);

        /**
         * Update flagged của 1 câu hỏi cụ thể
         * jsonb_set(student_answers, '{0,flagged}', 'true')
         */
        @Modifying
        @Query(value = "UPDATE result SET student_answers = " +
                        "jsonb_set(student_answers, CAST(:path AS text[]), CAST(:value AS jsonb)) " +
                        "WHERE id = :resultId", nativeQuery = true)
        int updateFlaggedPath(@Param("resultId") Long resultId,
                        @Param("path") String path,
                        @Param("value") String value);

        /**
         * Đếm số câu đã trả lời (có selectedAnswers.length > 0)
         */
        @Query(value = "SELECT COUNT(*) FROM jsonb_each(student_answers) " +
                        "WHERE jsonb_array_length(value->'selectedAnswers') > 0 " +
                        "AND (SELECT id FROM result WHERE id = :resultId) IS NOT NULL", nativeQuery = true)
        Integer countAnsweredQuestions(@Param("resultId") Long resultId);

        /**
         * Đếm số câu đã flag
         */
        @Query(value = "SELECT COUNT(*) FROM jsonb_each(student_answers) " +
                        "WHERE (value->>'flagged')::boolean = true " +
                        "AND (SELECT id FROM result WHERE id = :resultId) IS NOT NULL", nativeQuery = true)
        Integer countFlaggedQuestions(@Param("resultId") Long resultId);

        /*
         * Check result status
         */
        Optional<ResultStatusProjection> findFirstBySemesterTestIdAndStudentIdOrderByCreatedAtDesc(
                        Long semesterTestId,
                        Long studentId);

        /*
         * Lấy danh sách student đang có result
         */
        // @Query("SELECT DISTINCT r.studentId FROM Result r WHERE r.semesterTest.id =
        // :semesterTestId")
        List<ResultStatusProjection> findAllBySemesterTestId(Long semesterTestId);
}