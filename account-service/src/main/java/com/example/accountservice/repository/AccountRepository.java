package com.example.accountservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

        // Basic queries with visible filter
        Page<Account> findByVisible(Integer visible, Pageable pageable);

        Optional<Account> findByIdAndVisible(Long id, Integer visible);

        List<Account> findAllByIdInAndVisible(List<Long> ids, Integer visible);

        // Username queries
        Optional<Account> findByUsername(String username);

        Optional<Account> findByUsernameAndVisible(String username, Integer visible);

        boolean existsByUsernameAndVisible(String username, Integer visible);

        // Email queries
        Optional<Account> findByEmailAndVisible(String email, Integer visible);

        boolean existsByEmailAndVisible(String email, Integer visible);

        // CCCD queries
        Optional<Account> findByCccdAndVisible(String cccd, Integer visible);

        boolean existsByCccdAndVisible(String cccd, Integer visible);

        // Search queries
        @Query("SELECT DISTINCT a FROM Account a " +
                        "LEFT JOIN a.accountPositions ap " +
                        "WHERE a.visible = :visible " +

                        // Keyword search (optional) - with explicit CAST for PostgreSQL
                        "AND (:keyword IS NULL OR " +
                        "    LOWER(CAST(a.username AS string)) LIKE LOWER(:keyword) OR " +
                        "    LOWER(CAST(a.firstName AS string)) LIKE LOWER(:keyword) OR " +
                        "    LOWER(CAST(a.lastName AS string)) LIKE LOWER(:keyword) OR " +
                        "    LOWER(CAST(CONCAT(a.firstName, ' ', a.lastName) AS string)) LIKE LOWER(:keyword) OR " +
                        "    LOWER(CAST(CONCAT(a.lastName, ' ', a.firstName) AS string)) LIKE LOWER(:keyword) OR " +
                        "    CAST(a.phoneNumber AS string) LIKE :keyword OR " +
                        "    LOWER(CAST(a.email AS string)) LIKE LOWER(:keyword) OR " +
                        "    CAST(a.cccd AS string) LIKE :keyword) " +

                        // Role filter (optional)
                        "AND (:role IS NULL OR a.role = :role) " +

                        // Position filter (optional) - removed visible checks
                        "AND (:#{#positionIds == null or #positionIds.isEmpty()} = true OR " +
                        "    ap.position.id IN :positionIds) " +

                        // Birthday range filter (optional)
                        "AND (:birthdayFrom IS NULL OR a.birthDay >= :birthdayFrom) " +
                        "AND (:birthdayTo IS NULL OR a.birthDay <= :birthdayTo)")
        Page<Account> universalSearch(
                        @Param("keyword") String keyword,
                        @Param("role") Role role,
                        @Param("positionIds") List<Long> positionIds,
                        @Param("birthdayFrom") LocalDateTime birthdayFrom,
                        @Param("birthdayTo") LocalDateTime birthdayTo,
                        @Param("visible") Integer visible,
                        Pageable pageable);
}