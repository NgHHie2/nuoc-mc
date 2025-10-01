package com.example.learnservice.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.example.learnservice.dto.SemesterSearchDTO;
import com.example.learnservice.model.Semester;
import com.example.learnservice.model.SemesterAccount;
import com.example.learnservice.model.SemesterTeacher;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class SemesterSpecification {

    public static Specification<Semester> build(SemesterSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by keyword
            if (StringUtils.hasText(searchDTO.getKeyword())) {
                String keyword = "%" + searchDTO.getKeyword().toLowerCase() + "%";

                if (searchDTO.getSearchFields() != null && !searchDTO.getSearchFields().isEmpty()) {
                    // Tìm kiếm theo các trường được chỉ định
                    List<Predicate> fieldPredicates = new ArrayList<>();

                    for (String field : searchDTO.getSearchFields()) {
                        switch (field.toLowerCase()) {
                            case "name":
                                fieldPredicates.add(criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("name")), keyword));
                                break;
                            // Có thể thêm các trường khác nếu cần
                        }
                    }

                    if (!fieldPredicates.isEmpty()) {
                        predicates.add(criteriaBuilder.or(fieldPredicates.toArray(new Predicate[0])));
                    }
                } else {
                    // Tìm kiếm mặc định trong tên
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")), keyword));
                }
            }

            // Filter by start year
            if (searchDTO.getStartYear() != null) {
                LocalDateTime startOfYear = LocalDateTime.of(searchDTO.getStartYear(), 1, 1, 0, 0, 0);
                LocalDateTime endOfYear = LocalDateTime.of(searchDTO.getStartYear(), 12, 31, 23, 59, 59);

                predicates.add(criteriaBuilder.and(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), startOfYear),
                        criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), endOfYear)));
            }

            // Filter by end year
            if (searchDTO.getEndYear() != null) {
                LocalDateTime startOfYear = LocalDateTime.of(searchDTO.getEndYear(), 1, 1, 0, 0, 0);
                LocalDateTime endOfYear = LocalDateTime.of(searchDTO.getEndYear(), 12, 31, 23, 59, 59);

                predicates.add(criteriaBuilder.and(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), startOfYear),
                        criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), endOfYear)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification để filter semester có student cụ thể
     */
    public static Specification<Semester> hasStudent(Long studentId) {
        return (root, query, cb) -> {
            Join<Semester, SemesterAccount> accountJoin = root.join("semesterAccounts");
            return cb.equal(accountJoin.get("accountId"), studentId);
        };
    }

    /**
     * Specification để filter semester có teacher cụ thể
     */
    public static Specification<Semester> hasTeacher(Long teacherId) {
        return (root, query, cb) -> {
            Join<Semester, SemesterTeacher> teacherJoin = root.join("semesterTeachers");
            return cb.equal(teacherJoin.get("teacherId"), teacherId);
        };
    }
}