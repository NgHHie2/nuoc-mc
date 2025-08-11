package com.example.accountservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.atn.SemanticContext.AND;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

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

}