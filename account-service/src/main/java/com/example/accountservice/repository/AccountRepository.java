package com.example.accountservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.atn.SemanticContext.AND;
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
    @Query("""
            SELECT DISTINCT a FROM Account a
            LEFT JOIN a.accountPositions ap
            WHERE a.visible = 1
            AND (:role IS NULL OR a.role = :role)
            AND (:positionIds IS NULL OR ap.position.id IN :positionIds)
            """)
    Page<Account> search(
            @Param("role") Role role,
            @Param("positionIds") List<Long> positionIds,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT a FROM Account a
            LEFT JOIN a.accountPositions ap
            WHERE a.visible = 1
            AND (
                :keyword IS NULL OR
                LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(a.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(a.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(CONCAT(a.firstName, ' ', a.lastName)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(CONCAT(a.lastName, ' ', a.firstName)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                (a.phoneNumber IS NOT NULL AND a.phoneNumber LIKE CONCAT('%', :keyword, '%')) OR
                (a.email IS NOT NULL AND LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR
                (a.cccd IS NOT NULL AND a.cccd LIKE CONCAT('%', :keyword, '%'))
            )
            AND (:role IS NULL OR a.role = :role)
            AND (:positionIds IS NULL OR ap.position.id IN :positionIds)
            """)
    Page<Account> universalSearch(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("positionIds") List<Long> positionIds,
            Pageable pageable);

}